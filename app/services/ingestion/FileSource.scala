package services.ingestion

import java.io.IOException
import java.nio.file.{Files, Path}

import actions.DocumentActions
import db.{ContactRepository, IngestionLogRepository}
import play.api.{Configuration, Logger}
import services.contentextraction.ContentExtractorService
import services.thumbnail.ThumbnailService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/** Imports files from a given fixed source folder. */
class FileSource( id: Int,
                  username: String,
                  sourceDir: Path,
                  config: Configuration,
                  documentActions: DocumentActions,
                  thumbnailService: ThumbnailService,
                  contentExtractorService: ContentExtractorService,
                  contactRepository: ContactRepository,
                  ingestionNotifier: IngestionNotifier,
                  ingestionLogRepository: IngestionLogRepository,
                  executionContext: ExecutionContext)
  extends DocumentSource[Path](id,"file", username, config, documentActions, thumbnailService, contentExtractorService, contactRepository, ingestionNotifier, ingestionLogRepository, executionContext) {

  import scala.collection.JavaConverters._

  override def findDocuments: Try[Seq[(String, String, Path)]] = {
    try {
      val results = for (incomingFile: Path <- Files.newDirectoryStream(sourceDir).asScala.toSeq) yield {
        val name = incomingFile.getName(incomingFile.getNameCount - 1).toString
        (name, incomingFile.toString, incomingFile)
      }
      Success(results)
    } catch {
      case ex:IOException => Failure(ex)
    }
  }

  override def importToTemp(incomingFile: Path): Try[Path] = {
    try {
      val name = incomingFile.getName(incomingFile.getNameCount-1).toString
      val target = tempDir.resolve(name)
      Logger.info(s"Moving incoming ${incomingFile} to ${target}.")
      val file: Path = Files.move(incomingFile, target)
      Success(file)
    } catch {
       case ex:IOException => {
         Logger.info(s"Failed to move file: ${ex}")
         Failure(ex)
       }
    }
  }
}
