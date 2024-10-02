package uk.gov.justice.digital.hmpps.arnscoordinatorapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://arns-coordinator-api-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("https://arns-coordinator-api-test.hmpps.service.justice.gov.uk").description("Test"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .info(
      Info().title("HMPPS ARNS Coordinator API").version(version)
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )
    .components(
      Components().addSecuritySchemes(
        "strengths-and-needs-oasys",
        SecurityScheme().addBearerJwtRequirement("ROLE_STRENGTHS_AND_NEEDS_OASYS"),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("template-kotlin-ui-role", listOf("read")))
}

private fun SecurityScheme.addBearerJwtRequirement(role: String): SecurityScheme =
  type(SecurityScheme.Type.HTTP)
    .scheme("bearer")
    .bearerFormat("JWT")
    .`in`(SecurityScheme.In.HEADER)
    .name("Authorization")
    .description("A HMPPS Auth access token with the `$role` role.")
