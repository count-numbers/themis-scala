package db

import db.Tables._
import db.Tables.profile.api._
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

case class LogEntry(level: String, timestamp: Long, sourceId: Option[Int], username: Option[String], text: String, srcId: Option[Int], docId: Option[Int])
object LogEntry {
  implicit val writeLogEntry = Json.writes[LogEntry]
}

@Singleton
class IngestionLogRepository @Inject()(val dbConfigProvider: DatabaseConfigProvider, implicit val exec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  def warn(text: String, srcId: Option[Int], username: Option[String], docId: Option[Int]): Unit = {
    log("warn", text, srcId, username, docId)
  }

  def info(text: String, srcId: Option[Int], username: Option[String], docId: Option[Int]): Unit = {
    log("info", text, srcId, username, docId)
  }

  def log(level: String, text: String, srcId: Option[Int], username: Option[String], docId: Option[Int]): Unit = {
    val row: IngestionLogRow = IngestionLogRow(-1, level, new java.sql.Timestamp(System.currentTimeMillis()), docId, srcId, username, text)
    val action = (Tables.IngestionLog returning Tables.IngestionLog) += row
    dbConfig.db.run(action)
  }

  def latest(offset: Int, limit: Int): Future[Seq[LogEntry]] = {
    val q: DBIOAction[Seq[LogEntry], NoStream, Effect.Read] = for {
      rows: Seq[_root_.db.Tables.IngestionLogRow] <- Tables.IngestionLog.sortBy(_.timestamp desc).drop(offset).take(limit).result
    } yield {
      for {
        row: _root_.db.Tables.IngestionLogRow <- rows
      } yield {
        LogEntry(row.level, row.timestamp.getTime, row.srcid, row.username, row.text, row.srcid, row.docid)
      }
    }
    dbConfig.db.run(q)
  }
}
