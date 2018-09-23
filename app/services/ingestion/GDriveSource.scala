package services.ingestion

import java.io.{FileOutputStream, OutputStream}
import java.nio.file.Path

import actions.DocumentActions
import db.{ContactRepository, IngestionLogRepository}
import play.api.{Configuration, Logger}
import services.contentextraction.ContentExtractorService
import services.thumbnail.ThumbnailService
import util.{GDriveClient, GDriveFile}

import scala.concurrent.ExecutionContext
import scala.util.Try

class GDriveSource(id: Int,
                   username: String,
                   sourceFolderID: String,
                   archiveFolderID: String,
                   gdrive: GDriveClient,

                   config: Configuration,
                   documentActions: DocumentActions,
                   thumbnailService: ThumbnailService,
                   contentExtractorService: ContentExtractorService,
                   contactRepository: ContactRepository,
                   ingestionNotifier: IngestionNotifier,
                   ingestionLogRepository: IngestionLogRepository,
                   executionContext: ExecutionContext)
  extends DocumentSource[GDriveFile](id,"gdrive", username, config, documentActions, thumbnailService, contentExtractorService, contactRepository, ingestionNotifier, ingestionLogRepository, executionContext) {

  override def findDocuments: Try[Seq[(String, String, GDriveFile)]] = {
    val filesTry: Try[Seq[GDriveFile]] =  gdrive.listFolder(Some(sourceFolderID))
    filesTry.map(files => {
      val docs = for {
        file: GDriveFile <- files
      } yield {
        (file.name, file.embedLink, file)
      }
      docs
    })
  }

  override def importToTemp(file: GDriveFile): Try[Path] = {
    Logger.info(s"Downloading gdrive file ${file.name} to ${tempDir}.")
    ingestionLogRepository.info(s"Downloading gdrive file ${file.name} to ${tempDir}.", Some(id), Some(username), None)
    val dest: Path = tempDir.resolve(file.name)
    val out: OutputStream = new FileOutputStream(dest.toFile)
    gdrive.driveTry.map(drive => {
      drive.files().get(file.id).executeMediaAndDownloadTo(out)
      Logger.info(s"Moving downloaded gdrive file from ${dest.toFile} to ${archiveFolderID}.")
      ingestionLogRepository.info(s"Moving downloaded gdrive file from ${dest.toFile} to ${archiveFolderID}.", Some(id), Some(username), None)
      drive.files().update(file.id, null)
        .setAddParents(archiveFolderID)
        .setRemoveParents(sourceFolderID)
        .setFields("id, parents")
        .execute()
      dest
    })
  }
}
