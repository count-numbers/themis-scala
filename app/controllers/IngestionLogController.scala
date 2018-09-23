package controllers

import auth.AuthAction
import db.IngestionLogRepository
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext

/**
  */
@Singleton
class IngestionLogController @Inject()(val ingestionLogRepository: IngestionLogRepository, implicit val exec: ExecutionContext) extends Controller {

  def latest() = AuthAction().async {
    request => {
      for {
        log <- ingestionLogRepository.latest(0, 25)
      } yield {
        Ok(Json.toJson(log))
      }
    }
  }

}

