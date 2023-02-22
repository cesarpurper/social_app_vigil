package vigil.cesar.socialApp.model

import spray.json.DefaultJsonProtocol
import vigil.cesar.socialApp.util.Utils


//this trait is responsible for converting the Post object 'to' and 'from' JSON

trait PostJsonProtocol extends DefaultJsonProtocol {
  implicit val postFormat = jsonFormat7(Post)
}
case class Post(postId: Int, contents: String, image: String, dateCreated: String, dateEdited: String, author: String, authorId: Int) extends Ordered[Post]{

  //the overwritten implementation from Ordered to compare to posts according to the date they were created.
  //this will later be used to sort posts in a list
  override def compare(that: Post): Int = Utils.convertStringToDate(dateCreated).compareTo(Utils.convertStringToDate(that.dateCreated))
}
