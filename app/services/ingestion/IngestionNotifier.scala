package services.ingestion

import java.nio.file.{Path, Paths}
import javax.inject.{Inject, Singleton}

import db.DocumentRepository
import models.{Document, User}
import play.api.{Configuration, Logger}
import play.api.libs.mailer.{AttachmentFile, Email, MailerClient}
import util.FileSystemUtils

import scala.concurrent.ExecutionContext

/**
  *  Sends notification emails for newly ingested documents.
  *
  * Created by simfischer on 3/26/17.
  */
@Singleton
class IngestionNotifier @Inject() (val documentRepository: DocumentRepository, val mailerClient: MailerClient)
                                  (implicit val config: Configuration, val executionContext: ExecutionContext) {

  def notify(docId: Int) = {
    for {
      docOpt: Option[Document] <- documentRepository.getById(docId)
      doc: Document <- docOpt
      owner: User <- doc.owner
    } {
      val body: String = views.html.emailingestion(doc).body

      val attachments = for {
        attachment  <- doc.attachments.getOrElse(Seq())
        thumb: Path <- FileSystemUtils.thumbnailPath(attachment.id)
      } yield {
        AttachmentFile(
          name=attachment.name,
          file=thumb.toFile,
          contentId = Some(s"att${attachment.id}"),
          disposition = Some("inline"))
      }

      val email: Email = Email(
        subject=s"[Themis] New document: ${doc.name}",
        bodyHtml = Some(body),
        attachments=attachments,
        to=Seq(owner.email),
        from=config.getString("themis.email.from").getOrElse("themis@example.com")
      )

      val msgId: String = mailerClient.send(email)
      Logger.info(s"Sent email to ${email.to}. Message ID is ${msgId}.")
    }
  }
}
