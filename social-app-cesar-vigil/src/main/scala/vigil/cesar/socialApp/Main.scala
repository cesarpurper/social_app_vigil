package vigil.cesar.socialApp

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import vigil.cesar.socialApp.actors._
import vigil.cesar.socialApp.model._
import vigil.cesar.socialApp.actors.UserRegistrationActor._
import vigil.cesar.socialApp.actors.PostManagerActor._
import akka.pattern.ask
import spray.json._

import java.io.File
import java.nio.file.Paths
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main extends App
                    with UserJsonProtocol
                    with PostJsonProtocol{

  implicit val defaultTimeout = Timeout(2 seconds)
  implicit val system = ActorSystem("SocialApp", ConfigFactory.load())
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], "registrationManager")

  val postManagerActor = system.actorOf(Props[PostManagerActor], "postManagerActor")

  val IMAGE_FOLDER = "src/main/resources/imgs/"
  val SOCIAL_APP_PATH = "api/socialApp/"
  val SERVER_URL = ConfigFactory.load().getString("server-url")


  //helper method to envelope the payload in a Json HttpEntity
  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  //the main social app route object
  val socialAppServerRoute =
    // all requests to the social app must start with this prefix
    pathPrefix("api" / "socialApp") {
      //prifex for the requests starting with /api/socialApp/user
      pathPrefix("user") {
        //path for getting user date 'api/socialApp/user/{id}
        (get & path(IntNumber)) { userId =>
          //TODO: when user doesnt exist it is replying 'null'. Maybe try to respond with BAD REQUEST
          complete(
            (userRegistrationActor ? GetUserById(userId))
              .mapTo[Option[User]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        } ~
        //path for creating a user
        (post & path("create")) {
          //it gets data from Multipart.FormData, with the name fields passed as parameters
          formFields('userName, 'userEmail) { (userName, userEmail) =>
            //tell user registration actor to register given user
            userRegistrationActor ! RegisterUser(userName, userEmail)
            complete(StatusCodes.OK)
          }
        } ~
        //get all users
        (get & path("all")) {
          complete(
            (userRegistrationActor ? GetAllUsers)
              .mapTo[List[User]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
      //prefix for the requests starting with /api/socialApp/post
      pathPrefix("post") {
        //path for getting creating post 'api/socialApp/post/create/{usrId}
        (path("create" / IntNumber) & post & toStrictEntity(3.seconds)) { userId =>
          extractRequestContext { ctx =>
            implicit val materializer = ctx.materializer
            //using data from Multipart.Formdata with name fields passed as parameter
            formField('content, 'image) { (content, image) =>

              fileUpload("image") {
                case (metadata, byteSource) => {

                  //Because a simple storage strategy is being used, to handle possible duplicate images
                  //this millis seconds is prepended to the filename
                  val finalFileName = System.currentTimeMillis() + "_" + metadata.fileName
                  //this directive runs when the file is already in disk
                  onComplete(byteSource.runWith(FileIO.toPath(Paths.get(IMAGE_FOLDER + finalFileName)))) {
                    case Failure(exception) => complete(StatusCodes.BadRequest)
                    case Success(value) => {
                      val userFuture: Future[Option[User]] = (userRegistrationActor ? GetUserById(userId)).mapTo[Option[User]]
                      complete {
                        userFuture.map {
                          case None => StatusCodes.BadRequest
                          case Some(user) => {
                            //check if it was sent any image in the form
                            if (image != "")
                              postManagerActor ! CreatePost(content, finalFileName, user)
                            else
                              postManagerActor ! CreatePost(content, "", user)
                            StatusCodes.OK
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        } ~
        //path for getting editing posts 'api/socialApp/post/edit/{postId}
        (path("edit" / IntNumber) & post & toStrictEntity(3.seconds)) { postId =>
          extractRequestContext { ctx =>
            implicit val materializer = ctx.materializer
            //using data from Multipart.Formdata with name fields passed as parameter
            formField('content, 'image) { (content, image) =>
              fileUpload("image") {
                case (metadata, byteSource) => {
                  //Because a simple storage strategy is being used, to handle possible duplicate images
                  //this millis seconds is prepended to the filename
                  val finalFileName = System.currentTimeMillis() + "_" + metadata.fileName
                  //this directive runs when the file is already in disk
                  onComplete(byteSource.runWith(FileIO.toPath(Paths.get(IMAGE_FOLDER + finalFileName)))) {
                    case Failure(exception) => complete(StatusCodes.BadRequest)
                    case Success(value) => {
                      //check if it was sent any image in the form
                      if (image != "")
                        postManagerActor ! EditPost(postId, content, finalFileName)
                      else
                        postManagerActor ! EditPost(postId, content, "")

                      complete(StatusCodes.OK)
                    }
                  }
                }
              }
            }
          }
        }
      } ~
      //get posts with query parameters, sortOrderAsc as 1/0 and userId
      (get & path("posts" / IntNumber / IntNumber)) { (sortOrderAsc, userId) =>
        //get in boolean the value sent by parameter in the URL
        val finalSortOrder = if (sortOrderAsc == 0)
          false
        else
          true

        //
        complete(
          (postManagerActor ? GetPostsWithParams(finalSortOrder, userId))
            .mapTo[List[Post]]
            //this modified version prepend the link to the server with the appropriate path to query the server for the image
            .map(_.toJson.prettyPrint)
            //.map(_.map(x => Post(x.postId,x.contents, SERVER_URL+SOCIAL_APP_PATH+"image/"+x.image, x.dateCreated, x.dateEdited, x.author, x.authorId)).toJson.prettyPrint)
            .map(toHttpEntity)
        )
      } ~
      //this serves the image from the imgs/ folder
      (get & path("image" / Segment)) { imageFilename =>
        complete(HttpEntity.fromFile(MediaTypes.`image/webp`, new File(IMAGE_FOLDER + imageFilename)))
      }
    }

  Http().bindAndHandle(socialAppServerRoute, "0.0.0.0", 8080)
  println("Server Started!")


}
