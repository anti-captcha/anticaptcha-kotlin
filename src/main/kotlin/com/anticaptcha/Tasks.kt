package com.anticaptcha

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Which protocol your proxy speaks.
 */
public enum class ProxyType(internal val apiValue: String) {
    HTTP("http"),
    SOCKS4("socks4"),
    SOCKS5("socks5"),
}

/**
 * Your proxy, for the `*ProxyOn` methods.
 *
 * Do not use purchased or rented proxies from proxy services, use proper proxy
 * software like [Squid](https://anti-captcha.com/apidoc/articles/how-to-install-squid).
 */
public data class Proxy(
    val address: String,
    val port: Int,
    val type: ProxyType = ProxyType.HTTP,
    val login: String = "",
    val password: String = "",
)

/** [ImageToTextTask](https://anti-captcha.com/apidoc/task-types/ImageToTextTask) */
public data class ImageSettings(
    /** The image contains 2 or more words. */
    val phrase: Boolean = false,
    /** The answer is case sensitive. */
    val caseSensitive: Boolean = false,
    /** `0` no requirements, `1` digits only, `2` no digits. */
    val numeric: Int = 0,
    /** The answer is the result of a math operation, like `50+5`. */
    val mathOperation: Boolean = false,
    /** `0` for no limit. */
    val minLength: Int = 0,
    /** `0` for no limit. */
    val maxLength: Int = 0,
    /** `"en"` or `"rn"`. */
    val languagePool: String = "en",
    /** Hint for the worker. */
    val comment: String? = null,
    /** Optional, groups the dashboard statistics by website. */
    val websiteUrl: String? = null,
)

/** [ImageToCoordinatesTask](https://anti-captcha.com/apidoc/task-types/ImageToCoordinatesTask) */
public data class ImageToCoordinates(
    /** `"points"` or `"rectangles"`. */
    val mode: String = "points",
    /** Instruction for the worker. */
    val comment: String? = null,
    val websiteUrl: String? = null,
)

/** [RecaptchaV2TaskProxyless](https://anti-captcha.com/apidoc/task-types/RecaptchaV2TaskProxyless) */
public data class RecaptchaV2(
    val websiteUrl: String,
    /** The `data-sitekey` value. */
    val websiteKey: String,
    val websiteSToken: String? = null,
    /**
     * Set to `true` if there's `"size":"invisible"` in the render call.
     * Enterprise tasks receive the flag only when it is `true`.
     */
    val isInvisible: Boolean = false,
    /** The `data-s` parameter, typical for google.com websites. */
    val dataSValue: String? = null,
    /** Proxy-on tasks only. */
    val userAgent: String? = null,
    /** Switches the task to `RecaptchaV2EnterpriseTask`. */
    val isEnterprise: Boolean = false,
    /** Parameters passed to the `grecaptcha.enterprise.render` call. */
    val enterprisePayload: Map<String, String> = emptyMap(),
    /** e.g. `"recaptcha.net"`, when the script does not come from google.com. */
    val apiDomain: String? = null,
)

/**
 * [RecaptchaV3TaskProxyless](https://anti-captcha.com/apidoc/task-types/RecaptchaV3TaskProxyless)
 *
 * This captcha has no proxy-on version.
 */
public data class RecaptchaV3(
    val websiteUrl: String,
    val websiteKey: String,
    /** One of `0.3`, `0.7`, `0.9`. */
    val minScore: Double = 0.3,
    val pageAction: String? = null,
    val isEnterprise: Boolean = false,
    val apiDomain: String? = null,
)

/** [HCaptchaTaskProxyless](https://anti-captcha.com/apidoc/task-types/HCaptchaTaskProxyless) */
public data class HCaptcha(
    val websiteUrl: String,
    val websiteKey: String,
    val isInvisible: Boolean = false,
    val isEnterprise: Boolean = false,
    /** `rqdata`, `sentry`, `apiEndpoint`, `endpoint`, `reportapi`, `assethost`, `imghost`. */
    val enterprisePayload: Map<String, String> = emptyMap(),
    val userAgent: String? = null,
    /** Proxy-on tasks only, `name1=value1; name2=value2`. */
    val cookies: String? = null,
)

/** [FunCaptchaTaskProxyless](https://anti-captcha.com/apidoc/task-types/FunCaptchaTaskProxyless) */
public data class FunCaptcha(
    val websiteUrl: String,
    /** The `data-pkey` value. */
    val websitePublicKey: String,
    /** e.g. `"somewebsite-api.arkoselabs.com"`. */
    val apiSubdomain: String? = null,
    /** The `blob` value, as a JSON string. */
    val dataBlob: String? = null,
    val userAgent: String? = null,
)

