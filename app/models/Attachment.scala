package models

import db.Tables.AttachmentRow
import play.api.libs.json.Json

/**
  * Created by simfischer on 3/22/17.
  */
case class Attachment(id: Int, name: String, mimeType: String, size: Long)

object Attachment {

  def of(row: AttachmentRow) = Attachment(
    id = row.id,
    name = row.name,
    size = row.size,
    mimeType = row.mimetype
  )

  implicit val writeAttachment = Json.writes[Attachment]

}
