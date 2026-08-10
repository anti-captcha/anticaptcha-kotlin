package com.anticaptcha.examples

import com.anticaptcha.Altcha
import com.anticaptcha.Amazon
import com.anticaptcha.AnticaptchaClient
import com.anticaptcha.AnticaptchaException
import com.anticaptcha.AntiBotCookie
import com.anticaptcha.AntiGate
import com.anticaptcha.ApiException
import com.anticaptcha.FriendlyCaptcha
import com.anticaptcha.FunCaptcha
import com.anticaptcha.GeeTest
import com.anticaptcha.HCaptcha
import com.anticaptcha.ImageSettings
import com.anticaptcha.ImageToCoordinates
import com.anticaptcha.Prosopo
import com.anticaptcha.Proxy
import com.anticaptcha.ProxyType
import com.anticaptcha.RecaptchaV2
import com.anticaptcha.RecaptchaV3
import com.anticaptcha.Turnstile
import kotlinx.coroutines.runBlocking

/**
 * Runnable examples for every task type.
 *
 * Put your API key below and run:
 * ```
 * ./gradlew runExample -Pexample=image
 * ```
 * Run without an argument to see the list of available examples.
 */
private const val API_KEY = "API_KEY_HERE"

/**
 * Specify a soft id to earn 10% commission with your app.
 * Get yours at https://anti-captcha.com/clients/tools/devcenter
 */
private const val SOFT_ID = 0

private fun client() = AnticaptchaClient(apiKey = API_KEY, softId = SOFT_ID)
// pass verbose = false to turn the debug output off

/**
 * Every proxy-on example uses this. Do not use purchased or rented proxies from
 * proxy services, use proper proxy software like Squid.
 */
private fun sampleProxy() = Proxy(
    address = "1.2.3.4",
    port = 1234,
    type = ProxyType.HTTP,
    login = "login-optional",
    password = "password-optional",
)

