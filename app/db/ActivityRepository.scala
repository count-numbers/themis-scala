package db


import javax.inject.{Inject, Singleton}

import db.Tables._
import db.Tables.profile.api._
import models.Activity
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import slick.backend.DatabaseConfig
import slick.dbio.DBIOAction
import slick.dbio.Effect.Read
import slick.driver.JdbcProfile
import slick.profile.FixedSqlStreamingAction

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by simfischer on 3/16/17.
  */
@Singleton
class ActivityRepository @Inject()(val dbConfigProvider: DatabaseConfigProvider, implicit val exec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  /** Returns all comments, linked to users, for the given document ID in ascending order of timestamp. */
  def getForDocument(docId: Int): Future[Seq[models.Activity]] = {
    val query: FixedSqlStreamingAction[Seq[((ActivityRow, Option[UserRow]), Option[DocumentRow])], ((ActivityRow, Option[UserRow]), Option[DocumentRow]), Read] = (
        Tables.Activity.filter(_.docid === docId)
        joinLeft Tables.User on (_.userid === _.id)
        joinLeft Tables.Document on (_._1.docid === _.id)
        sortBy (_._1._1.timestamp.asc)  result)


    val q: DBIOAction[Seq[models.Activity], NoStream, Read] = for {
      rows: Seq[((ActivityRow, Option[UserRow]), Option[DocumentRow])] <- query
    } yield {
      rows.map{
        case ((activityRow: ActivityRow, userRowOpt: Option[UserRow]), documentRowOpt: Option[DocumentRow])
          // user and document are NOT NULL and foreign key, so safe to call get on option
        => models.Activity.of(activityRow, userRowOpt.get, documentRowOpt.get)
      }
    }

    dbConfig.db.run(q)
  }

  def latest(offset: Int, limit: Int): Future[Seq[Activity]] = {
    val q =
      (Tables.Activity
      joinLeft Tables.User on (_.userid === _.id)
      joinLeft Tables.Document on (_._1.docid === _.id)
      sortBy (_._1._1.timestamp.desc)
      drop (offset)
      take (limit)
        result)
    val result: DBIOAction[Seq[Activity], NoStream, Read] = for {
      rows: Seq[((ActivityRow, Option[UserRow]), Option[DocumentRow])] <- q
    } yield {
      for {
        ((activity: ActivityRow, userOpt: Option[UserRow]), docOpt: Option[DocumentRow]) <- rows
      } yield {
        models.Activity.of(activity, userOpt.get, docOpt.get)
      }
    }
    dbConfig.db.run(result)
  }

  /** Saves a new activity with the given parameters and links it to the given document and user.
    * Returns true on success. */
  def persist(docId: Int, userId: Int, activityType: String, arguments: Seq[String], timestamp: Long): Future[Boolean] = {
    val row = ActivityRow(id = -1,
      docid = docId,
      userid = userId,
      arguments = Some(Json.toJson(arguments).toString()),
      activitytype = activityType,
      timestamp = new java.sql.Timestamp(timestamp))
    val insert = (Tables.Activity returning Tables.Activity.map(_.id)) += row
    dbConfig.db.run(insert).map(_ != -1)
  }

}
