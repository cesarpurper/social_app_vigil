package vigil.cesar.socialApp.eventadapters

import akka.persistence.journal.{EventAdapter, EventSeq}
import vigil.cesar.socialApp.model.Post

object PostManagerEventAdapter{

  case class WrittenPostRegistration(postId: Int, contents: String, image: String, dateCreated: String, dateEdited: String, author: String, authorId: Int)
  case class WrittenPostEdit(postId: Int, contents: String, image: String, dateCreated: String, dateEdited: String, author: String, authorId: Int)
}

/**
 * This Event adapter is configured in the application.conf and is responsible for translating the persisted Message
 * into the actual message that is going to be persisted. This helps schema evolution becaus in case of schema changes,
 * we will handle them here, making it seamless to the actor trying to persist something.
 */
class PostManagerEventAdapter  extends EventAdapter {

  import PostManagerEventAdapter._
  import vigil.cesar.socialApp.actors.PostManagerActor._
  override def manifest(event: Any): String = "PMA"

  //this is called right before persisting messages to Journal. It catches the PostRegistered/PostEdited messages
  //and converts it to the message relative to the current schema
  override def toJournal(event: Any): Any = event match {
    case event @ PostRegistered(Post(postId, contents, image, dateCreated, dateEdited, author, authorId)) =>
      WrittenPostRegistration(postId, contents, image, dateCreated, dateEdited, author, authorId)
    case event@PostEdited(Post(postId, contents, image, dateCreated, dateEdited, author, authorId)) =>
      WrittenPostEdit(postId, contents, image, dateCreated, dateEdited, author, authorId)
  }

  //this is called during recovering before the actor receives the message. It converts the message relative to
  //the schema AT THE TIME IT WAS RECORDED, and transforms it to the default PostRegistered/PostEdited message, that is known to
  //the actor
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case event @ WrittenPostRegistration(postId, contents, image, dateCreated, dateEdited, author, authorId) =>
      EventSeq.single(PostRegistered(Post(postId, contents, image, dateCreated, dateEdited, author, authorId)))
    case event@WrittenPostEdit(postId, contents, image, dateCreated, dateEdited, author, authorId) =>
      EventSeq.single(PostRegistered(Post(postId, contents, image, dateCreated, dateEdited, author, authorId)))
    case other =>
      EventSeq.single(other)
  }
}
