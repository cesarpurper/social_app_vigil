package vigil.cesar.socialApp.actors

import akka.actor.ActorLogging
import akka.persistence.PersistentActor
import vigil.cesar.socialApp.model.User

import scala.collection.mutable



object UserRegistrationActor {
  //protocol
  case object UserRegisteredAck
  case object UserAlreadyExists
  case object GetAllUsers
  case class GetUserByEmail(email: String)
  case class GetUserById(userId: Int)


  case class RegisterUser(name: String, email: String)

//events
  case class UserRegistered(user: User)

}
case class UserRegistrationActor() extends PersistentActor with ActorLogging {

  import UserRegistrationActor._

  override def persistenceId: String = "user-registration-actor"

  //listening is called to be the starter receiving function with no parameter.
  //because it has default parameters, it starst with them currentId = 1 and an empty map
  override def receiveCommand: Receive = listening()

  //This is the function that will receive messages in this actor

  private def listening(currentId: Int = 1, users: Map[Int, User] = Map()): Receive =  {
    // case to handle the RegisterUser message
    case RegisterUser(name, email) => {
      //checks if the email was already used
      if (!isEmailInMap(users, email)) {
        val user = User(currentId, name, email)
        //persists the 'UserRegistered' Message, this will later be handled in the 'UserRegistrationEventAdapter'
        //event adapter and converted into proper messages that will be actually persisted in the journal
        persist(UserRegistered(user)) { e =>
          log.info(s"User persisted: $user")
          val newMap = users + (currentId -> user)
          context.become(listening(currentId + 1, newMap))

          sender() ! UserRegisteredAck
        }
      } else {
        sender() ! UserAlreadyExists
      }
    }
    // case to handle the GetUserByEmail message
    //this returns an Option[User] to the sender
    case GetUserByEmail(email: String) => {
      val user = getUserByEmailFromMap(users, email)
      sender() ! user
    }
    // case to handle the GetUserById message
    //this returns an Option[User] to the sender
    case GetUserById(userId: Int) => {
      val user = users.get(userId)
      sender() ! user
    }
    // case to handle the GetUserById message
    //this returns an List[User] to the sender, either an empty list or a list with all users
    case GetAllUsers => {
      val allusers = getAllUsersAsList(users)
      sender() ! allusers
    }
  }

  //The recover function that will handle the messages while recovering the actor
  override def receiveRecover: Receive = recovering()
  private def recovering(): Receive = {
    //variable that are going to keep all changes ang give the immutable map
    //to the listening function. The last become function will be the one that the actor will use as the 'receive'
    // function
    var users =  Map[Int, User]();
    var currentId = 1;
    {
      //The only message persisted is the UserRegistered, so this is the only case to handle here
      case event @ UserRegistered(user) =>
        users = users + (user.userId -> user)
        currentId += 1
        context.become(listening(currentId, users))
    }
  }


  /**
   *
   * This helper function returns a Boolean indicating if the given email is present in the given Map[Int, User]
   *
   * @param map
   * @param email
   * @return Boolean
   */
  private def isEmailInMap(map: Map[Int, User], email: String): Boolean = {
    val result = map.filter(x => x._2.email == email)
    !result.isEmpty
  }

  /**
   *
   * This functions returns a Option[User] from the map of the given email
   *
   * @param map
   * @param email
   * @return
   */
  private def getUserByEmailFromMap(map: Map[Int, User], email: String): Option[User] = {
    val result: Option[(Int, User)] = map.find { case (k, v) =>
      v.email == email
    }

    result.map {
      case (userId, user) =>{
        user
      }
    }
  }

  /**
   *
   * Return all Users in the map as a List[User]
   *
   * @param postsMap
   * @return
   */
  private def getAllUsersAsList(users: Map[Int, User]): List[User] = {
    val allPosts = for {
      (key, value) <- users
    } yield value
    allPosts.toList
  }

}
