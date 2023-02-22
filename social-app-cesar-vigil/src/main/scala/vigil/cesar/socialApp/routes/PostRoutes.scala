package vigil.cesar.socialApp.routes

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import vigil.cesar.socialApp.Main2.{IMAGE_FOLDER, postManagerActor, system, userRegistrationActor}
import vigil.cesar.socialApp.actors.UserRegistrationActor._
import vigil.cesar.socialApp.model.{Post, PostJsonProtocol, User, UserJsonProtocol}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, enrichAny}
import vigil.cesar.socialApp.Main.listFormat
import vigil.cesar.socialApp.actors.PostManagerActor._

import java.nio.file.Paths
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

object PostRoutes extends BasicRoute{

  implicit val timeout: Timeout = Timeout(2 seconds)



  val postRoutes: Route = //path for getting user date 'api/socialApp/user/{id}
  {
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
      }
  }

}
