package xmppz

case class ConnectionError(
  exception: Option[Exception] = None,
  message: String)
//maybe to add what we were waiting for?
