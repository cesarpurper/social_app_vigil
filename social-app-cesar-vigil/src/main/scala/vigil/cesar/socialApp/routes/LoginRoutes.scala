package vigil.cesar.socialApp.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol
import vigil.cesar.socialApp.Main.userRegistrationActor
import vigil.cesar.socialApp.model._

import scala.util._
import akka.pattern.ask
import akka.util.Timeout
import authentikat.jwt.{JsonWebToken, JwtHeader}
import vigil.cesar.socialApp.actors.UserRegistrationActor._
import vigil.cesar.socialApp.dto.LoginRequest
import vigil.cesar.socialApp.util.JWTUtils

import scala.concurrent.duration._
import vigil.cesar.socialApp.model.{User, UserJsonProtocol}
import vigil.cesar.socialApp.util.JWTUtils.{accessTokenHeaderName, header, secretKey}

object LoginRoutes extends BasicRoute{

  implicit val timeout: Timeout = Timeout(2 seconds)


  def loginRoutes(): Route =
      pathPrefix("login") {
        post {
          entity(as[LoginRequest]) {
          case lr @ LoginRequest(userName, userEmail) =>
            onComplete(userRegistrationActor ? GetUserByEmailAndName(userName, userEmail)) {
              case Failure(exception) =>
                complete(StatusCodes.Unauthorized)
              case Success(optionUser: Option[User]) => {
                optionUser match {
                  case None => complete(StatusCodes.Unauthorized)
                  case Some(user) =>
                    val claims = JWTUtils.setClaims(user.email)
                    respondWithHeader(RawHeader(accessTokenHeaderName, JsonWebToken(header, claims, secretKey))) {
                      complete(StatusCodes.OK)
                    }
                }
              }
            }
        }
      }
    }

}
