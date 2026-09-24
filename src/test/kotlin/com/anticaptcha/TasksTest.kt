package com.anticaptcha

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Offline tests: they check the JSON we send to the API. No network, no API key.
 */
class TasksTest {

    private val proxy = Proxy(
        address = "1.2.3.4",
        port = 1234,
        type = ProxyType.SOCKS5,
        login = "login",
        password = "password",
    )

    private fun json(text: String) = Json.parseToJsonElement(text) as JsonObject

    @Test
    fun `image task carries the flags the API expects`() {
        assertEquals(
            json(
                """{"type":"ImageToTextTask","body":"QUFB","phrase":false,"case":false,
                   "numeric":1,"math":false,"minLength":0,"maxLength":0,
                   "languagePool":"en","comment":"type in green"}""",
            ),
            Tasks.image("QUFB", ImageSettings(numeric = 1, comment = "type in green")),
        )

        assertFailsWith<InvalidTaskException> { Tasks.image("", ImageSettings()) }
    }

    @Test
    fun `image to coordinates defaults to points`() {
        val task = Tasks.imageToCoordinates("QUFB", ImageToCoordinates())

        assertEquals("ImageToCoordinatesTask", task["type"]?.jsonPrimitive?.content)
        assertEquals("points", task["mode"]?.jsonPrimitive?.content)
    }

    @Test
    fun `recaptcha v2 proxyless`() {
        assertEquals(
            json(
                """{"type":"RecaptchaV2TaskProxyless","websiteURL":"https://website.com/",
                   "websiteKey":"KEY","isInvisible":true}""",
            ),
            Tasks.recaptchaV2(
                RecaptchaV2(websiteUrl = "https://website.com/", websiteKey = "KEY", isInvisible = true),
                null,
            ),
        )
    }

    @Test
    fun `recaptcha v2 with a proxy`() {
        assertEquals(
            json(
                """{"type":"RecaptchaV2Task","websiteURL":"https://website.com/","websiteKey":"KEY",
                   "isInvisible":false,"proxyType":"socks5","proxyAddress":"1.2.3.4",
                   "proxyPort":1234,"proxyLogin":"login","proxyPassword":"password",
                   "userAgent":"UA"}""",
            ),
            Tasks.recaptchaV2(
                RecaptchaV2(websiteUrl = "https://website.com/", websiteKey = "KEY", userAgent = "UA"),
                proxy,
            ),
        )
    }

    @Test
    fun `recaptcha v2 enterprise switches the type`() {
        val task = Tasks.recaptchaV2(
            RecaptchaV2(
                websiteUrl = "https://website.com/",
                websiteKey = "KEY",
                isEnterprise = true,
                enterprisePayload = mapOf("s" to "TOKEN"),
                apiDomain = "recaptcha.net",
            ),
            null,
        )

        assertEquals("RecaptchaV2EnterpriseTaskProxyless", task["type"]?.jsonPrimitive?.content)
        assertEquals("TOKEN", (task["enterprisePayload"] as JsonObject)["s"]?.jsonPrimitive?.content)
        assertEquals("recaptcha.net", task["apiDomain"]?.jsonPrimitive?.content)
    }

    @Test
    fun `recaptcha v2 enterprise sends isInvisible only when true`() {
        val visible = RecaptchaV2(websiteUrl = "https://website.com/", websiteKey = "KEY", isEnterprise = true)
        val invisible = visible.copy(isInvisible = true)

        assertNull(Tasks.recaptchaV2(visible, null)["isInvisible"])
        assertNull(Tasks.recaptchaV2(visible, proxy)["isInvisible"])
        assertEquals("true", Tasks.recaptchaV2(invisible, null)["isInvisible"]?.jsonPrimitive?.content)
        assertEquals("true", Tasks.recaptchaV2(invisible, proxy)["isInvisible"]?.jsonPrimitive?.content)
    }