/** [GeeTestTaskProxyless](https://anti-captcha.com/apidoc/task-types/GeeTestTaskProxyless) */
public data class GeeTest(
    val websiteUrl: String,
    /** v3: the `gt` key, v4: the `captcha_id` value. */
    val gt: String,
    /** `3` or `4`. */
    val version: Int = 3,
    /** v3 only, a one-time value. */
    val challenge: String? = null,
    val apiSubdomain: String? = null,
    /** v4 only, e.g. `{"riskType": "slide"}`. */
    val initParameters: Map<String, String> = emptyMap(),
    val userAgent: String? = null,
)

/** [TurnstileTaskProxyless](https://anti-captcha.com/apidoc/task-types/TurnstileTaskProxyless) */
public data class Turnstile(
    val websiteUrl: String,
    val websiteKey: String,
    val action: String? = null,
    val cData: String? = null,
    val chlPageData: String? = null,
    val userAgent: String? = null,
)

/** [ProsopoTaskProxyless](https://anti-captcha.com/apidoc/task-types/ProsopoTaskProxyless) */
public data class Prosopo(
    val websiteUrl: String,
    val websiteKey: String,
    val userAgent: String? = null,
)

/** [FriendlyCaptchaTaskProxyless](https://anti-captcha.com/apidoc/task-types/FriendlyCaptchaTaskProxyless) */
public data class FriendlyCaptcha(
    val websiteUrl: String,
    val websiteKey: String,
    val userAgent: String? = null,
)

/** [AltchaTaskProxyless](https://anti-captcha.com/apidoc/task-types/AltchaTaskProxyless) */
public data class Altcha(
    val websiteUrl: String,
    /** Use this or [challengeJson]. */
    val challengeUrl: String? = null,
    /** Use this or [challengeUrl]. */
    val challengeJson: String? = null,
    val userAgent: String? = null,
)

/** [AmazonTaskProxyless](https://anti-captcha.com/apidoc/task-types/AmazonTaskProxyless) */
public data class Amazon(
    val websiteUrl: String,
    /** `key` from `window.gokuProps`, or the widget API key. */
    val websiteKey: String,
    val iv: String? = null,
    val context: String? = null,
    val captchaScript: String? = null,
    val challengeScript: String? = null,
    /** Required when [wafType] is `"widget"`. */
    val jsapiScript: String? = null,
    /** `"widget"` for a standalone widget, empty for the bot filtering page. */
    val wafType: String? = null,
    val userAgent: String? = null,
)

/** [AntiGateTask](https://anti-captcha.com/apidoc/task-types/AntiGateTask) */
public data class AntiGate(
    val websiteUrl: String,
    /** See the [template directory](https://anti-captcha.com/apidoc/antigate-templates). */
    val templateName: String,
    val variables: Map<String, String> = emptyMap(),
    val domainsOfInterest: List<String> = emptyList(),
    /** The proxy is optional for this task type. */
    val proxy: Proxy? = null,
)

/** [AntiBotCookieTask](https://anti-captcha.com/apidoc/task-types/AntiBotCookieTask) */
public data class AntiBotCookie(
    val websiteUrl: String,
    /** Required: the cookies are only valid for this IP address. */
    val proxy: Proxy,
)

// ---------------------------------------------------------------- task builders

internal object Tasks {

    private fun JsonObjectBuilder.putIfNotEmpty(name: String, value: String?) {
        if (!value.isNullOrEmpty()) put(name, value)
    }

    private fun JsonObjectBuilder.putIfNotEmpty(name: String, value: Map<String, String>) {
        if (value.isNotEmpty()) {
            put(name, buildJsonObject { value.forEach { (key, item) -> put(key, item) } })
        }
    }

    /** Validates the proxy and appends it to the task. */
    private fun JsonObjectBuilder.putProxy(proxy: Proxy, withType: Boolean = true) {
        if (proxy.address.isEmpty()) {
            throw InvalidTaskException("proxy address is empty")
        }

        if (proxy.port !in 1..65_535) {
            throw InvalidTaskException("proxy port ${proxy.port} is out of range")
        }

        if (withType) put("proxyType", proxy.type.apiValue)
        put("proxyAddress", proxy.address)
        put("proxyPort", proxy.port)

        if (proxy.login.isNotEmpty()) {
            put("proxyLogin", proxy.login)
            put("proxyPassword", proxy.password)
        }
    }

