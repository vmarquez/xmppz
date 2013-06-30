package xmppz.packet

import scala.xml.pull.{ XMLEventReader, XMLEvent }
import scala.annotation.tailrec
import scalaz.StateT

object PacketReader {
  def apply[T](ev: List[XMLEvent], listener: XMLEventReader, parser: StateT[Option, List[XMLEvent], List[T]]): (List[XMLEvent], List[T]) = {
    @tailrec
    def acc(run: Boolean, ev: List[XMLEvent], listener: XMLEventReader, packets: List[T]): (List[XMLEvent], List[T]) = {
      if (!listener.available() && run)
        Thread.sleep(200)
      if (listener.available) {
        val next = listener.next()
        val events = ev :+ next
        parser.run(events) match {
          case Some((nev, p)) =>
            acc(false, nev, listener, packets ::: p)
          case None =>
            acc(false, events, listener, packets)
        }
      } else {
        (ev, packets)
      }
    }
    acc(true, ev, listener, List())
  }
}
