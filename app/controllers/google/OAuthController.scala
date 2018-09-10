package controllers.google

import javax.inject.{Inject, Singleton}

import auth.AuthAction
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl
import db.ConfigRepository
import play.api.mvc._
import play.api.{Configuration, Logger}
import util.{GDriveClient, GDriveClientFactory}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OAuthController @Inject()(val config: Configuration, val configRepo: ConfigRepository,
                                val gDriveClientFactory: GDriveClientFactory,
                                implicit val exec: ExecutionContext) extends Controller {

  def oauthStart() = AuthAction().async {
    implicit req => {
      val gdrive: GDriveClient = gDriveClientFactory.build(req.username)
      val credentialsOpt: Option[Credential] = Option(gdrive.authCodeFlow.loadCredential(req.username))

      val result: Result = credentialsOpt match {
        case None => {
          val authUrl: GoogleAuthorizationCodeRequestUrl = gdrive.authCodeFlow.newAuthorizationUrl()
          authUrl.setRedirectUri(controllers.google.routes.OAuthController.oauthReturn(None).absoluteURL(req.secure))
          Logger.info(s"Redirecting user to Google OAuth flow at ${authUrl}.")
          Redirect(authUrl.toString, 302)
          }
        case Some(credentials) => {
          config.getString("themis.frontend.url") match {
            case Some(url: String) => Redirect(url + "#/profile?googleauth=skipped")
            case None => Ok("Frontend URL not configured.")
          }
        }
      }
      Future(result)
    }
  }

  def oauthRevoke() = AuthAction().async {
    req => {
      Logger.debug(s"Revoking oauth credentials.")
      val gdrive: GDriveClient = gDriveClientFactory.build(req.username)
      gdrive.authCodeFlow.getCredentialDataStore.delete(req.username)
      val result = config.getString("themis.frontend.url") match {
        case Some(url: String) => Redirect(url + "#/profile?googleauth=revoked")
        case None => Ok("Frontend URL not configured.")
      }
      Future(result)
    }
  }

  def oauthReturn(code: Option[String]) = AuthAction().async {
    implicit req => {
      Logger.debug(s"Returned from Google OAuth flow with code ${code}.")
      val gdrive: GDriveClient = gDriveClientFactory.build(req.username)
      val tokenRequest = gdrive.authCodeFlow.newTokenRequest(code.get)
      tokenRequest.setRedirectUri(controllers.google.routes.OAuthController.oauthReturn(None).absoluteURL(req.secure))
      Logger.info(s"Executing token request: ${tokenRequest}.")
      val credential = tokenRequest.execute()
      Logger.info(s"Got credential: ${credential}.")
      val x: Credential = gdrive.authCodeFlow.createAndStoreCredential(credential, req.username)
      Logger.info(s"Stored credential: ${credential}.")

      val result = config.getString("themis.frontend.url") match {
        case Some(url: String) => Redirect(url + "#/profile?googleauth=success")
        case None => Ok("Frontend URL not configured.")
      }
      Future(result)
    }
  }
}

