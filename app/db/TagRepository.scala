package db

import javax.inject.{Inject, Singleton}

import db.Tables._
import db.Tables.profile.api._
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by simfischer on 3/14/17.
  */
@Singleton
class TagRepository @Inject()(val dbConfigProvider: DatabaseConfigProvider, implicit val exec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  def search(q: String): Future[Seq[Tables.DtagRow]] = {
    val query = Tables.Dtag.filter(_.name.startsWith(q))
    dbConfig.db.run(query.result)
  }
}
