package controllers

import java.text.SimpleDateFormat

import models.{Comment, Document, Link}
import javax.inject._

import db.{CommentRepository, DocumentRepository}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.mvc.Action

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Singleton

import actions.DocumentActions
import auth.{AuthAction, AuthorizedRequest}
import play.api.{Configuration, Logger}
import util.ErrorResponse

/**
  * Created by simfischer on 3/9/17.
  */
@Singleton
class DocumentController @Inject()(val documentActions: DocumentActions,
                                   val documentRepository: DocumentRepository,
                                   val commentRepository: CommentRepository,
                                   val config: Configuration,
                                   implicit val exec: ExecutionContext) extends Controller {

  /** @see DocumentRepository#getById */
  def getById(id: Int) = AuthAction().async {
    request => {
      for {
        docOpt: Option[Document] <- documentRepository.getById(id)
      } yield {
          docOpt
            .map((d: Document) => Ok(Json.toJson(d)))
            .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No document with id ${id}.")))
        }
    }
  }

  /** @see DocumentRepository#search */
  def search(q: String,
             fromArchiveTimestamp: Option[Long],
             toArchiveTimestamp: Option[Long],
             fromModificationTimestamp: Option[Long],
             toModificationTime: Option[Long],
             offset: Option[Int],
             limit: Option[Int]) = AuthAction().async {
    request => {
      val resultFuture: Future[Seq[Document]] = documentRepository.search(q, fromArchiveTimestamp, toArchiveTimestamp, fromModificationTimestamp, toModificationTime, offset.getOrElse(0), limit.getOrElse(10))
      for (result <- resultFuture)
        yield Ok(Json.toJson(result))
    }
  }

  /** Returns 404 if document does not exist.
    * @see DocumentRepository#addTag*/
  def addTag(id: Int, tag: String) = AuthAction().async {
    implicit request => {
      for {
        doc: Option[Document] <- documentActions.addTag(id, tag, request.username)
      } yield {
        doc
          .map(d => Created.withHeaders("Location" -> getURI(id, "tags/"+tag)))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"Unknown document ${id}.")))
      }
    }
  }

  /** Returns 404 if document or tag do not exist.
    * @see DocumentRepository#addTag*/
  def deleteTag(id: Int, tag: String) = AuthAction().async {
    implicit request => {
      for {
        doc: Option[Document] <- documentActions.deleteTag(id, tag, request.username)
      } yield {
        doc
          .map(d => NoContent)
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No tag ${tag} for document ${id}.")))
      }
    }
  }

  def addComment(id: Int) = AuthAction().async(parse.text) {
    implicit request => {
      val text = request.body
      for {
        commentOpt: Option[Comment] <- commentRepository.addToDocument(docId = id, text = text, username = request.username)
      } yield {
        commentOpt
          .map(comment => Created(Json.toJson(comment)).withHeaders("Location" -> getURI(id, s"comments/${comment.id}")))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"Document ${id} not found.")))
      }
    }
  }

  def addLink(id: Int) = AuthAction().async(parse.json) {
    implicit request => {
      val link = request.body.as[Link]
      for {
        linkOpt: Option[Link] <- documentActions.addLink(docId = id, link = link, username = request.username)
      } yield {
        linkOpt
          .map(link => Created(Json.toJson(link)).withHeaders("Location" -> getURI(id, s"links/${link.id}")))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"Document ${id} not found.")))
      }
    }
  }

  /** Updates the document's description and returns the document if it exists. Otherwise 404s. */
  def patchDescription(id: Int) = AuthAction().async(parse.text) {
    implicit request => {
      val description = request.body
      for {
        docOpt: Option[Document] <- documentActions.setDescription(id, description, request.username)
      } yield {
        docOpt
          .map((d: Document) => Ok(Json.toJson(d)))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No document with id ${id}.")))
      }
    }
  }

  /** Updates the document's description and returns the document if it exists. Otherwise 404s. */
  def patchName(id: Int) = AuthAction().async(parse.text) {
    implicit request => {
      val name = request.body
      for {
          docOpt <- documentActions.rename(docId = id, name = name, username = request.username)
      } yield {
        docOpt
          .map((d: Document) => Ok(Json.toJson(d)))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No document with id ${id}.")))
      }
    }
  }

  def setContact(id: Int) = AuthAction().async(parse.text) {
    implicit request => {
      val contactId = request.body.toInt
      for {
        docOpt <- documentActions.setContact(docId = id, contactId = contactId, username = request.username)
      } yield {
        docOpt
          .map((d: Document) => Ok(Json.toJson(d)))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No document / contact with ids ${id} / ${contactId}.")))
      }
    }
  }

  def clearContact(id: Int) = AuthAction().async {
    implicit request => {
      for {
        docOpt <- documentActions.clearContact(docId = id, username = request.username)
      } yield {
        docOpt
          .map((d: Document) => Ok(Json.toJson(d)))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No document with ids ${id}.")))
      }
    }
  }

  /** Updates the document's description and returns the document if it exists. Otherwise 404s. */
  def setFollowup(id: Int) = AuthAction().async(parse.json) {
    implicit request => {
      println(s"Date: ${request.body}")
      val timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(request.body.as[String])
      for {
        docOpt <- documentActions.setFollowup(docId = id, timestamp = Some(timestamp), username = request.username)
      } yield {
        docOpt
          .map((d: Document) => Ok(Json.toJson(d)))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No document with id ${id}.")))
      }
    }
  }

  /** Updates the document's description and returns the document if it exists. Otherwise 404s. */
  def clearFollowup(id: Int) = AuthAction().async {
    implicit request => {
      for {
        docOpt <- documentActions.setFollowup(docId = id, timestamp = None, username = request.username)
      } yield {
        docOpt
          .map((d: Document) => Ok(Json.toJson(d)))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No document with id ${id}.")))
      }
    }
  }

  /** Updates the document's description and returns the document if it exists. Otherwise 404s. */
  def archivingComplete(id: Int) = AuthAction().async(parse.text) {
    implicit request => {
      val complete = request.body.toBoolean
      for {
        docOpt: Option[Document] <- documentActions.markArchivingComplete(id, complete, request.username)
      } yield {
        docOpt
          .map((d: Document) => Ok(Json.toJson(d)))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No document with id ${id}.")))
      }
    }
  }

  /** Updates the document's description and returns the document if it exists. Otherwise 404s. */
  def actionRequired(id: Int) = AuthAction().async(parse.text) {
    implicit request => {
      val actionRequired = request.body.toBoolean
      for {
        docOpt: Option[Document]   <- documentActions.markActionRequired(docId = id, actionRequired = actionRequired, username = request.username)
      } yield {
        docOpt
          .map((d: Document) => Ok(Json.toJson(d)))
          .getOrElse(NotFound(ErrorResponse(404, "Not found", s"No document with id ${id}.")))
      }
    }
  }

  def attention() = Action {
    request => Ok("[]")
  }

  /** Private helper to create Location headers for 201 CREATED responses. */
  private def getURI[T](docId: Int, suffix: String)(implicit request: AuthorizedRequest[T]) = {
    "http://"+request.domain+config.getString("play.http.context") + "/document/" + docId + "/" + suffix
  }
}
