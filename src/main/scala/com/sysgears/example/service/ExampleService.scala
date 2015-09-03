package com.sysgears.example.service

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import com.sysgears.example.config.ZooKeeperConfiguration
import com.sysgears.example.model.BindParameters
import org.apache.curator.framework.CuratorFramework
import spray.can.Http
import spray.http._

/**
 * HTTP Service actor.
 */
class ExampleService(implicit val zkClient: CuratorFramework) extends Actor with ActorLogging with ZooKeeperConfiguration {

  override def preStart() {
    log.info(s"Example service is running in $Environment environment")
  }

  override def postStop() {
    log.info("Example service has been terminated")
  }

  def receive = {

    case "bind-parameters" =>
      val host = getSetting(s"$Service.host").asString
      val port = getSetting(s"$Service.port").asInt
      sender() ! BindParameters(host, port)

    case _: Http.Connected =>
      sender() ! Http.Register(self)

    case HttpRequest(HttpMethods.GET, Uri.Path("/example"), _, _, _) =>
      val body = HttpEntity(MediaTypes.`text/html`, getIndex)
      sender() ! HttpResponse(status = StatusCodes.OK, entity = body)

    case HttpRequest(HttpMethods.GET, Uri.Path("/ping"), _, _, _) =>
      val body = HttpEntity(MediaTypes.`text/html`, "<h2>pong!</h2>")
      sender() ! HttpResponse(status = StatusCodes.OK, entity = body)

    case _: HttpRequest =>
      val body = HttpEntity(MediaTypes.`text/html`, "<h2>HTTP 404 - Not Found</h2>")
      sender() ! HttpResponse(status = StatusCodes.NotFound, entity = body)

    case _ =>
      val body = HttpEntity(MediaTypes.`text/html`, "<h2>HTTP 500 - Internal Server Error</h2>")
      sender() ! HttpResponse(status = StatusCodes.InternalServerError, entity = body)
  }

  private def getIndex: String = {
    def buildDatabaseURL: String = s"${getSetting("db.host").asString}${getSetting(s"$Service.db.name").asString}" +
      s"?user=${getSetting(s"$Service.db.user").asString}&password=${getSetting(s"$Service.db.password").asString}"

    s"""
      |<html>
      | <head>
      |   <title>Index</title>
      | </head>
      | <body>
      |   <h1>Welcome to the <i>$Service service</i></h1>
      |   <h3>Resources:</h3>
      |   <ul>
      |     <li><a href="/ping">/ping</a></li>
      |   </ul>
      |   <h3>Configuration:</h3>
      |   <table>
      |     <tr>
      |       <td>Environment</td><td><b>$Environment</b></td>
      |     </tr>
      |     <tr>
      |       <td>Startup Time</td><td>${new Date(context.system.startTime)}</td>
      |     </tr>
      |     <tr>
      |       <td>Host</td><td>${getSetting(s"$Service.host").asString}</td>
      |     </tr>
      |     <tr>
      |       <td>Port</td><td>${getSetting(s"$Service.port").asInt}</td>
      |     </tr>
      |     <tr>
      |       <td>Database URL</td><td>$buildDatabaseURL</td>
      |     </tr>
      |     <tr>
      |       <td>Max. Number of Connections</td><td>${getSetting("db.maxConnections").asInt}</td>
      |     </tr>
      |   </table>
      | </body>
      |</html>
    """.stripMargin
  }
}