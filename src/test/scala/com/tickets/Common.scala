package com.tickets

import com.tickets.server.{RegisterRequest, ReserveRequest, StateRequest, StateResponse}

trait Common {
  val totalSeats = 100
  val fclubId = "tt0137523"
  val screenId1 = "screen1_123456"
  val screenId2 = "screen2_123456"
  val stateRequest1 = StateRequest(fclubId, screenId1)
  val stateRequest2 = StateRequest(fclubId, screenId2)
  val registerRequest1 = RegisterRequest(fclubId, totalSeats, screenId1)
  val registerRequest2 = RegisterRequest(fclubId, totalSeats, screenId2)
  val reserveRequest1 = ReserveRequest(fclubId, screenId1)
  val stateResponse1 = StateResponse(registerRequest1.imdbId, registerRequest1.screenId, "Fight Club",
    registerRequest1.availableSeats, 0)
  val stateResponse2 = StateResponse(registerRequest1.imdbId, registerRequest2.screenId, "Fight Club",
    registerRequest1.availableSeats, 0)
}
