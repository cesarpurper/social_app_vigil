package vigil.cesar.socialApp.routes

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import vigil.cesar.socialApp.Main2.IMAGE_FOLDER

import java.io.File

object ImageRoutes extends BasicRoute {

  val imgRoutes: Route =

  //this serves the image from the imgs/ folder
    (get & path("image" / Segment)) { imageFilename =>
      complete(HttpEntity.fromFile(MediaTypes.`image/webp`, new File(IMAGE_FOLDER + imageFilename)))
    }


}
