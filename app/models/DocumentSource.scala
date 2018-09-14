package models

import play.api.libs.json.Json

case class DocumentSource(val id: Option[Int], `type`: String, gdriveSourceFolderId: Option[String], gdriveArchiveFolderId: Option[String], fileSourceFolder: Option[String])
object DocumentSource {

  def of(row: db.Tables.SourceRow) = {
    DocumentSource(Some(row.id), row.`type`, row.gdrivesourcefolder, row.gdrivearchivefolder, row.filesourcefolder)
  }

  implicit val writesDocumentSource = Json.writes[DocumentSource]
  implicit val readsDocumentSource = Json.reads[DocumentSource]

}