    @Test
    fun `recaptcha v3 checks the score`() {
        val task = Tasks.recaptchaV3(
            RecaptchaV3(
                websiteUrl = "https://website.com/",
                websiteKey = "KEY",
                pageAction = "login",
                minScore = 0.7,
            ),
        )

        assertEquals(0.7, task["minScore"]?.jsonPrimitive?.content?.toDouble())
        assertEquals("login", task["pageAction"]?.jsonPrimitive?.content)

        assertFailsWith<InvalidTaskException> {
            Tasks.recaptchaV3(RecaptchaV3("https://website.com/", "KEY", minScore = 0.55))
        }
    }

    @Test
    fun `hcaptcha sends isEnterprise, not IsEnterprise`() {
        val task = Tasks.hcaptcha(
            HCaptcha(websiteUrl = "https://website.com/", websiteKey = "KEY", isEnterprise = true),
            null,
        )

        assertEquals("true", task["isEnterprise"]?.jsonPrimitive?.content)
        assertNull(task["IsEnterprise"])
    }

    @Test
    fun `hcaptcha cookies need a proxy`() {
        val settings = HCaptcha(websiteUrl = "https://website.com/", websiteKey = "KEY", cookies = "a=1")

        assertEquals("a=1", Tasks.hcaptcha(settings, proxy)["cookies"]?.jsonPrimitive?.content)
        assertNull(Tasks.hcaptcha(settings, null)["cookies"])
    }

    @Test
    fun `funcaptcha keeps the blob a string`() {
        val task = Tasks.funcaptcha(
            FunCaptcha(
                websiteUrl = "https://website.com/",
                websitePublicKey = "KEY",
                apiSubdomain = "x.arkoselabs.com",
                dataBlob = """{"blob":"B"}""",
            ),
            Proxy("1.2.3.4", 8080),
        )

        assertEquals("FunCaptchaTask", task["type"]?.jsonPrimitive?.content)
        assertEquals("""{"blob":"B"}""", task["data"]?.jsonPrimitive?.content)
        assertEquals("http", task["proxyType"]?.jsonPrimitive?.content)
        assertNull(task["proxyLogin"], "an empty login must not be sent")
    }

    @Test
    fun `geetest versions`() {
        val v3 = Tasks.geetest(
            GeeTest(websiteUrl = "https://website.com/", gt = "GT", challenge = "CHALLENGE"),
            null,
        )

        assertEquals("GeeTestTaskProxyless", v3["type"]?.jsonPrimitive?.content)
        assertEquals("3", v3["version"]?.jsonPrimitive?.content)

        val v4 = Tasks.geetest(
            GeeTest(
                websiteUrl = "https://website.com/",
                gt = "GT",
                version = 4,
                initParameters = mapOf("riskType" to "slide"),
            ),
            Proxy("1.2.3.4", 8080),
        )

        assertEquals("GeeTestTask", v4["type"]?.jsonPrimitive?.content)
        assertEquals("4", v4["version"]?.jsonPrimitive?.content)
        assertEquals("slide", (v4["initParameters"] as JsonObject)["riskType"]?.jsonPrimitive?.content)
        assertNull(v4["challenge"])

        assertFailsWith<InvalidTaskException> {
            Tasks.geetest(GeeTest(websiteUrl = "x", gt = "GT"), null)
        }
        assertFailsWith<InvalidTaskException> {
            Tasks.geetest(GeeTest(websiteUrl = "x", gt = "GT", version = 5, challenge = "C"), null)
        }
    }

    @Test
    fun `turnstile and sitekey tasks`() {
        assertEquals(
            json(
                """{"type":"TurnstileTaskProxyless","websiteURL":"https://website.com/",
                   "websiteKey":"0x4","action":"login"}""",
            ),
            Tasks.turnstile(
                Turnstile(websiteUrl = "https://website.com/", websiteKey = "0x4", action = "login"),
                null,
            ),
        )

        val prosopo = Prosopo(websiteUrl = "x", websiteKey = "KEY")
        assertEquals("ProsopoTaskProxyless", Tasks.prosopo(prosopo, null)["type"]?.jsonPrimitive?.content)
        assertEquals("ProsopoTask", Tasks.prosopo(prosopo, proxy)["type"]?.jsonPrimitive?.content)

        assertEquals(
            "FriendlyCaptchaTaskProxyless",
            Tasks.friendlyCaptcha(FriendlyCaptcha("x", "KEY"), null)["type"]?.jsonPrimitive?.content,
        )
    }

