package db

import javax.inject.{Inject, Singleton}

import db.Tables.profile.api._
import models.User
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import _root_.db.Tables.UserRow

/**
  * Created by simfischer on 3/9/17.
  */
@Singleton
class UserRepository @Inject()(val dbConfigProvider: DatabaseConfigProvider, implicit val exec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  def getByUsername(username: String): Future[Option[User]] = {
      val query = db.Tables.User.withFilter(_.username === username)
      val resultFuture: Future[Seq[UserRow]] = dbConfig.db.run(query.result)
      resultFuture.map(
        (userRows: Seq[_root_.db.Tables.UserRow]) =>
          userRows.headOption.map(user=>
            User.of(user)))
  }

  def validateCredentials(username: String, password: String): Future[Option[User]] = {
    val query = db.Tables.User.withFilter(u => (u.username === username) && (u.password === password))
    val resultFuture: Future[Seq[UserRow]] = dbConfig.db.run(query.result)
    for (rows: Seq[UserRow] <- resultFuture)
      yield {
        for (row: UserRow <- rows.headOption)
          yield User.of(row)
      }
  }
}
