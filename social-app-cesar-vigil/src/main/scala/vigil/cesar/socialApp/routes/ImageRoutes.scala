package vigil.cesar.socialApp.routes

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import vigil.cesar.socialApp.Main.IMAGE_FOLDER

import java.io.File

object ImageRoutes extends BasicRoute {

  def imgRoutes(): Route =

  //this serves the image from the imgs/ folder
    pathPrefix("image"){
      (path(Segment) & get) { imageFilename =>
        complete(HttpEntity.fromFile(MediaTypes.`image/webp`, new File(IMAGE_FOLDER + imageFilename)))
      }
    }


}
