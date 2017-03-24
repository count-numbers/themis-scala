package db

import javax.inject.{Inject, Singleton}

import db.Tables._
import db.Tables.profile.api._
import models.Attachment
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.dbio.DBIOAction
import slick.dbio.Effect.Read
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by simfischer on 3/16/17.
  */
@Singleton
class AttachmentRepository @Inject()(val dbConfigProvider: DatabaseConfigProvider, implicit val exec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  /** Returns all comments, linked to users, for the given document ID in ascending order of timestamp. */
  def getForDocument(docId: Int): Future[Seq[models.Attachment]] = {
    val q: DBIOAction[Seq[Attachment], NoStream, Read] = for {
      rows: Seq[AttachmentRow] <- Tables.Attachment.filter(_.docid === docId).result
    } yield {
      rows.map(models.Attachment.of)
    }

    dbConfig.db.run(q)
  }

  def persist(docId: Int, name: String, size: Long, mimeType: String): Future[Int] = {
    val row = AttachmentRow(id = -1, docid = docId, name = name, size = size, mimetype = mimeType)
    val action = (Tables.Attachment returning Tables.Attachment.map(_.id)) += row
    dbConfig.db.run(action)
  }
}
