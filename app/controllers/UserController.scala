package controllers

import javax.inject.{Singleton, _}

import auth.AuthAction
import db.{DocumentRepository, UserRepository}
import models.{Document, User}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import play.api.mvc.{Action, _}
import util.ErrorResponse

import scala.concurrent.{ExecutionContext, Future}

case class PasswordUpdate(val oldPassword: String, val newPassword: String)

/**
  * Created by simfischer on 3/9/17.
  */
@Singleton
class UserController @Inject()(val userRepository: UserRepository, implicit val exec: ExecutionContext) extends Controller {

  def getMe() = AuthAction().async {
    request => {
      responseForUser(request.username)
    }
  }

  def getByUsername(username: String) = AuthAction().async {
    request => {
      responseForUser(username)
    }
  }

  //def changePassword(username: String) = ???

  /** Creates a response containing the single user with the given username. */
  private def responseForUser(username: String): Future[Result] = {
    for (userOpt: Option[User] <- userRepository.getByUsername(username))
      yield {
        userOpt
          .map((user: User) => Ok(Json.toJson(user)))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No user named ${username}.")))
      }
  }
}
