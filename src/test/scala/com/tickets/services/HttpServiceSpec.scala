package com.tickets.services

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestKit
import com.tickets.server.StateResponse
import com.tickets.services.http.HttpService
import com.tickets.{Common, TicketConfig, TicketConfigLoader}
import com.tickets.services.imdb.ImdbService
import org.scalatest.{Matchers, WordSpec}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.duration._

class HttpServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with TicketConfigLoader with Common {

  val config: TicketConfig = loadConfig.get
  val imbdActor: ActorRef = system.actorOf(Props(new ImdbService(config.imdb)))
  val service = new HttpService(imbdActor)

  override def afterAll(): Unit = {
    Http.get(system).shutdownAllConnectionPools().onComplete(_ => TestKit.shutdownActorSystem(system))
  }

  implicit val timeout = RouteTestTimeout(5.seconds)

  "The service" should {
    "pass register request with right data" in {
      Post("/tickets/register", registerRequest1) ~> service.route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "pass reserve request with right data" in {
      Post("/tickets/reserve", reserveRequest1) ~> service.route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "pass state request with right data" in {
      Get("/tickets/state", stateRequest1) ~> service.route ~> check {
        status shouldEqual StatusCodes.OK
        val stateResponse = responseAs[StateResponse]
        stateResponse.imdbId shouldEqual stateRequest1.imdbId
        stateResponse.availableSeats shouldEqual totalSeats - stateResponse.reservedSeats
      }
    }

    "not pass register request with wrong data" in {
      Post("/tickets/register", registerRequest1.copy(imdbId = "")) ~> service.route ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] == "Invalid validation"
      }
    }

    "not pass reserve request with wrong data" in {
      Post("/tickets/reserve", reserveRequest1.copy(screenId = "")) ~> service.route ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] == "Invalid validation"
      }
    }

    "not pass state request with wrong data" in {
      Get("/tickets/state", stateRequest1.copy(imdbId = "", screenId = "")) ~> service.route ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] == "Invalid validation"
      }
    }
  }
}
