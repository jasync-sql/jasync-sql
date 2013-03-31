package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 12:45 AM
 */
class NoticeMessage ( fields : Map[String,String] )
  extends InformationMessage( Message.Notice, fields )
