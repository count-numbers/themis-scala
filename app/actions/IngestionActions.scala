package actions

import java.nio.file.Paths

import com.google.inject.Inject
import db.{ContactRepository, IngestionLogRepository, SourceRepository, Tables}
import javax.inject.Singleton
import play.api.{Configuration, Logger}
import services.contentextraction.ContentExtractorService
import services.ingestion.{DocumentSource, FileSource, GDriveSource, IngestionNotifier}
import services.thumbnail.ThumbnailService
import util.GDriveClientFactory

import scala.concurrent.ExecutionContext
import scala.util.Failure


@Singleton
class IngestionActions @Inject() (val config: Configuration, val ingestionNotifier: IngestionNotifier,
                                  val gDriveClientFactory: GDriveClientFactory,
                                  val sourceRepository: SourceRepository,
                                  val documentActions: DocumentActions,
                                  val thumbnailService: ThumbnailService,
                                  val contentExtractorService: ContentExtractorService,
                                  val contactRepository: ContactRepository,
                                  val ingestionLogRepository: IngestionLogRepository,
                                  implicit val executionContext: ExecutionContext) {

  def runAll(): Unit= {
    Logger.debug(s"Running ingestion service for all sources.")

    for {
      sources: Seq[(Tables.SourceRow, Option[Tables.UserRow])] <- sourceRepository.getAllActive()
      sourceAndUser: (Tables.SourceRow, Option[Tables.UserRow]) <- sources
    } {
      Logger.debug(s"Found active document source configuration: ${sourceAndUser}")

      runSource(sourceAndUser)
    }
  }

  def runSingle(id: Int): Unit = {
    for {
      sourceAndUserOpt: Option[(_root_.db.Tables.SourceRow, Option[_root_.db.Tables.UserRow])] <- sourceRepository.getById(id)
      sourceAndUser: (_root_.db.Tables.SourceRow, Option[_root_.db.Tables.UserRow]) <- sourceAndUserOpt
    }  {
      runSource(sourceAndUser._1, sourceAndUser._2)
    }
  }

  private def runSource(sourceAndUser: (_root_.db.Tables.SourceRow, Option[_root_.db.Tables.UserRow])) = {
    val source: Option[DocumentSource[_]] = sourceAndUser match {
      case (Tables.SourceRow(id, "gdrive", _, _, Some(gdriveSourceFolder: String), Some(gdriveArchiveFolder: String), _),
      Some(Tables.UserRow(_, username: String, _, _, _)))
      => Some(new GDriveSource(id, username, gdriveSourceFolder, gdriveArchiveFolder,
        gDriveClientFactory.build(username), config, documentActions, thumbnailService, contentExtractorService, contactRepository, ingestionNotifier, ingestionLogRepository, executionContext))

      case (Tables.SourceRow(_, "file", _, _, _, _, Some(fileSourceFolder: String)),
      Some(Tables.UserRow(id, username: String, _, _, _)))
      =>
        Some(new FileSource(id, username, Paths.get(fileSourceFolder), config, documentActions, thumbnailService, contentExtractorService, contactRepository, ingestionNotifier, ingestionLogRepository, executionContext))

      case _ => {
        Logger.warn(s"Illegal or incomplete source definition ${sourceAndUser}.");
        ingestionLogRepository.warn(s"Illegal or incomplete source definition: ${sourceAndUser}", Some(sourceAndUser._1.id), sourceAndUser._2.map(_.username), None)
        None
      }
    }

    source.foreach(source => {
      var srcId = sourceAndUser._1.id;
      Logger.debug(s"Executing document source ${srcId} of type ${source.sourceId} for user ${source.username}.")
      source.run match {
        case Failure(ex) => {
          Logger.warn(s"Document source ${srcId} failed: ${ex}", ex)
          ingestionLogRepository.warn(s"Document source ${srcId} failed: ${ex}", Some(sourceAndUser._1.id), sourceAndUser._2.map(_.username), None)
          sourceRepository.deactivate(sourceAndUser._1.id)
            .onSuccess({ case _ => Logger.info(s"Deactivated source ${srcId}") })
        }
        case _ => {
          Logger.debug(s"Document source ${srcId} completed.")
          ingestionLogRepository.info(s"Document source ${srcId} completed.", Some(sourceAndUser._1.id), sourceAndUser._2.map(_.username), None)
        }
      }
    })

  }
}
