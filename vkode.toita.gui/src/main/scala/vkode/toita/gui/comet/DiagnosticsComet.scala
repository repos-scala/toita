package vkode.toita.gui.comet

import actors.threadpool.AtomicInteger
import net.liftweb.http.js.JsCmds.SetHtml
import vkode.toita.gui.backend.ToitaRegister
object DiagnosticsComet {

  sealed trait DiagnosticsEvent {
    def timestamp: Long = System.currentTimeMillis
  }

  case object StreamUp extends DiagnosticsEvent

  case object StreamDown extends DiagnosticsEvent

  case object LookupStarted extends DiagnosticsEvent

  case object LookupEnded extends DiagnosticsEvent
}

class DiagnosticsComet extends ToitaCSSComet with ToitaRegister {

  val streamCount = new AtomicInteger

  val lookupCount = new AtomicInteger

  def streams = <span>{ streamCount.get }</span>

  def lookups = <span>{ lookupCount.get }</span>

  val transformer = "#streams" #> streams & "#lookups" #> lookups

  protected def getNodeSeq = transformer(defaultXml)

  def update() {
    partialUpdate(SetHtml("diagnostics-area", getNodeSeq))
    reRender(false)
  }

  override def lowPriority = {
    case DiagnosticsComet.StreamUp =>
      streamCount.incrementAndGet
      update
    case DiagnosticsComet.StreamDown =>
      streamCount.decrementAndGet
      update
    case DiagnosticsComet.LookupStarted =>
      lookupCount.incrementAndGet
      update
    case DiagnosticsComet.LookupEnded =>
      lookupCount.decrementAndGet
      update
  }
}
