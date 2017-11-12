package com.tickets.models

import com.tickets.server.{KeyKeeper, StateResponse}

case class CinemaEntry(
  imdbId: String,
  screenId: String,
  movieTitle: String,
  availableSeats: Int,
  totalSeats: Int)
extends KeyKeeper {
  def toStateResponse = StateResponse(imdbId, screenId, movieTitle, availableSeats, totalSeats - availableSeats)
}