package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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
      entityType = EntityType.ASSESSMENT,
    )

    val planVersion = VersionDetails(
      uuid = planUuid,
      version = 1,
      status = "TEST",
      createdAt = date.atTime(10, 0),
      updatedAt = date.atTime(11, 0),
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
      entityType = EntityType.ASSESSMENT,
    )

    val planVersion = VersionDetails(
      uuid = planUuid,
      version = 1,
      status = "TEST",
      createdAt = date.atTime(10, 0),
      updatedAt = date.atTime(11, 0),
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
      entityType = EntityType.ASSESSMENT,
    )

    val planVersion = VersionDetails(
      uuid = planUuid,
      version = 1,
      status = "TEST",
      createdAt = planDate.atTime(10, 0),
      updatedAt = planDate.atTime(11, 0),
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
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 1,
        status = "TEST",
        createdAt = assessmentDate.atTime(10, 0),
        updatedAt = assessmentDate.atTime(11, 0),
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 0,
        status = "TEST",
        createdAt = assessmentDate.atTime(8, 0),
        updatedAt = assessmentDate.atTime(9, 0),
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
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 1,
        status = "TEST",
        createdAt = planDate.atTime(8, 0),
        updatedAt = planDate.atTime(9, 0),
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 0,
        status = "TEST",
        createdAt = assessmentDate.atTime(8, 0),
        updatedAt = assessmentDate.atTime(9, 0),
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
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 2,
            status = "TEST",
            createdAt = planDate.atTime(10, 0),
            updatedAt = planDate.atTime(11, 0),
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
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 2,
            status = "TEST",
            createdAt = planDate.atTime(10, 0),
            updatedAt = planDate.atTime(11, 0),
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
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 0,
            status = "TEST",
            createdAt = assessmentDate.atTime(8, 0),
            updatedAt = assessmentDate.atTime(9, 0),
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

    val planUuid = UUID.randomUUID()
    val assessmentUuid = UUID.randomUUID()

    val assessmentVersions = listOf(
      VersionDetails(
        uuid = assessmentUuid,
        version = 8,
        status = "TEST",
        createdAt = october.atTime(8, 30),
        updatedAt = october.atTime(9, 0),
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 7,
        status = "COUNTERSIGNED",
        createdAt = september.atTime(8, 30),
        updatedAt = september.atTime(9, 0),
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 6,
        status = "TEST",
        createdAt = september.atTime(5, 30),
        updatedAt = september.atTime(6, 0),
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 5,
        status = "COUNTERSIGNED",
        createdAt = august.atTime(8, 30),
        updatedAt = august.atTime(9, 0),
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 4,
        status = "TEST",
        createdAt = june.atTime(10, 30),
        updatedAt = june.atTime(11, 0),
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 3,
        status = "COUNTERSIGNED",
        createdAt = june.atTime(9, 0),
        updatedAt = june.atTime(10, 0),
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 2,
        status = "TEST",
        createdAt = may.atTime(11, 30),
        updatedAt = may.atTime(12, 0),
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 1,
        status = "TEST",
        createdAt = may.atTime(10, 0),
        updatedAt = may.atTime(11, 0),
        entityType = EntityType.ASSESSMENT,
      ),
      VersionDetails(
        uuid = assessmentUuid,
        version = 0,
        status = "COUNTERSIGNED",
        createdAt = may.atTime(8, 0),
        updatedAt = may.atTime(9, 0),
        entityType = EntityType.ASSESSMENT,
      ),
    )

    val planVersions = listOf(
      VersionDetails(
        uuid = planUuid,
        version = 7,
        status = "COUNTERSIGNED",
        createdAt = september.atTime(9, 0),
        updatedAt = september.atTime(10, 0),
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 6,
        status = "TEST",
        createdAt = september.atTime(5, 0),
        updatedAt = september.atTime(6, 0),
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 5,
        status = "TEST",
        createdAt = august.atTime(9, 0),
        updatedAt = august.atTime(10, 0),
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 4,
        status = "TEST",
        createdAt = july.atTime(10, 30),
        updatedAt = july.atTime(11, 0),
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 3,
        status = "COUNTERSIGNED",
        createdAt = july.atTime(9, 0),
        updatedAt = july.atTime(10, 0),
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 2,
        status = "TEST",
        createdAt = may.atTime(11, 30),
        updatedAt = may.atTime(12, 0),
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 1,
        status = "TEST",
        createdAt = may.atTime(10, 0),
        updatedAt = may.atTime(11, 0),
        entityType = EntityType.PLAN,
      ),
      VersionDetails(
        uuid = planUuid,
        version = 0,
        status = "COUNTERSIGNED",
        createdAt = may.atTime(8, 0),
        updatedAt = may.atTime(9, 0),
        entityType = EntityType.PLAN,
      ),
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(assessmentVersions)
    factory.addVersions(planVersions)

    val expectedResponse = VersionsResponse(
      allVersions = sortedMapOf(Comparator.reverseOrder(),
        // checking that if there are versions on the same day but after countersigned assessment and plan, these are added on a separate row for 'All versions' table in the UI
        may to LastVersionsOnDate(
          description = "Assessment and plan updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 2,
            status = "TEST",
            createdAt = may.atTime(11, 30),
            updatedAt = may.atTime(12, 0),
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 2,
            status = "TEST",
            createdAt = may.atTime(11, 30),
            updatedAt = may.atTime(12, 0),
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
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 2,
            status = "TEST",
            createdAt = may.atTime(11, 30),
            updatedAt = may.atTime(12, 0),
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
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 4,
            status = "TEST",
            createdAt = july.atTime(10, 30),
            updatedAt = july.atTime(11, 0),
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
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 5,
            status = "TEST",
            createdAt = august.atTime(9, 0),
            updatedAt = august.atTime(10, 0),
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
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 7,
            status = "COUNTERSIGNED",
            createdAt = september.atTime(9, 0),
            updatedAt = september.atTime(10, 0),
            entityType = EntityType.PLAN,
          ),
        ),
      ),
      countersignedVersions = sortedMapOf(Comparator.reverseOrder(),
        // checking that if there are versions on the same day but after countersigned assessment and plan, these are added on a separate row for 'All versions' table in the UI
        may to LastVersionsOnDate(
          description = "Assessment and plan updated",
          assessmentVersion = VersionDetails(
            uuid = assessmentUuid,
            version = 0,
            status = "COUNTERSIGNED",
            createdAt = may.atTime(8, 0),
            updatedAt = may.atTime(9, 0),
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 0,
            status = "COUNTERSIGNED",
            createdAt = may.atTime(8, 0),
            updatedAt = may.atTime(9, 0),
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
            entityType = EntityType.ASSESSMENT,
          ),
          planVersion = VersionDetails(
            uuid = planUuid,
            version = 7,
            status = "COUNTERSIGNED",
            createdAt = september.atTime(9, 0),
            updatedAt = september.atTime(10, 0),
            entityType = EntityType.PLAN,
          ),
        ),
      ),
    )

    val actualResponse = factory.getVersionsResponse()

    assertEquals(expectedResponse.allVersions.entries.toList(), actualResponse.allVersions.entries.toList())
    assertEquals(expectedResponse.countersignedVersions.entries.toList(), actualResponse.countersignedVersions.entries.toList())

  }
}
