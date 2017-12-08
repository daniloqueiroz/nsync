package ui.rest

import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Gson.auto


inline fun <reified T: Any> parseRequest(req: Request): T {
    val lens = Body.auto<T>().toLens()
    return lens.extract(req)
}

inline fun <reified T: Any> toResponse(value: T, resp: Response = Response(OK)): Response {
    val lens = Body.auto<T>().toLens()
    return lens.inject(value, resp)
}


data class Status(val uptimeMins: Long)
data class FSBody(val id: String? = null, val localUri: String, val remoteUri: String)
