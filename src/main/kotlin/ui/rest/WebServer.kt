package ui.rest

import commons.NoResponseException
import interactors.AddFsCommand
import interactors.ListFsCommand
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import nsync.KernelFacade
import okhttp3.OkHttpClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import java.time.Duration
import java.time.Instant

/*
 * REST API
 *
 * /server -> provide access to server operations
 * /filesystem -> provide access to filesystem operations
 */
interface ApiService {
    @GET("nsync/server")
    fun status(): Call<Status>

    @DELETE("nsync/server")
    fun shutdown(): Call<Status>

    @GET("nsync/filesystems")
    fun listFS(): Call<Collection<FSBody>>

    @POST("nsync/filesystems")
    fun addFS(@Body folder: FSBody): Call<FSBody>

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


class WebServer(private val port: Int, private val kernel: KernelFacade) {
    companion object : KLogging()

    private val apiHandler = routes(
            "/nsync" bind routes(
                    "/server" bind Method.GET to this::ping,
                    "/server" bind Method.DELETE to this::stop,
                    "/filesystems" bind routes(
                            "/" bind Method.GET to this::listFS,
                            "/" bind Method.POST to this::addFS
                    )
            )
    )

    private val startTime = Instant.now()
    private val uptimeMins: Long
        get () = Duration.between(this.startTime, Instant.now()).toMinutes()

    private var server: Http4kServer? = null

    fun start() {
        logger.info { "Starting REST server on port $port" }
        server = apiHandler.asServer(Netty(port)).start()
    }

    fun stop() {
        logger.info { "Stopping REST server" }
        server?.stop()
    }

    private fun ping(req: Request): Response {
        logger.info { "Status request" }
        return toResponse(Status(uptimeMins))
    }

    private fun stop(req: Request): Response = runBlocking<Response> {
        logger.info { "Shutdown request" }
        kernel.stop()
        stop()
        toResponse(Status(uptimeMins))
    }

    private fun listFS(req: Request): Response = runBlocking {
        logger.info { "List FS request" }
        try {
            val filesystems = ListFsCommand(kernel)()
            val body = filesystems.map { it ->
                FSBody(
                        id = it.identifier.toString(),
                        localUri = it.localFolder.toString(),
                        remoteUri = it.remoteFolder.toString()
                )
            }.toList()
            toResponse(body)
        } catch (err: NoResponseException) {
            logger.warn { "Unable to list FS due NoResponse error" }
            Response(BAD_REQUEST)
        }
    }

    private fun addFS(req: Request): Response = runBlocking {
        logger.info { "Add FS request" }
        val folderReq: FSBody = parseRequest(req)
        try {
            val id = AddFsCommand(kernel)(folderReq.localUri, folderReq.remoteUri)
            toResponse(FSBody(id = id.toString(), localUri = folderReq.localUri, remoteUri = folderReq.remoteUri))
        } catch (err: NoResponseException) {
            logger.warn { "Unable to list FS due NoResponse error" }
            Response(BAD_REQUEST)
        }
    }
}