package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import kotlin.random.Random

class RandomOasysPk {
  private val usedOasysPks: MutableSet<String> = mutableSetOf()

  fun get(): String = Random.nextLong(1, 999_999_999_999_999).toString()
    .takeIf { !usedOasysPks.contains(it) }
    ?.also { usedOasysPks.add(it) }
    ?: get()
}
