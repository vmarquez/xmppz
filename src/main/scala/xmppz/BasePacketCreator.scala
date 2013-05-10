package xmppz

import scala.xml.pull._

trait BaseCreator {

  def getMatchers: List[PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet]]

  def attribute(str: String)(implicit start: EvElemStart) =
    start.attrs.asAttrMap.get(str)
}