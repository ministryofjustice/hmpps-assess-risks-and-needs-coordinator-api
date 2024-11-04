package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OasysUserDetailsValidationTest {

  lateinit var validator: Validator

  @BeforeAll
  fun setUpBeforeAll() {
    val factory = Validation.buildDefaultValidatorFactory()
    validator = factory.validator
  }

  @Test
  fun `Valid OasysUserDetails validation will have no violations`() {
    val userName = "Test Name"
    val fifteenCharId = "ABCDEFGHIJKLMNO"
    val oasysUserDetails = OasysUserDetails(id = fifteenCharId, name = userName)

    val violations: Set<ConstraintViolation<OasysUserDetails>> = validator.validate(oasysUserDetails)

    assertThat(violations).hasSize(0)
  }

  @Test
  fun `Invalid OasysUserDetails will produce size violations`() {
    val userName = "SomebodyHasAReallyLongFirstName ItsAlmostAsLongAsTheirSurnameButNotQuite"
    val sixteenCharId = "ABCDEFGHIJKLMNOP"
    val oasysUserDetails = OasysUserDetails(id = sixteenCharId, name = userName)

    assertThat(validator.validate(oasysUserDetails)).hasSize(2)

    assertThat(validator.validateProperty(oasysUserDetails, "name").first().message).isEqualTo("size must be between 0 and 64")
    assertThat(validator.validateProperty(oasysUserDetails, "id").first().message).isEqualTo("size must be between 0 and 15")
  }
}
