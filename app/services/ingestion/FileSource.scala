package services.ingestion

import java.nio.file.{Files, Path}

import actions.DocumentActions
import play.api.Logger

import scala.concurrent.ExecutionContext

/**
  * Created by simfischer on 3/24/17.
  */
case class FileSource(val sourceId: String, val sourceDir: Path, val destinationDir: Path, val username: String)
                     (implicit val documentActions: DocumentActions, val executionContext: ExecutionContext) {

  /** Scans for new files and imports them. */
  def scanForNewFiles() = {
    import scala.collection.JavaConverters._

    // TODO: The first thing we should actually do is move the file out of the source folder
    //       so it is not processed a second time by an overlapping event
    for {
      file: Path <- Files.newDirectoryStream(sourceDir).asScala
    } {
      val name = file.getName(file.getNameCount-1).toString
      val size = Files.size(file)
      for {
        docId: Int <- documentActions.createNew(name = name,
                                                ownerUsername = username,
                                                sourceId = sourceId,
                                                sourceReference = file.toAbsolutePath.toString)
        attachmentId: Option[Int] <- documentActions.addAttachment(docId = docId, name = name, size = size, mimeType = "application/pdf", username = username)
      } {
        for (id <- attachmentId) {
          val dest = destinationDir.resolve(s"${id}.pdf")
          Logger.info(s"Ingested ${file} to ${dest}.")
          Files.move(file, dest)
        }
      }
    }
  }
}
