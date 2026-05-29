package online.coffemaniavpn.client.ktx

import kotlin.coroutines.Continuation

fun <T> Continuation<T>.tryResume(value: T) {
    try {
        resumeWith(Result.success(value))
    } catch (_: IllegalStateException) {
    }
}

fun <T> Continuation<T>.tryResumeWithException(exception: Throwable) {
    try {
        resumeWith(Result.failure(exception))
    } catch (_: IllegalStateException) {
    }
}
