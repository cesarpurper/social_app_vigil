package vigil.cesar.socialApp.actors

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.persistence.inmemory.extension.{InMemoryJournalStorage, StorageExtension}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest._
import vigil.cesar.socialApp.Main.system
import vigil.cesar.socialApp.actors.UserRegistrationActor._
import vigil.cesar.socialApp.model._

class UserRegistrationActorSpec
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

  "UserRegistrationActor" should {

    //default new user value
    val user = User(1, "Cesar Purper", "cpurper@gmail.com")

    "add an user and preserve it after restart" in {

      val persistenceId = "registrationManager1"

      val probe = TestProbe()

      //we instantiate the actor
      val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], persistenceId)


      //tell actor to register user
      userRegistrationActor ! RegisterUser(user.name, user.email)
      expectMsg(UserRegisteredAck)

      //terminate the actor
      userRegistrationActor ! PoisonPill

      // wait for the actor to shutdown
      probe.watch(userRegistrationActor)
      probe.expectTerminated(userRegistrationActor)

      // creating a new actor
      val userRegistrationActor2 = system.actorOf(Props[UserRegistrationActor], persistenceId)

      //get the user created to test the persistence
      userRegistrationActor2 ! GetUserById(user.userId)
      expectMsg(Some(user))
    }

    "not allow duplicate emails" in {
      val persistenceId = "registrationManager2"
      //we instantiate the actor
      val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], persistenceId)

      //tell actor to register user
      userRegistrationActor ! RegisterUser(user.name, user.email)
      expectMsg(UserRegisteredAck)

      //tell actor to register user with same email
      userRegistrationActor ! RegisterUser("New User Name", user.email)
      expectMsg(UserAlreadyExists)
    }

    "get user by a given email" in {
      val persistenceId = "registrationManager3"

      //we instantiate the actor
      val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], persistenceId)


      //tell actor to register user
      userRegistrationActor ! RegisterUser(user.name, user.email)
      expectMsg(UserRegisteredAck)

      //tell actor to get user by email
      userRegistrationActor ! GetUserByEmail(user.email)

      expectMsg(Some(user))
    }
    "get user by a given id" in {
      val persistenceId = "registrationManager4"
      //we instantiate the actor
      val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], persistenceId)

      //tell actor to register user
      userRegistrationActor ! RegisterUser(user.name, user.email)
      expectMsg(UserRegisteredAck)

      //tell actor to give user by given id
      userRegistrationActor ! GetUserById(user.userId)

      //tell actor to get user by email
      expectMsg(Some(user))
    }
    "get all users" in {
      val persistenceId = "registrationManager5"
      val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], persistenceId)
      val anotherUser = User(2, "Test User", "test@gmail.com")

      //tell actor to register user
      userRegistrationActor ! RegisterUser(user.name, user.email)
      expectMsg(UserRegisteredAck)

      //tell actor to register another user
      userRegistrationActor ! RegisterUser(anotherUser.name, anotherUser.email)
      expectMsg(UserRegisteredAck)

      //tell actor to get all users
      userRegistrationActor ! GetAllUsers

      expectMsg(List(user, anotherUser))
    }
    "get user by name and email" in {
      val persistenceId = "registrationManager6"
      val userRegistrationActor = system.actorOf(Props[UserRegistrationActor], persistenceId)
      val anotherUser = User(2, "Test User", "test@gmail.com")

      //tell actor to register user
      userRegistrationActor ! RegisterUser(user.name, user.email)
      expectMsg(UserRegisteredAck)

      //tell actor to register another user
      userRegistrationActor ! RegisterUser(anotherUser.name, anotherUser.email)
      expectMsg(UserRegisteredAck)

      //tell actor to get all users
      userRegistrationActor ! GetUserByEmailAndName(anotherUser.name, anotherUser.email)

      expectMsg(Some(anotherUser))

    }

  }

}