private val examples: Map<String, suspend () -> Unit> = mapOf(
    "balance" to {
        val ac = client()
        println("Balance: ${ac.getBalance()}")
        println("Captcha credits: ${ac.getCreditsBalance()}")
    },

    "image" to {
        val solution = client().solveImageFile(
            "captcha.jpg",
            ImageSettings(
                languagePool = "en", // "en" or "rn"
                comment = "Type in green characters",
                // phrase = true,          // the image has 2 or more words
                // caseSensitive = true,   // the answer is case sensitive
                // numeric = 1,            // 1 - digits only, 2 - no digits
                // mathOperation = true,   // the answer is the result of 50+5
                // minLength = 1,
                // maxLength = 10,
            ),
        )

        println("Captcha text: ${solution.text}")

        // If the answer turns out to be wrong:
        // client().reportIncorrectImageCaptcha(solution.taskId)
    },

    "coordinates" to {
        val solution = client().solveImageToCoordinatesFile(
            "coordinates.jpg",
            ImageToCoordinates(
                mode = "points", // "points" or "rectangles"
                comment = "Select objects in the specified order",
            ),
        )

        println("Objects X,Y coordinates: ${solution.coordinates}")
    },

    "recaptcha2" to {
        val solution = client().solveRecaptchaV2(
            RecaptchaV2(
                websiteUrl = "https://www.website.com/",
                websiteKey = "6Lcyu8UZAAAAACwSh6Xf58WrNXTu0LLu4F85xf20",
                // isInvisible = true,        // solving an invisible Recaptcha V2
                // dataSValue = "...",        // the "data-s" parameter, typical for google.com
            ),
        )

        println("g-response token: ${solution.gRecaptchaResponse}")
        println("Worker's user-agent: ${solution.userAgent}")
    },

    "recaptcha2-proxy" to {
        val solution = client().solveRecaptchaV2ProxyOn(
            RecaptchaV2(
                websiteUrl = "https://www.website.com/",
                websiteKey = "6Lcyu8UZAAAAACwSh6Xf58WrNXTu0LLu4F85xf20",
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            ),
            sampleProxy(),
        )

        println("g-response token: ${solution.gRecaptchaResponse}")
    },

    "recaptcha2-enterprise" to {
        val solution = client().solveRecaptchaV2(
            RecaptchaV2(
                websiteUrl = "https://store.steampowered.com/join",
                websiteKey = "6LdIFr0ZAAAAAO3vz0O0OQrtAefzdJcWQM2TMYQH",
                isEnterprise = true,
                enterprisePayload = mapOf("s" to "SOME_ADDITIONAL_TOKEN"),
                // apiDomain = "recaptcha.net",  // only for a non-google.com script domain
            ),
        )

        println("g-response token: ${solution.gRecaptchaResponse}")
    },

    "recaptcha3" to {
        val solution = client().solveRecaptchaV3(
            RecaptchaV3(
                websiteUrl = "https://www.website.com/",
                websiteKey = "6LcvNcwdAAAAAMWAuNRXH74u3QePsEzTm6GEjx0J",
                pageAction = "somefun",
                minScore = 0.9, // one of 0.3, 0.7, 0.9
                // isEnterprise = true,
            ),
        )

        println("g-response token: ${solution.gRecaptchaResponse}")
    },

    "hcaptcha" to {
        val solution = client().solveHCaptcha(
            HCaptcha(
                websiteUrl = "https://www.website.com/",
                websiteKey = "00000000-1111-2222-3333-444444444444",
                // isInvisible = true,
                // isEnterprise = true,
                // enterprisePayload = mapOf("rqdata" to "rqdata value from the target website"),
            ),
        )

        println("hCaptcha token: ${solution.gRecaptchaResponse}")
        println("Use this user-agent for the form: ${solution.userAgent}")
        println("respkey: ${solution.respKey}")
    },

    "funcaptcha" to {
        val solution = client().solveFunCaptcha(
            FunCaptcha(
                websiteUrl = "https://www.website.com/",
                websitePublicKey = "00000000-1111-2222-3333-444444444444",
                // Look for a URL like
                // https://somewebsite-api.arkoselabs.com/v2/00000000-1111-2222-3333-444444444444/api.js
                apiSubdomain = "somewebsite-api.arkoselabs.com",
                dataBlob = """{"blob":"HERE_COMES_THE_blob_VALUE"}""",
            ),
        )

        println("FunCaptcha token: ${solution.token}")
    },

    "geetest3" to {
        val solution = client().solveGeeTest(
            GeeTest(
                websiteUrl = "https://www.website.com/",
                gt = "b6e21f90a91a3c2d4a31fe84e10d0442",
                version = 3,
                // The challenge is one-time, grab a fresh one for every task
                challenge = "169acd4a58f2c99770322dfa5270c221",
            ),
        )

        println("challenge: ${solution.challenge}")
        println("seccode: ${solution.seccode}")
        println("validate: ${solution.validate}")
    },

    "geetest4" to {
        val solution = client().solveGeeTest(
            GeeTest(
                websiteUrl = "https://www.website.com/",
                gt = "e9ca9c9ca19ad540a8017f5c107b2d0f",
                version = 4,
                initParameters = mapOf("riskType" to "slide"),
            ),
        )

        println("captcha_id: ${solution.captchaId}")
        println("lot_number: ${solution.lotNumber}")
        println("pass_token: ${solution.passToken}")
        println("gen_time: ${solution.genTime}")
        println("captcha_output: ${solution.captchaOutput}")
    },

    "turnstile" to {
        val solution = client().solveTurnstile(
            Turnstile(
                websiteUrl = "https://www.website.com/",
                websiteKey = "0x4AAAAAAABD2Inoxs-yJ8bz",
                // action = "optional page action",
                // cData = "cdata token for cloudflare",
                // chlPageData = "chlPageData token for cloudflare",
            ),
        )

        println("Turnstile token: ${solution.token}")
    },

    "prosopo" to {
        val solution = client().solveProsopo(
            Prosopo(websiteUrl = "https://www.website.com/", websiteKey = "sitekey-here"),
        )

        println("Prosopo token: ${solution.token}")
    },

    "friendly" to {
        val solution = client().solveFriendlyCaptcha(
            FriendlyCaptcha(websiteUrl = "https://www.website.com/", websiteKey = "sitekey-here"),
        )

        println("Friendly Captcha token: ${solution.token}")
    },

    "amazon" to {
        val solution = client().solveAmazon(
            Amazon(
                websiteUrl = "https://www.website.com/",
                websiteKey = "key_value_from_window.gokuProps_object",
                iv = "iv_value_from_window.gokuProps_object",
                context = "context_value_from_window.gokuProps_object",
                // For a standalone widget instead of the bot filtering page:
                // wafType = "widget",
                // jsapiScript = "https://164cb210e333.edge.captcha-sdk.awswaf.com/164cb210e333/jsapi.js",
            ),
        )

        println("aws-waf-token: ${solution.token}")
    },

    "altcha" to {
        val solution = client().solveAltcha(
            Altcha(
                websiteUrl = "https://www.website.com/",
                // Use challengeUrl or challengeJson, not both
                challengeUrl = "/some/path/to/challenge/url",
                // challengeJson = """{"algorithm":"SHA-256","challenge":"..."}""",
            ),
        )

        println("Altcha token: ${solution.token}")
    },

    "antigate" to {
        val solution = client().solveAntiGate(
            AntiGate(
                websiteUrl = "http://antigate.com/logintest.php",
                templateName = "Sign-in and wait for control text",
                variables = mapOf(
                    "login_input_css" to "#login",
                    "login_input_value" to "the login",
                    "password_input_css" to "#password",
                    "password_input_value" to "the password",
                    "control_text" to "You have been logged successfully",
                ),
                // The proxy is optional for AntiGate tasks
                // proxy = sampleProxy(),
            ),
        )

        println("cookies: ${solution.cookies}")
        println("localStorage: ${solution.localStorage}")
        println("fingerprint: ${solution.fingerprint}")
        println("url: ${solution.url}")
    },

    "antibot-cookie" to {
        val solution = client().solveAntiBotCookie(
            AntiBotCookie(
                websiteUrl = "https://www.somewebsite.com/",
                // The cookies are bound to this IP address, use the very same proxy afterwards
                proxy = sampleProxy(),
            ),
        )

        println("Use these cookies for requests: ${solution.cookieHeader}")
        println("Use this user-agent for requests: ${solution.fingerprintUserAgent}")
    },
)

public fun main(args: Array<String>): Unit = runBlocking {
    val name = args.firstOrNull()
    val example = examples[name]

    if (example == null) {
        println("Usage: ./gradlew runExample -Pexample=<example>")
        println("Available examples: ${examples.keys.joinToString(", ")}")
        return@runBlocking
    }

    try {
        example()
    } catch (error: ApiException) {
        System.err.println("Failed: ${error.message}")
        System.err.println("See https://anti-captcha.com/apidoc/errors for ${error.errorCode}")
    } catch (error: AnticaptchaException) {
        System.err.println("Failed: ${error.message}")
    }
}
