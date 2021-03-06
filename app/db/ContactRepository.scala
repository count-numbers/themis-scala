package db

import db.Tables._
import db.Tables.profile.api._
import javax.inject.{Inject, Singleton}
import models.Contact
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by simfischer on 3/15/17.
  */
@Singleton
class ContactRepository @Inject()(val dbConfigProvider: DatabaseConfigProvider, implicit val exec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  def getById(id: Int): Future[Option[Contact]] = {
    val q = Tables.Contact.filter(_.id === id)
    dbConfig.db
      .run(q.result)
      .map(_.headOption)
      .map(_.map(Contact.of))
  }

  def save(contact: Contact, id: Int): Future[Boolean] = {
    val action = Tables.Contact.filter(_.id === id) update(toRow(contact))
    dbConfig.db.run(action).map(_ == 1)
  }

  def saveNew(contact: Contact): Future[Contact] = {
    val action = (Tables.Contact returning Tables.Contact) += toRow(contact).copy(id = -1)
    dbConfig.db.run(action).map(Contact.of)
  }

  def search(searchTerm: String, offset: Int, limit: Int): Future[Seq[Contact]] = {
    val searchExpression =s"%${searchTerm}%"
    val q =
      (if (searchTerm == "")
        Tables.Contact
      else
        Tables.Contact.filter(_.name like searchExpression))
      .drop(offset).take(limit)
    dbConfig.db
      .run(q.result)
      .map(_.map(Contact.of))
  }

  def keywords: Future[Seq[(Int, String)]] = {
    val q = Tables.Contact.filter(_.keywords.isDefined).map(c => (c.id, c.keywords.get))
    dbConfig.db.run(q.result)
  }

  def toRow(contact: Contact): ContactRow = ContactRow(
    id = contact.id.getOrElse(-1),
    identifier = contact.identifier,
    name = contact.name,
    address1 = contact.address1,
    address2 = contact.address2,
    zip = contact.zip,
    city = contact.city,
    region = contact.region,
    country = contact.country,
    email = contact.email,
    keywords =  contact.keywords
  )
}
