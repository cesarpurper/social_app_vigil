package vigil.cesar.socialApp.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.stream.ActorMaterializer
import spray.json._

import scala.util.{Failure, Success}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import vigil.cesar.socialApp.routes.PostRoutes._
import vigil.cesar.socialApp.routes.ImageRoutes._
import vigil.cesar.socialApp.routes.LoginRoutes._
import vigil.cesar.socialApp.util.JWTUtils
import vigil.cesar.socialApp.util.JWTUtils.secretKey

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._



object HttpApi extends DefaultJsonProtocol
  with SprayJsonSupport{

  import authentikat.jwt._
  import HttpApi._



  import vigil.cesar.socialApp.routes.UserRoutes._

  private def authenticated: Directive1[Map[String, Any]] =
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(jwt) if JWTUtils.isTokenExpired(jwt) =>
        complete(StatusCodes.Unauthorized -> "Session expired.")

      case Some(jwt) if JsonWebToken.validate(jwt, secretKey) =>
        provide(JWTUtils.getClaims(jwt))

      case _ => complete(StatusCodes.Unauthorized)
    }


  private def securedContent: Route =
    authenticated { claims =>
      //TODO: Use claims to check whether the user is trying to edit own post
      postRoutes() ~ userRoutesWithoutCreate() ~ imgRoutes()
    }
  def routes: Route =
    pathPrefix("api" / "socialApp") {

      securedContent ~ loginRoutes() ~ userCreateRoute()
    }

  def apply(host: String, port: Int, userRegistrationActor: ActorRef) = Props(new HttpApi(host, port, userRegistrationActor))

}
final class HttpApi(host: String, port: Int, userRegistrationActor: ActorRef) extends Actor with ActorLogging{

  import HttpApi._

  implicit val timeout: Timeout = Timeout(2 seconds)

  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  Http(context.system).bindAndHandle(routes, host, port).pipeTo(self)

  override def receive: Receive = {
    case ServerBinding(address) =>
      log.info("Server successfully bound at {}:{}", address.getHostName, address.getPort)
      //user admin is created just for initialization. without it we can't register the first user
    case Failure(cause) =>
      log.error("Failed to bind server", cause)
      context.system.terminate()
  }


}
