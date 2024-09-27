package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

sealed class OperationResult<out T> {
  data class Success<T>(val data: T) : OperationResult<T>()

  data class Failure<T>(
    val errorMessage: String,
    val cause: Throwable? = null,
  ) : OperationResult<T>()

  inline fun onSuccess(action: (T) -> Unit): OperationResult<T> {
    if (this is Success) {
      action(this.data)
    }
    return this
  }

  inline fun onFailure(action: (String) -> Unit): OperationResult<T> {
    if (this is Failure) {
      action(this.errorMessage)
    }
    return this
  }
}
