package com.sysgears.example

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.sysgears.example.config.ZooKeeperConfiguration
import com.sysgears.example.model.BindParameters
import com.sysgears.example.service.ExampleService
import org.apache.curator.framework.CuratorFramework
import spray.can.Http

import scala.concurrent.ExecutionContext

/**
 * Application entry point.
 */
object Boot extends App with ZooKeeperConfiguration {

  // Initializes actor system
  implicit val system: ActorSystem = ActorSystem("zookeeper-config-example")

  // Sets execution context
  implicit val context: ExecutionContext = system.dispatcher

  // Sets request timeout
  implicit val timeout: Timeout = Timeout(30L, TimeUnit.SECONDS)

  // Initializes logger
  val log: LoggingAdapter = Logging(system, this.getClass)

  // Initializes ZooKeeper client
  val zkClient: CuratorFramework = initZooKeeperClient(service = Service, environment = Environment)

  // Initializes HTTP service actor
  val exampleService: ActorRef = system.actorOf(Props(classOf[ExampleService], zkClient), "example-service")

  // Obtains HTTP bind parameters
  val f = exampleService ? "bind-parameters"
  f.onSuccess {
    case BindParameters(serviceHost, servicePort) =>
      log.info("Binding Example HTTP Service to {}:{}...", serviceHost, servicePort)
      IO(Http) ! Http.Bind(exampleService, interface = serviceHost, port = servicePort)
    case any: Any => log.error("Failed to execute bind command. Illegal bind parameters received: {}", any)
  }
  f.onFailure {
    case t: Throwable => log.error("Unable to start service: {}", t.getLocalizedMessage)
  }
}