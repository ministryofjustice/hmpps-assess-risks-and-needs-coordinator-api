package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

enum class EntityType {
  ASSESSMENT,
  PLAN,
}

@Entity
@Table(name = "oasys_associations", schema = "coordinator")
data class OasysAssociation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(nullable = false)
  val uuid: UUID? = UUID.randomUUID(),

  @Column(name = "created_at", nullable = false)
  val createdAt: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),

  @Enumerated(EnumType.STRING)
  @Column(name = "entity_type", nullable = false)
  var entityType: EntityType? = null,

  @Column(name = "entity_uuid", nullable = false)
  var entityUuid: UUID? = null,

  @Column(name = "oasys_assessment_pk", length = 64, nullable = false)
  val oasysAssessmentPk: String? = null,

  @Column(name = "region_prison_code", length = 64)
  val regionPrisonCode: String? = null,

  @Column(nullable = false)
  val deleted: Boolean? = false,
)
