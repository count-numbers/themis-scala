package services.ingestion

import java.nio.file.{DirectoryStream, Files, Path, Paths}
import javax.inject.{Named, Singleton}

import actions.DocumentActions
import akka.actor.{Actor, ActorRef, ActorSystem}
import com.google.inject.Inject
import play.api.Configuration
import services.contentextraction.ContentExtractorService
import services.thumbnail.ThumbnailService

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IngestionService @Inject() (val config: Configuration,
                                  val system: ActorSystem,
                                  @Named("ingestion-actor") val ingestionActor: ActorRef)(implicit ec: ExecutionContext) {

  system.scheduler.schedule(0.microseconds, 5.seconds, ingestionActor, "run-ingestion")
}

/**
  * Created by simfischer on 3/21/17.
  */
@Singleton
class IngestionServiceActor @Inject() (val config: Configuration, val ingestionNotifier: IngestionNotifier)
                                      (implicit val documentActions: DocumentActions,
                                       val thumbnailService: ThumbnailService,
                                       val contentExtractorService: ContentExtractorService,
                                       val ec: ExecutionContext) extends Actor {

  def fileSource: Option[FileSource] = for {
    sourceDir <- config.getString("themis.source.dir")
    destinationDir <- config.getString("themis.storage.dir")
    tempDir <- config.getString("themis.temp.dir")
    username <- config.getString("themis.source.user")
  } yield {
    FileSource(sourceId = "file",
      sourceDir = Paths.get(sourceDir),
      destinationDir = Paths.get(destinationDir),
      tempDir = Paths.get(tempDir),
      username = username)
  }

  override def receive: Receive = {
      case msg: String => {
        println(s"Got event: ${msg}")

        fileSource.foreach(_.scanForNewFiles().andThen(ingestionNotifier.notify(_)))
/*
        for {
          source: FileSource <- fileSource
          docFuture: Future[Int] <- source.scanForNewFiles()
          docId: Int <- docFuture
        } {
          //ingestionNotifier.notify(docId)
        }
        */
      }
  }
}
