package nsync.ui.rest

import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Gson.auto
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import java.time.Duration
import java.time.Instant

data class Status(val uptimeMins: Long)
data class FolderRequest(val localUri: String, val remoteUri: String)
data class FolderResponse(val uid: String, val localUri: String, val remoteUri: String)
data class ErrorResponse(val message: String?)

class WebServer(val port: Int) {
    companion object : KLogging()

    private val startTime = Instant.now()
    private val uptimeMins: Long
        get() = Duration.between(this.startTime, Instant.now()).toMinutes()
    private val statusLens = Body.auto<Status>().toLens()
    private val folderRequestLens = Body.auto<FolderRequest>().toLens()
    private val folderResponseLens = Body.auto<FolderResponse>().toLens()
    private val errorResponseLens = Body.auto<ErrorResponse>().toLens()

    private val apiHandler = routes(
            "/admin" bind routes(
                    "/status" bind Method.GET to this::ping,
                    "/shutdown" bind Method.POST to this::stop
            ),
            "/rest" bind routes(
                    "/folders" bind Method.POST to this::addSyncFolder
            )
    )
    private var server: Http4kServer? = null

    private fun ping(req: Request): Response {
        logger.info { "Status request" }
        return statusLens.inject(Status(uptimeMins), Response(OK))
    }

    private fun stop(req: Request): Response = runBlocking<Response> {
        logger.info { "Shutdown request" }
        Response(OK)
//        ui.send(StopCmd())
//        statusLens.inject(Status(uptimeMins), Response(OK))
    }

    private fun addSyncFolder(req: Request): Response = runBlocking {
        logger.info { "Add folder request" }
        Response(OK)
//        val cmd = folderRequestLens.extract(req).toAppCommand()
//        ui.send(cmd)
//        cmd.onResult {
//            when (it) {
//                is Success -> folderResponseLens.inject(FolderResponse(
//                        it.value.folderId, it.value.localFolder, it.value.remoteFolder), Response(OK))
//                is Failure -> errorResponseLens.inject(ErrorResponse(it.message), Response(BAD_REQUEST))
//            }
//        }
    }

    fun start() {
        logger.info { "Starting REST server on port $port" }
        server = apiHandler.asServer(Netty(port)).start()
    }

    fun stop() {
        logger.info { "Stopping REST server" }
        server?.stop()
    }
}