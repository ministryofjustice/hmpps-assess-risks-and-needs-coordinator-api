package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDate
import java.util.UUID

class VersionsResponseFactoryTest {
  @Test
  fun `addVersions should correctly add assessment version`() {
    val date = LocalDate.of(2025, 6, 24)
    val uuid = UUID.randomUUID()

    val versions = listOf(
      VersionDetails(
        uuid = uuid,
        version = 1,
        status = "TEST",
        createdAt = date.atTime(10, 0),
        updatedAt = date.atTime(11, 0),
        entityType = EntityType.ASSESSMENT,
      ),
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(versions)

    val expectedResponse = VersionsResponse(
      versions = sortedMapOf(
        date to VersionsOnDate(
          description = "Assessment updated",
          assessmentVersions = versions.toMutableList(),
        ),
      ),
    )

    assertEquals(expectedResponse, factory.getVersionsResponse())
  }

  @Test
  fun `addVersions should correctly add plan version`() {
    val date = LocalDate.of(2025, 6, 24)
    val uuid = UUID.randomUUID()

    val versions = listOf(
      VersionDetails(
        uuid = uuid,
        version = 1,
        status = "TEST",
        createdAt = date.atTime(10, 0),
        updatedAt = date.atTime(11, 0),
        entityType = EntityType.PLAN,
      ),
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(versions)

    val expectedResponse = VersionsResponse(
      versions = sortedMapOf(
        date to VersionsOnDate(
          description = "Plan updated",
          planVersions = versions.toMutableList(),
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

    val assessmentVersions = listOf(
      VersionDetails(
        uuid = assessmentUuid,
        version = 1,
        status = "TEST",
        createdAt = date.atTime(10, 0),
        updatedAt = date.atTime(11, 0),
        entityType = EntityType.ASSESSMENT,
      ),
    )

    val planVersions = listOf(
      VersionDetails(
        uuid = planUuid,
        version = 1,
        status = "TEST",
        createdAt = date.atTime(10, 0),
        updatedAt = date.atTime(11, 0),
        entityType = EntityType.PLAN,
      ),
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(assessmentVersions)
    factory.addVersions(planVersions)

    val expectedResponse = VersionsResponse(
      versions = sortedMapOf(
        date to VersionsOnDate(
          description = "Assessment and plan updated",
          assessmentVersions = assessmentVersions.toMutableList(),
          planVersions = planVersions.toMutableList(),
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

    val assessmentVersions = listOf(
      VersionDetails(
        uuid = assessmentUuid,
        version = 1,
        status = "TEST",
        createdAt = assessmentDate.atTime(10, 0),
        updatedAt = assessmentDate.atTime(11, 0),
        entityType = EntityType.ASSESSMENT,
      ),
    )

    val planVersions = listOf(
      VersionDetails(
        uuid = planUuid,
        version = 1,
        status = "TEST",
        createdAt = planDate.atTime(10, 0),
        updatedAt = planDate.atTime(11, 0),
        entityType = EntityType.PLAN,
      ),
    )

    val factory = VersionsResponseFactory()
    factory.addVersions(assessmentVersions)
    factory.addVersions(planVersions)

    val expectedResponse = VersionsResponse(
      versions = sortedMapOf(
        planDate to VersionsOnDate(
          description = "Plan updated",
          planVersions = planVersions.toMutableList(),
          assessmentVersions = assessmentVersions.toMutableList(),
        ),
        assessmentDate to VersionsOnDate(
          description = "Assessment updated",
          assessmentVersions = assessmentVersions.toMutableList(),
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
      versions = sortedMapOf(
        assessmentDate2 to VersionsOnDate(
          description = "Assessment updated",
          assessmentVersions = mutableListOf(
            VersionDetails(
              uuid = assessmentUuid,
              version = 2,
              status = "TEST",
              createdAt = assessmentDate2.atTime(10, 0),
              updatedAt = assessmentDate2.atTime(11, 0),
              entityType = EntityType.ASSESSMENT,
            ),
          ),
          planVersions = mutableListOf(
            VersionDetails(
              uuid = planUuid,
              version = 2,
              status = "TEST",
              createdAt = planDate.atTime(10, 0),
              updatedAt = planDate.atTime(11, 0),
              entityType = EntityType.PLAN,
            ),
          ),
        ),
        planDate to VersionsOnDate(
          description = "Plan updated",
          assessmentVersions = mutableListOf(
            VersionDetails(
              uuid = assessmentUuid,
              version = 1,
              status = "TEST",
              createdAt = assessmentDate.atTime(10, 0),
              updatedAt = assessmentDate.atTime(11, 0),
              entityType = EntityType.ASSESSMENT,
            ),
          ),
          planVersions = mutableListOf(
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
          ),
        ),
        assessmentDate to VersionsOnDate(
          description = "Assessment and plan updated",
          assessmentVersions = mutableListOf(
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
          ),
          planVersions = mutableListOf(
            VersionDetails(
              uuid = planUuid,
              version = 0,
              status = "TEST",
              createdAt = assessmentDate.atTime(8, 0),
              updatedAt = assessmentDate.atTime(9, 0),
              entityType = EntityType.PLAN,
            ),
          ),
        ),
      ),
    )

    assertEquals(expectedResponse, factory.getVersionsResponse())
  }
}