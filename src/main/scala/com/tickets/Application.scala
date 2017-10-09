package com.tickets

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.tickets.services.http.HttpService
import com.tickets.services.imdb.ImdbService
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object Application extends App with TicketConfigLoader with Logging {

  implicit val system: ActorSystem = ActorSystem("tickets-system")
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  loadConfig match {
    case Success(config) =>
      val imdbService: ActorRef = system.actorOf(Props(new ImdbService(config.imdb)))
      val router = new HttpService(imdbService)
      val bindingFuture = Http().bindAndHandle(router.route, config.server.host, config.server.port)
      log.info("Ticket service has been started ...")
      sys.addShutdownHook {
        log.info("Shutting down...")
        val theEnd = for {
          bind <- bindingFuture
          _ <- bind.unbind()
          _ <- Http().shutdownAllConnectionPools()
        } yield ()

        theEnd.onComplete(_ => system.terminate())
      }
    case Failure(ex) =>
      log.error(ex.getMessage)
      system.terminate()
  }
}

case class TicketConfig(server: ServerConfig, imdb: ImdbConfig)
case class ServerConfig(host: String, port: Int)
case class ImdbConfig(key: String)

trait TicketConfigLoader {
  def loadConfig: Try[TicketConfig] = Try {
    val conf = ConfigFactory.load()
    val server = ServerConfig(conf.getString("server.host"), conf.getInt("server.port"))
    val imdb = ImdbConfig(conf.getString("imdb.key"))
    TicketConfig(server, imdb)
  }
}

trait Logging {
  val log: Logger = LoggerFactory.getLogger(getClass)
}