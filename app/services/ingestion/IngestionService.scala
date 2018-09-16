package services.ingestion

import java.nio.file.Paths

import javax.inject.{Named, Singleton}
import actions.DocumentActions
import akka.actor.{Actor, ActorRef, ActorSystem}
import com.google.inject.Inject
import db.{SourceRepository, Tables}
import play.api.{Configuration, Logger}
import services.contentextraction.ContentExtractorService
import services.thumbnail.ThumbnailService
import util.GDriveClientFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Try}

@Singleton
class IngestionService @Inject() (val config: Configuration,
                                  val system: ActorSystem,
                                  @Named("ingestion-actor") val ingestionActor: ActorRef)(implicit ec: ExecutionContext) {

  system.scheduler.schedule(0.microseconds, 10.seconds, ingestionActor, "run-ingestion")
}

/**
  */
@Singleton
class IngestionServiceActor @Inject() (val config: Configuration, val ingestionNotifier: IngestionNotifier,
                                       val gDriveClientFactory: GDriveClientFactory,
                                       val sourceRepository: SourceRepository,
                                       val documentActions: DocumentActions,
                                       val thumbnailService: ThumbnailService,
                                       val contentExtractorService: ContentExtractorService,
                                       implicit val executionContext: ExecutionContext) extends Actor {

  override def receive: Receive = {
    case msg: String => {
      Logger.debug(s"Running ingestion service: ${msg}")

      for {
        sources: Seq[(Tables.SourceRow, Option[Tables.UserRow])] <- sourceRepository.getAllActive()
        sourceAndUser: (Tables.SourceRow, Option[Tables.UserRow]) <- sources
      } {
        Logger.debug(s"Found active document source configuration: ${sourceAndUser}")

        val source: Option[DocumentSource[_]] = sourceAndUser match {
          case (Tables.SourceRow(_, "gdrive", _, _, Some(gdriveSourceFolder: String), Some(gdriveArchiveFolder: String), _),
            Some(Tables.UserRow(_, username: String, _, _, _)))
          => Some(new GDriveSource(username, gdriveSourceFolder, gdriveArchiveFolder,
              gDriveClientFactory.build(username), config, documentActions, thumbnailService, contentExtractorService, ingestionNotifier, executionContext))

          case (Tables.SourceRow(_, "file", _, _, _, _, Some(fileSourceFolder: String)),
            Some(Tables.UserRow(_, username: String, _, _, _)))
          =>
            Some(new FileSource(username, Paths.get(fileSourceFolder), config, documentActions, thumbnailService, contentExtractorService, ingestionNotifier, executionContext))

          case _ => { Logger.warn(s"Illegal or incomplete source definition${sourceAndUser}."); None }
        }


        source.foreach(source => {
          var srcId = sourceAndUser._1.id;
          Logger.debug(s"Executing document source ${srcId} of type ${source.sourceId} for user ${source.username}.")
          source.run match {
            case Failure(ex) => {
              Logger.warn(s"Document source ${srcId } failed: ${ex}", ex)
              sourceRepository.deactivate(sourceAndUser._1.id)
                .onSuccess({case _ => Logger.info(s"Deactivated source ${srcId }")})
            }
            case _ => {
              Logger.debug(s"Document source ${srcId } completed.")
            }
          }
        })
      }
    }
  }
}
