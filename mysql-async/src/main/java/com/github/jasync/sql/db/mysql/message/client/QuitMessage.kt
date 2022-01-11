package com.github.jasync.sql.db.mysql.message.client

class QuitMessage : ClientMessage(ClientMessage.Quit) {
    companion object {
        val Instance = QuitMessage()
    }
}