    @Test
    fun `altcha needs a challenge`() {
        assertEquals(
            json(
                """{"type":"AltchaTaskProxyless","websiteURL":"https://website.com/",
                   "challengeURL":"/challenge"}""",
            ),
            Tasks.altcha(Altcha(websiteUrl = "https://website.com/", challengeUrl = "/challenge"), null),
        )

        assertFailsWith<InvalidTaskException> {
            Tasks.altcha(Altcha(websiteUrl = "https://website.com/"), null)
        }
    }

    @Test
    fun `amazon widget`() {
        assertEquals(
            json(
                """{"type":"AmazonTaskProxyless","websiteURL":"https://website.com/",
                   "websiteKey":"KEY","wafType":"widget","jsapiScript":"https://x/jsapi.js"}""",
            ),
            Tasks.amazon(
                Amazon(
                    websiteUrl = "https://website.com/",
                    websiteKey = "KEY",
                    wafType = "widget",
                    jsapiScript = "https://x/jsapi.js",
                ),
                null,
            ),
        )
    }

    @Test
    fun `antigate proxy is optional`() {
        val settings = AntiGate(
            websiteUrl = "http://website.com/",
            templateName = "Template",
            variables = mapOf("login" to "value"),
            domainsOfInterest = listOf("example.com"),
            proxy = Proxy("1.2.3.4", 1234),
        )

        val task = Tasks.antigate(settings)

        assertEquals("AntiGateTask", task["type"]?.jsonPrimitive?.content)
        assertEquals("value", (task["variables"] as JsonObject)["login"]?.jsonPrimitive?.content)
        assertEquals("http", task["proxyType"]?.jsonPrimitive?.content)

        assertNull(Tasks.antigate(settings.copy(proxy = null))["proxyAddress"])
    }

    @Test
    fun `antibot cookie task carries no proxyType`() {
        assertEquals(
            json(
                """{"type":"AntiBotCookieTask","websiteURL":"https://website.com/",
                   "proxyAddress":"1.2.3.4","proxyPort":3128,
                   "proxyLogin":"login","proxyPassword":"password"}""",
            ),
            Tasks.antibotCookie(
                AntiBotCookie(
                    websiteUrl = "https://website.com/",
                    proxy = Proxy("1.2.3.4", 3128, login = "login", password = "password"),
                ),
            ),
        )
    }

    @Test
    fun `proxy is validated`() {
        val settings = RecaptchaV2(websiteUrl = "x", websiteKey = "K")

        assertFailsWith<InvalidTaskException> { Tasks.recaptchaV2(settings, Proxy("", 1234)) }
        assertFailsWith<InvalidTaskException> { Tasks.recaptchaV2(settings, Proxy("1.2.3.4", 0)) }
        assertFailsWith<InvalidTaskException> { Tasks.recaptchaV2(settings, Proxy("1.2.3.4", 70_000)) }
    }

    @Test
    fun `base64 matches the reference vectors`() {
        // RFC 4648 test vectors
        assertEquals("", AnticaptchaClient.toBase64("".toByteArray()))
        assertEquals("Zg==", AnticaptchaClient.toBase64("f".toByteArray()))
        assertEquals("Zm8=", AnticaptchaClient.toBase64("fo".toByteArray()))
        assertEquals("Zm9v", AnticaptchaClient.toBase64("foo".toByteArray()))
        assertEquals("Zm9vYmFy", AnticaptchaClient.toBase64("foobar".toByteArray()))
        assertEquals("//79", AnticaptchaClient.toBase64(byteArrayOf(-1, -2, -3)))
    }

