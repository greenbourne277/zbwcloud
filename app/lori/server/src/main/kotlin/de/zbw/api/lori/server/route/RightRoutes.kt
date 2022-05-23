package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.ItemRight
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.RightRest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext

/**
 * REST-API routes for rights.
 *
 * Created on 04-01-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.rightRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/right") {
        get("{id}") {
            val span = tracer
                .spanBuilder("lori.LoriService.GET/api/v1/right/{id}")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val rightId = call.parameters["id"]
                    span.setAttribute("rightId", rightId ?: "null")
                    if (rightId == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                    } else {
                        val rightElements: List<ItemRight> = backend.getRightsByIds(listOf(rightId))
                        rightElements.takeIf { it.isNotEmpty() }?.let {
                            span.setStatus(StatusCode.OK)
                            call.respond(rightElements.first().toRest())
                        } ?: let {
                            span.setStatus(StatusCode.ERROR)
                            call.respond(HttpStatusCode.NotFound, "No item found for given id.")
                        }
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                } finally {
                    span.end()
                }
            }
        }

        post {
            val span = tracer
                .spanBuilder("lori.LoriService.POST/api/v1/right")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val right: RightRest =
                        call.receive(RightRest::class)
                            .takeIf { it.rightId != null }
                            ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("right", right.toString())
                    if (backend.rightContainsId(right.rightId)) {
                        span.setStatus(StatusCode.ERROR, "Conflict: Resource with this id already exists.")
                        call.respond(HttpStatusCode.Conflict, "Resource with this id already exists.")
                    } else {
                        backend.insertRight(right.toBusiness())
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.Created)
                    }
                } catch (e: BadRequestException) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, "Invalid input")
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                } finally {
                    span.end()
                }
            }
        }

        put {
            val span = tracer
                .spanBuilder("lori.LoriService.PUT/api/v1/right")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val right: RightRest =
                        call.receive(RightRest::class)
                            .takeIf { it.rightId != null }
                            ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("right", right.toString())
                    if (backend.rightContainsId(right.rightId)) {
                        backend.upsertRight(right.toBusiness())
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        backend.insertRight(right.toBusiness())
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.Created)
                    }
                } catch (e: BadRequestException) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, "Invalid input")
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                } finally {
                    span.end()
                }
            }
        }

        delete("{id}") {
            val span = tracer
                .spanBuilder("lori.LoriService.DELETE/api/v1/right")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val rightId = call.parameters["id"]
                    span.setAttribute("rightId", rightId ?: "null")
                    if (rightId == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                    } else if (backend.itemContainsRight(rightId)) {
                        span.setStatus(
                            StatusCode.ERROR,
                            "Conflict: Right id $rightId is still in use. Can't be deleted."
                        )
                        call.respond(HttpStatusCode.Conflict, "The right id is still in use and can't be deleted.")
                    } else {
                        backend.deleteRight(rightId)
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.OK)
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                } finally {
                    span.end()
                }
            }
        }
    }
}
