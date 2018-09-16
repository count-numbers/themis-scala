package services.ingestion

import java.nio.file.{Files, Path}

import actions.DocumentActions
import play.api.{Configuration, Logger}
import services.contentextraction.ContentExtractorService
import services.thumbnail.ThumbnailService

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

/** Imports files from a given fixed source folder. */
class FileSource( username: String,
                  sourceDir: Path,
                  config: Configuration,
                  documentActions: DocumentActions,
                  thumbnailService: ThumbnailService,
                  contentExtractorService: ContentExtractorService,
                  ingestionNotifier: IngestionNotifier,
                  executionContext: ExecutionContext)
  extends DocumentSource[Path]("file", username, config, documentActions, thumbnailService, contentExtractorService, ingestionNotifier, executionContext) {

  import scala.collection.JavaConverters._

  override def findDocuments: Try[Seq[(String, String, Path)]] = {
    val results = for (incomingFile: Path <- Files.newDirectoryStream(sourceDir).asScala.toSeq) yield {
      val name = incomingFile.getName(incomingFile.getNameCount-1).toString
      (name, incomingFile.toString, incomingFile)
    }
    Success(results)
  }

  override def importToTemp(incomingFile: Path): Try[Path] = {
    val name = incomingFile.getName(incomingFile.getNameCount-1).toString
    val file: Path = Files.move(incomingFile, tempDir.resolve(name))
    Logger.info(s"Moving incoming ${incomingFile} to ${file}.")
    Success(file)
  }
}
