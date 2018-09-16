package controllers.google

import auth.AuthAction
import db.ConfigRepository
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import util.{GDriveClient, GDriveClientFactory, GDriveFile}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class GDriveController @Inject()(val config: Configuration, val configRepo: ConfigRepository,
                                 val gDriveClientFactory: GDriveClientFactory,
                                 implicit val exec: ExecutionContext) extends Controller {

  def listFolder(id: String) = AuthAction().async {
       implicit req => {
         Future {
           val gdrive: GDriveClient = gDriveClientFactory.build(req.username)
           val filesTry: Try[Seq[GDriveFile]] = gdrive.listFolder(Some(id))
           filesTry match {
             case Success(files) => Ok(Json.toJson(files))
             case Failure(ex: IllegalStateException) => BadRequest(ex.getMessage)
             case Failure(ex) => InternalServerError(ex.getMessage)
           }
         }
       }
     }
}
