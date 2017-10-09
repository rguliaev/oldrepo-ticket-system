package com.tickets.services.imdb

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.tickets.ImdbConfig
import com.tickets.models.TicketEntry
import com.tickets.server._
import io.circe.parser._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.pipe
import akka.stream.Materializer
import com.tickets.services.imdb.ImdbService.{TicketOpt, WithDb}

object ImdbService {
  case class TicketOpt(entry: TicketEntry, newTitle: Boolean = false)
  case class WithDb(response: TicketResponse, entryOpt: Option[TicketOpt] = None)
}

class ImdbService(imdb: ImdbConfig)
                 (implicit system: ActorSystem, ec: ExecutionContext, m: Materializer) extends Actor with ActorLogging {

  private val titles: mutable.HashMap[String, String] = new mutable.HashMap[String, String]()
  private val db: ListBuffer[TicketEntry] = ListBuffer()

  def findWithIndex(id: String, screenId: String): Option[(TicketEntry, Int)] =
    db.zipWithIndex.find { case (entry, _) => entry.imdbId == id && entry.screenId == screenId }

  def find(id: String, screenId: String): Option[TicketEntry] =
    db.find(entry => entry.imdbId == id && entry.screenId == screenId)

  override def receive: Receive = {
    case request: RegisterRequest => pipe(register(request)).pipeTo(self)(sender)
    case request: ReserveRequest => sender ! reserve(request)
    case request: StateRequest => sender ! state(request)
    case WithDb(response, entryOpt) =>
      entryOpt.foreach { data =>
        if (data.newTitle) titles.put(data.entry.imdbId, data.entry.movieTitle)
        db += data.entry
      }
      sender ! response
  }

  def titleFromApi(id: String): Future[Either[Throwable, String]] = {
    val url = s"https://api.themoviedb.org/3/find/$id?api_key=${imdb.key}&external_source=imdb_id"

    val future = for {
      response <- Http().singleRequest(HttpRequest(uri = url))
      entity <- Unmarshal(response).to[String]
    } yield entity

    future.map { entity =>
      parse(entity).flatMap { json =>
        val cursor = json.hcursor
        cursor.downField("movie_results").downArray.first.get[String]("original_title")
      }
    } recover {
      case ex => Left(ex)
    }
  }

  def register(request: RegisterRequest): Future[WithDb] =
    titles.get(request.imdbId) match {
      case Some(title) =>
        find(request.imdbId, request.screenId) match {
          case Some(_) => Future.successful(WithDb(ErrorResponse("Found entry with the same title and screen")))
          case None =>
            Future.successful(WithDb(UnitResponse, Some(TicketOpt(request.toTicketEntry(title)))))
        }
      case None =>
        titleFromApi(request.imdbId).map {
          case Right(title) =>
            WithDb(UnitResponse, Some(TicketOpt(request.toTicketEntry(title), newTitle = true)))
          case Left(ex) =>
            log.error(ex.getMessage)
            WithDb(ErrorResponse(s"Not found title by id (${request.imdbId})"))
        }
    }

  def reserve(request: ReserveRequest): TicketResponse =
    findWithIndex(request.imdbId, request.screenId) match {
      case Some((entry, idx)) if entry.availableSeats > 0 =>
        db.update(idx, entry.copy(availableSeats = entry.availableSeats - 1))
        UnitResponse
      case Some(_) => ErrorResponse("No available seats")
      case None => movieNotFoundError
    }

  def state(request: StateRequest): TicketResponse =
    find(request.imdbId, request.screenId).map(_.toStateResponse).getOrElse(movieNotFoundError)

  lazy val movieNotFoundError = ErrorResponse("Not found the Movie")
}