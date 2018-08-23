
package com.github.mauricio.async.db.exceptions

import com.github.mauricio.async.db.KindedMessage

class EncoderNotAvailableException(message: KindedMessage)
  : DatabaseException("Encoder not available for name %s".format(message.kind()))
