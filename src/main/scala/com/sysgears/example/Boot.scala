package com.sysgears.example

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.event.{LoggingAdapter, Logging}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.sysgears.example.service.ExampleService
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import spray.can.Http

/**
 * Application entry point.
 */
object Boot extends App {

  // Initialize actor system
  implicit val system: ActorSystem = ActorSystem("zookeeper-config-example")

  // Set execution context
  implicit val context: ExecutionContext = system.dispatcher

  // Set request timeout
  implicit val timeout: Timeout = Timeout(30L, TimeUnit.SECONDS)

  // Initialize logger
  val log: LoggingAdapter = Logging(system, this.getClass)

  // Initialize HTTP service actor
  val exampleService: ActorRef = system.actorOf(Props[ExampleService], "example-service")

  // Obtain HTTP bind parameters
  val f = exampleService ? "bind-parameters"
  f.onSuccess {
    case bindParams: (String, Int) => {
      log.info("Binding Example HTTP Service to {}:{}...", bindParams._1, bindParams._2)
      IO(Http) ! Http.Bind(exampleService, interface = bindParams._1, port = bindParams._2)
    }
    case any: Any => log.error("Failed to execute bind command. Illegal bind parameters received: {}".format(any))
  }
  f.onFailure {
    case t: Throwable => log.error("Unable to start service: {}", t.getLocalizedMessage)
  }
}