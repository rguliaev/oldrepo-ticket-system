package com.tickets.server

import com.tickets.models.CinemaEntry

trait KeyKeeper {
  val imdbId: String
  val screenId: String
  val key: String = imdbId + screenId
}

trait CinemaRequest extends KeyKeeper {
  def validate: Boolean = !imdbId.contains(" ") && !screenId.contains(" ") && imdbId.nonEmpty && screenId.nonEmpty
}
case class RegisterRequest(imdbId: String, availableSeats: Int, screenId: String) extends CinemaRequest {
  override def validate: Boolean =
    !imdbId.contains(" ") && !screenId.contains(" ") && imdbId.nonEmpty && availableSeats > 0 && screenId.nonEmpty
  def toTicketEntry(title: String) = CinemaEntry(imdbId, screenId, title, availableSeats, availableSeats)
}
case class ReserveRequest(imdbId: String, screenId: String) extends CinemaRequest
case class StateRequest(imdbId: String, screenId: String) extends CinemaRequest

trait CinemaResponse
case class StateResponse(
  imdbId: String,
  screenId: String,
  movieTitle: String,
  availableSeats: Int,
  reservedSeats: Int
) extends CinemaResponse
case object UnitResponse extends CinemaResponse
case class ErrorResponse(msg: String) extends CinemaResponse