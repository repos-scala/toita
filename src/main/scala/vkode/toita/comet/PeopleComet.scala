package vkode.toita.comet

import vkode.toita.backend.TOUser
import scala.xml._
import net.liftweb.common._
import vkode.toita.backend._
import net.liftweb.http.js.JsCmds._
import java.util.Date
import scalaz.Options
import org.joda.time.DateTime
import net.liftweb.http._
import net.liftweb.common._
import net.liftweb.util._
import Helpers._
import SHtml._

class PeopleComet extends ToitaCSSComet {

  private var users = Map[BigInt,TOUser]()

  private def withUser(m: Map[BigInt, TOUser], u: TOUser) =
    if (m contains u.data.id) m else m + (u.data.id -> u)

  override def lowPriority = {
    case newUsers: List[TOUser] =>
      users = (users /: newUsers) (withUser(_, _))
      partialUpdate(SetHtml(area, getNodeSeq))
      reRender(false)
    case user: TOUser =>
      users = withUser(users, user)
      partialUpdate(SetHtml(area, getNodeSeq))
      reRender(false)
  }

  protected val area = "people-area"

  protected override def getNodeSeq: NodeSeq =
    if (users.isEmpty) <span>No friends!</span> else (Rendrer renderUsers (users.values.toList, defaultXml))
}
