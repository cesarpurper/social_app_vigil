package vigil.cesar.socialApp.actors

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.persistence.inmemory.extension.{InMemoryJournalStorage, StorageExtension}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest._
import akka.pattern.ask
import akka.util.Timeout
import vigil.cesar.socialApp.Main.system
import vigil.cesar.socialApp.actors.PostManagerActor._
import vigil.cesar.socialApp.actors.UserRegistrationActor._
import vigil.cesar.socialApp.model._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util._

class PostManagerActorSpec
  extends TestKit(ActorSystem("testUserRegistrationSystem", ConfigFactory.load("application-test.conf")))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ImplicitSender {


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }



  //clear the in memory journal before each test
  override protected def beforeEach(): Unit = {
    val tp = TestProbe()
    tp.send(StorageExtension(system).journalStorage, InMemoryJournalStorage.ClearJournal)
    tp.expectMsg(akka.actor.Status.Success(""))
    super.beforeEach()
  }

  "PostManagerActor" should {

    //default new post value
    val newPost = Post(
      1,
      "example content",
      "exampleImage.jpg",
      "",
      "",
      "Cesar Purper",
      1)

    //default user value
    val user = User(1, "Cesar Purper", "cpurper@gmail.com")

    "create a post and preserve it after restart" in {
      val persistenceId = "postManager1"

      val probe = TestProbe()

      //instantiating actors
      val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], "registrationManager1")
      val postManagerActor = system.actorOf(Props[PostManagerActor], persistenceId)


      //we populate the journal with the first user, its id will be 1
      userRegistrationActor ! RegisterUser(user.name, user.email)
      //user registraction actor will acknowledge with UserRegisteredAck
      expectMsg(UserRegisteredAck)


      //tell post manager actor to create post with given parameters
      postManagerActor ! CreatePost(newPost.contents, newPost.image, user)

      //matchs with partial function to the response PostRegisteredAck(Post) and extracts the post created in journal
      val postRegistered = expectMsgPF() {
        case PostRegisteredAck(post) => {
          post.postId shouldBe newPost.postId
          post.contents shouldBe newPost.contents
          post.image shouldBe newPost.image
          post.author shouldBe user.name
          post.authorId shouldBe user.userId

          post
        }
      }

      //terminates post manager actor
      postManagerActor ! PoisonPill

      // wait for the actor to shutdown
      probe.watch(postManagerActor)
      probe.expectTerminated(postManagerActor)

      //recreate post manager actor to check if it is recovering as it should
      val postManagerActor2 = system.actorOf(Props[PostManagerActor], persistenceId)

      postManagerActor2 ! GetPostsWithParams(true, 1)

      //checks if there is a list of one post containg the post we just created
      expectMsg(List(postRegistered))
    }

    "edit an post" in {
      val persistenceId = "postManager2"

      val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], "registrationManager2")
      val postManagerActor = system.actorOf(Props[PostManagerActor], persistenceId)


      //we populate the journal with the first user, its id will be 1
      userRegistrationActor ! RegisterUser(user.name, user.email)
      //user registraction actor will acknowledge with UserRegisteredAck
      expectMsg(UserRegisteredAck)


      //tell post manager actor to create post with given parameters
      postManagerActor ! CreatePost(newPost.contents, newPost.image, user)


      //matchs with partial function to the response PostRegisteredAck(Post) and extracts the post created in journal
      val postRegistered = expectMsgPF() {
        case PostRegisteredAck(post) => {
          post.postId shouldBe newPost.postId
          post.contents shouldBe newPost.contents
          post.image shouldBe newPost.image
          post.author shouldBe user.name
          post.authorId shouldBe user.userId

          post
        }
      }

      val editedContent = "edited content"
      val editedImg = "editedImg.jpg"

      //testing changing image
      postManagerActor ! EditPost(postRegistered.postId, editedContent, editedImg)

      expectMsg(PostEditedAck)

      postManagerActor ! GetPostsWithParams(true, 1)

      expectMsgPF() {
        case List(post: Post) => {
          post.contents shouldBe editedContent
          post.image shouldBe editedImg
        }
      }

      val editedContent2 = "edited content 2"
      //testing without changing image
      postManagerActor ! EditPost(postRegistered.postId, editedContent2, "")

      expectMsg(PostEditedAck)

      postManagerActor ! GetPostsWithParams(true, 1)

      expectMsgPF() {
        case List(post: Post) => {
          post.contents shouldBe editedContent2
          post.image shouldBe editedImg
        }
      }
    }

    "get sorted posts" in {
      val persistenceId = "postManager3"

      val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], "registrationManager3")
      val postManagerActor = system.actorOf(Props[PostManagerActor], persistenceId)
      val anotherUser = User(2, "test@gmail.com", "Test User")


      //we populate the journal with the first user, its id will be 1
      userRegistrationActor ! RegisterUser(user.name, user.email)
      //user registraction actor will acknowledge with UserRegisteredAck
      expectMsg(UserRegisteredAck)

      //we populate the journal with the first user, its id will be 1
      userRegistrationActor ! RegisterUser(anotherUser.name, anotherUser.email)
      //user registraction actor will acknowledge with UserRegisteredAck
      expectMsg(UserRegisteredAck)


      //tell post manager actor to create post with given parameters
      postManagerActor ! CreatePost(newPost.contents, newPost.image, user)
      val firstPostRegistered = expectMsgPF() {
        case PostRegisteredAck(post) => {
          post
        }
      }


      //tell post manager actor to create post with given parameters
      postManagerActor ! CreatePost("secondPost", "", anotherUser)
      val secondPostRegistered = expectMsgPF() {
        case PostRegisteredAck(post) => {
          post
        }
      }

      //test ascending order
      postManagerActor ! GetPostsWithParams(true, 0)
      expectMsg(List(firstPostRegistered, secondPostRegistered))

      //test descending order
      postManagerActor ! GetPostsWithParams(false, 0)
      expectMsg(List(secondPostRegistered, firstPostRegistered))



    }

    "get posts by user id" in {

      val persistenceId = "postManager4"

      val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], "registrationManager4")
      val postManagerActor = system.actorOf(Props[PostManagerActor], persistenceId)
      val anotherUser = User(2, "test@gmail.com", "Test User")


      //we populate the journal with the first user, its id will be 1
      userRegistrationActor ! RegisterUser(user.name, user.email)
      //user registraction actor will acknowledge with UserRegisteredAck
      expectMsg(UserRegisteredAck)

      //we populate the journal with the first user, its id will be 1
      userRegistrationActor ! RegisterUser(anotherUser.name, anotherUser.email)
      //user registraction actor will acknowledge with UserRegisteredAck
      expectMsg(UserRegisteredAck)


      //tell post manager actor to create post with given parameters
      postManagerActor ! CreatePost(newPost.contents, newPost.image, user)
      val firstPostRegistered = expectMsgPF() {
        case PostRegisteredAck(post) => {
          post
        }
      }

      //tell post manager actor to create post with given parameters
      postManagerActor ! CreatePost("secondPost", "", anotherUser)
      val secondPostRegistered = expectMsgPF() {
        case PostRegisteredAck(post) => {
          post
        }
      }

      //test filtering by user id 2
      postManagerActor ! GetPostsWithParams(true, 2)
      expectMsg(List(secondPostRegistered))

    }

    "edit a post and preserve it after restart" in {

      val persistenceId = "postManager5"

      val probe = TestProbe()

      //instantiating actors
      val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], "registrationManager5")
      val postManagerActor = system.actorOf(Props[PostManagerActor], persistenceId)


      //we populate the journal with the first user, its id will be 1
      userRegistrationActor ! RegisterUser(user.name, user.email)
      //user registraction actor will acknowledge with UserRegisteredAck
      expectMsg(UserRegisteredAck)


      //tell post manager actor to create post with given parameters
      postManagerActor ! CreatePost(newPost.contents, newPost.image, user)

      val postPersisted = expectMsgPF() {
        case PostRegisteredAck(post) => {
          post.contents shouldBe newPost.contents
          post.image shouldBe newPost.image

          post
        }
      }

      val editedContent = "edited content"
      val editedImg = "editedImg.jpg"

      //testing changing image
      postManagerActor ! EditPost(postPersisted.postId, editedContent, editedImg)

      expectMsg(PostEditedAck)

      //terminates post manager actor
      postManagerActor ! PoisonPill

      // wait for the actor to shutdown
      probe.watch(postManagerActor)
      probe.expectTerminated(postManagerActor)

      //recreate post manager actor to check if it is recovering as it should
      val postManagerActor2 = system.actorOf(Props[PostManagerActor], persistenceId)

      postManagerActor2 ! GetPostsWithParams(true, 1)

      expectMsgPF() {
        case List(post: Post) => {
          post.postId shouldBe postPersisted.postId
          post.contents shouldBe editedContent
          post.image shouldBe editedImg
        }
      }

    }
  }

}
