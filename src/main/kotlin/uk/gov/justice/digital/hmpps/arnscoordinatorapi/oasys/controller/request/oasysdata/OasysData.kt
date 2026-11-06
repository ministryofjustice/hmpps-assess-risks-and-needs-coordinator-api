package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.oasysdata

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Constraints

data class OasysData(
  @Schema(description = "3.4 Suitability of accommodation", example = "0")
  @JsonProperty("o3-4")
  val q3p4SuitabilityOfAccommodation: ZeroToTwoOrMissing?,

  @Schema(description = "4.3 Employment history", example = "1")
  @JsonProperty("o4-3")
  val q4p3EmploymentHistory: ZeroToTwoOrMissing?,

  @Schema(description = "4.9 Any educational or formal professional/vocational qualifications", example = "2")
  @JsonProperty("o4-9")
  val q4p9Qualifications: ZeroOrTwoOrMissing?,

  @Schema(description = "8.1 Drugs ever misused (in custody or community)", example = "NO")
  @JsonProperty("o8-1")
  val q8p1DrugsEverMisused: YesNo?,

  @Schema(description = "8.8 Motivation to tackle drug misuse", example = "M")
  @JsonProperty("o8-8")
  val q8p8MotivationToTackleDrugMisuse: ZeroToTwoOrMissing?,

  @Schema(description = "9.2 Binge drinking or excessive of alcohol in last 6 months", example = "0")
  @JsonProperty("o9-2")
  val q9p2BingeDrinking: ZeroToTwoOrMissing?,

  @Schema(description = "4.8 Has learning difficulties (optional)", example = "2")
  @JsonProperty("o4-8")
  val q4p8LearningDifficulties: ZeroToTwoOrMissing?,

  @Schema(description = "10.1 Difficulties coping", example = "2")
  @JsonProperty("o10-1")
  val q10p1DifficultiesCoping: ZeroToTwo?,

  @Schema(description = "10.4 Offender's attitude to themselves", example = "2")
  @JsonProperty("o10-4")
  val q10p4AttitudeToThemselves: ZeroToTwo?,

  @Schema(description = "6.9 Parental responsibilities", example = "YES")
  @JsonProperty("o6-9")
  val q6p9ParentalResponsibilities: YesNo?,

  @Schema(description = "6.1 Current relationship with close family members", example = "0")
  @JsonProperty("o6-1")
  val q6p1RelationshipWithCloseFamily: ZeroToTwoOrMissing?,

  @Schema(description = "6.3 Experience of childhood", example = "2")
  @JsonProperty("o6-3")
  val q6p3ExperienceOfChildhood: ZeroToTwoOrMissing?,

  @Schema(description = "10.7 Evidence of childhood behavioural problems", example = "YES")
  @JsonProperty("o10-7_V2_CHILDHOOD")
  val q10p7ChildhoodBehaviouralProblems: YesNo?,

  @Schema(description = "6.8 Current relationship status", example = "1")
  @JsonProperty("o6-8")
  val q6p8CurrentRelationshipStatus: RelationshipStatus?,

  @Schema(description = "6.4 Current relationship with partner", example = "1")
  @JsonProperty("o6-4")
  val q6p4RelationshipWithPartner: ZeroToTwoOrMissing?,

  @Schema(description = "11.7 Awareness of consequences", example = "1")
  @JsonProperty("o11-7")
  val q11p7AwarenessOfConsequences: ZeroToTwo?,

  @Schema(description = "11.6 Problem solving skills", example = "2")
  @JsonProperty("o11-6")
  val q11p6ProblemSolvingSkills: ZeroToTwo?,

  @Schema(description = "11.9 Understands other people's views", example = "2")
  @JsonProperty("o11-9")
  val q11p9UnderstandsOtherPeoplesViews: ZeroToTwo?,

  @Schema(description = "11.4 Temper control", example = "2")
  @JsonProperty("o11-4")
  val q11p4TemperControl: ZeroToTwo?,

  @Schema(description = "11.2 Impulsivity", example = "2")
  @JsonProperty("o11-2")
  val q11p2Impulsivity: ZeroToTwo?,

  @Schema(description = "12.9 Hostile Orientation", example = "2")
  @JsonProperty("o12-9")
  val q12p9HostileOrientation: ZeroToTwo?,

  @Schema(description = "12.4 Attitude towards supervision/licence", example = "2")
  @JsonProperty("o12-4")
  val q12p4AttitudeTowardsSupervision: ZeroToTwo?,

  @Schema(description = "12.1 Pro-criminal attitudes Attitude towards supervision/licence", example = "2")
  @JsonProperty("o12-1")
  val q12p1ProCriminalAttitudes: ZeroToTwo?,

  @Schema(description = "1.30 Have they ever committed a sexual or sexually motivated offence?", example = "YES")
  @JsonProperty("o1-30")
  val q1p30SexualOrSexuallyMotivatedOffence: YesNo?,

  @Schema(description = "2.1 Brief offence(s) details (indicate what exactly happened, when, where and how)", example = "See the details")
  @JsonProperty("o2-1")
  @field:Size(max = Constraints.OASYS_FREE_TEXT_MAX_CHAR_LENGTH)
  val q2p1BriefOffenceDetails: String?,

  @Schema(description = "2.8 Why did it happen - evidence of motivation and triggers", example = "See the details")
  @JsonProperty("o2-8")
  @field:Size(max = Constraints.OASYS_FREE_TEXT_MAX_CHAR_LENGTH)
  val q2p8MotivationAndTriggers: String?,

  @Schema(description = "2.7 Were there other offenders involved?", example = "YES")
  @JsonProperty("o2-7")
  val q2p7OtherOffendersInvolved: YesNo?,

  @Schema(description = "2.7.1 Number of others involved", example = "15+")
  @JsonProperty("o2-7-1")
  val q2p7p1NumberOfOthersInvolved: NumberOfOthersInvolved?,

  @Schema(description = "2.6 Does the offender recognise the impact and consequences of offending on victim, community/wider society?", example = "NO")
  @JsonProperty("o2-6")
  val q2p6RecognisesImpactOfOffending: YesNo?,

  @Schema(description = "2.11 Does the offender accept responsibility for the current offence(s)", example = "YES")
  @JsonProperty("o2-11")
  val q2p11AcceptsResponsibility: YesNo?,

  @Schema(description = "2.12 Pattern of offending (consider details of previous convictions)", example = "See the details")
  @JsonProperty("o2-12")
  @field:Size(max = Constraints.OASYS_FREE_TEXT_MAX_CHAR_LENGTH)
  val q2p12PatternOfOffending: String?,

  @Schema(description = "2.99 Analysis of offence issues linked to risk of serious harm, risks to the individual and other risks", example = "NO")
  @JsonProperty("o2-99")
  val q2p99OffenceAnalysisLinkedToRisk: YesNo?,
)
