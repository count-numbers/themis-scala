package controllers

import javax.inject.{Inject, Singleton}

import auth.AuthAction
import db.SourceRepository
import models.DocumentSource
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext

@Singleton
class SourceController @Inject()(val sourceRepository: SourceRepository, implicit val exec: ExecutionContext) extends Controller {

  def getMine() = AuthAction().async {
    implicit req => {
      sourceRepository
        .getAllForUser(req.username)
        .map((s: Seq[_root_.db.Tables.SourceRow]) => Ok(Json.toJson(s.map(DocumentSource.of(_)))))
    }
  }
}
