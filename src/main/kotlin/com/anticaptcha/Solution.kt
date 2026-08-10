package com.anticaptcha

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * The `solution` object of a completed task.
 *
 * Which properties return something depends on the task type; see the
 * [documentation](https://anti-captcha.com/apidoc) for the per-type description.
 * Properties for fields a task type does not fill return `null`.
 */
public class Solution internal constructor(
    /** The whole `solution` object, for fields this class does not expose yet. */
    public val raw: JsonObject,
    /** Id of the task that produced this solution, for the `report*` methods. */
    public val taskId: Long = 0,
    /** What the task cost, in US dollars. */
    public val cost: Double = 0.0,
) {
    /** Any field by its API name. */
    public operator fun get(field: String): JsonElement? =
        raw[field]?.takeUnless { it is JsonNull }

    private fun string(field: String): String? =
        (get(field) as? JsonPrimitive)?.takeIf { it.isString }?.content

    /** Image captchas. */
    public val text: String? get() = string("text")

    /** FunCaptcha, Turnstile, Prosopo, Friendly Captcha, Altcha, Amazon WAF. */
    public val token: String? get() = string("token")

    /** Recaptcha and hCaptcha. */
    public val gRecaptchaResponse: String? get() = string("gRecaptchaResponse")

    /** Recaptcha with `isExtended`. */
    public val gRecaptchaResponseMd5: String? get() = string("gRecaptchaResponseMd5")

    /** hCaptcha. */
    public val respKey: String? get() = string("respKey")

    /** User-agent of the worker who solved the captcha. Submit the form with it. */
    public val userAgent: String? get() = string("userAgent")

    /** AntiGate and AntiBotCookie tasks. */
    public val url: String? get() = string("url")

    /** Amazon WAF: the domain the `aws-waf-token` cookie belongs to. */
    public val domain: String? get() = string("domain")

    /** GeeTest v3. */
    public val challenge: String? get() = string("challenge")

    /** GeeTest v3. */
    public val seccode: String? get() = string("seccode")

    /** GeeTest v3. */
    public val validate: String? get() = string("validate")

    /** GeeTest v4. */
    public val captchaId: String? get() = string("captcha_id")

    /** GeeTest v4. */
    public val lotNumber: String? get() = string("lot_number")

    /** GeeTest v4. */
    public val passToken: String? get() = string("pass_token")

    /** GeeTest v4. */
    public val genTime: String? get() = string("gen_time")

    /** GeeTest v4. */
    public val captchaOutput: String? get() = string("captcha_output")

    /** AntiGate and AntiBotCookie tasks. */
    public val cookies: JsonObject? get() = get("cookies") as? JsonObject

    /** AntiGate and AntiBotCookie tasks. */
    public val localStorage: JsonObject? get() = get("localStorage") as? JsonObject

    /** AntiGate and AntiBotCookie tasks. */
    public val fingerprint: JsonObject? get() = get("fingerprint") as? JsonObject

    /** AntiBotCookie tasks: the headers the worker's browser sent last. */
    public val lastRequestHeaders: JsonArray? get() = get("lastRequestHeaders") as? JsonArray

    /** ImageToCoordinates tasks: `[x, y]` points or `[x1, y1, x2, y2]` boxes. */
    public val coordinates: JsonArray? get() = get("coordinates") as? JsonArray

    /** The cookies formatted as a `name1=value1; name2=value2` header value. */
    public val cookieHeader: String
        get() = cookies.orEmpty().entries.joinToString("; ") { (name, value) ->
            "$name=" + ((value as? JsonPrimitive)?.content ?: value.toString())
        }

    /** The worker's browser user-agent, taken from the AntiBotCookie fingerprint. */
    public val fingerprintUserAgent: String?
        get() = fingerprint?.get("self.navigator.userAgent")?.jsonPrimitive?.content

    /** True when the API returned a `solution` object without any usable field. */
    public val isEmpty: Boolean get() = raw.isEmpty()

    override fun toString(): String = "Solution(taskId=$taskId, fields=${raw.keys})"

    private fun JsonObject?.orEmpty(): JsonObject = this ?: JsonObject(emptyMap())
}
