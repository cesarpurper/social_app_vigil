package vigil.cesar.socialApp.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.persistence.PersistentActor


/**
 * Gonna leave this actor as is
 *
 * I tried to create this actor to manage network connections between users in our social app and further send
 * notifications for them
 *
 * As I am taking too much time to send my complete solution and I am not being able to better figure the best practices to
 * validate users before adding them to the network, I'll let this code here just so you can see I tried :)
 */
object NetworkManagerActor{

  case class NewFollower(userFollowing: Int, userToFollow: Int)

  case class RemoveFollower(userFollowing: Int, userToFollow: Int)

  case class FollowerRegistered(userFollowing: Int, userToFollow: Int)

  case class UnfollowRegistered(userFollowing: Int, userToFollow: Int)

  case class GetFollowersByUser(userId: Int)
  case object RelatitonshipUpdatedAck
}
case class NetworkManagerActor() extends PersistentActor with ActorLogging {

  import NetworkManagerActor._


  override def persistenceId: String = "SAA"

  override def receiveCommand: Receive = listening()

  private def listening(followersMap: Map[Int, Set[Int]] = Map()): Receive = {
    case NewFollower(userFollowing, userToFollow)  => {
      persist(FollowerRegistered(userFollowing, userToFollow)) { e =>


        val newMap = addFollowerToUser(followersMap, userFollowing, userToFollow)
        context.become(listening(newMap))

        sender() ! RelatitonshipUpdatedAck
      }
    }
    case RemoveFollower(userFollowing, userToFollow)  => {
      persist(UnfollowRegistered(userFollowing, userToFollow)) { e =>
        val newMap = removeFollowingFromUser(followersMap, userFollowing, userToFollow)
        context.become(listening(newMap))

        sender() ! RelatitonshipUpdatedAck
      }
    }
    case GetFollowersByUser(userId) => {

      sender() ! followersMap.get(userId)
    }

    case "print" =>
      log.info(s"printing network $followersMap")

  }
  override def receiveRecover: Receive = recovering()

  private def recovering(): Receive = {

    //variable that are going to keep all changes ang give the immutable map
    //to the listening function. The last become function will be the one that the actor will use as the 'receive'
    // function
    var followersMap = Map[Int, Set[Int]]();
    {
      case event @ FollowerRegistered(userFollowing, userToFollow) =>
        followersMap = addFollowerToUser(followersMap, userFollowing, userToFollow)
        context.become(listening(followersMap))
      case event @ UnfollowRegistered(userFollowing, userToFollow) =>
        followersMap = removeFollowingFromUser(followersMap, userFollowing, userToFollow)
        context.become(listening(followersMap))


    }

  }

  private def addFollowerToUser(folowersMap: Map[Int, Set[Int]], userFollowing: Int, userToFollow: Int): Map[Int, Set[Int]] = {
    if (folowersMap.contains(userToFollow)) {
      //get current user posts
      val userFollowers = folowersMap.get(userToFollow).get

      //add new post to the user posts
      val newFollowers = userFollowers + userFollowing

      //create a new map entry for the user ( userEmail -> List[Post])
      val newFollowersEntry = (userToFollow -> newFollowers)

      //add the new entry to the a new map
      folowersMap - userToFollow + newFollowersEntry

    } else {
      folowersMap + (userToFollow -> Set(userFollowing))
    }
  }

  private def removeFollowingFromUser(folowersMap: Map[Int, Set[Int]], userFollowing: Int, userToUnfollow: Int): Map[Int, Set[Int]] = {
    if (folowersMap.contains(userToUnfollow)) {
      //get current user posts
      val userFollowers = folowersMap.get(userToUnfollow).get

      //remove follower from set
      val newFollowers = userFollowers - userFollowing

      //create a new map entry for the user ( userEmail -> List[Post])
      val newFollowersEntry = (userToUnfollow -> newFollowers)

      //add the new entry to the a new map
      folowersMap - userToUnfollow + newFollowersEntry

    } else {
      folowersMap
    }
  }
}
