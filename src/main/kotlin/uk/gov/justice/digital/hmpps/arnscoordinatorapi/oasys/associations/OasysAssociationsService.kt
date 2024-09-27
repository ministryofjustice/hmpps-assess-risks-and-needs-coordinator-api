package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository

@Service
class OasysAssociationsService(
  private val oasysAssociationRepository: OasysAssociationRepository,
) {

  fun ensureNoExistingAssociation(oasysAssessmentPk: String): OperationResult<Unit> {
    val associations = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)

    return if (associations.isNotEmpty()) {
      val types = associations.map { it.entityType }.distinct()
      OperationResult.Failure("Existing associations found for ${types.joinToString(", ")}")
    } else {
      OperationResult.Success(Unit)
    }
  }

  fun storeAssociation(association: OasysAssociation): OperationResult<Unit> {
    return try {
      oasysAssociationRepository.save(association)
      OperationResult.Success(Unit)
    } catch (ex: Exception) {
      OperationResult.Failure("Failed to store association: ${ex.message}", ex)
    }
  }
}