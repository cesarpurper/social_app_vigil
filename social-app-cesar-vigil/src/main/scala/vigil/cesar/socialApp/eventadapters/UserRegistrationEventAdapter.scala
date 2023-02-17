package vigil.cesar.socialApp.eventadapters

import akka.persistence.journal.{EventAdapter, EventSeq}
import vigil.cesar.socialApp.actors.UserRegistrationActor._
import vigil.cesar.socialApp.model.User

object UserRegistrationEventAdapter{

  case class WrittenUserRegistration(userId: Int, name: String, email: String)
}

class UserRegistrationEventAdapter  extends EventAdapter {

  import UserRegistrationEventAdapter._

  override def manifest(event: Any): String = "URA"

  override def toJournal(event: Any): Any = event match {
    case event @ UserRegistered(user) =>
      WrittenUserRegistration(user.userId, user.name, user.email)
  }

  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case event @ WrittenUserRegistration(userId, name, email) =>
      EventSeq.single(UserRegistered(User(userId, name, email)))
    case other =>
      EventSeq.single(other)
  }
}
