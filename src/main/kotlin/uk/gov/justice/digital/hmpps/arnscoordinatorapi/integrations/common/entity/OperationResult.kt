package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

import org.springframework.http.HttpStatus

sealed class OperationResult<out T> {
  data class Success<T>(val data: T) : OperationResult<T>()

  data class Failure<T>(
    val errorMessage: String,
    val statusCode: HttpStatus? = null,
    val cause: Throwable? = null,
  ) : OperationResult<T>()

  inline fun onFailure(action: (String) -> Unit): OperationResult<T> {
    if (this is Failure) {
      action(this.errorMessage)
    }
    return this
  }
}
