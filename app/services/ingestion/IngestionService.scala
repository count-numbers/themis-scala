package services.ingestion

import java.nio.file.Paths
import javax.inject.{Named, Singleton}

import actions.DocumentActions
import akka.actor.{Actor, ActorRef, ActorSystem}
import com.google.inject.Inject
import db.ConfigRepository
import play.api.{Configuration, Logger}
import services.contentextraction.ContentExtractorService
import services.thumbnail.ThumbnailService
import util.GDriveClientFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

@Singleton
class IngestionService @Inject() (val config: Configuration,
                                  val system: ActorSystem,
                                  @Named("ingestion-actor") val ingestionActor: ActorRef)(implicit ec: ExecutionContext) {

  system.scheduler.schedule(0.microseconds, 5.seconds, ingestionActor, "run-ingestion")
}

/**
  */
@Singleton
class IngestionServiceActor @Inject() (val config: Configuration, val ingestionNotifier: IngestionNotifier,
                                       val gDriveClientFactory: GDriveClientFactory)
                                      (val configRepository: ConfigRepository,
                                       val documentActions: DocumentActions,
                                       val thumbnailService: ThumbnailService,
                                       val contentExtractorService: ContentExtractorService,
                                       val executionContext: ExecutionContext) extends Actor {

  val fileSource = new FileSource(
    username = config.getString("themis.source.user").get,
    sourceDir = Paths.get(config.getString("themis.source.dir").get),
    config = config,
    documentActions = documentActions,
    thumbnailService = thumbnailService,
    contentExtractorService = contentExtractorService,
    ingestionNotifier = ingestionNotifier,
    executionContext = executionContext
  )

  val gdriveSource: Option[GDriveSource] =
    for {
      sourceFolderId: String <- configRepository.getForKeySync("source.gdrive.source_folder_id")
      archiveFolderId: String <- configRepository.getForKeySync("source.gdrive.archive_folder_id")
    } yield {
      new GDriveSource(
        username = "admin",

        sourceFolderID = sourceFolderId,
        archiveFolderID = archiveFolderId,
        gdrive = gDriveClientFactory.build("admin"),

        config = config,
        documentActions = documentActions,
        thumbnailService = thumbnailService,
        contentExtractorService = contentExtractorService,
        ingestionNotifier = ingestionNotifier,
        executionContext = executionContext
      )
    }

  Logger.info(s"GDrive source configured: ${gdriveSource.isDefined}")

  override def receive: Receive = {
      case msg: String => {
        Logger.info(s"Running ingestion service: ${msg}")

        fileSource.run

        gdriveSource.foreach(_.run)
      }
  }
}
