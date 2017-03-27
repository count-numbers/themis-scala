package controllers

import java.nio.file.{Files, Path}
import javax.inject.{Inject, Singleton}

import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import auth.AuthAction
import db.{AttachmentRepository}
import models.{Attachment}
import play.api.Configuration
import play.api.http.HttpEntity
import play.api.mvc.{Controller, ResponseHeader, Result}
import util.{ErrorResponse, FileSystemUtils}

import scala.concurrent.{ExecutionContext}

/**
  * Created by simfischer on 3/12/17.
  */
@Singleton
class AttachmentController @Inject()(attachmentRepository: AttachmentRepository,
                                     implicit val config: Configuration,
                                     implicit val exec: ExecutionContext) extends Controller {

  def download(id: Int) = AuthAction().async {
    request => {
      for {
        attachment: Option[Attachment] <- attachmentRepository.getById(id)
      } yield {
        attachment match {
          case Some(attachment: Attachment) => {
            val pathOpt = FileSystemUtils.attachmentPath(attachment.id)
            pathOpt.map(path => {
              val source: Source[ByteString, _] = FileIO.fromPath(path)
              Result(
                header = ResponseHeader(200, Map.empty),
                body = HttpEntity.Streamed(source, Some(attachment.size), Some(attachment.mimeType))
              )
            }).getOrElse(NotFound(ErrorResponse(404, "Not found", s"Attachment file not found for ${id}")))
          }
          case _ => NotFound(ErrorResponse(404, "Not found", s"No such attachment ${id}"))
        }
      }
    }
  }

  def thumbnail(id: Int) = AuthAction().async {
    // TODO: Fallback for not-generated thumbnail (due to unknown file format)
    request => {
      for {
        attachment: Option[Attachment] <- attachmentRepository.getById(id)
      } yield {
        attachment match {
          case Some(attachment: Attachment) => {
            val pathOpt: Option[Path] = FileSystemUtils.thumbnailPath(attachment.id)
            pathOpt.map((path: Path) => {
              val source: Source[ByteString, _] = FileIO.fromPath(path)
              if (Files.exists(path)) {
                Result(
                  header = ResponseHeader(200, Map.empty),
                  body = HttpEntity.Streamed(source, Some(Files.size(path)), Some("image/png"))
                )
              } else {
                NotFound(ErrorResponse(404, "Not found", s"Thumbnail not generated for ${id}"))
              }
            }).getOrElse(NotFound(ErrorResponse(404, "Not found", s"Thumbnail file not found for ${id}")))
          }
          case _ => NotFound(ErrorResponse(404, "Not found", s"No such thumbnail ${id}"))
        }
      }
    }
  }
}