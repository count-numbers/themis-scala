package services.ingestion

import java.io.{FileOutputStream, OutputStream}
import java.nio.file.Path

import play.api.Logger
import util.{GDriveClient, GDriveFile}

import scala.concurrent.{ExecutionContext, Future}

class GDriveSource(sourceFolderID: String, archiveFolderID: String,
                   destinationDir: Path, tempDir: Path,
                   gdrive: GDriveClient, implicit val exec: ExecutionContext) {


  def scan() = {

    for (files: Seq[GDriveFile] <- gdrive.listFolder(Some(sourceFolderID))) {
      for (file: GDriveFile <- files) {
        Logger.info(s"Downloading gdrive file ${file.id} to ${tempDir}.")
        val out: OutputStream = new FileOutputStream(tempDir.resolve("gdrivefile").toFile)
        gdrive.drive.files().get(file.id).executeMediaAndDownloadTo(out)

        Logger.info(s"Moving gdrive file ${file.id} from ${sourceFolderID} to ${archiveFolderID}.")
        gdrive.drive.files().update(file.id, null)
          .setAddParents(archiveFolderID)
          .setRemoveParents(sourceFolderID)
          .setFields("id, parents")
          .execute()
      }
    }
  }
}
