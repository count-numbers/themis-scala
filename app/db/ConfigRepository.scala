package db

import javax.inject.{Inject, Singleton}

import db.Tables._
import db.Tables.profile.api._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ConfigRepository @Inject()(val dbConfigProvider: DatabaseConfigProvider, implicit val exec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  def getForKey(key: String): Future[Option[String]] = {
    val query = db.Tables.Config.withFilter(_.key === key)
    val resultFuture: Future[Seq[ConfigRow]] = dbConfig.db.run(query.result)
    resultFuture.map(
      (configRows: Seq[_root_.db.Tables.ConfigRow]) =>
        configRows.headOption.map(config => config.value))
  }

  def getForKeySync(key: String): Option[String] = {
    val value = getForKey(key)
    Await.ready(value, Duration.Inf).value.get match {
      case Success(v) => v
      case Failure(ex) => {
        Logger.warn(s"Failed to read config key ${key}: ${ex}", ex)
        None
      }
    }
  }

  def deleteKey(key: String): Future[Int] = {
    val query = db.Tables.Config.withFilter(_.key === key)
    dbConfig.db.run(query.delete)
  }
  def persist(key: String, value: String): Future[Int] = {
    val action = Tables.Config.insertOrUpdate(_root_.db.Tables.ConfigRow(key, value))

    val f: Future[Int] = dbConfig.db.run(action)
    f.onSuccess({ case rows: Int => Logger.info(s"Inserted config ${key} = ${value}") })
    f.onFailure({ case ex: Throwable => Logger.warn(s"Failed to insert config: ${ex}", ex) })
    return f;
  }

}
