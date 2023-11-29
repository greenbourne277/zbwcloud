package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.UserRole
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_SESSIONS
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import io.opentelemetry.api.trace.Tracer
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.util.UUID

/**
 * Execute SQL queries strongly related to user.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class UserDB(
    val connection: Connection,
    private val tracer: Tracer,
) {
    fun deleteSessionById(sessionID: String) {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_SESSION_BY_ID).apply {
            this.setString(1, sessionID)
        }
        val span = tracer.spanBuilder("deleteSessionById").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun insertSession(session: Session): String {
        val prepStmt = connection.prepareStatement(STATEMENT_INSERT_SESSION, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, UUID.randomUUID().toString())
            this.setBoolean(2, session.authenticated)
            this.setIfNotNull(3, session.firstName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(4, session.lastName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(5, session.role.toString()) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setTimestamp(6, Timestamp.from(session.validUntil))
        }

        val span = tracer.spanBuilder("insertSession").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun getSessionById(sessionId: String): Session? {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_SESSION_BY_ID).apply {
            this.setString(1, sessionId)
        }
        val span = tracer.spanBuilder("getSessionById").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        return if (rs.next()) {
            Session(
                sessionID = rs.getString(1),
                authenticated = rs.getBoolean(2),
                firstName = rs.getString(3),
                lastName = rs.getString(4),
                role = UserRole.valueOf(
                    rs.getString(5),
                ),
                validUntil = rs.getTimestamp(6).toInstant(),
            )
        } else null
    }

    companion object {
        const val STATEMENT_INSERT_SESSION = "INSERT INTO $TABLE_NAME_SESSIONS" +
            "(session_id,authenticated,first_name," +
            "last_name,role,valid_until) " +
            "VALUES(?,?,?," +
            "?,?::role_enum,?)"

        const val STATEMENT_GET_SESSION_BY_ID =
            "SELECT session_id,authenticated,first_name," +
                "last_name,role,valid_until " +
                "FROM $TABLE_NAME_SESSIONS " +
                "WHERE session_id=?"

        const val STATEMENT_DELETE_SESSION_BY_ID = "DELETE " +
            "FROM $TABLE_NAME_SESSIONS i " +
            "WHERE i.session_id = ?"
    }
}
