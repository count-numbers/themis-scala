package db

import db.Tables._
import db.Tables.profile.api._
import javax.inject.{Inject, Singleton}
import models.{Comment, DocumentSource}
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.dbio.DBIOAction
import slick.dbio.Effect.{Read, Write}
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * Stores configurations for DataSources.
  */
@Singleton
class SourceRepository @Inject()(val dbConfigProvider: DatabaseConfigProvider, implicit val exec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  def getById(sourceId: Int): Future[Option[(Tables.SourceRow, Option[Tables.UserRow])]] = {
    val q = Tables.Source.filter(_.id === sourceId) joinLeft Tables.User on (_.userid === _.id)
    dbConfig.db
      .run(q.result)
      .map(_.headOption)
  }

  def getAll(): Future[Seq[(Tables.SourceRow, Option[Tables.UserRow])]] = {
    val q = Tables.Source joinLeft Tables.User on (_.userid === _.id)
    dbConfig.db
      .run(q.result)
  }

  def getAllForUser(username: String): Future[Seq[_root_.db.Tables.SourceRow]] = {
    val query = for {
      (source, user) <- db.Tables.Source joinLeft db.Tables.User on (_.userid === _.id) if (user.map(_.username) === username)
    } yield (source)

    dbConfig.db.run(query.result)
  }

  def save(source: DocumentSource, username: String): Future[Boolean] = {
    val updateAction: DBIOAction[Int, NoStream, Read with Effect with Write] = for {
      // lookup matching users
      userRows: Seq[Tables.UserRow]   <- Tables.User.filter(_.username === username).result
      // take first (and only) user, if exists, otherwise stop here
      userRow: Tables.UserRow         <- userRows match {
        case Seq(userRow: Tables.UserRow) => DBIO.successful(userRow)
        case _ => DBIO.failed(new RuntimeException(s"User ${username} not found!"))
      }
      affectedRows: Int <- Tables.Source.filter(_.id === source.id.get) update(toRow(source, userRow.id))
    } yield {
      affectedRows
    }
    dbConfig.db.run(updateAction).map(_ == 1)
  }

  def saveNew(source: DocumentSource, username: String): Future[DocumentSource] = {
    val insertAction: DBIOAction[DocumentSource, NoStream, Read with Effect with Write] = for {
      // lookup matching users
      userRows: Seq[Tables.UserRow]   <- Tables.User.filter(_.username === username).result
      // take first (and only) user, if exists, otherwise stop here
      userRow: Tables.UserRow         <- userRows match {
        case Seq(userRow: Tables.UserRow) => DBIO.successful(userRow)
        case _ => DBIO.failed(new RuntimeException(s"User ${username} not found!"))
      }
      sourceRow: Tables.SourceRow     <- (Tables.Source returning Tables.Source) += toRow(source, userRow.id).copy(id = -1)
    } yield {
      DocumentSource.of(sourceRow)
    }
    dbConfig.db.run(insertAction)
  }

  def delete(id: Int): Future[Boolean] = {
    val q = Tables.Source.filter(_.id === id)
    dbConfig.db.run(q.delete).map(_ == 1)
  }


  def toRow(source: DocumentSource, userId: Int): SourceRow = Tables.SourceRow(
    id = source.id.getOrElse(-1),
    `type` = source.`type`,
    userid =  userId,
    filesourcefolder = source.fileSourceFolder,
    gdrivesourcefolder = source.gdriveSourceFolderId,
    gdrivearchivefolder = source.gdriveArchiveFolderId
  )

}
