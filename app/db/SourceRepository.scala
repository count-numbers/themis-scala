package db

import javax.inject.{Inject, Singleton}

import db.Tables._
import db.Tables.profile.api._
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
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
}
