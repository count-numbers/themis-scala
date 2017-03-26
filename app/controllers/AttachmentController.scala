package controllers

import java.nio.file.{Files, Paths}
import javax.inject.{Inject, Singleton}

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import auth.AuthAction
import db.{ActivityRepository, AttachmentRepository}
import models.{Activity, Attachment}
import play.api.Configuration
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.mvc.{Controller, ResponseHeader, Result}
import util.ErrorResponse

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by simfischer on 3/12/17.
  */
@Singleton
class AttachmentController @Inject()(attachmentRepository: AttachmentRepository,
                                     config: Configuration,
                                     implicit val exec: ExecutionContext) extends Controller {

  def storageDir: Option[String] = config.getString("themis.storage.dir")

  def download(id: Int) = AuthAction().async {
    request => {
      for {
        attachment: Option[Attachment] <- attachmentRepository.getById(id)
      } yield {
        (storageDir, attachment) match {
          case (Some(dir), Some(attachment: Attachment)) => {
            val path: java.nio.file.Path = Paths.get(dir, s"${attachment.id}.attachment")
            val source: Source[ByteString, _] = FileIO.fromPath(path)
            Result(
              header = ResponseHeader(200, Map.empty),
              body = HttpEntity.Streamed(source, Some(attachment.size), Some(attachment.mimeType))
            )
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
        (storageDir, attachment) match {
          case (Some(dir), Some(attachment: Attachment)) => {
            val path: java.nio.file.Path = Paths.get(dir, s"${attachment.id}.thumb")
            val source: Source[ByteString, _] = FileIO.fromPath(path)
            if (Files.exists(path)) {
              Result(
                header = ResponseHeader(200, Map.empty),
                body = HttpEntity.Streamed(source, Some(Files.size(path)), Some("image/png"))
              )
            } else {
              NotFound(ErrorResponse(404, "Not found", s"Thumbnail not generated for ${id}"))
            }
          }
          case _ => NotFound(ErrorResponse(404, "Not found", s"No such thumbnail ${id}"))
        }
      }
    }
  }
}