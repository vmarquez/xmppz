package xmppz
import javax.security.sasl._
import javax.security.auth.callback._
import java.util.HashMap
import PublicDomain.Base64

object SASLHandler {

  def getAuthTextPlain(username: String, password: String, domain: String, xcallback: Option[Seq[Callback] => Unit] = None): String = {
    val callbackHandler = new SaslCallbackHandler(Some(username), Some(password))
    val saslclient = Sasl.createSaslClient(List("PLAIN").toArray, username, "xmpp", domain, new HashMap[String, String](), callbackHandler)
    val response = saslclient.evaluateChallenge(List[Byte]().toArray)
    val authmechtext = Base64.encodeBytes(response)
    authmechtext
  }

  class SaslCallbackHandler(username: Option[String], password: Option[String]) extends CallbackHandler {
    def handle(callbacks: Array[Callback]) {

      for (callback <- callbacks)
        callback match {
          case nameCb: NameCallback =>
            username.foreach(nameCb.setName(_))
          case passwordCb: PasswordCallback =>
            password.foreach(pwd => passwordCb.setPassword(pwd.toCharArray()))
        }

    }
  }
}
