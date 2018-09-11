package services.ingestion

import java.io.{FileOutputStream, OutputStream}
import java.nio.file.Path

import actions.DocumentActions
import play.api.{Configuration, Logger}
import services.contentextraction.ContentExtractorService
import services.thumbnail.ThumbnailService
import util.{GDriveClient, GDriveFile}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class GDriveSource(username: String,
                   sourceFolderID: String,
                   archiveFolderID: String,
                   gdrive: GDriveClient,

                   config: Configuration,
                   documentActions: DocumentActions,
                   thumbnailService: ThumbnailService,
                   contentExtractorService: ContentExtractorService,
                   ingestionNotifier: IngestionNotifier,
                   executionContext: ExecutionContext)
  extends DocumentSource[GDriveFile]("gdrive", username, config, documentActions, thumbnailService, contentExtractorService, ingestionNotifier, executionContext) {


  override def findDocuments: Seq[(String, String, GDriveFile)] = {
    val f: Future[Seq[GDriveFile]] = gdrive.listFolder(Some(sourceFolderID))
    Await.ready(f, Duration.Inf).value.get.get.map(file => (file.name, file.embedLink, file))
  }

  override def importToTemp(file: GDriveFile) = {
    Logger.info(s"Downloading gdrive file ${file.id} to ${tempDir}.")
    val dest: Path = tempDir.resolve(file.name)
    val out: OutputStream = new FileOutputStream(dest.toFile)
    gdrive.drive.files().get(file.id).executeMediaAndDownloadTo(out)

    Logger.debug(s"Moving gdrive file ${file.id} from ${sourceFolderID} to ${archiveFolderID}.")
    gdrive.drive.files().update(file.id, null)
      .setAddParents(archiveFolderID)
      .setRemoveParents(sourceFolderID)
      .setFields("id, parents")
      .execute()
    dest
  }
}
