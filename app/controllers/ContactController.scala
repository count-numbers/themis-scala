package controllers

import javax.inject._

import actions.DocumentActions
import play.api.mvc.Result._
import auth.{AuthAction, AuthorizedRequest}
import db.{CommentRepository, ContactRepository, DocumentRepository}
import models.{Contact, Document}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.Controller
import util.ErrorResponse

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by simfischer on 3/18/17.
  */
@Singleton
class ContactController @Inject()(val config: Configuration,
                                  contactRepository: ContactRepository,
                                  documentRepository: DocumentRepository,
                                  implicit val exec: ExecutionContext) extends Controller {


  def getById(id: Int) = AuthAction().async {
    request => {
      contactRepository
        .getById(id)
        .map(_.map((c: Contact) => Ok(Json.toJson(c)))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No such contact ${id}"))))
    }
  }

  def createNew() = AuthAction().async(parse.json) {
    implicit request => {
      val newContact = request.body.as[Contact]
      contactRepository
        .saveNew(newContact)
        .map((c: Contact) => Ok(Json.toJson(c)).withHeaders("Location" -> getURI(c.id.getOrElse(0))))
    }
  }

  def update(id: Int) = AuthAction().async(parse.json) {
    implicit request => {
      val contact = request.body.as[Contact]
      contactRepository
        .save(contact, id)
        .map((success: Boolean) => if (success) Ok(Json.toJson(contact)) else NotFound(ErrorResponse(404, "Not found", s"No such contact ${id}.")))
    }
  }

  def search(q: String, offset: Option[Int], limit: Option[Int]) = AuthAction().async {
    implicit request => {
      contactRepository
        .search(searchTerm = q, limit = limit.getOrElse(10), offset = offset.getOrElse(0))
        .map((results: Seq[Contact]) => Ok(Json.toJson(results)))
    }
  }

  def getDocumentsForContact(id: Int, offset: Option[Int], limit: Option[Int]) = AuthAction().async {
    implicit request => {
      documentRepository
        .getDocumentsForContact(id, offset.getOrElse(0), limit.getOrElse(10))
        .map((results: Seq[Document]) => Ok(Json.toJson(results)))
    }
  }

  /** Private helper to create Location headers for 201 CREATED responses. */
  private def getURI[T](docId: Int, suffix: String)(implicit request: AuthorizedRequest[T]) = {
    "http://"+request.domain+config.getString("play.http.context") + "/contact/" + docId + "/" + suffix
  }

  /** Private helper to create Location headers for 201 CREATED responses. */
  private def getURI[T](id: Int)(implicit request: AuthorizedRequest[T]) = {
    "http://"+request.domain+config.getString("play.http.context") + "/contact/" + id
  }

}
