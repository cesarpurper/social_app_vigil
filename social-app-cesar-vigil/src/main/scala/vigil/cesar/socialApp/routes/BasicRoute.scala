package vigil.cesar.socialApp.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import spray.json.DefaultJsonProtocol
import vigil.cesar.socialApp.dto.LoginRequestJsonProtocol
import vigil.cesar.socialApp.model.{PostJsonProtocol, UserJsonProtocol}


trait BasicRoute  extends DefaultJsonProtocol with SprayJsonSupport  with UserJsonProtocol with PostJsonProtocol with LoginRequestJsonProtocol {


  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)




}
