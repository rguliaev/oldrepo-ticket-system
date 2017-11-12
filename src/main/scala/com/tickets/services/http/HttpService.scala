package com.tickets.services.http

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import com.tickets.server._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import akka.pattern.ask
import akka.util.Timeout
import com.tickets.Logging
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class HttpService(imdbService: ActorRef)(implicit executionContext: ExecutionContext) extends Logging {

  implicit val timeout: Timeout = Timeout(5.seconds)

  private def processRequest[T <: CinemaRequest](implicit um: FromRequestUnmarshaller[T]): Route =
    entity(as[T]) { request =>
      if (request.validate) run(request) else complete((400, "Invalid validation"))
    }

  private def run[T <: CinemaRequest](data: T) = {
    val future = (imdbService ? data).mapTo[CinemaResponse] recover { case ex =>
      log.error(ex.getMessage)
      ErrorResponse("Something went wrong")
    }
    onSuccess(future) {
      case UnitResponse => complete(200)
      case state: StateResponse => complete((200, state))
      case error: ErrorResponse => complete((400, error.msg))
    }
  }

  val route: Route =
    pathPrefix("tickets") {
      post {
        path("register") {
          processRequest[RegisterRequest]
        } ~
          path("reserve") {
            processRequest[ReserveRequest]
          }
      } ~
      get {
        path("state") {
          processRequest[StateRequest]
        }
      }
    }
}