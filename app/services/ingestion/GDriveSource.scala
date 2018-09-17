package services.ingestion

import java.io.{FileOutputStream, OutputStream}
import java.nio.file.Path

import actions.DocumentActions
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag
import play.api.{Configuration, Logger}
import services.contentextraction.ContentExtractorService
import services.thumbnail.ThumbnailService
import util.{GDriveClient, GDriveFile}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

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
    Logger.info(s"Downloading gdrive file ${file.id} to ${tempDir}.")
    val dest: Path = tempDir.resolve(file.name)
    val out: OutputStream = new FileOutputStream(dest.toFile)
    gdrive.driveTry.map(drive => {
      drive.files().get(file.id).executeMediaAndDownloadTo(out)
      Logger.debug(s"Moving gdrive file ${file.id} from ${sourceFolderID} to ${archiveFolderID}.")
      drive.files().update(file.id, null)
        .setAddParents(archiveFolderID)
        .setRemoveParents(sourceFolderID)
        .setFields("id, parents")
        .execute()
      dest
    })
  }
}
