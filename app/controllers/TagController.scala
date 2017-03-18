package controllers

import javax.inject._

import auth.AuthAction
import db.TagRepository
import play.api.libs.json.Json
import play.api.mvc.Controller

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by simfischer on 3/14/17.
  */

@Singleton
class TagController @Inject()(val tagRepository: TagRepository, implicit val exec: ExecutionContext) extends Controller {

  def search(q: String) = AuthAction().async {
    request => {
      for {
        tags: Seq[_root_.db.Tables.DtagRow] <- tagRepository.search(q)
      } yield {
        Ok(Json.toJson(tags.map(_.name)))
      }
    }
  }
}
