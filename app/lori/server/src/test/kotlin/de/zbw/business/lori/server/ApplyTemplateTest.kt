package de.zbw.business.lori.server

import de.zbw.business.lori.server.LoriServerBackendTest.Companion.TEST_METADATA
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.RightError
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertTrue

/**
 * Test applying a template.
 *
 * Created on 06-12-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ApplyTemplateTest : DatabaseTest() {
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
        ),
        mockk(),
    )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> = mapOf(
        item1ZDB1 to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2000, 1, 1),
                endDate = LocalDate.of(2000, 12, 31),
            )
        ),
        item1ZDB2 to emptyList(),
        item2ZDB2 to emptyList(),
    )

    @BeforeClass
    fun fillDB() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns ItemDBTest.NOW.toInstant()
        getInitialMetadata().forEach { entry ->
            backend.insertMetadataElement(entry.key)
            entry.value.forEach { right ->
                val r = backend.insertRight(right)
                backend.insertItemEntry(entry.key.metadataId, r)
            }
        }
    }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @Test
    fun testApplyTemplate() {
        // Create Bookmark
        val bookmarkId = backend.insertBookmark(
            Bookmark(
                bookmarkName = "applyBookmark",
                bookmarkId = 0,
                zdbIdFilter = ZDBIdFilter(
                    zdbIds = listOf(
                        ZDB_1,
                    )
                )
            )
        )

        // Create Template
        val rightId = backend.insertTemplate(TEST_RIGHT.copy(templateName = "test", isTemplate = true))

        // Connect Bookmark and Template
        backend.insertBookmarkTemplatePair(
            bookmarkId = bookmarkId,
            rightId = rightId,
        )

        val received: Pair<List<String>, List<RightError>> = backend.applyTemplate(rightId)
        assertThat(
            received.first,
            `is`(listOf(item1ZDB1.metadataId))
        )

        // Verify that new right is assigned to metadata id
        val rightIds = backend.getRightEntriesByMetadataId(item1ZDB1.metadataId).map { it.rightId }
        assertTrue(rightIds.contains(rightId))

        assertThat(
            backend.getRightById(rightId)!!.lastAppliedOn,
            `is`(ItemDBTest.NOW),
        )

        // Repeat Apply Operation without duplicate entries errors
        val received2: Pair<List<String>, List<RightError>> = backend.applyTemplate(rightId)
        assertThat(
            received2.first,
            `is`(listOf(item1ZDB1.metadataId))
        )

        // Add two new items to database matching bookmark
        backend.insertMetadataElements(
            listOf(
                item2ZDB1,
                item3ZDB1
            )
        )
        // Update old item from database so it no longer matches for bookmark
        backend.upsertMetadata(listOf(item1ZDB1.copy(zdbId = "foobar")))

        // Apply Template
        val received3: Pair<List<String>, List<RightError>> = backend.applyTemplate(rightId)
        assertThat(
            received3.first,
            `is`(
                listOf(
                    item2ZDB1.metadataId,
                    item3ZDB1.metadataId,
                )
            )
        )
        // Verify that only the new items are connected to template
        assertThat(
            backend.dbConnector.itemDB.countItemByRightId(rightId),
            `is`(2),
        )

        val applyAllReceived: Map<String, Pair<List<String>, List<RightError>>> = backend.applyAllTemplates()
        assertThat(
            applyAllReceived.values.map { it.first }.flatten().toSet(),
            `is`(
                setOf(
                    item2ZDB1.metadataId,
                    item3ZDB1.metadataId,
                )
            )
        )

        // Create conflicting template
        val rightIdConflict = backend.insertTemplate(TEST_RIGHT.copy(isTemplate = true, templateName = "conflicting"))

        // Connect Bookmark and Template
        backend.insertBookmarkTemplatePair(
            bookmarkId = bookmarkId,
            rightId = rightIdConflict,
        )
        val receivedConflict: Pair<List<String>, List<RightError>> = backend.applyTemplate(rightIdConflict)
        assertThat(
            receivedConflict.second.size,
            `is`(2)
        )
    }

    @Test
    fun testApplyTemplateWithException() {
        // Create Bookmarks
        val bookmarkIdUpper = backend.insertBookmark(
            Bookmark(
                bookmarkName = "allZDB2",
                bookmarkId = 10,
                zdbIdFilter = ZDBIdFilter(
                    zdbIds = listOf(
                        ZDB_2,
                    )
                )
            )
        )

        val bookmarkIdException = backend.insertBookmark(
            Bookmark(
                bookmarkName = "zdb2AndHandle",
                bookmarkId = 20,
                zdbIdFilter = ZDBIdFilter(
                    zdbIds = listOf(
                        ZDB_2,
                    )
                ),
                searchTerm = "hdl:bar"
            )
        )

        // Create Templates
        val rightIdUpper =
            backend.insertTemplate(TEST_RIGHT.copy(templateName = "upper", isTemplate = true))

        // Connect Bookmarks and Templates
        backend.insertBookmarkTemplatePair(
            bookmarkId = bookmarkIdUpper,
            rightId = rightIdUpper,
        )

        // Without exception
        val receivedUpperNoExc: Pair<List<String>, List<RightError>> = backend.applyTemplate(rightIdUpper)
        assertThat(
            receivedUpperNoExc.first.toSet(),
            `is`(setOf(item1ZDB2.metadataId, item2ZDB2.metadataId))
        )

        val rightIdException =
            backend.insertTemplate(
                TEST_RIGHT.copy(
                    templateName = "exception",
                    isTemplate = true,
                    exceptionFrom = rightIdUpper
                )
            )

        backend.insertBookmarkTemplatePair(
            bookmarkId = bookmarkIdException,
            rightId = rightIdException,
        )

        val receivedUpperWithExc: Pair<List<String>, List<RightError>> = backend.applyTemplate(rightIdUpper)
        assertThat(
            receivedUpperWithExc.first.toSet(),
            `is`(setOf(item1ZDB2.metadataId))
        )

        val receivedException: Pair<List<String>, List<RightError>> = backend.applyTemplate(rightIdException)
        assertThat(
            receivedException.first,
            `is`(listOf(item2ZDB2.metadataId))
        )
    }

    companion object {
        const val ZDB_1 = "zdb1"
        const val ZDB_2 = "zdb2"
        val TEST_RIGHT = RightFilterTest.TEST_RIGHT
        val item1ZDB1 = TEST_METADATA.copy(
            metadataId = "zdb1",
            collectionName = "common zdb",
            zdbId = ZDB_1,
            publicationDate = LocalDate.of(2010, 1, 1),
            publicationType = PublicationType.BOOK,
        )
        val item2ZDB1 = TEST_METADATA.copy(
            metadataId = "zdb2",
            collectionName = "common zdb",
            zdbId = ZDB_1,
            publicationDate = LocalDate.of(2010, 1, 1),
            publicationType = PublicationType.BOOK,
        )
        val item3ZDB1 = TEST_METADATA.copy(
            metadataId = "zdb3",
            collectionName = "common zdb",
            zdbId = ZDB_1,
            publicationDate = LocalDate.of(2010, 1, 1),
            publicationType = PublicationType.BOOK,
        )
        val item1ZDB2 = TEST_METADATA.copy(
            metadataId = "foo-zdb2",
            zdbId = ZDB_2,
            handle = "foo",
        )
        val item2ZDB2 = TEST_METADATA.copy(
            metadataId = "bar-zdb2",
            zdbId = ZDB_2,
            handle = "bar",
        )
    }
}
