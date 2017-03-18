package controllers

import javax.inject.Singleton
import javax.inject.Inject

import auth.AuthAction
import db.DocumentRepository
import play.api.libs.json.Json
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext

/**
  * Created by simfischer on 3/13/17.
  */
@Singleton
class MetaController @Inject()(val documentRepository: DocumentRepository, implicit val exec: ExecutionContext) extends Controller {

  def documentsMeta() = AuthAction().async {
    request => {
      for {
        meta <- documentRepository.meta()
      } yield {
        Ok(Json.toJson(meta))
      }
    }
  }

}
