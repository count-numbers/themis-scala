package db

import javax.inject.{Inject, Singleton}

import db.Tables._
import db.Tables.profile.api._
import models.Link
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.dbio.{DBIOAction, NoStream}
import slick.dbio.Effect.Read
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by simfischer on 3/15/17.
  */
@Singleton
class LinkRepository @Inject()(val dbConfigProvider: DatabaseConfigProvider, implicit val exec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  def getForDocument(docId: Int): Future[Seq[Link]] = {
    val q: DBIOAction[Seq[models.Link], NoStream, Read] = for {
      rows: Seq[LinkRow] <-  Tables.Link.filter(_.docid === docId) result
    } yield {
      rows.map(models.Link.of(_))
    }

    dbConfig.db.run(q)
  }

  def persist(link: Link, docId: Int): Future[Tables.LinkRow] = {
    val action = (Tables.Link returning Tables.Link) += LinkRow(id = -1, docid = docId, url = link.url, title = link.title, linktype = link.linkType)
    dbConfig.db.run(action)
  }
}
