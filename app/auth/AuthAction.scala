package auth

import util.ErrorResponse
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

case class TokenInfo(val sub: String, val email: Option[String], val name: Option[String], val picture: Option[String])

/**
  * Created by simfischer on 12/21/16.
  */
class AuthAction(implicit executionContext: ExecutionContext) extends ActionBuilder[AuthorizedRequest] {

  implicit val readsTokenInfo = Json.reads[TokenInfo]

  override def invokeBlock[A](request: Request[A], block: (AuthorizedRequest[A]) => Future[Result]): Future[Result] = {
    val tokenOpt: Option[String] = getTokenFromCookie(request)
    tokenOpt match {
      case None => Future.successful(makeUnauthorized("No JWT cookie"))
      case Some(token) => {
        Logger.debug(s"Found token in cookie: ${token.take(4)}...${token.drop(token.length-4)}")
        val usernameOpt: Option[String] = JWT.validateJWT(token)
        usernameOpt match {
          case None => Future.successful(makeUnauthorized("Bad JWT token"))
          case Some(username: String) => {
            val authRequest = new AuthorizedRequest[A](username, request)
            Logger.debug(s"Authorizing user ${username}")
            block(authRequest)
          }
        }
      }
    }
  }

  private def getTokenFromCookie(rh: RequestHeader): Option[String] = {
    for {
      cookie: Cookie <- rh.cookies.get(AuthAction.CookieName)
    } yield cookie.value
  }

  private def makeUnauthorized(msg: String): Result = {
    Results.Unauthorized(ErrorResponse(401, "Unauthorized", msg))
  }
}

object AuthAction {
  val CookieName = "themis-auth-jwt"
  def apply()(implicit ec: ExecutionContext): AuthAction = new AuthAction()
}

/** Holds the regular expression patterns that are used for reading the tokens from headers and query parameters. Extracted here due to performance reasons.*/
object Patterns {
  //val queryParamTokenPattern = new Regex("^(?i)(.+)$", "token")
  val headerTokenPattern = new Regex("^(?i)(Bearer (.+))$", "all", "token")
}