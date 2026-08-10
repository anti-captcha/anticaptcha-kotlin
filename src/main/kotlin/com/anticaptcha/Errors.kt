package com.anticaptcha

/**
 * Base class of every error this library throws.
 */
public sealed class AnticaptchaException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * A task is missing a required parameter, or a value is out of range.
 * Nothing was sent to the API.
 */
public class InvalidTaskException(message: String) : AnticaptchaException(message)

/**
 * The API could not be reached.
 */
public class NetworkException(
    message: String,
    cause: Throwable? = null,
) : AnticaptchaException(message, cause)

/**
 * The API answered with something that is not the JSON we expect.
 *
 * @property raw first 500 characters of what the API actually sent
 */
public class BadResponseException(
    message: String,
    public val raw: String? = null,
) : AnticaptchaException(if (raw == null) message else "$message; raw response: $raw")

/**
 * The API answered with a non-zero `errorId`.
 *
 * See [the error list](https://anti-captcha.com/apidoc/errors).
 */
public class ApiException(
    public val errorId: Int,
    public val errorCode: String,
    public val description: String,
) : AnticaptchaException("API error $errorId $errorCode: $description") {

    /**
     * True when retrying the very same task could still succeed.
     */
    public val isRetryable: Boolean
        get() = errorCode in RETRYABLE

    private companion object {
        private val RETRYABLE = setOf(
            "ERROR_NO_SLOT_AVAILABLE",
            "ERROR_SERVICE_OVERLOAD",
            "ERROR_RATE_LIMIT",
            "ERROR_CAPTCHA_UNSOLVABLE",
        )
    }
}

/**
 * The task was still unsolved when the waiting limit ran out.
 */
public class TaskTimeoutException(seconds: Long) :
    AnticaptchaException("the task was not solved in $seconds seconds")
