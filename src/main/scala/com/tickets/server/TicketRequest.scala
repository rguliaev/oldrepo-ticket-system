package com.tickets.server

import com.tickets.models.TicketEntry

trait TicketRequest {
  val imdbId: String
  val screenId: String
  def validate: Boolean = !imdbId.contains(" ") && !screenId.contains(" ") && imdbId.nonEmpty && screenId.nonEmpty
}
case class RegisterRequest(imdbId: String, availableSeats: Int, screenId: String) extends TicketRequest {
  override def validate: Boolean =
    !imdbId.contains(" ") && !screenId.contains(" ") && imdbId.nonEmpty && availableSeats > 0 && screenId.nonEmpty
  def toTicketEntry(title: String) = TicketEntry(imdbId, screenId, title, availableSeats, availableSeats)
}
case class ReserveRequest(imdbId: String, screenId: String) extends TicketRequest
case class StateRequest(imdbId: String, screenId: String) extends TicketRequest

trait TicketResponse
case class StateResponse(
  imdbId: String,
  screenId: String,
  movieTitle: String,
  availableSeats: Int,
  reservedSeats: Int
) extends TicketResponse
case object UnitResponse extends TicketResponse
case class ErrorResponse(msg: String) extends TicketResponse