package controllers

import auth.{AuthAction, AuthorizedRequest}
import db.{SourceRepository, Tables}
import javax.inject.{Inject, Singleton}
import models.DocumentSource
import play.api.libs.json.Json
import play.api.mvc.Controller
import play.api.{Configuration, Logger}
import services.ingestion.IngestionServiceRunner
import util.ErrorResponse

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SourceController @Inject()(val sourceRepository: SourceRepository,
                                 val config: Configuration,
                                 val ingestionServiceRunner: IngestionServiceRunner,
                                 implicit val exec: ExecutionContext) extends Controller {

  def getMine() = AuthAction().async {
    implicit req => {
      sourceRepository
        .getAllForUser(req.username)
        .map((s: Seq[Tables.SourceRow]) => Ok(Json.toJson(s.map(DocumentSource.of(_)))))
    }
  }

  def get(id: Int) = AuthAction().async {
    sourceRepository
      .getById(id)
      .map({
        case None => NotFound
        case Some((sr: Tables.SourceRow, _)) => Ok(Json.toJson(DocumentSource.of(sr)))
      })
  }

  def create() = AuthAction().async(parse.json) {
    implicit request => {
      val docSource = request.body.as[DocumentSource]
      sourceRepository
        .saveNew(docSource, request.username)
        .map((s: DocumentSource) => Created(Json.toJson(s)).withHeaders("Location" -> getURI(s.id.getOrElse(0))))
    }
  }

  def update(id: Int) = AuthAction().async(parse.json) {
    implicit request => {
      val docSource = request.body.as[DocumentSource]
      sourceRepository
        .save(docSource, request.username)
        .map((success: Boolean) => if (success) Ok(Json.toJson(docSource)) else NotFound(ErrorResponse(404, "Not found", s"No such contact ${id}.")))
    }
  }

  def delete(id: Int) = AuthAction().async {
    implicit request => {
      sourceRepository
        .delete(id)
        .map(if (_) NoContent else NotFound)
    }
  }

  def execute(id: Int) = AuthAction().async {
    implicit request => {
      Logger.info(s"Executing source ${id}.")
      ingestionServiceRunner.runSingle(id)
      Future{NoContent}
    }
  }

  /** Private helper to create Location headers for 201 CREATED responses. */
  private def getURI[T](id: Int)(implicit request: AuthorizedRequest[T]) = {
    "http://"+request.domain+config.getString("play.http.context") + "/source/" + id
  }
}
