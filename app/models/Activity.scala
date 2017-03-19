package models

import db.Tables
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Created by simfischer on 3/16/17.
  */
case class Activity(val username: String, userId: Int, timestamp: Long, activityType: String, documentName: String, documentId: Int, arguments: Seq[String], val entityType: String = "activity")

object Activity {

  private def parseArray(array: Option[String]): Seq[String] = {
    array.map(Json.parse(_).as[Seq[String]]).getOrElse(Seq())
  }

  def of(activityRow: Tables.ActivityRow, userRow: Tables.UserRow, document: Tables.DocumentRow) =
    Activity(
      username = userRow.username,
      userId = userRow.id,
      timestamp = activityRow.timestamp.getTime,
      activityType = activityRow.activitytype,
      documentName = document.name,
      documentId = document.id,
      arguments = parseArray(activityRow.arguments))


  // we need to rename linkType since type is a keyword in Scala
  implicit val writesActivity: Writes[Activity] = (
      (JsPath \ "username").write[String] and
      (JsPath \ "userId").write[Int] and
      (JsPath \ "timestamp").write[Long] and
      (JsPath \ "type").write[String] and
      (JsPath \ "documentName").write[String] and
      (JsPath \ "documentId").write[Int] and
      (JsPath \ "arguments").write[Seq[String]] and
      (JsPath \ "entityType").write[String]
    )(unlift(Activity.unapply))
}

object ActivityType extends Enumeration {
  type ActivityType = Value
  val Created = Value("CREATED")
  val Tagged = Value("TAGGED")
  val Untagged = Value("UNTAGGED")
  val Renamed = Value("RENAMED")
  val Attached = Value("ATTACHED")
  val SetContact = Value("SET_CONTACT")
  val SetDescription = Value("SET_DESCRIPTION")
  val MarkedComplete = Value("MARKED_COMPLETE")
  val CreatedLink = Value("CREATED_LINK")
  val MarkedActionRequired = Value("MARKED_ACTION_REQUIRED")
  val SetFollowup = Value("SET_FOLLOWUP")
  val FollowupExpired = Value("FOLLOWUP_EXPIRED")
}
