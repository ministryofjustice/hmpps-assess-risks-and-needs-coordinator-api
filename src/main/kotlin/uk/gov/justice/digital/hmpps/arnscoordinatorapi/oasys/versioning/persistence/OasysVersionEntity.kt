package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import java.time.LocalDateTime
import java.util.UUID

enum class OasysEvent {
  AWAITING_COUNTERSIGN,
  AWAITING_DOUBLE_COUNTERSIGN,
  CLONED,
  COUNTERSIGNED,
  CREATED,
  DOUBLE_COUNTERSIGNED,
  LOCKED,
  REJECTED,
  RESET,
  ROLLED_BACK,
  SELF_SIGNED,
}

@Entity
@Table(name = "oasys_version", schema = "coordinator")
@SQLRestriction("deleted IS FALSE")
class OasysVersionEntity(
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "uuid", nullable = false)
  var uuid: UUID = UUID.randomUUID(),

  @Column(name = "created_at", nullable = false)
  var createdAt: LocalDateTime = Clock.now(),

  @Enumerated(EnumType.STRING)
  @Column(name = "created_by", nullable = false)
  var createdBy: OasysEvent,

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = Clock.now(),

  @Column(name = "version", nullable = false)
  var version: Long,

  @Column(name = "entity_uuid", nullable = false)
  var entityUuid: UUID,

  @Column(name = "deleted")
  var deleted: Boolean = false,
)
