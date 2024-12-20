package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.sql.SQLException
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testing [MetadataDB].
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class MetadataDBTest : DatabaseTest() {
    private val dbConnector =
        DatabaseConnector(
            connectionPool = ConnectionPool(testDataSource),
            tracer = OpenTelemetry.noop().getTracer("foo"),
        )

    @BeforeMethod
    fun beforeTest() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.toInstant()
    }

    @AfterMethod
    fun afterTest() {
        unmockkAll()
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testInsertHeaderException() =
        runBlocking {
            // given
            val testHeaderId = "double_entry"
            val testMetadata = TEST_Metadata.copy(handle = testHeaderId)

            // when
            dbConnector.metadataDB.insertMetadata(testMetadata)

            // exception
            dbConnector.metadataDB.insertMetadata(testMetadata)
        }

    @Test
    fun testMetadataRoundtrip() =
        runBlocking {
            // given
            val testId = "id_test"
            val testMetadata = TEST_Metadata.copy(handle = testId, title = "foo")

            // when
            val responseInsert = dbConnector.metadataDB.insertMetadata(testMetadata)

            // then
            assertThat(responseInsert, `is`(testId))

            // when
            val receivedMetadata: List<ItemMetadata> = dbConnector.metadataDB.getMetadata(listOf(testId))

            // then
            assertThat(
                receivedMetadata.first(),
                `is`(testMetadata),
            )

            // when
            assertThat(
                dbConnector.metadataDB.getMetadata(listOf("not_in_db")),
                `is`(listOf()),
            )

            // when
            val deletedMetadata = dbConnector.metadataDB.deleteMetadata(listOf(testId))

            // then
            assertThat(deletedMetadata, `is`(1))
            assertThat(dbConnector.metadataDB.getMetadata(listOf(testId)), `is`(listOf()))
        }

    @Test
    fun testBatchUpsert() =
        runBlocking {
            // given
            val id1 = "upsert1"
            val id2 = "upsert2"
            val m1 = TEST_Metadata.copy(handle = id1, title = "foo")
            val m2 = TEST_Metadata.copy(handle = id2, title = "bar")

            // when
            val responseUpsert = dbConnector.metadataDB.upsertMetadataBatch(listOf(m1, m2))

            // then
            assertThat(responseUpsert, `is`(IntArray(2) { 1 }))

            // when
            val receivedM1: List<ItemMetadata> = dbConnector.metadataDB.getMetadata(listOf(id1))

            // then
            assertThat(
                receivedM1.first(),
                `is`(m1),
            )

            val receivedM2: List<ItemMetadata> = dbConnector.metadataDB.getMetadata(listOf(id2))

            // then
            assertThat(
                receivedM2.first(),
                `is`(m2),
            )

            // when
            unmockkAll()

            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.plusDays(1).toInstant()
            val m1Changed = m1.copy(title = "foo2", lastUpdatedBy = "user2", lastUpdatedOn = NOW.plusDays(1))
            val m2Changed = m2.copy(title = "bar2", lastUpdatedBy = "user2", lastUpdatedOn = NOW.plusDays(1))

            val responseUpsert2 = dbConnector.metadataDB.upsertMetadataBatch(listOf(m1Changed, m2Changed))

            // then
            assertThat(responseUpsert2, `is`(IntArray(2) { 1 }))

            // when
            val receivedM1Changed: List<ItemMetadata> = dbConnector.metadataDB.getMetadata(listOf(id1))

            // then
            assertThat(
                receivedM1Changed.first(),
                `is`(m1Changed),
            )

            val receivedM2Changed: List<ItemMetadata> = dbConnector.metadataDB.getMetadata(listOf(id2))

            // then
            assertThat(
                receivedM2Changed.first(),
                `is`(m2Changed),
            )
        }

    @Test
    fun testContainsMetadata() =
        runBlocking {
            // given
            val handle = "handleContainCheck"
            val expectedMetadata = TEST_Metadata.copy(handle = handle)

            // when
            val containedBefore = dbConnector.metadataDB.metadataContainsHandle(handle)
            assertFalse(containedBefore, "Metadata should not exist yet")

            // when
            dbConnector.metadataDB.insertMetadata(expectedMetadata)
            val containedAfter = dbConnector.metadataDB.metadataContainsHandle(handle)
            assertTrue(containedAfter, "Metadata should exist now")
        }

    @Test
    fun testMetadataRange() =
        runBlocking {
            // given
            val givenMetadata =
                listOf(
                    TEST_Metadata.copy(handle = "aaaa"),
                    TEST_Metadata.copy(handle = "aaab"),
                    TEST_Metadata.copy(handle = "aaac"),
                )
            // when
            givenMetadata.map {
                dbConnector.metadataDB.insertMetadata(it)
            }

            // then
            assertThat(
                dbConnector.metadataDB.getMetadataRange(limit = 3, offset = 0).toSet(),
                `is`(givenMetadata.toSet()),
            )
            assertThat(
                dbConnector.metadataDB.getMetadataRange(limit = 2, offset = 1).toSet(),
                `is`(givenMetadata.subList(1, 3).toSet()),
            )
        }
}