    @Test
    fun `solution reads the fields`() {
        val solution = Solution(
            json(
                """{"gRecaptchaResponse":"TOKEN","userAgent":"UA","respKey":"RK",
                   "cookies":{"a":"1","b":"2"},
                   "fingerprint":{"self.navigator.userAgent":"Mozilla/5.0"},
                   "coordinates":[[10,20],[30,40]],"gen_time":"1692000000","nothing":null}""",
            ),
            taskId = 42,
            cost = 0.0007,
        )

        assertEquals("TOKEN", solution.gRecaptchaResponse)
        assertEquals("UA", solution.userAgent)
        assertEquals("RK", solution.respKey)
        assertEquals("1692000000", solution.genTime)
        assertEquals("Mozilla/5.0", solution.fingerprintUserAgent)
        assertEquals("a=1; b=2", solution.cookieHeader)
        assertEquals(2, solution.coordinates?.size)
        assertEquals(42L, solution.taskId)
        assertNull(solution.text)
        assertNull(solution["nothing"], "an explicit JSON null counts as absent")
    }

    @Test
    fun `empty solution`() {
        val solution = Solution(JsonObject(emptyMap()))

        assertTrue(solution.isEmpty)
        assertNull(solution.token)
        assertEquals("", solution.cookieHeader)
    }

    @Test
    fun `api error knows whether it is retryable`() {
        assertTrue(ApiException(2, "ERROR_NO_SLOT_AVAILABLE", "no workers").isRetryable)
        assertTrue(!ApiException(1, "ERROR_KEY_DOES_NOT_EXIST", "no such key").isRetryable)
    }

    @Test
    fun `empty api key is refused before any request`() = runTest {
        val client = AnticaptchaClient(apiKey = "", verbose = false)

        assertFailsWith<InvalidTaskException> { client.getBalance() }
    }

    @Test
    fun `validation happens before the network`() = runTest {
        val client = AnticaptchaClient(apiKey = "KEY", verbose = false)

        assertFailsWith<InvalidTaskException> {
            client.solveAltcha(Altcha(websiteUrl = "https://website.com/"))
        }
        assertFailsWith<InvalidTaskException> {
            client.solveGeeTest(GeeTest(websiteUrl = "https://website.com/", gt = "GT"))
        }
    }

    @Test
    fun `task type names match the api documentation`() {
        val expected = listOf(
            "ImageToTextTask",
            "ImageToCoordinatesTask",
            "RecaptchaV2TaskProxyless",
            "RecaptchaV3TaskProxyless",
            "HCaptchaTaskProxyless",
            "FunCaptchaTaskProxyless",
            "GeeTestTaskProxyless",
            "TurnstileTaskProxyless",
            "ProsopoTaskProxyless",
            "FriendlyCaptchaTaskProxyless",
            "AltchaTaskProxyless",
            "AmazonTaskProxyless",
            "AntiGateTask",
            "AntiBotCookieTask",
        )

        val actual = listOf(
            Tasks.image("QUFB", ImageSettings()),
            Tasks.imageToCoordinates("QUFB", ImageToCoordinates()),
            Tasks.recaptchaV2(RecaptchaV2("x", "K"), null),
            Tasks.recaptchaV3(RecaptchaV3("x", "K")),
            Tasks.hcaptcha(HCaptcha("x", "K"), null),
            Tasks.funcaptcha(FunCaptcha("x", "K"), null),
            Tasks.geetest(GeeTest("x", "GT", challenge = "C"), null),
            Tasks.turnstile(Turnstile("x", "K"), null),
            Tasks.prosopo(Prosopo("x", "K"), null),
            Tasks.friendlyCaptcha(FriendlyCaptcha("x", "K"), null),
            Tasks.altcha(Altcha("x", challengeUrl = "/c"), null),
            Tasks.amazon(Amazon("x", "K"), null),
            Tasks.antigate(AntiGate("x", "T")),
            Tasks.antibotCookie(AntiBotCookie("x", Proxy("1.2.3.4", 8080))),
        ).map { it["type"]?.jsonPrimitive?.content }

        assertContentEquals(expected, actual)
    }
}
