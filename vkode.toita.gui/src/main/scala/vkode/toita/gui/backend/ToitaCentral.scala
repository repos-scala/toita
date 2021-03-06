package vkode.toita.gui.backend

import scalaz.Options
import akka.actor.{ActorRef, Actor}
import vkode.toita.gui.comet.DiagnosticsComet
import collection.mutable.{Map => MutMap}
import vkode.toita.events.{UserRef, UserSession, TwitterEvent}
import vkode.toita.toita.{DB, StreamEmitter}

class ToitaCentral extends Actor with Options {

  //  implicit def trackerToKey (trackable: ToitaTrackable) = trackable.sessions → trackable.eventTypes

  override def receive = {
    case msg: DiagnosticsComet.DiagnosticsEvent => diagnostic(msg)
    case CometUp(diagnostics: DiagnosticsComet) =>
      diagnosticians = diagnosticians :+ diagnostics
    case CometDown(diagnostics: DiagnosticsComet) =>
      diagnosticians = diagnosticians filterNot (_ == diagnostics)
    case CometUp(cometActor: ToitaTrackable) =>
      setup(cometActor)
    case CometDown(cometActor: ToitaTrackable) =>
      dismantle(cometActor)
    case x =>
      log.warn("Unhandled: " + x)
  }

  def diagnostic(msg: Any) {
    diagnosticians foreach (_ ! msg)
  }

  var diagnosticians = List[DiagnosticsComet]()

  val trackerRefs = MutMap[(UserRef, List[Class[_ <: TwitterEvent]]),ActorRef]()

  private def setup (trackable: ToitaTrackable) {
    trackerRefFor(trackable) foreach (_ ! Tracker.Add(trackable.cometActor))
  }

  private def trackerRefFor(trackable: ToitaTrackable): List[ActorRef] =
    trackable.sessions map (session => {
      val key = session → trackable.eventTypes
      trackerRefs get key match {
        case None =>
          val userSession = DB(session).get
          newTrackerRef(trackable, StreamEmitter(userSession), session) match {
            case None =>
              log.warn("No tracker could be gleaned from trackable " + trackable)
              None
            case newTrackerRef =>
              log.info("Wiring up new tracker " + newTrackerRef.get + " for " + trackable)
              trackerRefs (key) = newTrackerRef.get
              newTrackerRef
          }
        case trackerRef => trackerRef
      }
    }) filter (_ isDefined) map (_ get)

  private def newTrackerRef(trackable: ToitaTrackable, emitter: StreamEmitter, user: UserRef): Option[ActorRef] =
    trackable tracker emitter match {
      case Some(trackerRef) =>
        trackerRef.start()
        emitter addReceiver (trackerRef, trackable.eventTypes)
        log.info("Tracker for " + trackable + " started: " + trackerRef + 
                 ", events of " + trackable.eventTypes.mkString(","))
//        RemoteTrackers(user, trackable.eventTypes) = trackerRef 
        Some(trackerRef)
      case None =>
        log.warn("No tracker for " + trackable);
        None
    }

  private def dismantle (trackable: ToitaTrackable) =
    trackable.sessions map (session => {
      val key = session → trackable.eventTypes
      trackerRefs get key match {
        case Some(trackerRef) =>
          log.info("Dismantling tracker for " + trackable + ": " + trackerRef)
          trackerRefs -= key
          trackerRef ! Tracker.Remove(trackable.cometActor)
          trackerRef.stop()
        case None =>
          log.warn("No tracker for " + trackable + " was registered");
      }
    })
}
