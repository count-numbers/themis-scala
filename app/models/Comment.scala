package models

import play.api.libs.json.Json

/**
  * Created by simfischer on 3/15/17.
  */
case class Comment(id: Int, text: String, username: String, userId: Int, timestamp: Long, val entityType: String = "comment")

object Comment {
  def of(commentRow: _root_.db.Tables.CommentRow, userRow: _root_.db.Tables.UserRow): Comment
  = Comment(id = commentRow.id, text = commentRow.text, username = userRow.username, userId = userRow.id, timestamp = commentRow.timestamp.getTime)

  implicit val formatComment = Json.format[Comment]
  implicit val readsComment  = Json.reads[Comment]
  implicit val writesComment = Json.writes[Comment]

}
