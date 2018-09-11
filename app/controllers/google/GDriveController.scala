package controllers.google

import javax.inject.{Inject, Singleton}

import auth.AuthAction
import db.ConfigRepository
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Configuration}
import util.{GDriveClient, GDriveClientFactory}

import scala.concurrent.{ExecutionContext}

@Singleton
class GDriveController @Inject()(val config: Configuration, val configRepo: ConfigRepository,
                                 val gDriveClientFactory: GDriveClientFactory,
                                 implicit val exec: ExecutionContext) extends Controller {

  def listFolder(id: String) = AuthAction().async {
       implicit req => {
         val gdrive: GDriveClient = gDriveClientFactory.build(req.username)
         gdrive
           .listFolder(Some(id))
           .map(f => Ok(Json.toJson(f)))
           .recover{case ex: IllegalStateException => BadRequest(ex.getMessage)}
       }
     }
}