    fun image(body: String, settings: ImageSettings): JsonObject {
        if (body.isEmpty()) throw InvalidTaskException("captcha image is empty")

        return buildJsonObject {
            put("type", "ImageToTextTask")
            put("body", body)
            put("phrase", settings.phrase)
            put("case", settings.caseSensitive)
            put("numeric", settings.numeric)
            put("math", settings.mathOperation)
            put("minLength", settings.minLength)
            put("maxLength", settings.maxLength)
            put("languagePool", settings.languagePool)
            putIfNotEmpty("comment", settings.comment)
            putIfNotEmpty("websiteURL", settings.websiteUrl)
        }
    }

    fun imageToCoordinates(body: String, settings: ImageToCoordinates): JsonObject {
        if (body.isEmpty()) throw InvalidTaskException("captcha image is empty")

        return buildJsonObject {
            put("type", "ImageToCoordinatesTask")
            put("body", body)
            put("mode", settings.mode)
            putIfNotEmpty("comment", settings.comment)
            putIfNotEmpty("websiteURL", settings.websiteUrl)
        }
    }

    fun recaptchaV2(settings: RecaptchaV2, proxy: Proxy?): JsonObject = buildJsonObject {
        val type = when {
            settings.isEnterprise && proxy != null -> "RecaptchaV2EnterpriseTask"
            settings.isEnterprise -> "RecaptchaV2EnterpriseTaskProxyless"
            proxy != null -> "RecaptchaV2Task"
            else -> "RecaptchaV2TaskProxyless"
        }

        put("type", type)
        put("websiteURL", settings.websiteUrl)
        put("websiteKey", settings.websiteKey)
        if (!settings.isEnterprise || settings.isInvisible) {
            put("isInvisible", settings.isInvisible)
        }
        putIfNotEmpty("websiteSToken", settings.websiteSToken)
        putIfNotEmpty("recaptchaDataSValue", settings.dataSValue)
        putIfNotEmpty("apiDomain", settings.apiDomain)
        putIfNotEmpty("enterprisePayload", settings.enterprisePayload)

        if (proxy != null) {
            putProxy(proxy)
            putIfNotEmpty("userAgent", settings.userAgent)
        }
    }

    fun recaptchaV3(settings: RecaptchaV3): JsonObject {
        if (settings.minScore !in listOf(0.3, 0.7, 0.9)) {
            throw InvalidTaskException(
                "minScore must be one of 0.3, 0.7, 0.9; got ${settings.minScore}",
            )
        }

        return buildJsonObject {
            put("type", "RecaptchaV3TaskProxyless")
            put("websiteURL", settings.websiteUrl)
            put("websiteKey", settings.websiteKey)
            put("minScore", settings.minScore)
            put("isEnterprise", settings.isEnterprise)
            putIfNotEmpty("pageAction", settings.pageAction)
            putIfNotEmpty("apiDomain", settings.apiDomain)
        }
    }

    fun hcaptcha(settings: HCaptcha, proxy: Proxy?): JsonObject = buildJsonObject {
        put("type", if (proxy != null) "HCaptchaTask" else "HCaptchaTaskProxyless")
        put("websiteURL", settings.websiteUrl)
        put("websiteKey", settings.websiteKey)
        put("isInvisible", settings.isInvisible)
        put("isEnterprise", settings.isEnterprise)
        putIfNotEmpty("enterprisePayload", settings.enterprisePayload)

        if (proxy != null) {
            putProxy(proxy)
            putIfNotEmpty("userAgent", settings.userAgent)
            putIfNotEmpty("cookies", settings.cookies)
        }
    }

    fun funcaptcha(settings: FunCaptcha, proxy: Proxy?): JsonObject = buildJsonObject {
        put("type", if (proxy != null) "FunCaptchaTask" else "FunCaptchaTaskProxyless")
        put("websiteURL", settings.websiteUrl)
        put("websitePublicKey", settings.websitePublicKey)
        putIfNotEmpty("funcaptchaApiJSSubdomain", settings.apiSubdomain)
        putIfNotEmpty("data", settings.dataBlob)

        if (proxy != null) {
            putProxy(proxy)
            putIfNotEmpty("userAgent", settings.userAgent)
        }
    }

    fun geetest(settings: GeeTest, proxy: Proxy?): JsonObject {
        if (settings.version !in listOf(3, 4)) {
            throw InvalidTaskException("GeeTest version must be 3 or 4, got ${settings.version}")
        }

        if (settings.version == 3 && settings.challenge.isNullOrEmpty()) {
            throw InvalidTaskException("GeeTest v3 requires a challenge value")
        }

        return buildJsonObject {
            put("type", if (proxy != null) "GeeTestTask" else "GeeTestTaskProxyless")
            put("websiteURL", settings.websiteUrl)
            put("gt", settings.gt)
            put("version", settings.version)
            putIfNotEmpty("challenge", settings.challenge)
            putIfNotEmpty("geetestApiServerSubdomain", settings.apiSubdomain)
            putIfNotEmpty("initParameters", settings.initParameters)

            if (proxy != null) {
                putProxy(proxy)
                putIfNotEmpty("userAgent", settings.userAgent)
            }
        }
    }

