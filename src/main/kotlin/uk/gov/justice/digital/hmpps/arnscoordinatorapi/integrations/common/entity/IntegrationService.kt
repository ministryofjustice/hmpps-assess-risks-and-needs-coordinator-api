package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

interface IntegrationService {
  fun create(createData: CreateData): OperationResult<VersionedEntity>
}
