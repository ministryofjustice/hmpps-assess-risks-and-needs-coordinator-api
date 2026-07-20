package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentTypeConfig

@Component
class EntityTypeValidator(
  val config: AssessmentTypeConfig,
) : ConstraintValidator<EnabledEntityType, String> {

  override fun isValid(type: String?, context: ConstraintValidatorContext): Boolean {
    if (type == null) return false

    return config.enabledEntityTypes().contains(runCatching { EntityType.valueOf(type) }.getOrElse { null })
  }
}