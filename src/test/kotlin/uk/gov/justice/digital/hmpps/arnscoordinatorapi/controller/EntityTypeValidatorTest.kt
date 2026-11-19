package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller

import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentTypeConfig

class EntityTypeValidatorTest {
  val context: ConstraintValidatorContext = mock()

  val validator = EntityTypeValidator(
    config = AssessmentTypeConfig(
      planType = EntityType.AAP_PLAN,
      assessmentType = EntityType.AAP_SAN,
    ),
  )

  @Test
  fun `returns true when supported by the AssessmentTypeConfig`() {
    setOf("AAP_PLAN", "AAP_SAN").forEach { type ->
      assertTrue { validator.isValid(type, context) }
    }
  }

  @Test
  fun `returns false when not supported by the AssessmentTypeConfig`() {
    setOf("ASSESSMENT", "NOT_AN_ENTITY_TYPE").forEach { type ->
      assertFalse { validator.isValid(type, context) }
    }
  }
}
