package controllers.google

import javax.inject.{Inject, Singleton}

import auth.AuthAction
import db.ConfigRepository
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Configuration, Logger}
import util.{GDriveClient, GDriveClientFactory, GDriveFile}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class GDriveController @Inject()(val config: Configuration, val configRepo: ConfigRepository,
                                 val gDriveClientFactory: GDriveClientFactory,
                                 implicit val exec: ExecutionContext) extends Controller {

  def listFolder(id: Option[String]) = AuthAction().async {
       implicit req => {
         val gdrive: GDriveClient = gDriveClientFactory.build(req.username)
         val files: Try[Seq[GDriveFile]] = gdrive.listFolder(id)

         val r: Result = files match {
           case Failure(ex: Throwable) => BadRequest(ex.getMessage)
           case Success(children) => Ok(Json.toJson(children))
         }
         Future(r)
       }
     }
}
