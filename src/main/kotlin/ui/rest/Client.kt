package ui.rest


import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import commons.Failure
import commons.Result
import commons.Success

interface ApiService {
    @GET("nsync/server")
    fun status(): Call<Status>

    @DELETE("nsync/server")
    fun shutdown(): Call<Status>

    @POST("nsync/filesystems")
    fun addFS(@Body folder: FSBody): Call<String>

    companion object Factory {
        fun create(url: String): ApiService {
            return Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(OkHttpClient())
                    .baseUrl(url)
                    .build().create(ApiService::class.java)
        }
    }
}

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