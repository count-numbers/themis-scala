package controllers

import java.text.SimpleDateFormat

import actions.DocumentActions
import auth.{AuthAction, AuthorizedRequest}
import db.{CommentRepository, DocumentRepository}
import javax.inject.{Singleton, _}
import models.{Comment, Document, Link}
import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import play.api.mvc._
import util.ErrorResponse

import scala.concurrent.{ExecutionContext, Future, Promise}

case class PatchableDocFields(name: Option[String],
                              description: Option[String],
                              archivingComplete: Option[Boolean],
                              actionRequired: Option[Boolean],
                              followUpTimestamp: Option[String],
                              documentDate: Option[String])
object PatchableDocFields {
  implicit val readsPatchableDocFields = Json.reads[PatchableDocFields]
}

case class PostableComment(text: String)
object PostableComment {
  implicit val readsPostableComment = Json.reads[PostableComment]
}

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
      Logger.debug(s"Searching ${q} from ${fromArchiveTimestamp} to ${toArchiveTimestamp}.")
      val resultFuture: Future[Seq[Document]] = documentRepository.searchWithTags(q, fromArchiveTimestamp, toArchiveTimestamp, fromModificationTimestamp, toModificationTime, offset.getOrElse(0), limit.getOrElse(10))
      for (result <- resultFuture)
        yield Ok(Json.toJson(result))
    }
  }

  def attention(offset: Option[Int],
                limit: Option[Int]) = AuthAction().async {
    request => {
      val resultFuture: Future[Seq[Document]] = documentRepository.attentionRequired(offset.getOrElse(0), limit.getOrElse(10))
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

  def addComment(id: Int) = AuthAction().async(parse.json) {
    implicit request => {
      val comment = request.body.as[PostableComment]
      for {
        commentOpt: Option[Comment] <- commentRepository.addToDocument(docId = id, text = comment.text, username = request.username)
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

  def patch(id: Int) = AuthAction().async(parse.json) {
    implicit request => {
      val patchedDoc = request.body.as[PatchableDocFields];
      val doc: Future[Option[Document]] = patchedDoc match {
        case PatchableDocFields(Some(name), None, None, None, None, None) => documentActions.rename(id, name, request.username)
        case PatchableDocFields(None, Some(description), None, None, None, None) => documentActions.setDescription(id, description, request.username)
        case PatchableDocFields(None, None, Some(archivingComplete), None, None, None) => documentActions.markArchivingComplete(id, archivingComplete, request.username)
        case PatchableDocFields(None, None, None, Some(actionRequired), None, None) => documentActions.markActionRequired(id, actionRequired, request.username)
        case PatchableDocFields(None, None, None, None, Some(followUp), None) => {
          val parsedFollowUp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(followUp)
          documentActions.setFollowup(id, Some(parsedFollowUp), request.username)
        }
        case PatchableDocFields(None, None, None, None, None, Some(documentDate)) => {
          // field is nullable, so interpret empty string as null
          val documentDateOpt: Option[String] = if (documentDate == "") None else Some(documentDate)
          documentActions.setDocumentDate(id, documentDateOpt, request.username)
        }
        case _ => Promise[Option[Document]]().failure(new IllegalArgumentException("Exactly one field must be set")).future
      }
      for {
        docOpt <- doc
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

  /** Private helper to create Location headers for 201 CREATED responses. */
  private def getURI[T](docId: Int, suffix: String)(implicit request: AuthorizedRequest[T]) = {
    "http://"+request.domain+config.getString("play.http.context") + "/document/" + docId + "/" + suffix
  }
}
