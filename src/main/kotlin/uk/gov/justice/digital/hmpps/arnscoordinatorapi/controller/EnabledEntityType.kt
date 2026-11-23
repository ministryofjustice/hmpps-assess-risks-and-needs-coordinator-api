package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [EntityTypeValidator::class])
annotation class EnabledEntityType(
  val message: String = "Entity type is not supported",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)
