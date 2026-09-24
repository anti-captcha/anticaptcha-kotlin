## Official Anti-Captcha.com Kotlin library ##

Official anti-captcha.com Kotlin library for solving images with text, Recaptcha v2/v3 Enterprise/non-Enterprise, Funcaptcha, GeeTest, HCaptcha Enterprise/non-Enterprise, Turnstile, Amazon WAF, Prosopo, Friendly Captcha and Altcha.

[Anti-captcha](https://anti-captcha.com) is an oldest and cheapest web service dedicated to solving captchas by human workers from around the world. By solving captchas with us you help people in poorest regions of the world to earn money, which not only cover their basic needs, but also gives them ability to financially help their families, study and avoid jobs where they're simply not happy.

To use the service you need to [register](https://anti-captcha.com/clients/) and topup your balance. Prices start from $0.0005 per image captcha and $0.002 for Recaptcha. That's $0.5 per 1000 for images and $2 for 1000 Recaptchas.

For more technical information and articles visit our [documentation](https://anti-captcha.com/apidoc) page.

### Install

**Gradle**:
```kotlin
implementation("com.anti-captcha:anticaptcha-kotlin:1.0.1")
```

**Maven**:
```xml
<dependency>
  <groupId>com.anti-captcha</groupId>
  <artifactId>anticaptcha-kotlin</artifactId>
  <version>1.0.1</version>
</dependency>
```

Requires **Java 11 or newer** at runtime. Dependencies are `kotlinx-coroutines-core` and `kotlinx-serialization-json` — HTTP comes from the JDK itself.

> Building this repository needs **JDK 17 or newer**, because Gradle 9 itself refuses to run on anything older. That is a build-time requirement only: the artifact is compiled to Java 11 bytecode. If `./gradlew` reports `Gradle requires JVM 17 or later`, point `JAVA_HOME` at a newer JDK:
> ```bash
> export JAVA_HOME=$(/usr/libexec/java_home -v 17+)   # macOS
> ```

> Writing plain Java? Use [anticaptcha](https://github.com/anti-captcha/anticaptcha-java) instead: `com.anti-captcha:anticaptcha`. This library is coroutine-first and is awkward to call from Java.

The API is `suspend`-based. Every settings class is a data class with defaults, so you only name what you need.

**Examples how to solve:**

- [Image Captcha](#solve-image-captcha)
- [Recaptcha V2](#solve-recaptcha-v2)
- [Recaptcha V2 Enterprise](#solve-recaptcha-v2-enterprise)
- [Recaptcha V3](#solve-recaptcha-v3)
- [hCaptcha](#solve-hcaptcha)
- [FunCaptcha](#solve-funcaptcha)
- [GeeTest](#solve-geetest)
- [Turnstile](#solve-turnstile)
- [Image to coordinates](#image-to-coordinates)
- [AntiGate (custom tasks)](#solve-antigate-custom-tasks)
- [AntiBot cookies](#get-antibot-cookies)
- [Prosopo](#solve-prosopo)
- [Friendly Captcha](#solve-friendly-captcha)
- [Amazon WAF](#solve-amazon-waf)
- [Altcha](#solve-altcha)

### Solve image captcha
```kotlin
import com.anticaptcha.AnticaptchaClient
import com.anticaptcha.ImageSettings
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Create the API client and set the API key
    val ac = AnticaptchaClient(
        apiKey = "API_KEY_HERE",
        // Specify a soft id to earn 10% commission with your app.
        // Get yours at https://anti-captcha.com/clients/tools/devcenter
        softId = 0,
        // Set to false to turn the debug output off
        verbose = true,
    )

    // Make sure the API key funds balance is positive
    val balance = ac.getBalance()
    if (balance <= 0) {
        // Stop here to make sure you don't DDoS the API while having empty balance
        System.err.println("Empty balance")
        return@runBlocking
    }
    println("Balance: $balance")

    val solution = ac.solveImageFile(
        "captcha.jpg",
        ImageSettings(
            // Optional settings, see https://anti-captcha.com/apidoc/task-types/ImageToTextTask
            // phrase = true,             // the image has 2 or more words
            // caseSensitive = true,      // the answer is case sensitive
            // numeric = 1,               // 1 - digits only, 2 - no digits
            // mathOperation = true,      // the answer is the result of 50+5
            // minLength = 1,
            // maxLength = 10,
            languagePool = "en",          // "en" or "rn"
            comment = "Type in green characters",
        ),
    )
    // OR ac.solveImage("image-encoded-in-base64", settings)

    println("Captcha Solution: ${solution.text}")

    // If the answer turns out to be wrong:
    // ac.reportIncorrectImageCaptcha(solution.taskId)
}
```
&nbsp;

### Solve Recaptcha V2
```kotlin
import com.anticaptcha.RecaptchaV2

val solution = ac.solveRecaptchaV2(
    RecaptchaV2(
        websiteUrl = "https://www.website.com/",
        websiteKey = "6Lcyu8UZAAAAACwSh6Xf58WrNXTu0LLu4F85xf20",
        isInvisible = false,   // set to true if you are solving an invisible Recaptcha V2
        dataSValue = null,     // fill this for a Recaptcha V2 with the "data-s" parameter,
                               // typically found at google.com websites
    )
)

println("Recaptcha g-response token: ${solution.gRecaptchaResponse}")
// In case you need the worker's user-agent
println("User-Agent: ${solution.userAgent}")
```
Also with [proxy](https://anti-captcha.com/apidoc/task-types/RecaptchaV2Task):
```kotlin
import com.anticaptcha.Proxy
import com.anticaptcha.ProxyType

val solution = ac.solveRecaptchaV2ProxyOn(
    RecaptchaV2(
        websiteUrl = "https://www.website.com/",
        websiteKey = "6Lcyu8UZAAAAACwSh6Xf58WrNXTu0LLu4F85xf20",
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
    ),
    Proxy(
        address = "1.2.3.4",
        port = 1234,
        type = ProxyType.HTTP,     // HTTP, SOCKS4 or SOCKS5, HTTP by default
        login = "login-optional",
        password = "pass-optional",
    ),
)
```
&nbsp;

### Solve Recaptcha V2 Enterprise
The same class, `isEnterprise` switches the task type:
```kotlin
val solution = ac.solveRecaptchaV2(
    RecaptchaV2(
        websiteUrl = "https://store.steampowered.com/join",
        websiteKey = "6LdIFr0ZAAAAAO3vz0O0OQrtAefzdJcWQM2TMYQH",
        isEnterprise = true,
        // set to "true" if there's "size":"invisible" option in "grecaptcha.enterprise.render" function
        isInvisible = false,
        // Additional parameters passed to the grecaptcha.enterprise.render call
        enterprisePayload = mapOf("s" to "TEMPORARY_TOKEN_VALUE_IF_PRESENT", "action" to "form_submit"),
        // apiDomain = "recaptcha.net",   // only for a non-google.com script domain
    )
)
```
Use `solveRecaptchaV2ProxyOn` for the proxy-on version.

&nbsp;

### Solve Recaptcha V3
```kotlin
import com.anticaptcha.RecaptchaV3

val solution = ac.solveRecaptchaV3(
    RecaptchaV3(
        websiteUrl = "https://www.website.com/",
        websiteKey = "6LcvNcwdAAAAAMWAuNRXH74u3QePsEzTm6GEjx0J",
        pageAction = "somefun",
        minScore = 0.9,          // one of 0.3, 0.7, 0.9
        // isEnterprise = true,  // set to true for a Recaptcha V3 Enterprise
    )
)

println("Recaptcha g-response token: ${solution.gRecaptchaResponse}")
```
Recaptcha V3 has no proxy-on version.

&nbsp;

### Solve Hcaptcha
```kotlin
import com.anticaptcha.HCaptcha

val solution = ac.solveHCaptcha(
    HCaptcha(
        websiteUrl = "https://www.website.com/",
        websiteKey = "00000000-1111-2222-3333-444444444444",
        // isInvisible = true,
        // isEnterprise = true,
        // hCaptcha Enterprise parameters like rqdata, sentry, apiEndpoint, endpoint,
        // reportapi, assethost, imghost:
        // enterprisePayload = mapOf("rqdata" to "rqdata value from the target website"),
    )
)

println("Hcaptcha Token: ${solution.gRecaptchaResponse}")
// Use this user-agent for the form submission
println("User-Agent: ${solution.userAgent}")
// Optional "respkey" value, you may need it too
println("respkey: ${solution.respKey}")
```
Also with [proxy](https://anti-captcha.com/apidoc/task-types/HCaptchaTask) — `solveHCaptchaProxyOn(settings, proxy)`.

&nbsp;

### Solve FunCaptcha
```kotlin
import com.anticaptcha.FunCaptcha

val solution = ac.solveFunCaptcha(
    FunCaptcha(
        websiteUrl = "https://www.website.com/",
        websitePublicKey = "00000000-1111-2222-3333-444444444444",
        // Make sure to find and set this correctly, look for a URL like
        // https://somewebsite-api.arkoselabs.com/v2/00000000-1111-2222-3333-444444444444/api.js
        apiSubdomain = "somewebsite-api.arkoselabs.com",
        dataBlob = """{"blob":"HERE_COMES_THE_blob_VALUE"}""",
    )
)

println("Funcaptcha Token: ${solution.token}")
```
Also with [proxy](https://anti-captcha.com/apidoc/task-types/FunCaptchaTask) — `solveFunCaptchaProxyOn(settings, proxy)`.

&nbsp;

### Solve Turnstile
```kotlin
import com.anticaptcha.Turnstile

val solution = ac.solveTurnstile(
    Turnstile(
        websiteUrl = "https://www.website.com/",
        websiteKey = "0x4AAAAAAABD2Inoxs-yJ8bz",
        // action = "optional page action",
        // cData = "cdata token for cloudflare",
        // chlPageData = "chlPageData token for cloudflare",
    )
)

println("Turnstile Token: ${solution.token}")
// In case you need the worker's user-agent
println("User-Agent: ${solution.userAgent}")
```
Also with [proxy](https://anti-captcha.com/apidoc/task-types/TurnstileTask) — `solveTurnstileProxyOn(settings, proxy)`.

&nbsp;

### Solve GeeTest
GeeTest has 2 versions, number 3 and 4. Number 3 requires the parameter `challenge`. Number 4 has the optional setting `initParameters`.
```kotlin
import com.anticaptcha.GeeTest

val solution = ac.solveGeeTest(
    GeeTest(
        websiteUrl = "https://bitget.com/",
        gt = "e9ca9c9ca19ad540a8017f5c107b2d0f",

        // Solve GeeTest 4:
        version = 4,
        initParameters = mapOf("riskType" to "slide"),

        // Solve GeeTest 3:
        // version = 3,
        // challenge = "1234567890abcdef1234567890abcdef",
    )
)

// GeeTest v4
println("captcha_id: ${solution.captchaId}")
println("lot_number: ${solution.lotNumber}")
println("pass_token: ${solution.passToken}")
println("gen_time: ${solution.genTime}")
println("captcha_output: ${solution.captchaOutput}")

// GeeTest v3
// println("${solution.challenge} ${solution.seccode} ${solution.validate}")
```
Also with [proxy](https://anti-captcha.com/apidoc/task-types/GeeTestTask) — `solveGeeTestProxyOn(settings, proxy)`.

&nbsp;

### Image to coordinates
```kotlin
import com.anticaptcha.ImageToCoordinates

val solution = ac.solveImageToCoordinatesFile(
    "coordinates.jpg",
    ImageToCoordinates(
        mode = "points",   // "points" or "rectangles"
        comment = "Select objects in the specified order",
    ),
)
// OR ac.solveImageToCoordinates("image-encoded-in-base64", settings)

println("Objects X,Y coordinates: ${solution.coordinates}")
```
&nbsp;

### Solve AntiGate (custom tasks)
```kotlin
import com.anticaptcha.AntiGate

val solution = ac.solveAntiGate(
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
        proxy = Proxy(address = "1.2.3.4", port = 1234),
        // domainsOfInterest = listOf("some-other-domain.com"),
    )
)

println("cookies: ${solution.cookies}")
println("localStorage: ${solution.localStorage}")
println("fingerprint: ${solution.fingerprint}")
println("url: ${solution.url}")
```
&nbsp;

### Get AntiBot cookies
Makes a worker open the page through your proxy and hands you back the anti-bot cookies, so you can reuse them in your own requests. The proxy is required — the cookies are only valid for the IP address they were issued to.
```kotlin
import com.anticaptcha.AntiBotCookie

val solution = ac.solveAntiBotCookie(
    AntiBotCookie(
        websiteUrl = "https://www.somewebsite.com/",
        proxy = Proxy(address = "1.2.3.4", port = 3128, login = "login", password = "password"),
    )
)

// Ready to be sent as a Cookie header
println("Cookie: ${solution.cookieHeader}")
println("User-Agent: ${solution.fingerprintUserAgent}")
```
&nbsp;

### Solve Prosopo
```kotlin
import com.anticaptcha.Prosopo

val solution = ac.solveProsopo(
    Prosopo(websiteUrl = "https://www.website.com/", websiteKey = "sitekey-here")
)

println("Prosopo Token: ${solution.token}")
```
Also with [proxy](https://anti-captcha.com/apidoc/task-types/ProsopoTask) — `solveProsopoProxyOn(settings, proxy)`.

&nbsp;

### Solve Friendly Captcha
```kotlin
import com.anticaptcha.FriendlyCaptcha

val solution = ac.solveFriendlyCaptcha(
    FriendlyCaptcha(websiteUrl = "https://www.website.com/", websiteKey = "sitekey-here")
)

println("Friendly Captcha Token: ${solution.token}")
```
Also with [proxy](https://anti-captcha.com/apidoc/task-types/FriendlyCaptchaTask) — `solveFriendlyCaptchaProxyOn(settings, proxy)`.

&nbsp;

### Solve Amazon WAF
Two options here:

1. When the captcha is at the bot filtering page and you need the `aws-waf-token` cookie:
```kotlin
import com.anticaptcha.Amazon

val solution = ac.solveAmazon(
    Amazon(
        websiteUrl = "https://www.website.com/",
        websiteKey = "key_value_from_window.gokuProps_object",
        iv = "iv_value_from_window.gokuProps_object",
        context = "context_value_from_window.gokuProps_object",
        // captchaScript = "optional_captcha.js_script_url",
        // challengeScript = "optional_challenge.js_script_url",
    )
)

println("aws-waf-token: ${solution.token}")
```

2. When the captcha is a standalone widget triggered by a user's action:
```kotlin
val solution = ac.solveAmazon(
    Amazon(
        websiteUrl = "https://www.website.com/",
        // Captcha widget's API key from the AwsWafCaptcha.renderCaptcha function
        websiteKey = "captcha_key_value",
        wafType = "widget",
        // Full URL to jsapi.js
        jsapiScript = "https://164cb210e333.edge.captcha-sdk.awswaf.com/164cb210e333/jsapi.js",
    )
)
```
Both options have a [proxy-on](https://anti-captcha.com/apidoc/task-types/AmazonTask) version — `solveAmazonProxyOn(settings, proxy)`.

&nbsp;

### Solve Altcha
```kotlin
import com.anticaptcha.Altcha

val solution = ac.solveAltcha(
    Altcha(
        websiteUrl = "https://www.website.com/",

        // Option 1: use the challenge URL (use one of the options!)
        challengeUrl = "/some/path/to/challenge/url",

        // Option 2: use the challenge JSON
        // challengeJson = """{"algorithm":"SHA-256","challenge":"1a40f7ba3393f9513016879de41c7221f14e563856de2f647233a00accf9c28b","salt":"0887f273d79df143355b9e5f","signature":"1de2bbf282420aef6ca0a84c38c85e2b1e40023d28bef72278d735555a8f47fb"}""",
    )
)

println("Altcha Token: ${solution.token}")
```
Also with [proxy](https://anti-captcha.com/apidoc/task-types/AltchaTask) — `solveAltchaProxyOn(settings, proxy)`.

&nbsp;

### Error handling
Every method throws on failure. All exceptions descend from `AnticaptchaException`:

| Class | When |
|---|---|
| `InvalidTaskException` | a required parameter is missing or out of range, nothing was sent |
| `NetworkException` | the API could not be reached |
| `BadResponseException` | the API answered with something unexpected, see `raw` |
| `ApiException` | the API answered with a non-zero `errorId`, see `errorCode` |
| `TaskTimeoutException` | the task was still unsolved when the waiting limit ran out |

`AnticaptchaException` is a sealed class, so `when` over it is exhaustive:

```kotlin
try {
    val solution = ac.solveRecaptchaV2(params)
    println(solution.gRecaptchaResponse)
} catch (error: ApiException) {
    // https://anti-captcha.com/apidoc/errors
    if (error.isRetryable) println("try again later")
    System.err.println("${error.errorCode}: ${error.description}")
} catch (error: AnticaptchaException) {
    System.err.println(error.message)
}
```

### Reading the solution
`Solution` wraps the API's `solution` object. Properties for fields a task type does not fill return `null`:

```kotlin
solution.text                  // image captchas
solution.token                 // FunCaptcha, Turnstile, Prosopo, Friendly Captcha, Altcha, Amazon
solution.gRecaptchaResponse    // Recaptcha, hCaptcha
solution.userAgent             // worker's user-agent
solution.cookies               // AntiGate, AntiBotCookie -> JsonObject?
solution.cookieHeader          // the same cookies as a ready "name=value; ..." string
solution.coordinates           // ImageToCoordinates -> JsonArray?
solution.taskId                // for the report* methods
solution.cost                  // what the task cost, in US dollars
solution.raw                   // the whole JsonObject, for anything not listed above
solution["someNewField"]       // any field by its API name
```

### Other settings
```kotlin
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

val ac = AnticaptchaClient(
    apiKey = "API_KEY_HERE",
    softId = 1187,                                  // earn 10% commission with your app
    verbose = false,
    connectionTimeout = Duration.ofSeconds(30),     // per HTTP call to the API
    firstAttemptWaitingInterval = 5.seconds,        // before the first status request
    normalWaitingInterval = 5.seconds,              // between the following ones
    maxWaitingTime = 300.seconds,                   // before giving up on the task
)
```

A task type this library does not wrap yet can still be sent by hand:
```kotlin
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

val solution = ac.solveTask(
    buildJsonObject {
        put("type", "SomeNewTaskProxyless")
        put("websiteURL", "https://www.website.com/")
    }
)
```

### Running the examples and tests
```bash
git clone https://github.com/anti-captcha/anticaptcha-kotlin.git
cd anticaptcha-kotlin

# unit tests, they need no API key and no network
./gradlew test

# put your API key into src/test/kotlin/com/anticaptcha/examples/Examples.kt first
./gradlew runExample -Pexample=image
```
Run `./gradlew runExample` without a name to see the full list.

### Publishing
```bash
# builds every artifact, signs it and packs the Central Portal bundle
./gradlew centralBundle
```
The task prints the upload command at the end. The account, namespace and GPG setup is described in [maven_instructions.md](https://github.com/anti-captcha/anticaptcha-java/blob/master/maven_instructions.md) of the Java library (in Russian) — it is the same Central Portal account and the same key.
