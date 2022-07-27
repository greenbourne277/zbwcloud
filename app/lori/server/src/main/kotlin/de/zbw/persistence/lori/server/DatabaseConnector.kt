package de.zbw.persistence.lori.server

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.business.lori.server.AccessState
import de.zbw.business.lori.server.BasisAccessState
import de.zbw.business.lori.server.BasisStorage
import de.zbw.business.lori.server.ItemMetadata
import de.zbw.business.lori.server.ItemRight
import de.zbw.business.lori.server.PublicationType
import io.opentelemetry.api.trace.Tracer
import java.sql.Connection
import java.sql.Date
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Connector for interacting with the postgres database.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnector(
    val connection: Connection,
    private val tracer: Tracer,
) {

    constructor(
        config: LoriConfiguration,
        tracer: Tracer,
    ) : this(
        DriverManager.getConnection(config.sqlUrl, config.sqlUser, config.sqlPassword),
        tracer
    )

    fun itemContainsMetadata(metadataId: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_ITEM_CONTAINS_METADATA).apply {
            this.setString(1, metadataId)
        }
        val span = tracer.spanBuilder("itemContainsMetadata").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun itemContainsEntry(metadataId: String, rightId: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_ITEM_CONTAINS_ENTRY).apply {
            this.setString(1, metadataId)
            this.setString(2, rightId)
        }
        val span = tracer.spanBuilder("itemContainsEntry").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun itemContainsRight(rightId: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_ITEM_CONTAINS_RIGHT).apply {
            this.setString(1, rightId)
        }
        val span = tracer.spanBuilder("itemContainsRight").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun insertItem(metadataId: String, rightId: String): String {
        val prepStmt = connection.prepareStatement(STATEMENT_INSERT_ITEM, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, metadataId)
            this.setString(2, rightId)
        }

        val span = tracer.spanBuilder("insertItem").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = prepStmt.run { this.executeUpdate() }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun upsertMetadataBatch(itemMetadatas: List<ItemMetadata>): IntArray {
        val prep = connection.prepareStatement(STATEMENT_UPSERT_METADATA)
        connection.autoCommit = false
        itemMetadatas.map {
            val p = insertUpsertMetadataSetParameters(it, prep)
            p.addBatch()
        }
        val span = tracer.spanBuilder("upsertMetadataBatch").startSpan()
        try {
            span.makeCurrent()
            val rows: IntArray = prep.executeBatch()
            connection.commit()
            connection.autoCommit = true
            return rows
        } finally {
            span.end()
        }
    }

    fun insertMetadata(itemMetadata: ItemMetadata): String {
        val prepStmt = insertUpsertMetadataSetParameters(
            itemMetadata,
            connection.prepareStatement(STATEMENT_INSERT_METADATA, Statement.RETURN_GENERATED_KEYS),
        )

        val span = tracer.spanBuilder("insertMetadata").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = prepStmt.run { this.executeUpdate() }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    private fun insertUpsertMetadataSetParameters(
        itemMetadata: ItemMetadata,
        prep: PreparedStatement,
    ): PreparedStatement {
        val now = Instant.now()
        return prep.apply {
            this.setString(1, itemMetadata.metadataId)
            this.setString(2, itemMetadata.handle)
            this.setIfNotNull(3, itemMetadata.ppn) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(4, itemMetadata.ppnEbook) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setString(5, itemMetadata.title)
            this.setIfNotNull(6, itemMetadata.titleJournal) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(7, itemMetadata.titleSeries) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setInt(8, itemMetadata.publicationYear)
            this.setIfNotNull(9, itemMetadata.band) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setString(10, itemMetadata.publicationType.toString())
            this.setIfNotNull(11, itemMetadata.doi) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(12, itemMetadata.serialNumber) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(13, itemMetadata.isbn) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(14, itemMetadata.rightsK10plus) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(15, itemMetadata.paketSigel) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(16, itemMetadata.zbdId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(17, itemMetadata.issn) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setTimestamp(18, Timestamp.from(now))
            this.setTimestamp(19, Timestamp.from(now))
            this.setIfNotNull(20, itemMetadata.createdBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(21, itemMetadata.lastUpdatedBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(22, itemMetadata.author) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(23, itemMetadata.collectionName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(24, itemMetadata.communityName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setTimestamp(25, Timestamp.from(itemMetadata.storageDate.toInstant()))
        }
    }

    fun insertRight(right: ItemRight): String {
        val prepStmt =
            insertRightSetParameters(
                right,
                connection.prepareStatement(STATEMENT_INSERT_RIGHT, Statement.RETURN_GENERATED_KEYS)
            )
        val span = tracer.spanBuilder("insertRight").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = prepStmt.run { this.executeUpdate() }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun upsertRight(right: ItemRight): Int {
        val prepStmt =
            upsertRightSetParameters(
                right,
                connection.prepareStatement(STATEMENT_UPSERT_RIGHT)
            )
        val span = tracer.spanBuilder("upsertRight").startSpan()
        try {
            span.makeCurrent()
            return prepStmt.run { this.executeUpdate() }
        } finally {
            span.end()
        }
    }

    private fun upsertRightSetParameters(
        right: ItemRight,
        prep: PreparedStatement,
    ): PreparedStatement {
        val now = Instant.now()

        return prep.apply {
            this.setString(1, right.rightId)
            this.setTimestamp(2, Timestamp.from(now))
            this.setTimestamp(3, Timestamp.from(now))
            this.setIfNotNull(4, right.createdBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(5, right.lastUpdatedBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(6, right.accessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(7, right.startDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(8, right.endDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(9, right.notesGeneral) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(10, right.licenceContract) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(11, right.authorRightException) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(12, right.zbwUserAgreement) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(13, right.openContentLicence) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(14, right.nonStandardOpenContentLicenceURL) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(15, right.nonStandardOpenContentLicence) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(16, right.restrictedOpenContentLicence) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(17, right.notesFormalRules) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(18, right.basisStorage) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(19, right.basisAccessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(20, right.notesProcessDocumentation) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(21, right.notesManagementRelated) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
    }

    private fun insertRightSetParameters(
        right: ItemRight,
        prep: PreparedStatement,
    ): PreparedStatement {
        val now = Instant.now()

        return prep.apply {
            this.setTimestamp(1, Timestamp.from(now))
            this.setTimestamp(2, Timestamp.from(now))
            this.setIfNotNull(3, right.createdBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(4, right.lastUpdatedBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(5, right.accessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(6, right.startDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(7, right.endDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(8, right.notesGeneral) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(9, right.licenceContract) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(10, right.authorRightException) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(11, right.zbwUserAgreement) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(12, right.openContentLicence) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(13, right.nonStandardOpenContentLicenceURL) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(14, right.nonStandardOpenContentLicence) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(15, right.restrictedOpenContentLicence) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(16, right.notesFormalRules) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(17, right.basisStorage) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(18, right.basisAccessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(19, right.notesProcessDocumentation) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(20, right.notesManagementRelated) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
    }

    fun getMetadata(metadataIds: List<String>): List<ItemMetadata> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_METADATA).apply {
            this.setArray(1, connection.createArrayOf("text", metadataIds.toTypedArray()))
        }

        val span = tracer.spanBuilder("getMetadata").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        return generateSequence {
            if (rs.next()) {
                ItemMetadata(
                    metadataId = rs.getString(1),
                    handle = rs.getString(2),
                    ppn = rs.getString(3),
                    ppnEbook = rs.getString(4),
                    title = rs.getString(5),
                    titleJournal = rs.getString(6),
                    titleSeries = rs.getString(7),
                    publicationYear = rs.getInt(8),
                    band = rs.getString(9),
                    publicationType = PublicationType.valueOf(rs.getString(10)),
                    doi = rs.getString(11),
                    serialNumber = rs.getString(12),
                    isbn = rs.getString(13),
                    rightsK10plus = rs.getString(14),
                    paketSigel = rs.getString(15),
                    zbdId = rs.getString(16),
                    issn = rs.getString(17),
                    createdOn = rs.getTimestamp(18)?.toOffsetDateTime(),
                    lastUpdatedOn = rs.getTimestamp(19)?.toOffsetDateTime(),
                    createdBy = rs.getString(20),
                    lastUpdatedBy = rs.getString(21),
                    author = rs.getString(22),
                    collectionName = rs.getString(23),
                    communityName = rs.getString(24),
                    storageDate = rs.getTimestamp(25).toOffsetDateTime(),
                )
            } else null
        }.takeWhile { true }.toList()
    }

    fun countItemByRightId(rightId: String): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_COUNT_ITEM_BY_RIGHTID).apply {
            this.setString(1, rightId)
        }
        val span = tracer.spanBuilder("countItemByRightId").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.run { this.executeQuery() }
        } finally {
            span.end()
        }
        if (rs.next()) {
            return rs.getInt(1)
        } else throw IllegalStateException("No count found.")
    }

    fun deleteItem(
        metadataId: String,
        rightId: String,
    ): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_ITEM).apply {
            this.setString(1, rightId)
            this.setString(2, metadataId)
        }
        val span = tracer.spanBuilder("deleteItem").startSpan()
        return try {
            span.makeCurrent()
            prepStmt.run { this.executeUpdate() }
        } finally {
            span.end()
        }
    }

    fun deleteItemByMetadata(
        metadataId: String,
    ): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_ITEM_BY_METADATA).apply {
            this.setString(1, metadataId)
        }
        val span = tracer.spanBuilder("deleteItem").startSpan()
        return try {
            span.makeCurrent()
            prepStmt.run { this.executeUpdate() }
        } finally {
            span.end()
        }
    }

    fun deleteItemByRight(
        rightId: String,
    ): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_ITEM_BY_RIGHT).apply {
            this.setString(1, rightId)
        }
        val span = tracer.spanBuilder("deleteItem").startSpan()
        return try {
            span.makeCurrent()
            prepStmt.run { this.executeUpdate() }
        } finally {
            span.end()
        }
    }

    fun deleteRights(rightIds: List<String>): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_RIGHTS).apply {
            this.setArray(1, connection.createArrayOf("text", rightIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("deleteRights").startSpan()
        return try {
            span.makeCurrent()
            prepStmt.run { this.executeUpdate() }
        } finally {
            span.end()
        }
    }

    fun deleteMetadata(metadataIds: List<String>): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_METADATA).apply {
            this.setArray(1, connection.createArrayOf("text", metadataIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("deleteMetadata").startSpan()
        return try {
            span.makeCurrent()
            prepStmt.run { this.executeUpdate() }
        } finally {
            span.end()
        }
    }

    fun getRights(rightsIds: List<String>): List<ItemRight> {

        val prepStmt = connection.prepareStatement(STATEMENT_GET_RIGHTS).apply {
            this.setArray(1, connection.createArrayOf("text", rightsIds.toTypedArray()))
        }

        val span = tracer.spanBuilder("getRights").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        return generateSequence {
            if (rs.next()) {
                ItemRight(
                    rightId = rs.getString(1),
                    createdOn = rs.getTimestamp(2)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                    lastUpdatedOn = rs.getTimestamp(3)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                    createdBy = rs.getString(4),
                    lastUpdatedBy = rs.getString(5),
                    accessState = rs.getString(6)?.let { AccessState.valueOf(it) },
                    startDate = rs.getDate(7).toLocalDate(),
                    endDate = rs.getDate(8)?.toLocalDate(),
                    notesGeneral = rs.getString(9),
                    licenceContract = rs.getString(10),
                    authorRightException = rs.getBoolean(11),
                    zbwUserAgreement = rs.getBoolean(12),
                    openContentLicence = rs.getString(13),
                    nonStandardOpenContentLicenceURL = rs.getString(14),
                    nonStandardOpenContentLicence = rs.getBoolean(15),
                    restrictedOpenContentLicence = rs.getBoolean(16),
                    notesFormalRules = rs.getString(17),
                    basisStorage = rs.getString(18)?.let { BasisStorage.valueOf(it) },
                    basisAccessState = rs.getString(19)?.let { BasisAccessState.valueOf(it) },
                    notesProcessDocumentation = rs.getString(20),
                    notesManagementRelated = rs.getString(21),
                )
            } else null
        }.takeWhile { true }.toList()
    }

    fun metadataContainsId(metadataId: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_METADATA_CONTAINS_ID).apply {
            this.setString(1, metadataId)
        }
        val span = tracer.spanBuilder("metadataContainsId").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun rightContainsId(rightId: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_RIGHT_CONTAINS_ID).apply {
            this.setString(1, rightId)
        }
        val span = tracer.spanBuilder("rightContainsId").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun getMetadataRange(limit: Int, offset: Int): List<String> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_METADATA_RANGE).apply {
            this.setInt(1, limit)
            this.setInt(2, offset)
        }

        val span = tracer.spanBuilder("getMetadataRange").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }

        return generateSequence {
            if (rs.next()) {
                rs.getString(1)
            } else null
        }.takeWhile { true }.toList()
    }

    fun getRightIdsByMetadata(metadataId: String): List<String> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_RIGHTSIDS_FOR_METADATA).apply {
            this.setString(1, metadataId)
        }
        val span = tracer.spanBuilder("getRightIdsByMetadata").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        return generateSequence {
            if (rs.next()) {
                rs.getString(1)
            } else null
        }.takeWhile { true }.toList()
    }

    private fun <T> PreparedStatement.setIfNotNull(
        idx: Int,
        element: T?,
        setter: (T, Int, PreparedStatement) -> Unit,
    ) = element?.let { setter(element, idx, this) } ?: this.setNull(idx, Types.NULL)

    companion object {
        private const val TABLE_NAME_ITEM = "item"
        private const val TABLE_NAME_ITEM_METADATA = "item_metadata"
        private const val TABLE_NAME_ITEM_RIGHT = "item_right"

        const val STATEMENT_COUNT_ITEM_BY_RIGHTID = "SELECT COUNT(*) " +
            "FROM $TABLE_NAME_ITEM " +
            "WHERE right_id = ?;"

        const val STATEMENT_INSERT_ITEM = "INSERT INTO $TABLE_NAME_ITEM" +
            "(metadata_id, right_id) " +
            "VALUES(?,?)"

        const val STATEMENT_UPSERT_METADATA = "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
            "(metadata_id,handle,ppn,ppn_ebook,title,title_journal," +
            "title_series,published_year,band,publication_type,doi," +
            "serial_number,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
            "created_on,last_updated_on,created_by,last_updated_by," +
            "author, collection_name, community_name, storage_date) " +
            "VALUES(?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?) " +
            "ON CONFLICT (metadata_id) " +
            "DO UPDATE SET " +
            "handle = EXCLUDED.handle," +
            "ppn = EXCLUDED.ppn," +
            "ppn_ebook = EXCLUDED.ppn_ebook," +
            "title = EXCLUDED.title," +
            "title_journal = EXCLUDED.title_journal," +
            "title_series = EXCLUDED.title_series," +
            "published_year = EXCLUDED.published_year," +
            "band = EXCLUDED.band," +
            "publication_type = EXCLUDED.publication_type," +
            "doi = EXCLUDED.doi," +
            "serial_number = EXCLUDED.serial_number," +
            "isbn = EXCLUDED.isbn," +
            "rights_k10plus = EXCLUDED.rights_k10plus," +
            "paket_sigel = EXCLUDED.paket_sigel," +
            "zbd_id = EXCLUDED.zbd_id," +
            "issn = EXCLUDED.issn," +
            "last_updated_on = EXCLUDED.last_updated_on," +
            "last_updated_by = EXCLUDED.last_updated_by," +
            "author = EXCLUDED.author," +
            "collection_name = EXCLUDED.collection_name," +
            "community_name = EXCLUDED.community_name," +
            "storage_date = EXCLUDED.storage_date;"

        const val STATEMENT_INSERT_METADATA = "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
            "(metadata_id,handle,ppn,ppn_ebook,title,title_journal," +
            "title_series,published_year,band,publication_type,doi," +
            "serial_number,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
            "created_on,last_updated_on,created_by,last_updated_by," +
            "author, collection_name, community_name, storage_date) " +
            "VALUES(?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?)"

        const val STATEMENT_INSERT_RIGHT = "INSERT INTO $TABLE_NAME_ITEM_RIGHT" +
            "(created_on, last_updated_on," +
            "created_by, last_updated_by, access_state," +
            "start_date, end_date, notes_general," +
            "licence_contract, author_right_exception, zbw_user_agreement," +
            "open_content_licence, non_standard_open_content_licence_url, non_standard_open_content_licence," +
            "restricted_open_content_licence, notes_formal_rules, basis_storage," +
            "basis_access_state, notes_process_documentation, notes_management_related) " +
            "VALUES(?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?,?)"

        const val STATEMENT_UPSERT_RIGHT =
            "INSERT INTO $TABLE_NAME_ITEM_RIGHT" +
                "(right_id, created_on, last_updated_on," +
                "created_by, last_updated_by, access_state," +
                "start_date, end_date, notes_general," +
                "licence_contract, author_right_exception, zbw_user_agreement," +
                "open_content_licence, non_standard_open_content_licence_url, non_standard_open_content_licence," +
                "restricted_open_content_licence, notes_formal_rules, basis_storage," +
                "basis_access_state, notes_process_documentation, notes_management_related) " +
                "VALUES(?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?)" +
                " ON CONFLICT (right_id) " +
                "DO UPDATE SET " +
                "last_updated_on = EXCLUDED.last_updated_on," +
                "last_updated_by = EXCLUDED.last_updated_by," +
                "access_state = EXCLUDED.access_state," +
                "start_date = EXCLUDED.start_date," +
                "end_date = EXCLUDED.end_date," +
                "notes_general = EXCLUDED.notes_general," +
                "licence_contract = EXCLUDED.licence_contract," +
                "open_content_licence = EXCLUDED.open_content_licence ," +
                "non_standard_open_content_licence_url = EXCLUDED.non_standard_open_content_licence_url," +
                "non_standard_open_content_licence = EXCLUDED.non_standard_open_content_licence," +
                "restricted_open_content_licence = EXCLUDED.restricted_open_content_licence," +
                "notes_formal_rules = EXCLUDED.notes_formal_rules," +
                "basis_storage = EXCLUDED.basis_storage," +
                "basis_access_state = EXCLUDED.basis_access_state," +
                "notes_process_documentation = EXCLUDED.notes_process_documentation," +
                "notes_management_related = EXCLUDED.notes_management_related;"

        const val STATEMENT_GET_METADATA = "SELECT metadata_id,handle,ppn,ppn_ebook,title,title_journal," +
            "title_series,published_year,band,publication_type,doi," +
            "serial_number,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
            "created_on,last_updated_on,created_by,last_updated_by," +
            "author, collection_name, community_name, storage_date " +
            "FROM $TABLE_NAME_ITEM_METADATA " +
            "WHERE metadata_id = ANY(?)"

        const val STATEMENT_DELETE_ITEM = "DELETE " +
            "FROM $TABLE_NAME_ITEM i " +
            "WHERE i.right_id = ? " +
            "AND i.metadata_id = ?"

        const val STATEMENT_DELETE_ITEM_BY_METADATA = "DELETE " +
            "FROM $TABLE_NAME_ITEM i " +
            "WHERE i.metadata_id = ?"

        const val STATEMENT_DELETE_ITEM_BY_RIGHT = "DELETE " +
            "FROM $TABLE_NAME_ITEM i " +
            "WHERE i.right_id = ?"

        const val STATEMENT_DELETE_RIGHTS = "DELETE " +
            "FROM $TABLE_NAME_ITEM_RIGHT r " +
            "WHERE r.right_id = ANY(?)"

        const val STATEMENT_DELETE_METADATA =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_METADATA h " +
                "WHERE h.metadata_id = ANY(?)"

        const val STATEMENT_GET_RIGHTS =
            "SELECT right_id, created_on, last_updated_on, created_by," +
                "last_updated_by, access_state, start_date, end_date, notes_general," +
                "licence_contract, author_right_exception, zbw_user_agreement," +
                "open_content_licence, non_standard_open_content_licence_url, non_standard_open_content_licence," +
                "restricted_open_content_licence, notes_formal_rules, basis_storage," +
                "basis_access_state, notes_process_documentation, notes_management_related " +
                "FROM $TABLE_NAME_ITEM_RIGHT " +
                "WHERE right_id = ANY(?)"

        const val STATEMENT_GET_METADATA_RANGE =
            "SELECT metadata_id FROM $TABLE_NAME_ITEM_METADATA ORDER BY metadata_id ASC LIMIT ? OFFSET ?"

        const val STATEMENT_GET_RIGHTSIDS_FOR_METADATA = "SELECT right_id" +
            " FROM $TABLE_NAME_ITEM" +
            " WHERE metadata_id = ?"

        const val STATEMENT_METADATA_CONTAINS_ID =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM_METADATA WHERE metadata_id=?)"

        const val STATEMENT_RIGHT_CONTAINS_ID =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM_RIGHT WHERE right_id=?)"

        const val STATEMENT_ITEM_CONTAINS_METADATA =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE metadata_id=?)"

        const val STATEMENT_ITEM_CONTAINS_ENTRY =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE metadata_id=? AND right_id=?)"

        const val STATEMENT_ITEM_CONTAINS_RIGHT =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE right_id=?)"

        fun Timestamp.toOffsetDateTime(): OffsetDateTime =
            OffsetDateTime.ofInstant(
                this.toInstant(),
                ZoneId.of("UTC+00:00"),
            )
    }
}
