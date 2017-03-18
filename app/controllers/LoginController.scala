package controllers

import javax.inject.Inject

import auth.{AuthAction, JWT}
import db.UserRepository
import models.User
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller, Cookie, Result}
import util.ErrorResponse

import scala.concurrent.{ExecutionContext, Future}

case class UsernamePassword(val username: String, val password: String)
object UsernamePassword {
  implicit val readsUsernamePassword = Json.reads[UsernamePassword]
}

/**
  * Created by simfischer on 3/10/17.
  */
class LoginController @Inject()(val userRepository: UserRepository, implicit val exec: ExecutionContext) extends Controller  {

  /** Parses username and password from Json structure in payload and sets a JWT cookie if validate successfully. */
  def login() = Action.async(parse.json) {
    request => {
      // parse payload to usernam / password
      val up: UsernamePassword = request.body.as[UsernamePassword]

      // validate password
      val userFuture: Future[Option[User]] = userRepository.validateCredentials(up.username, up.password)
      userFuture.map{
        case None => Unauthorized(ErrorResponse(401, "Unauthorized", "Credentials wrong"))
        case Some(user: User) => {
          Logger.debug(s"Creating JWT for ${user.username}.")
          val claims = JWT.makeClaims(user)
          val jwt = JWT.makeJWT(claims)
          Ok(claims.toJson).withCookies(Cookie(name = AuthAction.CookieName, value = "\"" + jwt + "\"", maxAge = Some(JWT.JwtLifetimeSecs)))
        }
      }
    }
  }
}
