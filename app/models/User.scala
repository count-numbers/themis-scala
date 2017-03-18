package models

import db.Tables.UserRow
import play.api.libs.json.Json

/**
  * Created by simfischer on 3/9/17.
  */
case class User(id: Int, username: String, name: String, email: String) {

}

object User {

  def of(userRow: UserRow): User = User(userRow.id, userRow.username, userRow.name, userRow.email)

  implicit val formatDocument = Json.format[User]
  implicit val readsDocument = Json.reads[User]
  implicit val writesDocument = Json.writes[User]
}