package com.github.jasync.sql.db.exceptions

import com.github.jasync.sql.db.KindedMessage

@Suppress("RedundantVisibilityModifier")
public class EncoderNotAvailableException(message: KindedMessage) :
    DatabaseException("Encoder not available for name %s".format(message.kind))
