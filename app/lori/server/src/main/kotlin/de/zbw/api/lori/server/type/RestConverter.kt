package de.zbw.api.lori.server.type

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.UserRole
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.MetadataRest
import de.zbw.lori.model.RightRest
import de.zbw.lori.model.RoleRest
import org.apache.logging.log4j.LogManager
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Conversion functions between rest interface and business logic.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */

fun ItemRest.toBusiness() =
    Item(
        metadata = metadata.toBusiness(),
        rights = rights?.map { it.toBusiness() } ?: emptyList(),
    )

fun Item.toRest() =
    ItemRest(
        metadata = metadata.toRest(),
        rights = rights.map { it.toRest() },
    )

fun MetadataRest.toBusiness() =
    ItemMetadata(
        metadataId = metadataId,
        author = author,
        band = band,
        collectionName = collectionName,
        communityName = communityName,
        createdBy = createdBy,
        createdOn = createdOn,
        doi = doi,
        handle = handle,
        isbn = isbn,
        issn = issn,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        paketSigel = paketSigel,
        ppn = ppn,
        publicationType = publicationType.toBusiness(),
        publicationDate = publicationDate,
        rightsK10plus = rightsK10plus,
        storageDate = storageDate,
        title = title,
        titleJournal = titleJournal,
        titleSeries = titleSeries,
        zdbId = zbdId,
    )

fun ItemMetadata.toRest(): MetadataRest =
    MetadataRest(
        metadataId = metadataId,
        author = author,
        band = band,
        collectionName = collectionName,
        communityName = communityName,
        createdBy = createdBy,
        createdOn = createdOn,
        doi = doi,
        handle = handle,
        isbn = isbn,
        issn = issn,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        paketSigel = paketSigel,
        ppn = ppn,
        publicationType = publicationType.toRest(),
        publicationDate = publicationDate,
        rightsK10plus = rightsK10plus,
        storageDate = storageDate,
        title = title,
        titleJournal = titleJournal,
        titleSeries = titleSeries,
        zbdId = zdbId,
    )

fun RightRest.toBusiness(): ItemRight =
    ItemRight(
        rightId = rightId,
        accessState = accessState?.toBusiness(),
        authorRightException = authorRightException,
        basisAccessState = basisAccessState?.toBusiness(),
        basisStorage = basisStorage?.toBusiness(),
        createdBy = createdBy,
        createdOn = createdOn,
        endDate = endDate,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        licenceContract = licenceContract,
        nonStandardOpenContentLicence = nonStandardOpenContentLicence,
        nonStandardOpenContentLicenceURL = nonStandardOpenContentLicenceURL,
        notesGeneral = notesGeneral,
        notesFormalRules = notesFormalRules,
        notesProcessDocumentation = notesProcessDocumentation,
        notesManagementRelated = notesManagementRelated,
        openContentLicence = openContentLicence,
        restrictedOpenContentLicence = restrictedOpenContentLicence,
        startDate = startDate,
        zbwUserAgreement = zbwUserAgreement,
    )

fun ItemRight.toRest(): RightRest =
    RightRest(
        rightId = rightId,
        accessState = accessState?.toRest(),
        authorRightException = authorRightException,
        basisAccessState = basisAccessState?.toRest(),
        basisStorage = basisStorage?.toRest(),
        createdBy = createdBy,
        createdOn = createdOn,
        endDate = endDate,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        licenceContract = licenceContract,
        nonStandardOpenContentLicence = nonStandardOpenContentLicence,
        nonStandardOpenContentLicenceURL = nonStandardOpenContentLicenceURL,
        notesGeneral = notesGeneral,
        notesFormalRules = notesFormalRules,
        notesProcessDocumentation = notesProcessDocumentation,
        notesManagementRelated = notesManagementRelated,
        openContentLicence = openContentLicence,
        restrictedOpenContentLicence = restrictedOpenContentLicence,
        startDate = startDate,
        zbwUserAgreement = zbwUserAgreement,
    )

internal fun RightRest.AccessState.toBusiness(): AccessState =
    when (this) {
        RightRest.AccessState.closed -> AccessState.CLOSED
        RightRest.AccessState.open -> AccessState.OPEN
        RightRest.AccessState.restricted -> AccessState.RESTRICTED
    }

