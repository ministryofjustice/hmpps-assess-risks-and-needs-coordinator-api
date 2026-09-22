package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDate
import java.util.Comparator
import java.util.UUID

class VersionsResponseFactoryTest {
  @Test
  fun `addVersions should correctly add assessment version`() {
    val date = LocalDate.of(2025, 6, 24)
    val uuid = UUID.randomUUID()

    val version = VersionDetails(
      uuid = uuid,
      version = 1,
      status = "TEST",
      createdAt = date.atTime(10, 0),
      updatedAt = date.atTime(11, 0),
      planAgreementStatus = null,
      entityType = EntityType.ASSESSMENT,
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(listOf(version))

    val expectedResponse = VersionsResponse(
      allVersions = sortedMapOf(
        date to LastVersionsOnDate(
          description = "Assessment updated",
          assessmentVersion = version,
        ),
      ),
    )

    assertEquals(expectedResponse, factory.getVersionsResponse())
  }

  @Test
  fun `addVersions should correctly add plan version`() {
    val date = LocalDate.of(2025, 6, 24)
    val uuid = UUID.randomUUID()

    val version = VersionDetails(
      uuid = uuid,
      version = 1,
      status = "TEST",
      createdAt = date.atTime(10, 0),
      updatedAt = date.atTime(11, 0),
      planAgreementStatus = "TEST",
      entityType = EntityType.PLAN,
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(listOf(version))

    val expectedResponse = VersionsResponse(
      allVersions = sortedMapOf(
        date to LastVersionsOnDate(
          description = "Plan updated",
          planVersion = version,
        ),
      ),
    )

    assertEquals(expectedResponse, factory.getVersionsResponse())
  }

  @Test
  fun `addVersions should correctly add countersigned plan version`() {
    val date = LocalDate.of(2025, 6, 24)
    val uuid = UUID.randomUUID()

    val version = VersionDetails(
      uuid = uuid,
      version = 1,
      status = "COUNTERSIGNED",
      createdAt = date.atTime(10, 0),
      updatedAt = date.atTime(11, 0),
      planAgreementStatus = "TEST",
      entityType = EntityType.PLAN,
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(listOf(version))

    val expectedResponse = VersionsResponse(
      countersignedVersions = sortedMapOf(
        date to LastVersionsOnDate(
          description = "Plan updated",
          planVersion = version,
        ),
      ),
    )

    assertEquals(expectedResponse, factory.getVersionsResponse())
  }

  @Test
  fun `addVersions should correctly combine assessment and plan versions on the same date`() {
    val date = LocalDate.of(2025, 6, 24)
    val planUuid = UUID.randomUUID()
    val assessmentUuid = UUID.randomUUID()

    val assessmentVersion = VersionDetails(
      uuid = assessmentUuid,
      version = 1,
      status = "TEST",
      createdAt = date.atTime(10, 0),
      updatedAt = date.atTime(11, 0),
      planAgreementStatus = null,
      entityType = EntityType.ASSESSMENT,
    )

    val planVersion = VersionDetails(
      uuid = planUuid,
      version = 1,
      status = "TEST",
      createdAt = date.atTime(10, 0),
      updatedAt = date.atTime(11, 0),
      planAgreementStatus = "TEST",
      entityType = EntityType.PLAN,
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(listOf(assessmentVersion))
    factory.addVersions(listOf(planVersion))

    val expectedResponse = VersionsResponse(
      allVersions = sortedMapOf(
        date to LastVersionsOnDate(
          description = "Assessment and plan updated",
          assessmentVersion = assessmentVersion,
          planVersion = planVersion,
        ),
      ),
    )

    assertEquals(expectedResponse, factory.getVersionsResponse())
  }

  @Test
  fun `addVersions should correctly combine countersigned assessment version and plan version on the same date`() {
    val date = LocalDate.of(2025, 6, 24)
    val planUuid = UUID.randomUUID()
    val assessmentUuid = UUID.randomUUID()

    val assessmentVersion = VersionDetails(
      uuid = assessmentUuid,
      version = 1,
      status = "COUNTERSIGNED",
      createdAt = date.atTime(10, 0),
      updatedAt = date.atTime(11, 0),
      planAgreementStatus = null,
      entityType = EntityType.ASSESSMENT,
    )

    val planVersion = VersionDetails(
      uuid = planUuid,
      version = 1,
      status = "TEST",
      createdAt = date.atTime(10, 0),
      updatedAt = date.atTime(11, 0),
      planAgreementStatus = "TEST",
      entityType = EntityType.PLAN,
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(listOf(assessmentVersion))
    factory.addVersions(listOf(planVersion))

    val expectedResponse = VersionsResponse(
      allVersions = sortedMapOf(
        date to LastVersionsOnDate(
          description = "Assessment and plan updated",
          assessmentVersion = assessmentVersion,
          planVersion = planVersion,
        ),
      ),
      countersignedVersions = sortedMapOf(
        date to LastVersionsOnDate(
          description = "Assessment updated",
          assessmentVersion = assessmentVersion,
        ),
      ),
    )

    assertEquals(expectedResponse, factory.getVersionsResponse())
  }

  @Test
  fun `addVersions should correctly combine assessment and plan versions on different dates`() {
    val planDate = LocalDate.of(2025, 6, 24)
    val assessmentDate = LocalDate.of(2025, 5, 24)

    val planUuid = UUID.randomUUID()
    val assessmentUuid = UUID.randomUUID()

    val assessmentVersion = VersionDetails(
      uuid = assessmentUuid,
      version = 1,
      status = "TEST",
      createdAt = assessmentDate.atTime(10, 0),
      updatedAt = assessmentDate.atTime(11, 0),
      planAgreementStatus = null,
      entityType = EntityType.ASSESSMENT,
    )

    val planVersion = VersionDetails(
      uuid = planUuid,
      version = 1,
      status = "TEST",
      createdAt = planDate.atTime(10, 0),
      updatedAt = planDate.atTime(11, 0),
      planAgreementStatus = "TEST",
      entityType = EntityType.PLAN,
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(listOf(assessmentVersion))
    factory.addVersions(listOf(planVersion))

    val expectedResponse = VersionsResponse(
      allVersions = sortedMapOf(
        planDate to LastVersionsOnDate(
          description = "Plan updated",
          planVersion = planVersion,
          assessmentVersion = assessmentVersion,
        ),
        assessmentDate to LastVersionsOnDate(
          description = "Assessment updated",
          assessmentVersion = assessmentVersion,
        ),
      ),
    )

    assertEquals(expectedResponse, factory.getVersionsResponse())
  }

  @Test
  fun `addVersions should correctly combine multiple assessment and plan versions`() {
    val planDate = LocalDate.of(2025, 6, 24)
    val assessmentDate = LocalDate.of(2025, 5, 24)
    val assessmentDate2 = LocalDate.of(2025, 7, 24)

    val planUuid = UUID.randomUUID()
    val assessmentUuid = UUID.randomUUID()

    val assessmentVersions = listOf(
      VersionDetails(
        uuid = assessmentUuid,
        version = 2,
        status = "TEST",
        createdAt = assessmentDate2.atTime(10, 0),
        updatedAt = assessmentDate2.atTime(11, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 1,
        status = "TEST",
        createdAt = assessmentDate.atTime(10, 0),
        updatedAt = assessmentDate.atTime(11, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 0,
        status = "TEST",
        createdAt = assessmentDate.atTime(8, 0),
        updatedAt = assessmentDate.atTime(9, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
    )

    val planVersions = listOf(
      VersionDetails(
        uuid = planUuid,
        version = 2,
        status = "TEST",
        createdAt = planDate.atTime(10, 0),
        updatedAt = planDate.atTime(11, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 1,
        status = "TEST",
        createdAt = planDate.atTime(8, 0),
        updatedAt = planDate.atTime(9, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 0,
        status = "TEST",
        createdAt = assessmentDate.atTime(8, 0),
        updatedAt = assessmentDate.atTime(9, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(assessmentVersions)
    factory.addVersions(planVersions)

    val expectedResponse = VersionsResponse(
      allVersions = sortedMapOf(
        assessmentDate2 to LastVersionsOnDate(
          description = "Assessment updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 2,
            status = "TEST",
            createdAt = assessmentDate2.atTime(10, 0),
            updatedAt = assessmentDate2.atTime(11, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 2,
            status = "TEST",
            createdAt = planDate.atTime(10, 0),
            updatedAt = planDate.atTime(11, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
        planDate to LastVersionsOnDate(
          description = "Plan updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 1,
            status = "TEST",
            createdAt = assessmentDate.atTime(10, 0),
            updatedAt = assessmentDate.atTime(11, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 2,
            status = "TEST",
            createdAt = planDate.atTime(10, 0),
            updatedAt = planDate.atTime(11, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
        assessmentDate to LastVersionsOnDate(
          description = "Assessment and plan updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 1,
            status = "TEST",
            createdAt = assessmentDate.atTime(10, 0),
            updatedAt = assessmentDate.atTime(11, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 0,
            status = "TEST",
            createdAt = assessmentDate.atTime(8, 0),
            updatedAt = assessmentDate.atTime(9, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
      ),
    )

    assertEquals(expectedResponse, factory.getVersionsResponse())
  }

  @Test
  fun `addVersions should correctly combine a mixture of multiple countersigned and other assessment and plan versions and return them in the descending order`() {
    val may = LocalDate.of(2025, 5, 24)
    val june = LocalDate.of(2025, 6, 24)
    val july = LocalDate.of(2025, 7, 24)
    val august = LocalDate.of(2025, 8, 24)
    val september = LocalDate.of(2025, 9, 24)
    val october = LocalDate.of(2025, 10, 24)
    val november = LocalDate.of(2025, 11, 24)

    val planUuid = UUID.randomUUID()
    val assessmentUuid = UUID.randomUUID()

    val assessmentVersions = listOf(
      VersionDetails(
        uuid = assessmentUuid,
        version = 9,
        status = "DOUBLE_COUNTERSIGNED",
        createdAt = november.atTime(8, 30),
        updatedAt = november.atTime(9, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 8,
        status = "TEST",
        createdAt = october.atTime(8, 30),
        updatedAt = october.atTime(9, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 7,
        status = "COUNTERSIGNED",
        createdAt = september.atTime(8, 30),
        updatedAt = september.atTime(9, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 6,
        status = "TEST",
        createdAt = september.atTime(5, 30),
        updatedAt = september.atTime(6, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 5,
        status = "COUNTERSIGNED",
        createdAt = august.atTime(8, 30),
        updatedAt = august.atTime(9, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 4,
        status = "TEST",
        createdAt = june.atTime(10, 30),
        updatedAt = june.atTime(11, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 3,
        status = "COUNTERSIGNED",
        createdAt = june.atTime(9, 0),
        updatedAt = june.atTime(10, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 2,
        status = "TEST",
        createdAt = may.atTime(11, 30),
        updatedAt = may.atTime(12, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 1,
        status = "TEST",
        createdAt = may.atTime(10, 0),
        updatedAt = may.atTime(11, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 0,
        status = "COUNTERSIGNED",
        createdAt = may.atTime(8, 0),
        updatedAt = may.atTime(9, 0),
        planAgreementStatus = null,
        entityType = EntityType.ASSESSMENT,
      ),
    )

    val planVersions = listOf(
      VersionDetails(
        uuid = planUuid,
        version = 8,
        status = "DOUBLE_COUNTERSIGNED",
        createdAt = november.atTime(9, 0),
        updatedAt = november.atTime(10, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 7,
        status = "COUNTERSIGNED",
        createdAt = september.atTime(9, 0),
        updatedAt = september.atTime(10, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 6,
        status = "TEST",
        createdAt = september.atTime(5, 0),
        updatedAt = september.atTime(6, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 5,
        status = "TEST",
        createdAt = august.atTime(9, 0),
        updatedAt = august.atTime(10, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 4,
        status = "TEST",
        createdAt = july.atTime(10, 30),
        updatedAt = july.atTime(11, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 3,
        status = "COUNTERSIGNED",
        createdAt = july.atTime(9, 0),
        updatedAt = july.atTime(10, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 2,
        status = "TEST",
        createdAt = may.atTime(11, 30),
        updatedAt = may.atTime(12, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 1,
        status = "TEST",
        createdAt = may.atTime(10, 0),
        updatedAt = may.atTime(11, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 0,
        status = "COUNTERSIGNED",
        createdAt = may.atTime(8, 0),
        updatedAt = may.atTime(9, 0),
        planAgreementStatus = "TEST",
        entityType = EntityType.PLAN,
      ),
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(assessmentVersions)
    factory.addVersions(planVersions)

    val expectedResponse = VersionsResponse(
      allVersions = sortedMapOf(
        Comparator.reverseOrder(),
        // checking that if there are versions on the same day but after countersigned assessment and plan, these are added on a separate row for 'All versions' table in the UI
        may to LastVersionsOnDate(
          description = "Assessment and plan updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 2,
            status = "TEST",
            createdAt = may.atTime(11, 30),
            updatedAt = may.atTime(12, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 2,
            status = "TEST",
            createdAt = may.atTime(11, 30),
            updatedAt = may.atTime(12, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
        june to LastVersionsOnDate(
          // checking that the countersignedDescription is accurate if for some reason there is only countersigned assessment but no countersigned plan;
          // verifying that a new assessment post countersigned version is added alongside a last month's plan version for 'All versions' table in UI with the correct description
          description = "Assessment updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 4,
            status = "TEST",
            createdAt = june.atTime(10, 30),
            updatedAt = june.atTime(11, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 2,
            status = "TEST",
            createdAt = may.atTime(11, 30),
            updatedAt = may.atTime(12, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
        july to LastVersionsOnDate(
          // checking that the countersignedDescription is accurate if for some reason there is only countersigned plan but no countersigned assessment;
          // verifying that a new plan post countersigned version is added alongside a last month's assessment version for 'All versions' table in UI with the correct description
          description = "Plan updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 4,
            status = "TEST",
            createdAt = june.atTime(10, 30),
            updatedAt = june.atTime(11, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 4,
            status = "TEST",
            createdAt = july.atTime(10, 30),
            updatedAt = july.atTime(11, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
        august to LastVersionsOnDate(
          // checking that the countersignedDescription is accurate if for some reason there is only countersigned assessment but no countersigned plan;
          // verifying that a plan version on that date but without 'COUNTERSIGNED' status is added in 'All versions' table in UI and countersigned assessment is used for that row
          description = "Assessment and plan updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 5,
            status = "COUNTERSIGNED",
            createdAt = august.atTime(8, 30),
            updatedAt = august.atTime(9, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 5,
            status = "TEST",
            createdAt = august.atTime(9, 0),
            updatedAt = august.atTime(10, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
        october to LastVersionsOnDate(
          // given that both plan and assessment are countersigned on one day, and only assessment is updated on the following day;
          // verify that the countersigned plan is used as "last plan" alongside the updated assessment
          description = "Assessment updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 8,
            status = "TEST",
            createdAt = october.atTime(8, 30),
            updatedAt = october.atTime(9, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 7,
            status = "COUNTERSIGNED",
            createdAt = september.atTime(9, 0),
            updatedAt = september.atTime(10, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
      ),
      countersignedVersions = sortedMapOf(
        Comparator.reverseOrder(),
        // checking that if there are versions on the same day but after countersigned assessment and plan, these are added on a separate row for 'All versions' table in the UI
        may to LastVersionsOnDate(
          description = "Assessment and plan updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 0,
            status = "COUNTERSIGNED",
            createdAt = may.atTime(8, 0),
            updatedAt = may.atTime(9, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 0,
            status = "COUNTERSIGNED",
            createdAt = may.atTime(8, 0),
            updatedAt = may.atTime(9, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
        june to LastVersionsOnDate(
          // checking that the countersignedDescription is accurate if for some reason there is only countersigned assessment but no countersigned plan;
          // verifying that a new assessment post countersigned version is added alongside a last month's plan version for 'All versions' table in UI with the correct description
          description = "Assessment updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 3,
            status = "COUNTERSIGNED",
            createdAt = june.atTime(9, 0),
            updatedAt = june.atTime(10, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
        ),
        july to LastVersionsOnDate(
          // checking that the countersignedDescription is accurate if for some reason there is only countersigned plan but no countersigned assessment;
          // verifying that a new plan post countersigned version is added alongside a last month's assessment version for 'All versions' table in UI with the correct description
          description = "Plan updated",
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 3,
            status = "COUNTERSIGNED",
            createdAt = july.atTime(9, 0),
            updatedAt = july.atTime(10, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
        august to LastVersionsOnDate(
          // checking that the countersignedDescription is accurate if for some reason there is only countersigned assessment but no countersigned plan;
          // verifying that a plan version on that date but without 'COUNTERSIGNED' status is added in 'All versions' table in UI and countersigned assessment is used for that row
          description = "Assessment updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 5,
            status = "COUNTERSIGNED",
            createdAt = august.atTime(8, 30),
            updatedAt = august.atTime(9, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
        ),
        september to LastVersionsOnDate(
          // checking that if there are assessment and plan versions on the same date but before the countersigned versions, these are ignored
          description = "Assessment and plan updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 7,
            status = "COUNTERSIGNED",
            createdAt = september.atTime(8, 30),
            updatedAt = september.atTime(9, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 7,
            status = "COUNTERSIGNED",
            createdAt = september.atTime(9, 0),
            updatedAt = september.atTime(10, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
        november to LastVersionsOnDate(
          // given both assessment and plan versions are "DOUBLE_COUNTERSIGNED"
          // verify that both are added into countersignedVersions field
          description = "Assessment and plan updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 9,
            status = "DOUBLE_COUNTERSIGNED",
            createdAt = november.atTime(8, 30),
            updatedAt = november.atTime(9, 0),
            planAgreementStatus = null,
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 8,
            status = "DOUBLE_COUNTERSIGNED",
            createdAt = november.atTime(9, 0),
            updatedAt = november.atTime(10, 0),
            planAgreementStatus = "TEST",
            entityType = EntityType.PLAN,
          ),
        ),
      ),
    )

    val actualResponse = factory.getVersionsResponse()

    assertEquals(expectedResponse.allVersions.entries.toList(), actualResponse.allVersions.entries.toList())
    assertEquals(expectedResponse.countersignedVersions.entries.toList(), actualResponse.countersignedVersions.entries.toList())
  }

  @Test
  fun `plan agreed then updated on the same day should preserve planAgreementStatus AGREED`() {
    val date = LocalDate.of(2026, 3, 19)
    val planUuid = UUID.randomUUID()

    val agreedVersion = VersionDetails(
      uuid = planUuid,
      version = 1,
      status = "UNSIGNED",
      createdAt = date.atTime(10, 0),
      updatedAt = date.atTime(10, 30),
      planAgreementStatus = "AGREED",
      entityType = EntityType.AAP_PLAN,
    )

    val updatedVersion = VersionDetails(
      uuid = planUuid,
      version = 2,
      status = "UNSIGNED",
      createdAt = date.atTime(14, 0),
      updatedAt = date.atTime(14, 30),
      planAgreementStatus = "",
      entityType = EntityType.AAP_PLAN,
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(listOf(agreedVersion, updatedVersion))

    val expectedResponse = VersionsResponse(
      allVersions = sortedMapOf(
        date to LastVersionsOnDate(
          description = "Plan updated",
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 2,
            status = "UNSIGNED",
            createdAt = date.atTime(14, 0),
            updatedAt = date.atTime(14, 30),
            planAgreementStatus = "AGREED",
            entityType = EntityType.AAP_PLAN,
          ),
        ),
      ),
    )

    assertEquals(expectedResponse, factory.getVersionsResponse())
  }

  @Test
  fun `plan agreed as the only event on a day should retain planAgreementStatus AGREED`() {
    val date = LocalDate.of(2026, 3, 19)
    val planUuid = UUID.randomUUID()

    val agreedVersion = VersionDetails(
      uuid = planUuid,
      version = 1,
      status = "UNSIGNED",
      createdAt = date.atTime(10, 0),
      updatedAt = date.atTime(10, 30),
      planAgreementStatus = "AGREED",
      entityType = EntityType.AAP_PLAN,
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(listOf(agreedVersion))

    val expectedResponse = VersionsResponse(
      allVersions = sortedMapOf(
        date to LastVersionsOnDate(
          description = "Plan updated",
          planVersion = agreedVersion,
        ),
      ),
    )

    assertEquals(expectedResponse, factory.getVersionsResponse())
  }

  @Test
  fun `plan agreed then updated across two days should show AGREED on the agreement day`() {
    val day1 = LocalDate.of(2026, 3, 19)
    val day2 = LocalDate.of(2026, 3, 20)
    val planUuid = UUID.randomUUID()

    val agreedVersion = VersionDetails(
      uuid = planUuid,
      version = 1,
      status = "UNSIGNED",
      createdAt = day1.atTime(10, 0),
      updatedAt = day1.atTime(10, 30),
      planAgreementStatus = "AGREED",
      entityType = EntityType.AAP_PLAN,
    )

    val updatedVersion = VersionDetails(
      uuid = planUuid,
      version = 2,
      status = "UNSIGNED",
      createdAt = day2.atTime(9, 0),
      updatedAt = day2.atTime(9, 30),
      planAgreementStatus = "",
      entityType = EntityType.AAP_PLAN,
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(listOf(agreedVersion, updatedVersion))

    val expectedResponse = VersionsResponse(
      allVersions = sortedMapOf(
        Comparator.reverseOrder(),
        day2 to LastVersionsOnDate(
          description = "Plan updated",
          planVersion = updatedVersion,
        ),
        day1 to LastVersionsOnDate(
          description = "Plan updated",
          planVersion = agreedVersion,
        ),
      ),
    )

    val actualResponse = factory.getVersionsResponse()

    assertEquals(expectedResponse.allVersions.entries.toList(), actualResponse.allVersions.entries.toList())
  }

  @ParameterizedTest
  @EnumSource(EntityType::class, names = ["PLAN", "AAP_PLAN"])
  fun `first date should only say assessment updated when the plan has no later version`(planType: EntityType) {
    val day1 = LocalDate.of(2025, 6, 24)
    val day2 = LocalDate.of(2025, 6, 25)

    val factory = VersionsResponseFactory()
    val assessment = version(EntityType.ASSESSMENT, day1)
    val plan = version(planType, day1)
    val laterAssessment = version(EntityType.ASSESSMENT, day2).copy(version = 2)
    factory.addVersions(listOf(assessment, plan, laterAssessment))
    if (planType == EntityType.AAP_PLAN) {
      factory.addVersions(listOf(version(planType, day1, status = "CREATED", hour = 10).copy(version = 0)))
    }

    assertEquals(
      VersionsResponse(
        allVersions = sortedMapOf(
          day1 to LastVersionsOnDate("Assessment updated", assessment, plan),
          day2 to LastVersionsOnDate("Assessment updated", laterAssessment, plan),
        ),
      ),
      factory.getVersionsResponse(),
    )
  }

  @ParameterizedTest
  @EnumSource(EntityType::class, names = ["PLAN", "AAP_PLAN"])
  fun `first date should only say plan updated when the assessment has no later version`(planType: EntityType) {
    val day1 = LocalDate.of(2025, 6, 24)
    val day2 = LocalDate.of(2025, 6, 25)

    val factory = VersionsResponseFactory()
    val assessment = version(EntityType.ASSESSMENT, day1)
    val plan = version(planType, day1)
    val laterPlan = version(planType, day2).copy(version = 2)
    factory.addVersions(listOf(assessment, plan, laterPlan))

    assertEquals(
      VersionsResponse(
        allVersions = sortedMapOf(
          day1 to LastVersionsOnDate("Plan updated", assessment, plan),
          day2 to LastVersionsOnDate("Plan updated", assessment, laterPlan),
        ),
      ),
      factory.getVersionsResponse(),
    )
  }

  @Test
  fun `first date should say assessment and plan updated when both have a later version`() {
    val day1 = LocalDate.of(2025, 6, 24)
    val day2 = LocalDate.of(2025, 6, 25)

    val factory = VersionsResponseFactory()
    factory.addVersions(
      listOf(
        version(EntityType.ASSESSMENT, day1),
        version(EntityType.AAP_PLAN, day1),
        version(EntityType.ASSESSMENT, day2),
        version(EntityType.AAP_PLAN, day2),
      ),
    )

    assertEquals("Assessment and plan updated", factory.getVersionsResponse().allVersions[day1]?.description)
  }

  @Test
  fun `label correction should preserve the original response when neither entity has a later version`() {
    val day = LocalDate.of(2025, 6, 24)

    val factory = VersionsResponseFactory()
    val assessment = version(EntityType.ASSESSMENT, day)
    val plan = version(EntityType.AAP_PLAN, day, hour = 12)
    factory.addVersions(listOf(assessment, version(EntityType.AAP_PLAN, day, status = "CREATED"), plan))

    assertEquals(
      VersionsResponse(allVersions = sortedMapOf(day to LastVersionsOnDate("Assessment and plan updated", assessment, plan))),
      factory.getVersionsResponse(),
    )
  }

  @Test
  fun `first date should keep its row when neither entity has a later version but one has been signed`() {
    val day = LocalDate.of(2025, 6, 24)

    val factory = VersionsResponseFactory()
    factory.addVersions(
      listOf(
        version(EntityType.ASSESSMENT, day, status = "SELF_SIGNED"),
        version(EntityType.AAP_PLAN, day, status = "CREATED", hour = 11),
        version(EntityType.AAP_PLAN, day, hour = 12),
      ),
    )

    assertEquals("Assessment and plan updated", factory.getVersionsResponse().allVersions[day]?.description)
  }

  @ParameterizedTest
  @ValueSource(strings = ["SELF_SIGNED", "COUNTERSIGNED", "LOCKED", "AWAITING_COUNTERSIGN"])
  fun `later versions of the other entity should not hide a recorded status from the first label`(status: String) {
    val day = LocalDate.of(2025, 6, 24)
    for (recordedType in listOf(EntityType.ASSESSMENT, EntityType.AAP_PLAN)) {
      val factory = VersionsResponseFactory()
      val assessment = version(EntityType.ASSESSMENT, day, status = if (recordedType == EntityType.ASSESSMENT) status else "UNSIGNED")
      val plan = version(EntityType.AAP_PLAN, day, status = if (recordedType == EntityType.AAP_PLAN) status else "UNSIGNED")
      val otherType = if (recordedType == EntityType.ASSESSMENT) EntityType.AAP_PLAN else EntityType.ASSESSMENT
      factory.addVersions(listOf(assessment, plan, version(otherType, day.plusDays(1))))

      assertEquals(LastVersionsOnDate("Assessment and plan updated", assessment, plan), factory.getVersionsResponse().allVersions[day])
    }
  }

  @Test
  fun `an agreed plan should retain its row and links alongside a current SAN version`() {
    val day = LocalDate.of(2025, 6, 24)
    val assessment = version(EntityType.ASSESSMENT, day)
    val agreedPlan = version(EntityType.AAP_PLAN, day, hour = 12).copy(planAgreementStatus = "AGREED")
    val factory = VersionsResponseFactory()
    factory.addVersions(listOf(assessment, version(EntityType.AAP_PLAN, day, status = "CREATED"), agreedPlan))

    assertEquals(
      LastVersionsOnDate("Assessment and plan updated", assessment, agreedPlan),
      factory.getVersionsResponse().allVersions[day],
    )
    factory.addVersions(listOf(version(EntityType.ASSESSMENT, day.plusDays(1))))
    assertEquals(
      LastVersionsOnDate("Assessment and plan updated", assessment, agreedPlan),
      factory.getVersionsResponse().allVersions[day],
    )
  }

  @Test
  fun `multiple unsigned assessment versions on the same day should not be treated as one live version`() {
    val day = LocalDate.of(2025, 6, 24)
    val assessment = version(EntityType.ASSESSMENT, day)
    val newerAssessment = assessment.copy(uuid = UUID.randomUUID(), version = 2, updatedAt = day.atTime(12, 0))
    val plan = version(EntityType.AAP_PLAN, day)
    val factory = VersionsResponseFactory()
    factory.addVersions(listOf(assessment, newerAssessment, plan, version(EntityType.AAP_PLAN, day.plusDays(1))))

    assertEquals(
      LastVersionsOnDate("Assessment and plan updated", newerAssessment, plan),
      factory.getVersionsResponse().allVersions[day],
    )
  }

  private fun version(entityType: EntityType, date: LocalDate, status: String = "UNSIGNED", hour: Int = 11) = VersionDetails(
    uuid = UUID.randomUUID(),
    version = 1,
    status = status,
    createdAt = date.atTime(10, 0),
    updatedAt = date.atTime(hour, 0),
    planAgreementStatus = null,
    entityType = entityType,
  )
}