    fun turnstile(settings: Turnstile, proxy: Proxy?): JsonObject = buildJsonObject {
        put("type", if (proxy != null) "TurnstileTask" else "TurnstileTaskProxyless")
        put("websiteURL", settings.websiteUrl)
        put("websiteKey", settings.websiteKey)
        putIfNotEmpty("action", settings.action)
        putIfNotEmpty("cData", settings.cData)
        putIfNotEmpty("chlPageData", settings.chlPageData)

        if (proxy != null) {
            putProxy(proxy)
            putIfNotEmpty("userAgent", settings.userAgent)
        }
    }

    /** Prosopo and Friendly Captcha take exactly the same parameters. */
    private fun sitekeyTask(
        type: String,
        websiteUrl: String,
        websiteKey: String,
        userAgent: String?,
        proxy: Proxy?,
    ): JsonObject = buildJsonObject {
        put("type", type)
        put("websiteURL", websiteUrl)
        put("websiteKey", websiteKey)

        if (proxy != null) {
            putProxy(proxy)
            putIfNotEmpty("userAgent", userAgent)
        }
    }

    fun prosopo(settings: Prosopo, proxy: Proxy?): JsonObject = sitekeyTask(
        if (proxy != null) "ProsopoTask" else "ProsopoTaskProxyless",
        settings.websiteUrl,
        settings.websiteKey,
        settings.userAgent,
        proxy,
    )

    fun friendlyCaptcha(settings: FriendlyCaptcha, proxy: Proxy?): JsonObject = sitekeyTask(
        if (proxy != null) "FriendlyCaptchaTask" else "FriendlyCaptchaTaskProxyless",
        settings.websiteUrl,
        settings.websiteKey,
        settings.userAgent,
        proxy,
    )

    fun altcha(settings: Altcha, proxy: Proxy?): JsonObject {
        if (settings.challengeUrl.isNullOrEmpty() && settings.challengeJson.isNullOrEmpty()) {
            throw InvalidTaskException("set either challengeUrl or challengeJson")
        }

        return buildJsonObject {
            put("type", if (proxy != null) "AltchaTask" else "AltchaTaskProxyless")
            put("websiteURL", settings.websiteUrl)
            putIfNotEmpty("challengeURL", settings.challengeUrl)
            putIfNotEmpty("challengeJSON", settings.challengeJson)

            if (proxy != null) {
                putProxy(proxy)
                putIfNotEmpty("userAgent", settings.userAgent)
            }
        }
    }

    fun amazon(settings: Amazon, proxy: Proxy?): JsonObject = buildJsonObject {
        put("type", if (proxy != null) "AmazonTask" else "AmazonTaskProxyless")
        put("websiteURL", settings.websiteUrl)
        put("websiteKey", settings.websiteKey)
        putIfNotEmpty("wafType", settings.wafType)
        putIfNotEmpty("iv", settings.iv)
        putIfNotEmpty("context", settings.context)
        putIfNotEmpty("captchaScript", settings.captchaScript)
        putIfNotEmpty("challengeScript", settings.challengeScript)
        putIfNotEmpty("jsapiScript", settings.jsapiScript)

        if (proxy != null) {
            putProxy(proxy)
            putIfNotEmpty("userAgent", settings.userAgent)
        }
    }

    fun antigate(settings: AntiGate): JsonObject = buildJsonObject {
        put("type", "AntiGateTask")
        put("websiteURL", settings.websiteUrl)
        put("templateName", settings.templateName)
        putIfNotEmpty("variables", settings.variables)

        if (settings.domainsOfInterest.isNotEmpty()) {
            put(
                "domainsOfInterest",
                kotlinx.serialization.json.buildJsonArray {
                    settings.domainsOfInterest.forEach {
                        add(kotlinx.serialization.json.JsonPrimitive(it))
                    }
                },
            )
        }

        settings.proxy?.let { putProxy(it) }
    }

    fun antibotCookie(settings: AntiBotCookie): JsonObject = buildJsonObject {
        put("type", "AntiBotCookieTask")
        put("websiteURL", settings.websiteUrl)
        // This task type takes no proxyType, only http proxies are supported.
        putProxy(settings.proxy, withType = false)
    }
}
