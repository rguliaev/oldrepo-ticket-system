package com.tickets.services.cinema

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.pipe
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.tickets.ImdbConfig
import com.tickets.models.CinemaEntry
import com.tickets.server.{CinemaRequest, _}
import io.circe.parser._

import scala.concurrent.{ExecutionContext, Future}
import akka.stream.Materializer
import cats.data.EitherT
import cats.implicits._
import com.tickets.services.cinema.CinemaService.{StateActorRequest, StateActorResponse}
import com.tickets.services.cinema.StateService.{CinemaResult, CinemaState}

object CinemaService {
  case class StateActorRequest(cinemaRequest: CinemaRequest, origin: ActorRef)
  case class StateActorResponse(cinemaResponse: CinemaResponse, origin: ActorRef)
}

class CinemaService(imdb: ImdbConfig)(implicit system: ActorSystem, ec: ExecutionContext, m: Materializer)
  extends Actor with ActorLogging {
  val stateActor: ActorRef = context.system.actorOf(Props(new StateService(imdb)))

  override def receive: Receive = {
    case cinemaRequest: CinemaRequest      => stateActor ! StateActorRequest(cinemaRequest, sender())
    case stateResponse: StateActorResponse => stateResponse.origin ! stateResponse.cinemaResponse
  }
}

object StateService {
  case class CinemaState(
    titles: Map[String, String] = Map.empty[String, String],
    db: Map[String, CinemaEntry] = Map.empty[String, CinemaEntry]
  ){
    def findByRequest(request: CinemaRequest): Option[CinemaEntry] = db.get(request.key)
    def updateTitle(id: String, title: String): CinemaState = this.copy(titles = titles + (id -> title))
    def updateDb(entry: CinemaEntry): CinemaState = this.copy(db = db + (entry.key -> entry))
  }

  case class CinemaResult(response: CinemaResponse, state: CinemaState)
}

class StateService(imdb: ImdbConfig)(implicit system: ActorSystem, ec: ExecutionContext, m: Materializer)
  extends Actor with ActorLogging {

  implicit class ToCinemaResult(in: EitherT[Future, CinemaResponse, CinemaResult]) {
    def asCinemaResult(state: CinemaState): Future[CinemaResult] = in.value.map {
      case Right(result) => result
      case Left(ex) => CinemaResult(ex, state)
    }
  }

  override def receive: Receive = active(CinemaState())

  def active(cinemaState: CinemaState): Receive = {
    case request: StateActorRequest =>
      context.become(waitForFuture(cinemaState, request.origin))
      handleRequest(request.cinemaRequest, cinemaState).asCinemaResult(cinemaState).pipeTo(self)(sender())
  }

  def waitForFuture(cinemaState: CinemaState, origin: ActorRef): Receive = {
    case request: StateActorRequest       => self forward request
    case CinemaResult(response, newState) =>
      context.become(active(newState))
      sender() ! StateActorResponse(response, origin)
  }

  def handleRequest[T <: CinemaRequest](body: T, cinemaState: CinemaState): EitherT[Future, CinemaResponse, CinemaResult] =
    body match {
      case request: RegisterRequest =>
        cinemaState.titles.get(request.imdbId) match {
          case Some(found) =>
            cinemaState.findByRequest(request) match {
              case Some(_) =>
                EitherT.pure(CinemaResult(ErrorResponse("Found entry with the same title and screen"), cinemaState))
              case None =>
                EitherT.pure(CinemaResult(UnitResponse, cinemaState.updateDb(request.toTicketEntry(found))))
            }
          case None =>
            titleFromApi(request.imdbId).transform {
              case Right(title) =>
                Right(
                  CinemaResult(
                    UnitResponse,
                    cinemaState.updateTitle(request.imdbId, title).updateDb(request.toTicketEntry(title))
                  )
                )
              case Left(ex) =>
                log.error(ex.getMessage)
                Left(ErrorResponse(s"Not found title by id (${request.imdbId})"))
            }
        }
      case request: ReserveRequest =>
        EitherT.pure(
          cinemaState.findByRequest(request) match {
            case Some(found) if found.availableSeats > 0 =>
              CinemaResult(UnitResponse, cinemaState.updateDb(found.copy(availableSeats = found.availableSeats - 1)))
            case Some(_) =>
              CinemaResult(ErrorResponse("No available seats"), cinemaState)
            case None =>
              CinemaResult(ErrorResponse("Not found the Movie"), cinemaState)
          }
        )
      case request: StateRequest =>
        EitherT.fromOption(cinemaState.findByRequest(request).map(found =>
          CinemaResult(found.toStateResponse, cinemaState)), ErrorResponse("Not found the Movie"))
    }

  def titleFromApi(id: String): EitherT[Future, Throwable, String] = {
    val url = s"https://api.themoviedb.org/3/find/$id?api_key=${imdb.key}&external_source=imdb_id"

    val future = for {
      response <- Http().singleRequest(HttpRequest(uri = url))
      entity <- Unmarshal(response).to[String]
    } yield entity

    EitherT(future.map { entity =>
      parse(entity).flatMap { json =>
        val cursor = json.hcursor
        cursor.downField("movie_results").downArray.first.get[String]("original_title")
      }
    } recover { case ex => Left(ex) })
  }
}