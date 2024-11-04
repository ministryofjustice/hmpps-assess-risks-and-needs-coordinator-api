package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysTransferAssociation

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OasysTransferAssociationValidationTest {

  lateinit var validator: Validator

  @BeforeAll
  fun setUpBeforeAll() {
    val factory = Validation.buildDefaultValidatorFactory()
    validator = factory.validator
  }

  @Test
  fun `Valid OasysTransferAssociation validation will have no violations`() {
    val oasysTransferAssociation = OasysTransferAssociation(oldOasysAssessmentPK = "1", newOasysAssessmentPK = "2")

    val violations: Set<ConstraintViolation<OasysTransferAssociation>> = validator.validate(oasysTransferAssociation)

    assertThat(violations).hasSize(0)
  }

  @Test
  fun `Invalid OasysTransferAssociation will produce size violations`() {
    val invalidOasysAssessmentPK = "1234567890123456"
    val oasysTransferAssociation = OasysTransferAssociation(invalidOasysAssessmentPK, invalidOasysAssessmentPK)

    assertThat(validator.validate(oasysTransferAssociation)).hasSize(2)

    assertThat(validator.validateProperty(oasysTransferAssociation, "oldOasysAssessmentPK").first().message).isEqualTo("size must be between 1 and 15")
    assertThat(validator.validateProperty(oasysTransferAssociation, "newOasysAssessmentPK").first().message).isEqualTo("size must be between 1 and 15")
  }

  @Test
  fun `Invalid OasysTransferAssociation will produce numeric only violations`() {
    val invalidOasysAssessmentPK = "1A"
    val oasysTransferAssociation = OasysTransferAssociation(invalidOasysAssessmentPK, invalidOasysAssessmentPK)

    assertThat(validator.validate(oasysTransferAssociation)).hasSize(2)

    assertThat(validator.validateProperty(oasysTransferAssociation, "oldOasysAssessmentPK").first().message).isEqualTo("Must only contain numeric characters")
    assertThat(validator.validateProperty(oasysTransferAssociation, "newOasysAssessmentPK").first().message).isEqualTo("Must only contain numeric characters")
  }
}
