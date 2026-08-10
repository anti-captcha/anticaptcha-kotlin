package com.anticaptcha

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Base64
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Talks to the [Anti-Captcha](https://anti-captcha.com) API.
 *
 * The client holds no per-task state, so one instance can solve any number of
 * captchas concurrently.
 *
 * ```kotlin
 * val ac = AnticaptchaClient("API_KEY_HERE")
 *
 * val solution = ac.solveRecaptchaV2(
 *     RecaptchaV2(
 *         websiteUrl = "https://www.website.com/",
 *         websiteKey = "SITE_KEY",
 *     )
 * )
 *
 * println(solution.gRecaptchaResponse)
 * ```
 *
 * @param apiKey your key from [the settings page](https://anti-captcha.com/clients/settings/apisetup)
 * @param softId specify it to earn 10% commission with your app, see
 *   [devcenter](https://anti-captcha.com/clients/tools/devcenter)
 * @param verbose debug output to stderr, on by default
 */
public class AnticaptchaClient(
    public var apiKey: String,
    public var softId: Int = 0,
    public var verbose: Boolean = true,
    /** Timeout of a single HTTP call to the API. */
    public var connectionTimeout: Duration = Duration.ofSeconds(30),
    /** How long to wait before the first status request. */
    public var firstAttemptWaitingInterval: kotlin.time.Duration = 5.seconds,
    /** How long to wait between the following status requests. */
    public var normalWaitingInterval: kotlin.time.Duration = 5.seconds,
    /** How long to keep polling before giving up. */
    public var maxWaitingTime: kotlin.time.Duration = 300.seconds,
    private val http: HttpClient = defaultHttpClient(),
) {

    // ------------------------------------------------------------------- money

    /** Account balance in US dollars. */
    public suspend fun getBalance(): Double =
        jsonRequest("getBalance")["balance"]?.jsonPrimitive?.double ?: 0.0

    /** Number of prepaid captcha credits on the account. */
    public suspend fun getCreditsBalance(): Double =
        jsonRequest("getBalance")["captchaCredits"]?.jsonPrimitive?.double ?: 0.0

    // --------------------------------------------------------------- low level

    /** Calls any API method with your own payload. The client key is added for you. */
    public suspend fun jsonRequest(method: String, payload: JsonObject = JsonObject(emptyMap())): JsonObject {
        if (apiKey.isEmpty()) throw InvalidTaskException("API key is not set")

        val body = buildJsonObject {
            payload.forEach { (key, value) -> put(key, value) }
            put("clientKey", apiKey)
        }

        val url = API_HOST + method
        log("POST $url")

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(connectionTimeout)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Accept", "application/json")
            .header("User-Agent", "anticaptcha-kotlin")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
            .build()

        val response = try {
            withContext(Dispatchers.IO) {
                http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            }
        } catch (e: IOException) {
            throw NetworkException("request to $url failed: ${e.message}", e)
        }

        val raw = response.body().orEmpty()

        val parsed = try {
            JSON.parseToJsonElement(raw) as? JsonObject
                ?: throw BadResponseException("the API response is not a JSON object", raw.take(500))
        } catch (e: SerializationLikeException) {
            throw BadResponseException("could not parse the API response as JSON: ${e.message}", raw.take(500))
        }

        if (response.statusCode() !in 200..299) {
            throw BadResponseException("API answered with HTTP ${response.statusCode()}", raw.take(500))
        }

        val errorId = parsed["errorId"]?.jsonPrimitive?.int
            ?: throw BadResponseException("the API response carries no errorId", raw.take(500))

        if (errorId != 0) {
            throw ApiException(
                errorId = errorId,
                errorCode = parsed["errorCode"]?.jsonPrimitive?.content.orEmpty(),
                description = parsed["errorDescription"]?.jsonPrimitive?.content
                    ?: "(no error description)",
            )
        }

        return parsed
    }

    /** Submits a task and returns its id. */
    public suspend fun createTask(task: JsonObject): Long {
        log("Creating task: $task")

        val payload = buildJsonObject {
            put("task", task)
            put("softId", softId)
        }

        val response = jsonRequest("createTask", payload)
        val taskId = response["taskId"]?.jsonPrimitive?.long
            ?: throw BadResponseException("API did not return a task id", response.toString().take(500))

        log("Task id: $taskId")

        return taskId
    }

    /** Polls until the task is solved or the waiting limit runs out. */
    public suspend fun waitForResult(taskId: Long): Solution {
        val started = TimeSource.Monotonic.markNow()
        var interval = firstAttemptWaitingInterval

        while (true) {
            log("Waiting ${interval.inWholeSeconds} seconds...")
            delay(interval)
            interval = normalWaitingInterval

            val response = jsonRequest("getTaskResult", buildJsonObject { put("taskId", taskId) })

            when (val status = response["status"]?.jsonPrimitive?.content) {
                "ready" -> {
                    val cost = response["cost"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    val body = response["solution"] as? JsonObject
                        ?: throw BadResponseException(
                            "the task is ready but carries no solution",
                            response.toString().take(500),
                        )

                    val solution = Solution(body, taskId, cost)

                    if (solution.isEmpty) {
                        throw BadResponseException(
                            "the API returned an empty solution",
                            response.toString().take(500),
                        )
                    }

                    log("The task is complete")

                    return solution
                }

                "processing" -> {
                    if (started.elapsedNow() >= maxWaitingTime) {
                        throw TaskTimeoutException(maxWaitingTime.inWholeSeconds)
                    }

                    log("The task is still processing...")
                }

                else -> throw BadResponseException(
                    "unknown task status \"$status\", please update the library",
                    response.toString().take(500),
                )
            }
        }
    }

    /**
     * Sends a task type this library does not wrap yet, and waits for the result.
     * See [the task types](https://anti-captcha.com/apidoc/task-types) for the shape.
     */
    public suspend fun solveTask(task: JsonObject): Solution = waitForResult(createTask(task))

    // ------------------------------------------------------------------- tasks

    public suspend fun solveImage(base64Body: String, settings: ImageSettings = ImageSettings()): Solution =
        solveTask(Tasks.image(base64Body, settings))

    public suspend fun solveImageFile(path: Path, settings: ImageSettings = ImageSettings()): Solution =
        solveImage(fileToBase64(path), settings)

    public suspend fun solveImageFile(path: String, settings: ImageSettings = ImageSettings()): Solution =
        solveImageFile(Path.of(path), settings)

    public suspend fun solveImageToCoordinates(
        base64Body: String,
        settings: ImageToCoordinates = ImageToCoordinates(),
    ): Solution = solveTask(Tasks.imageToCoordinates(base64Body, settings))

    public suspend fun solveImageToCoordinatesFile(
        path: String,
        settings: ImageToCoordinates = ImageToCoordinates(),
    ): Solution = solveImageToCoordinates(fileToBase64(Path.of(path)), settings)

    public suspend fun solveRecaptchaV2(settings: RecaptchaV2): Solution =
        solveTask(Tasks.recaptchaV2(settings, null))

    public suspend fun solveRecaptchaV2ProxyOn(settings: RecaptchaV2, proxy: Proxy): Solution =
        solveTask(Tasks.recaptchaV2(settings, proxy))

    public suspend fun solveRecaptchaV3(settings: RecaptchaV3): Solution =
        solveTask(Tasks.recaptchaV3(settings))

    public suspend fun solveHCaptcha(settings: HCaptcha): Solution =
        solveTask(Tasks.hcaptcha(settings, null))

    public suspend fun solveHCaptchaProxyOn(settings: HCaptcha, proxy: Proxy): Solution =
        solveTask(Tasks.hcaptcha(settings, proxy))

    public suspend fun solveFunCaptcha(settings: FunCaptcha): Solution =
        solveTask(Tasks.funcaptcha(settings, null))

    public suspend fun solveFunCaptchaProxyOn(settings: FunCaptcha, proxy: Proxy): Solution =
        solveTask(Tasks.funcaptcha(settings, proxy))

    public suspend fun solveGeeTest(settings: GeeTest): Solution =
        solveTask(Tasks.geetest(settings, null))

    public suspend fun solveGeeTestProxyOn(settings: GeeTest, proxy: Proxy): Solution =
        solveTask(Tasks.geetest(settings, proxy))

    public suspend fun solveTurnstile(settings: Turnstile): Solution =
        solveTask(Tasks.turnstile(settings, null))

    public suspend fun solveTurnstileProxyOn(settings: Turnstile, proxy: Proxy): Solution =
        solveTask(Tasks.turnstile(settings, proxy))

    public suspend fun solveProsopo(settings: Prosopo): Solution =
        solveTask(Tasks.prosopo(settings, null))

    public suspend fun solveProsopoProxyOn(settings: Prosopo, proxy: Proxy): Solution =
        solveTask(Tasks.prosopo(settings, proxy))

    public suspend fun solveFriendlyCaptcha(settings: FriendlyCaptcha): Solution =
        solveTask(Tasks.friendlyCaptcha(settings, null))

    public suspend fun solveFriendlyCaptchaProxyOn(settings: FriendlyCaptcha, proxy: Proxy): Solution =
        solveTask(Tasks.friendlyCaptcha(settings, proxy))

    public suspend fun solveAltcha(settings: Altcha): Solution =
        solveTask(Tasks.altcha(settings, null))

    public suspend fun solveAltchaProxyOn(settings: Altcha, proxy: Proxy): Solution =
        solveTask(Tasks.altcha(settings, proxy))

    public suspend fun solveAmazon(settings: Amazon): Solution =
        solveTask(Tasks.amazon(settings, null))

    public suspend fun solveAmazonProxyOn(settings: Amazon, proxy: Proxy): Solution =
        solveTask(Tasks.amazon(settings, proxy))

    public suspend fun solveAntiGate(settings: AntiGate): Solution =
        solveTask(Tasks.antigate(settings))

    public suspend fun solveAntiBotCookie(settings: AntiBotCookie): Solution =
        solveTask(Tasks.antibotCookie(settings))

    // ----------------------------------------------------------------- reports

    /** Reports the solved image captcha as answered incorrectly. */
    public suspend fun reportIncorrectImageCaptcha(taskId: Long): Unit =
        report("reportIncorrectImageCaptcha", taskId)

    public suspend fun reportIncorrectRecaptcha(taskId: Long): Unit =
        report("reportIncorrectRecaptcha", taskId)

    public suspend fun reportCorrectRecaptcha(taskId: Long): Unit =
        report("reportCorrectRecaptcha", taskId)

    public suspend fun reportIncorrectHcaptcha(taskId: Long): Unit =
        report("reportIncorrectHcaptcha", taskId)

    private suspend fun report(method: String, taskId: Long) {
        jsonRequest(method, buildJsonObject { put("taskId", taskId) })
    }

    private fun log(message: String) {
        if (verbose) System.err.println("[anticaptcha] $message")
    }

    public companion object {
        private const val API_HOST: String = "https://api.anti-captcha.com/"

        private val JSON = Json { ignoreUnknownKeys = true }

        private fun defaultHttpClient(): HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        /** Reads a file and encodes it in base64. */
        public fun fileToBase64(path: Path): String = try {
            Base64.getEncoder().encodeToString(Files.readAllBytes(path))
        } catch (e: IOException) {
            throw InvalidTaskException("could not read $path: ${e.message}")
        }

        /** Encodes bytes in base64. */
        public fun toBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    }
}

/** Alias so the catch clause reads well; kotlinx.serialization throws this hierarchy. */
private typealias SerializationLikeException = kotlinx.serialization.SerializationException
