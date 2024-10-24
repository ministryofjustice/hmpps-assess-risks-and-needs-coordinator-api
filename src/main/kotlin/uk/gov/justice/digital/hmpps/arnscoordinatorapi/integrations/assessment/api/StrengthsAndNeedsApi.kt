package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CounterSignAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.RollbackData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.AssessmentResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UndeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["app.strategies.assessment"], havingValue = "true")
class StrengthsAndNeedsApi(
  val sanApiWebClient: WebClient,
  val apiProperties: StrengthsAndNeedsApiProperties,
) {

  fun createAssessment(createData: CreateAssessmentData): ApiOperationResult<VersionedEntity> {
    return try {
      val result = sanApiWebClient.post()
        .uri(apiProperties.endpoints.create)
        .body(BodyInserters.fromValue(createData))
        .retrieve()
        .bodyToMono(AssessmentResponse::class.java)
        .map {
          VersionedEntity(
            id = it.metaData.uuid,
            version = it.metaData.versionNumber,
            entityType = EntityType.ASSESSMENT,
          )
        }
        .block()

      result?.let {
        ApiOperationResult.Success(it)
      } ?: throw IllegalStateException("Unexpected error during createAssessment")
    } catch (ex: WebClientResponseException) {
      ApiOperationResult.Failure("HTTP error during create assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}", ex)
    } catch (ex: Exception) {
      ApiOperationResult.Failure("Unexpected error during createAssessment: ${ex.message}", ex)
    }
  }

  fun cloneAssessment(createData: CreateAssessmentData, assessmentUuid: UUID): ApiOperationResult<VersionedEntity> {
    return try {
      val result = sanApiWebClient.post()
        .uri(apiProperties.endpoints.clone.replace("{uuid}", assessmentUuid.toString()))
        .body(BodyInserters.fromValue(createData))
        .retrieve()
        .bodyToMono(AssessmentResponse::class.java)
        .map {
          VersionedEntity(
            id = it.metaData.uuid,
            version = it.metaData.versionNumber,
            entityType = EntityType.ASSESSMENT,
          )
        }
        .block()

      result?.let {
        ApiOperationResult.Success(it)
      } ?: throw IllegalStateException("Unexpected error during cloneAssessment")
    } catch (ex: WebClientResponseException) {
      ApiOperationResult.Failure("HTTP error during clone assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}", ex)
    } catch (ex: Exception) {
      ApiOperationResult.Failure("Unexpected error during cloneAssessment: ${ex.message}", ex)
    }
  }

  fun lockAssessment(lockData: LockData, assessmentUuid: UUID): ApiOperationResultExtended<VersionedEntity> {
    return try {
      val result = sanApiWebClient.post()
        .uri(apiProperties.endpoints.lock.replace("{uuid}", assessmentUuid.toString()))
        .body(BodyInserters.fromValue(lockData))
        .retrieve()
        .bodyToMono(AssessmentResponse::class.java)
        .map {
          VersionedEntity(
            id = it.metaData.uuid,
            version = it.metaData.versionNumber,
            entityType = EntityType.ASSESSMENT,
          )
        }
        .block()

      result?.let {
        ApiOperationResultExtended.Success(it)
      } ?: throw IllegalStateException("Unexpected error during lockAssessment")
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode.value() == HttpStatus.CONFLICT.value()) {
        return ApiOperationResultExtended.Conflict("Assessment already locked")
      }
      ApiOperationResultExtended.Failure("HTTP error during lock assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}")
    } catch (ex: Exception) {
      ApiOperationResultExtended.Failure("Unexpected error during lockAssessment: ${ex.message}", ex)
    }
  }

  fun rollback(data: RollbackData, assessmentUuid: UUID): ApiOperationResultExtended<VersionedEntity> {
    return try {
      val result = sanApiWebClient.post()
        .uri(apiProperties.endpoints.rollback.replace("{uuid}", assessmentUuid.toString()))
        .body(BodyInserters.fromValue(data))
        .retrieve()
        .bodyToMono(AssessmentResponse::class.java)
        .map {
          VersionedEntity(
            id = it.metaData.uuid,
            version = it.metaData.versionNumber,
            entityType = EntityType.ASSESSMENT,
          )
        }
        .block()

      result?.let {
        ApiOperationResultExtended.Success(it)
      } ?: throw IllegalStateException("Unexpected error during rollback")
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode.value() == HttpStatus.CONFLICT.value()) {
        return ApiOperationResultExtended.Conflict("Unable to roll back this assessment version")
      }
      ApiOperationResultExtended.Failure("HTTP error during rollback assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}")
    } catch (ex: Exception) {
      ApiOperationResultExtended.Failure("Unexpected error during rollbackAssessment: ${ex.message}", ex)
    }
  }

  fun signAssessment(signData: SignData, assessmentUuid: UUID): ApiOperationResultExtended<VersionedEntity> {
    return try {
      val result = sanApiWebClient.post()
        .uri(apiProperties.endpoints.sign.replace("{uuid}", assessmentUuid.toString()))
        .body(BodyInserters.fromValue(signData))
        .retrieve()
        .bodyToMono(AssessmentResponse::class.java)
        .map {
          VersionedEntity(
            id = it.metaData.uuid,
            version = it.metaData.versionNumber,
            entityType = EntityType.ASSESSMENT,
          )
        }
        .block()

      result?.let {
        ApiOperationResultExtended.Success(it)
      } ?: throw IllegalStateException("Unexpected error during signAssessment")
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode.value() == HttpStatus.CONFLICT.value()) {
        return ApiOperationResultExtended.Conflict("Assessment already signed")
      }
      ApiOperationResultExtended.Failure("HTTP error during sign assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}")
    } catch (ex: Exception) {
      ApiOperationResultExtended.Failure("Unexpected error during signAssessment: ${ex.message}", ex)
    }
  }

  fun getAssessment(assessmentUuid: UUID): ApiOperationResult<AssessmentResponse> {
    return try {
      val result = sanApiWebClient.get()
        .uri("${apiProperties.endpoints.fetch}/$assessmentUuid")
        .retrieve()
        .bodyToMono(AssessmentResponse::class.java)
        .block()

      result?.let {
        ApiOperationResult.Success(it)
      } ?: throw IllegalStateException("Unexpected null response during getAssessment")
    } catch (ex: WebClientResponseException) {
      ApiOperationResult.Failure("HTTP error during get assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}", ex)
    } catch (ex: Exception) {
      ApiOperationResult.Failure(errorMessage = "Unexpected error during getAssessment: ${ex.message}", cause = ex)
    }
  }

  fun counterSign(assessmentUuid: UUID, data: CounterSignAssessmentData): ApiOperationResultExtended<VersionedEntity> {
    return try {
      val result = sanApiWebClient.post()
        .uri(apiProperties.endpoints.counterSign.replace("{uuid}", assessmentUuid.toString()))
        .body(BodyInserters.fromValue(data))
        .retrieve()
        .bodyToMono(AssessmentResponse::class.java)
        .map {
          VersionedEntity(
            id = it.metaData.uuid,
            version = it.metaData.versionNumber,
            entityType = EntityType.ASSESSMENT,
          )
        }
        .block()

      result?.let {
        ApiOperationResultExtended.Success(it)
      } ?: throw IllegalStateException("Unexpected error during counterSign")
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode.value() == HttpStatus.CONFLICT.value()) {
        return ApiOperationResultExtended.Conflict("Assessment could not be counter-signed, Response body: ${ex.responseBodyAsString}")
      }
      ApiOperationResultExtended.Failure("HTTP error during counterSign assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}")
    } catch (ex: Exception) {
      ApiOperationResultExtended.Failure("Unexpected error during counterSignAssessment: ${ex.message}", ex)
    }
  }

  fun undelete(data: UndeleteData, assessmentUuid: UUID): ApiOperationResultExtended<VersionedEntity> {
    return try {
      val result = sanApiWebClient.post()
        .uri(apiProperties.endpoints.undelete.replace("{uuid}", assessmentUuid.toString()))
        .body(BodyInserters.fromValue(data))
        .retrieve()
        .bodyToMono(AssessmentResponse::class.java)
        .map {
          VersionedEntity(
            id = it.metaData.uuid,
            version = it.metaData.versionNumber,
            entityType = EntityType.ASSESSMENT,
          )
        }
        .block()

      result?.let {
        ApiOperationResultExtended.Success(it)
      } ?: throw IllegalStateException("Unexpected error during undelete")
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode.value() == HttpStatus.CONFLICT.value()) {
        return ApiOperationResultExtended.Conflict("Unable to undelete the requested assessment versions: ${ex.responseBodyAsString}")
      }
      ApiOperationResultExtended.Failure("HTTP error during undelete assessment versions: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}")
    } catch (ex: Exception) {
      ApiOperationResultExtended.Failure("Unexpected error during undeleteAssessmentVersions: ${ex.message}", ex)
    }
  }

  sealed class ApiOperationResult<out T> {
    data class Success<T>(val data: T) : ApiOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : ApiOperationResult<T>() {
      init {
        LoggerFactory.getLogger(StrengthsAndNeedsApi::class.java).error(errorMessage)
      }
    }
  }

  sealed class ApiOperationResultExtended<out T> {
    data class Success<T>(val data: T) : ApiOperationResultExtended<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : ApiOperationResultExtended<T>() {
      init {
        LoggerFactory.getLogger(StrengthsAndNeedsApi::class.java).error(errorMessage)
      }
    }

    data class Conflict<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : ApiOperationResultExtended<T>() {
      init {
        LoggerFactory.getLogger(StrengthsAndNeedsApi::class.java).error(errorMessage)
      }
    }
  }
}
