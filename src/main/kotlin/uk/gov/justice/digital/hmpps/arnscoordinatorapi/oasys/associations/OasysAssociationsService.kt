package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import java.util.UUID

@Service
class OasysAssociationsService(
  private val oasysAssociationRepository: OasysAssociationRepository,
) {
  fun findAssociationsByPk(oasysAssessmentPk: String, includeDeleted: Boolean = false): List<OasysAssociation> = if (includeDeleted) {
    oasysAssociationRepository.findAllByOasysAssessmentPkIncludingDeleted(oasysAssessmentPk)
  } else {
    oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)
  }

  fun findAssociationsByPkAndType(oasysAssessmentPk: String, entityTypes: Collection<EntityType>): List<OasysAssociation> = oasysAssociationRepository.findAllByOasysAssessmentPkAndEntityTypeIn(oasysAssessmentPk, entityTypes)

  fun findDeletedAssociations(oasysAssessmentPk: String): List<OasysAssociation> = oasysAssociationRepository.findAllDeletedByOasysAssessmentPk(oasysAssessmentPk)

  fun findOasysPkByEntityId(entityUuid: UUID): String? = oasysAssociationRepository.findAllByEntityUuid(entityUuid).maxByOrNull { it.createdAt }?.oasysAssessmentPk

  fun findAllIncludingDeleted(entityUuid: UUID): List<OasysAssociation> = oasysAssociationRepository.findAllByEntityUuidIncludingDeleted(entityUuid)

  fun ensureNoExistingAssociation(oasysAssessmentPk: String): OperationResult<Unit> {
    val associations = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)

    return if (associations.isNotEmpty()) {
      val types = associations.map { it.entityType }.distinct()
      OperationResult.Failure("Existing associations found for ${types.joinToString(", ")}")
    } else {
      OperationResult.Success(Unit)
    }
  }

  fun storeAssociation(association: OasysAssociation): OperationResult<Unit> = try {
    oasysAssociationRepository.save(association)
    OperationResult.Success(Unit)
  } catch (ex: Exception) {
    OperationResult.Failure("Failed to store association: ${ex.message}", cause = ex)
  }
}
