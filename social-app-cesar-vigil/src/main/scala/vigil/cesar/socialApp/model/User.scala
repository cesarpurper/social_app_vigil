package vigil.cesar.socialApp.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol


//this trait is responsible for converting the User object 'to' and 'from' JSON
trait UserJsonProtocol extends DefaultJsonProtocol{
  // step 3
  implicit val userFormat = jsonFormat3(User.apply)
}
case class User(userId: Int, name: String, email: String)
