package com.tickets.services

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestKit}
import com.tickets.server._
import com.tickets.{Common, TicketConfig, TicketConfigLoader}
import com.tickets.services.imdb.ImdbService
import org.scalatest._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class ImdbServiceSpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with TicketConfigLoader with Common {

  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val config: TicketConfig = loadConfig.get
  val imbdActor: ActorRef = system.actorOf(Props(new ImdbService(config.imdb)))

  override def afterAll(): Unit =
    Http().shutdownAllConnectionPools().onComplete(_ => TestKit.shutdownActorSystem(system))

  "An ImdbService actor" must {

    "register a Fight Club request and return State" in {
      imbdActor ! registerRequest1
      expectMsg(UnitResponse)
      imbdActor ! stateRequest1
      expectMsg(stateResponse1)
    }

    "not register a Fight Club two times" in {
      imbdActor ! registerRequest1
      expectMsg(ErrorResponse("Found entry with the same title and screen"))
    }

    "register a Fight Club two times with different screens" in {
      imbdActor ! registerRequest2
      expectMsg(UnitResponse)
      imbdActor ! stateRequest2
      expectMsg(stateResponse2)
    }

    s"reserve all seats out of $totalSeats" in {
      (1 to totalSeats).foreach(_ => imbdActor ! reserveRequest1)
      receiveN(totalSeats, FiniteDuration(3, java.util.concurrent.TimeUnit.SECONDS))
      imbdActor ! stateRequest1
      expectMsg(stateResponse1.copy(reservedSeats = totalSeats, availableSeats = 0))
    }

    "not reserve a seat when no available seats" in {
      imbdActor ! reserveRequest1
      expectMsg(ErrorResponse("No available seats"))
    }
  }
}