package ui.rest

import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import nsync.KernelFacade
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import java.time.Duration
import java.time.Instant


/*
 * REST API
 *
 * /server -> provide access to server operations
 * /filesystem -> provide access to filesystem operations
 */
class WebServer(private val port: Int, private val kernel: KernelFacade) {
    companion object : KLogging()

    private val apiHandler = routes(
            "/nsync" bind routes(
                    "/server" bind Method.GET to this::ping,
                    "/server" bind Method.DELETE to this::stop,
                    "/filesystems" bind routes(
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

    private fun addFS(req: Request): Response = runBlocking {
        logger.info { "Add folder request" }
        val folderReq: FSBody = parseRequest(req)
        kernel.addFS(folderReq.localUri, folderReq.remoteUri)
        Response(ACCEPTED)
    }
}