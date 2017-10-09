package com.tickets.validators

import com.tickets.Common
import org.scalatest.{FlatSpec, Matchers}

class RequestValidatorsSpec extends FlatSpec with Matchers with Common {
  it should "validate right Incoming Requests" in {
    assert(registerRequest1.validate)
    assert(reserveRequest1.validate)
    assert(stateRequest1.validate)
  }

  it should "not validate wrong Incoming Requests" in {
    assert(!registerRequest1.copy(imdbId = "  ").validate)
    assert(!registerRequest1.copy(screenId = " ").validate)
    assert(!reserveRequest1.copy(imdbId = "").validate)
    assert(!reserveRequest1.copy(screenId = "").validate)
    assert(!stateRequest1.copy(imdbId = "").validate)
    assert(!stateRequest1.copy(screenId = "").validate)
  }
}
