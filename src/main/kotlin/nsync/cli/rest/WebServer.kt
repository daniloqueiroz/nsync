package nsync.cli.rest

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import nsync.SyncFolder
import nsync.app.AddSyncFolderCmd
import nsync.app.AppCommand
import nsync.app.StopCmd
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Gson.auto
import java.time.Duration
import java.time.Instant

data class Status(val uptimeMins: Long)
data class FolderRequest(val localUri: String, val remoteUri: String) {
    fun toAppCommand(): AddSyncFolderCmd {
        return AddSyncFolderCmd(this.localUri, this.remoteUri)
    }
}
data class FolderResponse(val uid: String, val localUri: String, val remoteUri: String)

class WebServer(val port: Int, val application: Channel<AppCommand>) {
    companion object : KLogging()

    private val startTime = Instant.now()
    private val uptimeMins: Long
        get() = Duration.between(this.startTime, Instant.now()).toMinutes()
    private val statusLens = Body.auto<Status>().toLens()
    private val folderRequestLens = Body.auto<FolderRequest>().toLens()
    private val folderResponseLens = Body.auto<FolderResponse>().toLens()

    private val apiHandler = routes(
            "/admin" bind routes(
                    "/status" to Method.GET bind this::ping,
                    "/shutdown" to Method.POST bind this::stop
            ),
            "/rest" bind routes(
                    "/folders" to Method.POST bind this::addSyncFolder
            )
    )
    private var server: Http4kServer? = null

    private fun ping(req: Request): Response {
        logger.info { "Status request" }
        return statusLens.inject(Status(uptimeMins), Response(OK))
    }

    private fun stop(req: Request): Response = runBlocking<Response> {
        logger.info { "Shutdown request" }
        application.send(StopCmd())
        statusLens.inject(Status(uptimeMins), Response(OK))
    }

    private fun addSyncFolder(req: Request): Response = runBlocking<Response> {
        logger.info { "Add folder request" }
        val cmd = folderRequestLens.extract(req).toAppCommand()
        application.send(cmd)
        val sync = cmd.outbox.receive()
        folderResponseLens.inject(FolderResponse(sync.uid, sync.localFolder, sync.remoteFolder), Response(OK))
    }

    fun start() {
        logger.info { "Starting REST server on port ${port}" }
        server = apiHandler.asServer(Netty(port)).start()
    }

    fun stop() {
        logger.info { "Stopping REST server" }
        server?.stop()
    }
}