internal fun AccessState.toRest(): RightRest.AccessState =
    when (this) {
        AccessState.CLOSED -> RightRest.AccessState.closed
        AccessState.OPEN -> RightRest.AccessState.open
        AccessState.RESTRICTED -> RightRest.AccessState.restricted
    }

internal fun RightRest.BasisAccessState.toBusiness(): BasisAccessState =
    when (this) {
        RightRest.BasisAccessState.authorRightException -> BasisAccessState.AUTHOR_RIGHT_EXCEPTION
        RightRest.BasisAccessState.licenceContract -> BasisAccessState.LICENCE_CONTRACT
        RightRest.BasisAccessState.licenceContractOa -> BasisAccessState.LICENCE_CONTRACT_OA
        RightRest.BasisAccessState.openContentLicence -> BasisAccessState.OPEN_CONTENT_LICENCE
        RightRest.BasisAccessState.userAgreement -> BasisAccessState.USER_AGREEMENT
        RightRest.BasisAccessState.zbwPolicy -> BasisAccessState.ZBW_POLICY
    }

internal fun BasisAccessState.toRest(): RightRest.BasisAccessState =
    when (this) {
        BasisAccessState.AUTHOR_RIGHT_EXCEPTION -> RightRest.BasisAccessState.authorRightException
        BasisAccessState.LICENCE_CONTRACT -> RightRest.BasisAccessState.licenceContract
        BasisAccessState.LICENCE_CONTRACT_OA -> RightRest.BasisAccessState.licenceContractOa
        BasisAccessState.OPEN_CONTENT_LICENCE -> RightRest.BasisAccessState.openContentLicence
        BasisAccessState.USER_AGREEMENT -> RightRest.BasisAccessState.userAgreement
        BasisAccessState.ZBW_POLICY -> RightRest.BasisAccessState.zbwPolicy
    }

internal fun RightRest.BasisStorage.toBusiness(): BasisStorage =
    when (this) {
        RightRest.BasisStorage.authorRightException -> BasisStorage.AUTHOR_RIGHT_EXCEPTION
        RightRest.BasisStorage.licenceContract -> BasisStorage.LICENCE_CONTRACT
        RightRest.BasisStorage.openContentLicence -> BasisStorage.LICENCE_CONTRACT
        RightRest.BasisStorage.userAgreement -> BasisStorage.USER_AGREEMENT
        RightRest.BasisStorage.zbwPolicyRestricted -> BasisStorage.ZBW_POLICY_RESTRICTED
        RightRest.BasisStorage.zbwPolicyUnanswered -> BasisStorage.ZBW_POLICY_UNANSWERED
    }

internal fun BasisStorage.toRest(): RightRest.BasisStorage =
    when (this) {
        BasisStorage.AUTHOR_RIGHT_EXCEPTION -> RightRest.BasisStorage.authorRightException
        BasisStorage.LICENCE_CONTRACT -> RightRest.BasisStorage.licenceContract
        BasisStorage.OPEN_CONTENT_LICENCE -> RightRest.BasisStorage.openContentLicence
        BasisStorage.USER_AGREEMENT -> RightRest.BasisStorage.userAgreement
        BasisStorage.ZBW_POLICY_RESTRICTED -> RightRest.BasisStorage.zbwPolicyRestricted
        BasisStorage.ZBW_POLICY_UNANSWERED -> RightRest.BasisStorage.zbwPolicyUnanswered
    }

internal fun MetadataRest.PublicationType.toBusiness(): PublicationType =
    when (this) {
        MetadataRest.PublicationType.article -> PublicationType.ARTICLE
        MetadataRest.PublicationType.book -> PublicationType.BOOK
        MetadataRest.PublicationType.bookPart -> PublicationType.BOOK_PART
        MetadataRest.PublicationType.periodicalPart -> PublicationType.PERIODICAL_PART
        MetadataRest.PublicationType.workingPaper -> PublicationType.WORKING_PAPER
        MetadataRest.PublicationType.researchReport -> PublicationType.RESEARCH_REPORT
        MetadataRest.PublicationType.proceedings -> PublicationType.PROCEEDINGS
        MetadataRest.PublicationType.thesis -> PublicationType.THESIS
        MetadataRest.PublicationType.conferencePaper -> PublicationType.CONFERENCE_PAPER
    }

