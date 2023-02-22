package vigil.cesar.socialApp.dto

import spray.json.DefaultJsonProtocol
import vigil.cesar.socialApp.model.Post

trait LoginRequestJsonProtocol extends DefaultJsonProtocol {
  implicit val loginRequestFormat = jsonFormat2(LoginRequest)
}
case class LoginRequest(userName: String, userEmail: String)
