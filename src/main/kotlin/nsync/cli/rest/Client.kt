package nsync.cli.rest


import nsync.SyncFolder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("admin/status")
    fun status(): Call<Status>

    @POST("admin/shutdown")
    fun shutdown(): Call<Status>

    @POST("rest/folders")
    fun addFolder(@Body folder: FolderRequest): Call<FolderResponse>

    companion object Factory {
        fun create(url: String): ApiService {
            return Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(OkHttpClient())
                    .baseUrl(url)
                    .build().create(ApiService::class.java);
        }
    }
}

class Client(port: Int) {
    private val api: ApiService = ApiService.create("http://localhost:$port")

    private fun <T> execute(call: Call<T>, onSuccess: (T) -> Unit) {
        try {
            val resp = call.execute()
            if (!resp.isSuccessful) {
                System.err.println("Server returned an error!")
            } else {
                onSuccess(resp.body()!!)
            }
        } catch (err: Exception) {
            System.err.println("Error executing command: ${err.message}")
        }
    }

    fun status() {
        this.execute(this.api.status(), {
            print("Server is running. Uptime: ${it.uptimeMins} minutes")
        })
    }

    fun shutdown() {
        this.execute(this.api.shutdown(), {
            print("Server is shutting down. Uptime: ${it.uptimeMins} minutes")
        })
    }

    fun addFolder(localUri: String, remoteUri: String) {
        this.execute(this.api.addFolder(FolderRequest(localUri, remoteUri)), {
            print("Sync folder created. Uid: ${it.uid}")
        })
    }
}