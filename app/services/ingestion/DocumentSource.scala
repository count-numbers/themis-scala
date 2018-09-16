package services.ingestion

import java.nio.file.{Files, Path, Paths}

import actions.DocumentActions
import play.api.{Configuration, Logger}
import services.contentextraction.ContentExtractorService
import services.thumbnail.ThumbnailService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/** Abstract superclass of active document sources.
  * The document source is invoked at intervals, importing documents in batches.
  * The batch to be imported is identified by subclasses via {@link #importToTemp}, returning a pair of (name,T) where
  * T is the subclasses way to identify the document later. Subsequently, {@link #importToTemp} is called for each
  * element of the batch, instructing subclasses to move the document to {@link #tempDir}.
  *
  * A series of operation is then performed on the temp document: thumbnail and content are extracted, the document
  * record is created in the database, and the file itself is moved as an attachment to the final storage folder.
  * Eventually, the IngestionNotifier is executed. */
abstract class DocumentSource[T](val sourceId: String,
                                 val username: String,
                                 config: Configuration,
                                 documentActions: DocumentActions,
                                 thumbnailService: ThumbnailService,
                                 contentExtractorService: ContentExtractorService,
                                 ingestionNotifier: IngestionNotifier,
                                 implicit val executionContext: ExecutionContext) {

  val destinationDir: Path = Paths.get(config.getString("themis.storage.dir").get)
  val tempDir: Path = Paths.get(config.getString("themis.temp.dir").get)


  def findDocuments: Try[Seq[(String, String, T)]]

  def importToTemp(t: T): Try[Path]

  def run(): Try[Unit] = {
    val foundTry: Try[Seq[(String, String, T)]] = findDocuments

    foundTry.map((found: Seq[(String, String, T)]) => {
      Logger.debug(s"Found ${found.length} new docs for ${sourceId}.")
      for ((name: String, sourceRef: String, input: T) <- found) {
        val fileTry: Try[Path] = importToTemp(input)

        fileTry.map((file:Path) => {
          Logger.info(s"Received incoming file ${file}.")

          val size = Files.size(file)
          val mimeType = Files.probeContentType(file)
          val description: Option[String] = for {
            contentExtractor <- contentExtractorService.forMimetype(mimeType)
          } yield {
            val txt = contentExtractor.extractContent(file)
            Logger.debug(s"Extracted ${txt.length} characters from ${file} (${mimeType}).")
            txt
          }
          Logger.debug(s"Extracted ${description.map(_.length)} bytes of content.")
          for {
            docId: Int <- documentActions.createNew(name = name,
              description = description,
              ownerUsername = username,
              sourceId = sourceId,
              sourceReference = sourceRef)
            attachmentId: Option[Int] <- documentActions.addAttachment(docId = docId, name = name, size = size, mimeType = mimeType, username = username)
          } yield {
            Logger.debug(s"Assigned new document ID ${docId}, attachment ${attachmentId}.")
            // if we are successful creating the document in the DB, move files around and extract thumbnail
            for (id <- attachmentId) {
              val dest = destinationDir.resolve(s"${id}.attachment")
              Files.move(file, dest)
              Logger.info(s"Ingested ${file} to ${dest}.")
              for (thumbnailExtractor <- thumbnailService.forMimetype(mimeType)) {
                thumbnailExtractor.extractFromFile(dest, destinationDir.resolve(s"${id}.thumb"))
              }
            }
            //ingestionNotifier.notify(docId)
          }
        })
      }
    })
  }
}
