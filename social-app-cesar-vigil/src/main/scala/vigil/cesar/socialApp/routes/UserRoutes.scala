package vigil.cesar.socialApp.routes

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import vigil.cesar.socialApp.Main.{system, userRegistrationActor}
import vigil.cesar.socialApp.actors.UserRegistrationActor._
import vigil.cesar.socialApp.model.{User, UserJsonProtocol}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, enrichAny}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object UserRoutes extends BasicRoute {

  implicit val timeout: Timeout = Timeout(2 seconds)

  def userCreateRoute(): Route = {
    //path for creating a user
    pathPrefix("register"){
      (path("user") & post) {
        println("user1")
        //it gets data from Multipart.FormData, with the name fields passed as parameters
        formFields('userName, 'userEmail) { (userName, userEmail) =>
          //tell user registration actor to register given user
          userRegistrationActor ! RegisterUser(userName, userEmail)
          complete(StatusCodes.OK)
        }
      }
    }
  }


  def userRoutesWithoutCreate(): Route =  //path for getting user date 'api/socialApp/user/{id}
    //path for getting user date 'api/socialApp/user/{id}
    pathPrefix("user"){
      (path(IntNumber) & get) { userId =>
        //TODO: when user doesnt exist it is replying 'null'. Maybe try to respond with BAD REQUEST
        complete(
          (userRegistrationActor ? GetUserById(userId))
            .mapTo[Option[User]]
            .map(_.toJson.prettyPrint)
            .map(toHttpEntity)
        )
      } ~
        //get all users
        (path("all") & get) {
          complete(
            (userRegistrationActor ? GetAllUsers)
              .mapTo[List[User]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
    }


}
