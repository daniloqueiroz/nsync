package ui.rest


import commons.Failure
import commons.Result
import commons.Success
import retrofit2.Call

class Client(port: Int) {
    val api: ApiService = ApiService.create("http://localhost:$port")

    operator fun <T> invoke(call: Call<T>): Result<T> {
        try {
            val resp = call.execute()
            return if (resp.isSuccessful) {
                Success(resp.body()!!)
            } else {
                Failure<T>(Exception("Unexpected response: ${resp.code()}"))
            }
        } catch (err: Exception) {
            return Failure<T>(err)
        }
    }
}