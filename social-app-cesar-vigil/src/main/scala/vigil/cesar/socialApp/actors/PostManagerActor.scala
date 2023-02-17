package vigil.cesar.socialApp.actors

import akka.actor.{ActorLogging}
import akka.persistence.PersistentActor
import vigil.cesar.socialApp.model.{Post, User}
import vigil.cesar.socialApp.util.Utils

import java.util.Date

object PostManagerActor {

  //protocol
  case class PostRegisteredAck(post: Post)
  case class GetPostsWithParams(sortDateAsc: Boolean, userId: Int)
  case object PostEditedAck
  case object PostDoesntExist
  case class CreatePost(content: String, image: String, user: User)
  case class EditPost(postId: Int, content: String, image: String)

  //events
  case class PostRegistered(post: Post)
  case class PostEdited(post: Post)


}
case class PostManagerActor() extends PersistentActor with ActorLogging {

  import PostManagerActor._
  override def persistenceId: String = "post-manager-actor"

  //listening is called to be the starter receiving function with no parameter.
  //because it has default parameters, it starst with them currentId = 1 and an empty map
  override def receiveCommand: Receive = listening()

  //This is the function that will receive messages in this actor
  private def listening(currentId: Int = 1, posts: Map[Int, List[Post]] = Map()): Receive = {
    // case to handle the CreatePost message
    case CreatePost(contents, image, user) =>{
      log.info(s"persisting post $contents")
      val dateCreatedAsString = Utils.getDateAsString(new Date())
      val newPost = Post(currentId, contents, image, dateCreatedAsString, "", user.name, user.userId)

      //persists the 'PostRegistered' Message, this will later be handled in the 'PostManagerEventAdapter'
      //event adapter and converted into proper messages that will be actually persisted in the journal
      persist(PostRegistered(newPost)){ e =>

        val newMap = addPostToAuthor(posts, newPost)

        updateIdAndContext(currentId, newMap)

        log.info(s"Registering post: $newPost")
        sender() ! PostRegisteredAck(newPost)
      }
    }
    //case to handle EditPost messages
    case EditPost(postId, contents, image) => {
      val dateEditedAsString = Utils.getDateAsString(new Date())
      val postToEdit = getPostsByPostId(postId, posts)

      postToEdit match {
        case None => sender() ! PostDoesntExist
        case Some(post) =>
          //if image is empty we keep the old one
          val finalImage = if (image != "")
            image
          else
            post.image

          //this is the final edited post object with the news fileds evaluated(contents and image)
          val editedPost = Post(post.postId, contents, finalImage, post.dateCreated, dateEditedAsString, post.author, post.authorId)
          //persists the 'PostEdited' Message, this will later be handled in the 'UserRegistrationEventAdapter'
          //event adapter and converted into proper messages that will be actually persisted in the journal
          persist(PostEdited(editedPost)) { e =>

            val newMap = editPost(posts, editedPost)
            newMap match {
              case None =>
              case Some(newMap) => {
                updateIdAndContext(currentId, newMap)
                sender() ! PostEditedAck
              }
            }
          }
      }
    }
    //case to handle GetPostsWithParams messages
    //this messages are queries to the postsMap with sorting and filtering by userId oprions
    case GetPostsWithParams(sortDateAsc: Boolean, userId: Int) => {
      val postsByAuthor = getPostsByAuthorId(userId, posts)

      postsByAuthor match {
        case None => sender() ! List[Post]()
        case Some(list) =>{
          //The Post case class implemented Ordering, so if the 'sortDateAsc' is true, the posts
          //will be sort by date in ascendening order, if false in descending order
          val finalList = if (sortDateAsc) {
            list.sorted
          } else {
            list.sorted.reverse
          }
          log.info(s"Sending the list of posts: $finalList")
          sender() ! finalList
        }
      }
    }

  }

  //The recover function that will handle the messages while recovering the actor
  override def receiveRecover: Receive = recovering()

  private def recovering(): Receive = {

    //variable that are going to keep all changes ang give the immutable map to the listening function.
    //The last become function will be the one that the actor will use as the 'receive' function
    var posts = Map[Int, List[Post]]();
    var currentId = 1;
    {
      case event @ PostRegistered(post) =>
        log.info(s"recovering post creation: $post")
        posts = addPostToAuthor(posts, post)
        //in the recovering we have to keep the currentId in the mutable variable
        currentId += 1
        //this context become doesnt actually do nothing, but when the last message is recived
        //it has to have the right parameters in the listening function
        context.become(listening(currentId, posts))
      case event @ PostEdited(editedPost) =>
        log.info(s"recovering post edit: $editedPost")
        val newMap = editPost(posts, editedPost)

        //we change the posts variable so it can keep being changed by the
        //recovering events until the last map is passed to the context.become
        posts = newMap.get
        context.become(listening(currentId, posts))

    }

  }

  /**
   *
   * The function responsible for updating currentId and changing the actor context
   *
   * @param currentId
   * @param newMap
   */
  private def updateIdAndContext(currentId: Int, newMap: Map[Int, List[Post]]): Unit = {
    val newCurrentId = currentId + 1
    context.become(listening(newCurrentId, newMap))
  }

  /**
   *
   * Get a List[Post] of posts by author Id
   *
   * @param userId
   * @param postsMap
   * @return
   */
  private def getPostsByAuthorId(authorId: Int, postsMap: Map[Int, List[Post]]): Option[List[Post]] =
    if (authorId != 0) {
      postsMap.get(authorId)
    } else {
      Option(getAllPostsAsList(postsMap))
    }

  /**
   *  Get the Post by 'postId'
   *
   * @param postId
   * @param postsMap
   * @return
   */
  private def getPostsByPostId(postId: Int, postsMap: Map[Int, List[Post]]): Option[Post] = {
    val allPosts = getAllPostsAsList(postsMap)
    allPosts.collectFirst{
      case post: Post if post.postId == postId => post
    }
  }


  /**
   *
   * Return all posts in the map as a List[Post]
   *
   * @param postsMap
   * @return
   */
  private def getAllPostsAsList(postsMap: Map[Int, List[Post]]): List[Post] = {
    val allPosts = for {
      (key, value) <- postsMap
    } yield value

    allPosts.toList.flatten
  }

  /**
   *
   * Add post to authir and return a new map conting the new post
   *
   * @param postsMap
   * @param newPost
   * @return
   */
  private def addPostToAuthor(postsMap: Map[Int, List[Post]], newPost: Post): Map[Int, List[Post]] = {
    val authorId = newPost.authorId
    if (postsMap.contains(authorId)) {
      //get current user posts
      val authorPosts = postsMap.get(authorId).get

      //add new post to the user posts
      val newAuthorPosts = authorPosts :+ newPost

      //create a new map entry for the user ( userEmail -> List[Post])
      val newUserEntry = (authorId -> newAuthorPosts)

      //add the new entry to the a new map
      postsMap - authorId + newUserEntry

    } else {
      postsMap + (authorId -> List(newPost))
    }
  }

  /**
   *
   * Edit a post and return the new map containg the edition
   *
   * @param postsMap
   * @param editedPost
   * @return
   */
  private def editPost(postsMap: Map[Int, List[Post]], editedPost: Post): Option[Map[Int, List[Post]]] = {
    val authorId = editedPost.authorId
    val postId = editedPost.postId

    //get a possible list of author posts
    val postsByAuthor = getPostsByAuthorId(authorId, postsMap)

    //get a list of this possible posts without the post being edited
    val postsWithoutEditedPost = postsByAuthor.map{ posts =>
      posts.filter(_.postId != postId)
    }

    //return the postsMap removing all posts from authorId and putting a fresh new list
    //containing the edited post instead the old one
    postsWithoutEditedPost.map{ posts =>
      postsMap - postId + (authorId -> (posts :+ editedPost))
    }
  }
}
