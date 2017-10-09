package com.tickets.models

import com.tickets.server.StateResponse

case class ImdbEntry(imdbId: String, title: String)
case class TicketEntry(imdbId: String, screenId: String, movieTitle: String, availableSeats: Int, totalSeats: Int) {
  def toStateResponse = StateResponse(imdbId, screenId, movieTitle, availableSeats, totalSeats - availableSeats)
}