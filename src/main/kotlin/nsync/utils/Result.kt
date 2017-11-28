package nsync.utils

sealed class Result<T> {
    fun then(block: (Result<T>) -> Unit): Result<T> {
        block(this)
        return this
    }
}

data class Failure<T>(val error: Exception) : Result<T>() {
    val message: String? = error.message
}

data class Success<T>(val value: T) : Result<T>()
