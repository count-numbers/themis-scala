package services.ingestion

import java.nio.file.{Files, Path}

import actions.DocumentActions
import play.api.{Configuration, Logger}
import services.contentextraction.ContentExtractorService
import services.thumbnail.ThumbnailService

import scala.concurrent.{ExecutionContext}

/** Imports files from a given fixed source folder. */
class FileSource(sourceId: String,
                  sourceDir: Path,
                  username: String,
                  config: Configuration,
                  documentActions: DocumentActions,
                  thumbnailService: ThumbnailService,
                  contentExtractorService: ContentExtractorService,
                  ingestionNotifier: IngestionNotifier,
                  executionContext: ExecutionContext)
  extends DocumentSource[Path](sourceId, username, config, documentActions, thumbnailService, contentExtractorService, ingestionNotifier, executionContext) {

  import scala.collection.JavaConverters._

  override def findDocuments: Seq[(String, Path)] = {
    for (incomingFile: Path <- Files.newDirectoryStream(sourceDir).asScala.toSeq) yield {
      val name = incomingFile.getName(incomingFile.getNameCount-1).toString
      (name, incomingFile)
    }
  }

  override def importToTemp(incomingFile: Path) = {
    val name = incomingFile.getName(incomingFile.getNameCount-1).toString
    val file: Path = Files.move(incomingFile, tempDir.resolve(name))
    Logger.info(s"Moving incoming ${incomingFile} to ${file}.")
    file
  }
}
