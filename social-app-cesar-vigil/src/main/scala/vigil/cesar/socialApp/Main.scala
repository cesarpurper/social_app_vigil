package vigil.cesar.socialApp

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import authentikat.jwt.JwtHeader
import com.typesafe.config.ConfigFactory
import vigil.cesar.socialApp.actors.UserRegistrationActor.RegisterUser
import vigil.cesar.socialApp.actors.{HttpApi, PostManagerActor, UserRegistrationActor}

import scala.concurrent.ExecutionContextExecutor



object Main extends App{
  implicit val system = ActorSystem("SocialApp", ConfigFactory.load())
  implicit val materializer = ActorMaterializer()

  val IMAGE_FOLDER = "src/main/resources/imgs/"




  val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], "registrationManager")
  val postManagerActor = system.actorOf(Props[PostManagerActor], "postManagerActor")

  system.actorOf(HttpApi("0.0.0.0", 8080, userRegistrationActor), "http-server")

}
