package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.exceptions.UnsupportedAuthenticationMethodException
import com.github.mauricio.postgresql.messages.backend._

object AuthenticationStartupParser extends MessageParser {

  val AuthenticationOk = 0
  val AuthenticationKerberosV5 = 2
  val AuthenticationCleartextPassword = 3
  val AuthenticationMD5Password = 5
  val AuthenticationSCMCredential = 6
  val AuthenticationGSS = 7
  val AuthenticationGSSContinue = 8
  val AuthenticationSSPI = 9

  override def parseMessage(b: ChannelBuffer): Message = {

    val authenticationType = b.readInt()

    authenticationType match {
      case AuthenticationOk => AuthenticationOkMessage.Instance
      case AuthenticationCleartextPassword => AuthenticationChallengeCleartextMessage.Instance
      case AuthenticationMD5Password => AuthenticationChallengeMD5.Instance
      case _ => {
        throw new UnsupportedAuthenticationMethodException(authenticationType)
      }

    }

  }

}