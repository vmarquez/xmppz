package xmppz

//Plumber powers the connection by dealing with the IO.  This will handle underlying Sockets/Netty/File, http, who knows what the transport could be
//should this be a case class?  Mutability....BAD
trait ConnectionPlumber {
  def run(f: Seq[Packet] => Unit, errorF: ConnectionError => Unit)

  def switchToTLS()

  def write(str: String)

  def shutDown()
}

