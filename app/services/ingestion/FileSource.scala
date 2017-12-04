package services.ingestion

import java.nio.file.{Files, Path}

import actions.DocumentActions
import play.api.Logger
import services.contentextraction.ContentExtractorService
import services.thumbnail.ThumbnailService

import scala.concurrent.{ExecutionContext, Future}

case class InsertionResult(documentIdFutures: Iterable[Future[Int]])(implicit val ec: ExecutionContext) {

  def andThen(action: Int => Unit): InsertionResult = {
    for {
      docIdFuture: Future[Int] <- documentIdFutures
      docId: Int <- docIdFuture
    } {
      action(docId)
    }
    this
  }
}

/**
  * Created by simfischer on 3/24/17.
  */
case class FileSource(sourceId: String, sourceDir: Path, destinationDir: Path, tempDir: Path, username: String)
                     (implicit val documentActions: DocumentActions,
                      val thumbnailService: ThumbnailService,
                      val contentExtractorService: ContentExtractorService,
                      val executionContext: ExecutionContext) {

  /** Scans for new files and imports them. */
  def scanForNewFiles(): InsertionResult = {
    import scala.collection.JavaConverters._

    val insertionResults: Iterable[Future[Int]] = for {
      incomingFile: Path <- Files.newDirectoryStream(sourceDir).asScala
    } yield {
      val name = incomingFile.getName(incomingFile.getNameCount-1).toString
      // first move to temp folder. this way, we're not going to process it a second time in case of an
      // overlapping schedule
      val file: Path = Files.move(incomingFile, tempDir.resolve(name))
      Logger.info(s"Moving incoming ${incomingFile} to ${file}.")

      val size = Files.size(file)
      val mimeType = Files.probeContentType(file)
      val description: Option[String] = for {
        contentExtractor <- contentExtractorService.forMimetype(mimeType)
      } yield {
        contentExtractor.extractContent(file)
      }
      Logger.debug(s"Extracted ${description.map(_.length)} bytes of content.")
      for {
        docId: Int <- documentActions.createNew(name = name,
                                                description = description,
                                                ownerUsername = username,
                                                sourceId = sourceId,
                                                sourceReference = file.toAbsolutePath.toString)
        attachmentId: Option[Int] <- documentActions.addAttachment(docId = docId, name = name, size = size, mimeType = mimeType, username = username)
      } yield {
        Logger.debug(s"Assigned new document ID ${docId}.")
        // if we are successful creating the document in the DB, move files around and extract thumbnail
        for (id <- attachmentId) {
          val dest = destinationDir.resolve(s"${id}.attachment")
          Files.move(file, dest)
          Logger.info(s"Ingested ${file} to ${dest}.")
          for (thumbnailExtractor <- thumbnailService.forMimetype(mimeType)) {
            thumbnailExtractor.extractFromFile(dest, destinationDir.resolve(s"${id}.thumb"))
          }
        }
        docId
      }
    }
    InsertionResult(insertionResults)
  }
}
