package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.RightError
import de.zbw.lori.model.BookmarkIdsRest
import de.zbw.lori.model.ErrorRest
import de.zbw.lori.model.RightIdCreated
import de.zbw.lori.model.RightIdsRest
import de.zbw.lori.model.RightRest
import de.zbw.lori.model.TemplateApplicationRest
import de.zbw.lori.model.TemplateApplicationsRest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import org.postgresql.util.PSQLException

/**
 * REST-API routes for templates.
 *
 * Created on 04-18-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.templateRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/template") {
        authenticate("auth-login") {
            post {
                val span: Span =
                    tracer.spanBuilder("lori.LoriService.POST/api/v1/template").setSpanKind(SpanKind.SERVER).startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val right: RightRest = call.receive(RightRest::class)
                        span.setAttribute("template", right.toString())
                        val pk: String = backend.insertTemplate(right.toBusiness())
                        span.setStatus(StatusCode.OK)
                        call.respond(
                            HttpStatusCode.Created, RightIdCreated(rightId = pk)
                        )
                    } catch (pe: PSQLException) {
                        if (pe.sqlState == ApiError.PSQL_CONFLICT_ERR_CODE) {
                            span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                            call.respond(
                                HttpStatusCode.Conflict,
                                ApiError.conflictError(
                                    detail = "Ein Template mit diesem Namen existiert bereits.",
                                )
                            )
                        } else {
                            span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiError.internalServerError(
                                    detail = "Ein interner Datenbankfehler ist aufgetreten.",
                                )
                            )
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError.internalServerError(
                                detail = "Ein interner Fehler ist aufgetreten.",
                            ),
                        )
                    } finally {
                        span.end()
                    }
                }
            }
            /**
             * Update an existing Template.
             */
            put {
                val span =
                    tracer.spanBuilder("lori.LoriService.PUT/api/v1/template").setSpanKind(SpanKind.SERVER).startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val right: RightRest =
                            call.receive(RightRest::class)
                                .takeIf { it.rightId != null && it.isTemplate && it.templateName != null }
                                ?: throw BadRequestException("Invalid Json has been provided")
                        span.setAttribute("template", right.toString())
                        val insertedRows = backend.upsertRight(right.toBusiness())
                        if (insertedRows == 1) {
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            span.setStatus(StatusCode.ERROR)
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiError.notFoundError(
                                    detail = "Für das Template mit Id ${right.rightId} existiert kein Eintrag.",
                                )
                            )
                        }
                    } catch (pe: PSQLException) {
                        if (pe.sqlState == ApiError.PSQL_CONFLICT_ERR_CODE) {
                            span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                            call.respond(
                                HttpStatusCode.Conflict,
                                ApiError.conflictError(
                                    detail = "Ein Template mit diesem Namen existiert bereits.",
                                )
                            )
                        } else {
                            span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiError.internalServerError(
                                    detail = "Ein interner Datenbankfehler ist aufgetreten.",
                                )
                            )
                        }
                    } catch (e: BadRequestException) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(
                                detail = "Das JSON Format ist ungültig und konnte nicht gelesen werden.",
                            ),
                        )
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError.internalServerError(),
                        )
                    } finally {
                        span.end()
                    }
                }
            }
        }
    }
    route("/api/v1/template") {
        authenticate("auth-login") {
            /**
             * Apply given templates.
             */
            post("/applications") {
                val span =
                    tracer.spanBuilder("lori.LoriService.POST/api/v1/template/applications")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val rightIds: List<String> =
                            call.receive(RightIdsRest::class).takeIf { it.rightIds != null }?.rightIds
                                ?: throw BadRequestException("Invalid Json has been provided")
                        val all: Boolean = call.request.queryParameters["all"]?.toBoolean() ?: false
                        span.setAttribute("rightIds", rightIds.toString())
                        span.setAttribute("Query Parameter 'all'", all.toString())
                        val appliedMap: Map<String, Pair<List<String>, List<RightError>>> =
                            if (all) {
                                backend.applyAllTemplates()
                            } else {
                                backend.applyTemplates(rightIds)
                            }
                        val result = TemplateApplicationsRest(
                            templateApplication =
                            appliedMap.entries.map { e: Map.Entry<String, Pair<List<String>, List<RightError>>> ->
                                TemplateApplicationRest(
                                    rightId = e.key,
                                    metadataIds = e.value.first,
                                    errors = e.value.second.map { it.toRest() },
                                    numberOfAppliedEntries = e.value.first.size
                                )
                            }
                        )
                        call.respond(result)
                    } catch (e: BadRequestException) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(
                                detail = "Das JSON Format ist ungültig und konnte nicht gelesen werden.",
                            ),
                        )
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError.internalServerError(),
                        )
                    } finally {
                        span.end()
                    }
                }
            }

            post("{id}/bookmarks") {
                val span =
                    tracer.spanBuilder("lori.LoriService.POST/api/v1/template/{id}/bookmarks")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val rightId = call.parameters["id"]
                        val deleteOld: Boolean = call.request.queryParameters["deleteOld"]?.toBoolean() ?: false
                        val bookmarkIds = call.receive(BookmarkIdsRest::class).bookmarkIds
                        span.setAttribute("rightId", rightId ?: "null")
                        span.setAttribute("deleteOld", deleteOld)
                        span.setAttribute("bookmarkIds", bookmarkIds?.toString() ?: "null")
                        if (rightId == null || bookmarkIds == null) {
                            span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                            call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                        } else {
                            if (deleteOld) {
                                backend.deleteBookmarkTemplatePairsByRightId(rightId)
                            }
                            val generatedPairs: List<BookmarkTemplate> = backend.upsertBookmarkTemplatePairs(
                                bookmarkIds.map {
                                    BookmarkTemplate(
                                        bookmarkId = it,
                                        rightId = rightId,
                                    )
                                }
                            )
                            call.respond(
                                HttpStatusCode.Created, generatedPairs.map { it.toRest() }
                            )
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError.internalServerError(
                                detail = "Ein interner Fehler ist aufgetreten.",
                            ),
                        )
                    }
                }
            }

            /**
             * Return all bookmarks connected to a given RightId.
             */
            get("{id}/bookmarks") {
                val span =
                    tracer.spanBuilder("lori.LoriService.GET/api/v1/template/{id}/bookmarks")
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
                            val bookmarks: List<Bookmark> = backend.getBookmarksByRightId(rightId)
                            span.setStatus(StatusCode.OK)
                            call.respond(bookmarks.map { it.toRest() })
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError.internalServerError(),
                        )
                    } finally {
                        span.end()
                    }
                }
            }

            /**
             * Return Template for a given RightId.
             */
            get("{id}") {
                val span =
                    tracer.spanBuilder("lori.LoriService.GET/api/v1/template/{id}").setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val rightId = call.parameters["id"]
                        span.setAttribute("rightId", rightId ?: "null")
                        if (rightId == null) {
                            span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                            call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                        } else {
                            val right: ItemRight? = backend.getRightById(rightId)
                            right?.let {
                                span.setStatus(StatusCode.OK)
                                call.respond(right.toRest())
                            } ?: let {
                                span.setStatus(StatusCode.ERROR)
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiError.notFoundError(
                                        detail = "Für das Template mit Id: $rightId existiert kein Eintrag.",
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError.internalServerError(),
                        )
                    } finally {
                        span.end()
                    }
                }
            }

            /**
             * Delete Template by RightId.
             */
            delete("{id}") {
                val span =
                    tracer.spanBuilder("lori.LoriService.DELETE/api/v1/template/{id}").setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val rightId = call.parameters["id"]
                        span.setAttribute("templateId", rightId ?: "null")
                        if (rightId == null) {
                            span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiError.badRequestError(
                                    detail = "Keine valide numerische Id wurde übergeben",
                                ),
                            )
                        } else {
                            val entriesDeleted = backend.deleteRight(rightId)
                            if (entriesDeleted == 1) {
                                span.setStatus(StatusCode.OK)
                                call.respond(HttpStatusCode.OK)
                            } else {
                                span.setStatus(StatusCode.ERROR)
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiError.notFoundError(
                                        detail = "Für die TemplateId $rightId existiert kein Eintrag.",
                                    ),
                                )
                            }
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError.internalServerError(
                                detail = "Ein interner Datenbankfehler ist aufgetreten.",
                            ),
                        )
                    } finally {
                        span.end()
                    }
                }
            }
        }

        route("/list") {
            get {
                val span =
                    tracer.spanBuilder("lori.LoriService.GET/api/v1/template/list").setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val limit: Int = call.request.queryParameters["limit"]?.toInt() ?: 100
                        val offset: Int = call.request.queryParameters["offset"]?.toInt() ?: 0
                        if (limit < 1 || limit > 200) {
                            span.setStatus(
                                StatusCode.ERROR, "BadRequest: Limit parameter is expected to be between 1 and 200."
                            )
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorRest(
                                    type = "/errors/badrequest",
                                    title = "Ungültiger Query Parameter.",
                                    detail = "Der Limit Parameter muss zwischen 1 und 500 sein..",
                                    status = "400",
                                ),
                            )
                            return@withContext
                        }
                        val receivedTemplates: List<ItemRight> = backend.getTemplateList(limit, offset)
                        span.setStatus(StatusCode.OK)
                        call.respond(receivedTemplates.map { it.toRest() })
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorRest(
                                type = "/errors/internalservererror",
                                title = "Unerwarteter Fehler.",
                                detail = "Ein interner Fehler ist aufgetreten.",
                                status = "500",
                            ),
                        )
                    } finally {
                        span.end()
                    }
                }
            }
        }
    }
}