internal fun PublicationType.toRest(): MetadataRest.PublicationType =
    when (this) {
        PublicationType.ARTICLE -> MetadataRest.PublicationType.article
        PublicationType.BOOK -> MetadataRest.PublicationType.book
        PublicationType.BOOK_PART -> MetadataRest.PublicationType.bookPart
        PublicationType.CONFERENCE_PAPER -> MetadataRest.PublicationType.conferencePaper
        PublicationType.PERIODICAL_PART -> MetadataRest.PublicationType.periodicalPart
        PublicationType.WORKING_PAPER -> MetadataRest.PublicationType.workingPaper
        PublicationType.RESEARCH_REPORT -> MetadataRest.PublicationType.researchReport
        PublicationType.PROCEEDINGS -> MetadataRest.PublicationType.proceedings
        PublicationType.THESIS -> MetadataRest.PublicationType.thesis
    }

fun DAItem.toBusiness(): ItemMetadata? {
    val metadata = this.metadata
    val handle = RestConverter.extractMetadata("dc.identifier.uri", metadata)
    val publicationType = RestConverter.extractMetadata("dc.type", metadata)?.let {
        PublicationType.valueOf(it.uppercase().replace(oldChar = ' ', newChar = '_'))
    }
    val publicationDate = RestConverter.extractMetadata("dc.date.issued", metadata)
    val title = RestConverter.extractMetadata("dc.title", metadata)

    return if (
        handle == null ||
        publicationDate == null ||
        publicationType == null ||
        title == null
    ) {
        null
    } else {
        ItemMetadata(
            metadataId = this.id.toString(),
            author = RestConverter.extractMetadata("dc.contributor.author", metadata),
            band = null, // Not in DA yet
            collectionName = this.parentCollection?.name,
            communityName = this.parentCommunityList.takeIf { it.isNotEmpty() }?.first()?.name,
            createdBy = null,
            createdOn = null,
            doi = RestConverter.extractMetadata("dc.identifier.pi", metadata),
            handle = handle,
            isbn = RestConverter.extractMetadata("dc.identifier.isbn", metadata),
            issn = RestConverter.extractMetadata("dc.identifier.issn", metadata),
            lastUpdatedBy = null,
            lastUpdatedOn = null,
            paketSigel = RestConverter.extractMetadata("dc.identifier.packageid", metadata),
            ppn = RestConverter.extractMetadata("dc.identifier.ppn", metadata),
            publicationType = publicationType,
            publicationDate = RestConverter.parseToDate(publicationDate),
            rightsK10plus = RestConverter.extractMetadata("dc.rights", metadata),
            storageDate = RestConverter.extractMetadata("dc.date.accessioned", metadata)
                ?.let { OffsetDateTime.parse(it) },
            title = title,
            titleJournal = RestConverter.extractMetadata("dc.journalname", metadata),
            titleSeries = RestConverter.extractMetadata("dc.seriesname", metadata),
            zdbId = RestConverter.extractMetadata("dc.relation.journalzdbid", metadata),
        )
    }
}

fun RoleRest.Role.toBusiness(): UserRole =
    when (this) {
        RoleRest.Role.readOnly -> UserRole.READONLY
        RoleRest.Role.readWrite -> UserRole.READWRITE
        RoleRest.Role.admin -> UserRole.ADMIN
    }

class RestConverter {
    companion object {
        fun extractMetadata(key: String, metadata: List<DAMetadata>): String? =
            metadata.filter { dam -> dam.key == key }.takeIf { it.isNotEmpty() }?.first()?.value

        fun parseToDate(s: String): LocalDate {
            return if (s.matches("\\d{4}-\\d{2}-\\d{2}".toRegex())) {
                LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
            } else if (s.matches("\\d{4}-\\d{2}".toRegex())) {
                LocalDate.parse("$s-01", DateTimeFormatter.ISO_LOCAL_DATE)
            } else if (s.matches("\\d{4}/\\d{2}".toRegex())) {
                LocalDate.parse(
                    "${s.substringBefore('/')}-${s.substringAfter('/')}-01",
                    DateTimeFormatter.ISO_LOCAL_DATE
                )
            } else if (s.matches("\\d{4}".toRegex())) {
                LocalDate.parse("$s-01-01", DateTimeFormatter.ISO_LOCAL_DATE)
            } else {
                LOG.warn("Date format can't be recognized: $s")
                LocalDate.parse("1970-01-01", DateTimeFormatter.ISO_LOCAL_DATE)
            }
        }

        private val LOG = LogManager.getLogger(RestConverter::class.java)
    }
}
