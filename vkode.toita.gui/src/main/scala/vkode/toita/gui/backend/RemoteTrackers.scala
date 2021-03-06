package vkode.toita.gui.backend

import akka.actor.ActorRef
import akka.actor.Actors._
import vkode.toita.events.{TwitterEvent, UserRef}

object RemoteTrackers {
  
  private val localModule = remote.start("localhost", 4002)
  
  private val casting = remote.actorFor("casting", "localhost", 4020)
  
  def update(userRef: UserRef, eventTypes: List[Class[_ <: TwitterEvent]], tracker: Option[ActorRef]) = tracker match {
    case Some(tracker) => localModule.register(userRef.screenName, tracker)
    case None => localModule unregister userRef.screenName
  }
}