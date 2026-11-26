package uk.gov.justice.digital.hmpps.arnscoordinatorapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.EntityAssociationDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysAssociationEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.repository.OasysAssociationRepository
import java.util.UUID

@Service
class OasysAssociationsService(
  private val oasysAssociationRepository: OasysAssociationRepository,
) {
  fun findAssociationsByPk(oasysAssessmentPk: String, includeDeleted: Boolean = false): List<OasysAssociationEntity> = if (includeDeleted) {
    oasysAssociationRepository.findAllByOasysAssessmentPkIncludingDeleted(oasysAssessmentPk)
  } else {
    oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)
  }

  fun findAssociationsByPkAndType(oasysAssessmentPk: String, entityType: EntityType): List<OasysAssociationEntity> = oasysAssociationRepository.findAllByOasysAssessmentPkAndEntityType(oasysAssessmentPk, entityType)

  fun findDeletedAssociations(oasysAssessmentPk: String): List<OasysAssociationEntity> = oasysAssociationRepository.findAllDeletedByOasysAssessmentPk(oasysAssessmentPk)

  fun findOasysPkByEntityId(entityUuid: UUID): String? = oasysAssociationRepository.findAllByEntityUuid(entityUuid).maxByOrNull { it.createdAt }?.oasysAssessmentPk

  fun findLatestAssociationDetailsByEntityIds(entityUuids: Collection<UUID>): Map<UUID, EntityAssociationDetails> = oasysAssociationRepository
    .findAllByEntityUuidIn(entityUuids)
    .groupBy { it.entityUuid }
    .mapNotNull { (entityUuid, associations) ->
      val latest = associations.maxByOrNull { it.createdAt } ?: return@mapNotNull null
      val pk = latest.oasysAssessmentPk ?: return@mapNotNull null
      entityUuid to EntityAssociationDetails(
        oasysAssessmentPk = pk,
        regionPrisonCode = latest.regionPrisonCode,
        baseVersion = latest.baseVersion,
      )
    }
    .toMap()

  fun findAllIncludingDeleted(entityUuid: UUID): List<OasysAssociationEntity> = oasysAssociationRepository.findAllByEntityUuidIncludingDeleted(entityUuid)

  fun getExistingAssociationTypes(oasysAssessmentPk: String): List<EntityType> = oasysAssociationRepository
    .findAllByOasysAssessmentPk(oasysAssessmentPk)
    .map { it.entityType }
    .distinct()
    .filterNotNull()

  fun save(association: OasysAssociationEntity) = oasysAssociationRepository.save(association)
}
