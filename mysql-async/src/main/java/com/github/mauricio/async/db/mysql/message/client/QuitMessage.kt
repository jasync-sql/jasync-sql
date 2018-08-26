package com.github.mauricio.async.db.mysql.message.client


class QuitMessage : ClientMessage(ClientMessage.Quit) {
  companion object {
    val Instance = QuitMessage();
  }
}
