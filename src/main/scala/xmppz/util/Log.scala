package xmppz.util

trait LogMsg[T] {
  def label: String
  def msg: String
  def thread: String
  def caller: Option[T]
}

object Log {
  def apply[T](l: String, m: String) = new LogMsg[T] {
    def label = l
    def msg = m
    def thread = Thread.currentThread.toString
    def caller = None
  }

  def apply[T](l: String, m: String, t: T) = new LogMsg[T] {
    def label = l
    def msg = m
    def thread = Thread.currentThread.toString
    def caller = Some(t)
  }

  //implicit def stringToLog(str: String): LogMsg = Log("info", str)

  implicit def logToList[T](log: LogMsg[T]): List[LogMsg[T]] = List[LogMsg[T]](log)
}

