package models

import db.Tables.{DocumentRow, ContactRow, DtagRow, UserRow}
import play.api.libs.json.Json

/**
  * Created by simfischer on 3/9/17.
  */
case class Document(id: Int,
                    name: String,
                    description: Option[String],

                    owner: Option[User],
                    contact: Option[Contact],

                    archiveTimestamp: Long,
                    modificationTimestamp: Long,
                    followUpTimestamp: Option[Long],
                    documentDate: Option[String],

                    archivingComplete: Boolean,
                    actionRequired: Boolean,

                    sourceId: String,
                    sourceReference: String,
                    thumbnailId: Option[Int],

                    tags: Option[Seq[String]] = None,

                    attachments: Option[Seq[Attachment]] = None,
                    comments: Option[Seq[Comment]] = None,
                    links: Option[Seq[Link]] = None,
                    activityHistory: Option[Seq[Activity]] = None) // is followup a property of the document?

object Document {

  def of(doc: DocumentRow,
         owner: Option[UserRow] = None,
         contact: Option[ContactRow] = None,
         tags: Option[Seq[DtagRow]] = None,
         attachments: Option[Seq[Attachment]] = None,
         comments: Option[Seq[Comment]] = None,
         links: Option[Seq[Link]] = None,
         activityHistory: Option[Seq[Activity]] = None) = Document(
    id = doc.id,
    name = doc.name,
    description = doc.description,

    owner = owner.map(User.of(_)),
    contact = contact.map(Contact.of(_)),

    tags = tags.map(_.map(_.name)),
    comments = comments,
    attachments = attachments,
    links = links,
    activityHistory = activityHistory,

    archiveTimestamp = doc.archivetimestamp.getTime,
    modificationTimestamp = doc.modificationtimestamp.getTime,
    followUpTimestamp = doc.followuptimestamp.map(_.getTime),
    documentDate = doc.documentdate.map(_.toString),

    archivingComplete = doc.archivingcomplete,
    actionRequired =  doc.actionrequired,

    sourceId = doc.sourceid,
    sourceReference = doc.sourcereference,
    thumbnailId = doc.thumbnailid)

  // implicit val formatDocument = Json.format[Document]
  // implicit val readsDocument = Json.reads[Document]
  implicit val writesDocument = Json.writes[Document]
}