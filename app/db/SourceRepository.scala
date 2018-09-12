package db

import javax.inject.{Inject, Singleton}

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
}
