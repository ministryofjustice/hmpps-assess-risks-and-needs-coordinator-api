package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.oasysdata
import com.fasterxml.jackson.annotation.JsonValue

enum class ZeroToTwoOrMissing(@get:JsonValue val value: String) {
  ZERO("0"),
  ONE("1"),
  TWO("2"),
  MISSING("M"),
}

enum class ZeroOrTwoOrMissing(@get:JsonValue val value: String) {
  ZERO("0"),
  TWO("2"),
  MISSING("M"),
}

enum class ZeroToTwo(@get:JsonValue val value: String) {
  ZERO("0"),
  ONE("1"),
  TWO("2"),
}

enum class YesNo(@get:JsonValue val value: String) {
  YES("YES"),
  NO("NO"),
}

enum class RelationshipStatus(@get:JsonValue val value: String) {
  IN_RELATIONSHIP_LIVING_TOGETHER("1"),
  IN_RELATIONSHIP_NOT_LIVING_TOGETHER("2"),
  NOT_IN_RELATIONSHIP("3"),
}

enum class NumberOfOthersInvolved(@get:JsonValue val value: String) {
  ZERO("0"),
  ONE("1"),
  TWO("2"),
  THREE("3"),
  FOUR("4"),
  FIVE("5"),
  SIX_TO_TEN("6-10"),
  ELEVEN_TO_FIFTEEN("11-15"),
  FIFTEEN_PLUS("15+"),
}