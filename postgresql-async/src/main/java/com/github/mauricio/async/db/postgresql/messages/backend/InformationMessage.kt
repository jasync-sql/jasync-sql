package com.github.mauricio.async.db.postgresql.messages.backend


abstract class InformationMessage(messageType: Int, val fields: Map<Char, String>) : ServerMessage(messageType) {

  val message: String = this.fields['M'] ?: "" //TODO: handle null

  override fun toString(): String {
    return "%s(fields=%s)".format(this.javaClass.simpleName, fields.map { pair ->
      InformationMessage.fieldName(pair.key) to pair.value
    })
  }

  companion object {
    private const val Severity = 'S'
    private const val SQLState = 'C'
    private const val Message = 'M'
    private const val Detail = 'D'
    private const val Hint = 'H'
    private const val Position = 'P'
    private const val InternalQuery = 'q'
    private const val Where = 'W'
    private const val File = 'F'
    private const val Line = 'L'
    private const val Routine = 'R'

    private val Fields = mapOf(
        Severity to "Severity",
        SQLState to "SQLSTATE",
        Message to "Message",
        Detail to "Detail",
        Hint to "Hint",
        Position to "Position",
        InternalQuery to "Internal Query",
        Where to "Where",
        File to "File",
        Line to "Line",
        Routine to "Routine"
    )

    fun fieldName(name: Char): String = Fields.getOrElse(name) { name.toString() }
  }
}