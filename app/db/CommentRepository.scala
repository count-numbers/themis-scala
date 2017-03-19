package db

import javax.inject.{Inject, Singleton}

import db.Tables._
import db.Tables.profile.api._
import models.Comment
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.dbio.DBIOAction
import slick.dbio.Effect.{Read, Write}
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by simfischer on 3/15/17.
  */
@Singleton
class CommentRepository @Inject()(val dbConfigProvider: DatabaseConfigProvider, implicit val exec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  /** Returns all comments, linked to users, for the given document ID in ascending order of timestamp. */
  def getForDocument(docId: Int): Future[Seq[models.Comment]] = {
    val q: DBIOAction[Seq[models.Comment], NoStream, Read] = for {
      rows: Seq[(CommentRow, Option[UserRow])] <-  Tables.Comment.filter(_.docid === docId) joinLeft Tables.User on (_.userid === _.id) sortBy (_._1.timestamp.asc)  result
    } yield {
      rows.map{
        case (commentRow: CommentRow, userRowOpt: Option[UserRow])
        // user is NOT NULL and foreign key, so userRow cannot be None
        => models.Comment.of(commentRow, userRowOpt.get)
      }
    }

    dbConfig.db.run(q)
  }

  /** Adds a comment and links it to the document. Returns future to None if document does not exist. */
  def addToDocument(docId: Int, text: String, username: String): Future[Option[Comment]] = {
    val insertAction: DBIOAction[Some[Comment], NoStream, Read with Read with Effect with Write] = for {
      // lookup whether we have this document first
      docFound: Boolean               <- Tables.Document.filter(_.id === docId).exists.result
      // lookup matching users, but only if docFound, otherwise we stop here
      userRows: Seq[Tables.UserRow]   <- docFound match {
                                          case true => Tables.User.filter(_.username === username).result
                                          case false => DBIO.successful(Seq())
                                        }
      // take first (and only) user, if exists, otherwise stop here
      userRow: Tables.UserRow         <- userRows match {
                                            case Seq(userRow: Tables.UserRow) => DBIO.successful(userRow)
                                            case _ => DBIO.failed(new RuntimeException(s"User ${username} not found!"))
                                         }
      // insert comment
      commentRow: Tables.CommentRow   <- Tables.Comment returning Tables.Comment += Tables.CommentRow(id = -1, docid = docId, text = text, userid = userRow.id, timestamp = new java.sql.Timestamp(System.currentTimeMillis()))
    } yield {
      Some(models.Comment.of(commentRow, userRow))
    }
    dbConfig.db.run(insertAction)
  }
}
