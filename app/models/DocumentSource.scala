package models

import play.api.libs.json.Json

case class DocumentSource(val id: Long, `type`: String, gdriveSourceFolderId: Option[String], gdriveArchiveFolderId: Option[String], fileSourceFolder: Option[String])
object DocumentSource {

  def of(row: db.Tables.SourceRow) = {
    DocumentSource(row.id, row.`type`, row.gdrivesourcefolder, row.gdrivearchivefolder, row.filesourcefolder)
  }

  implicit val writesDocumentSource = Json.writes[DocumentSource]

}
