package services.ingestion

import java.util.Locale

import actions.IngestionActions
import akka.actor.{Actor, ActorRef, ActorSystem}
import com.google.inject.Inject
import javax.inject.{Named, Singleton}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Singleton
class IngestionService @Inject() (val config: Configuration,
                                  val system: ActorSystem,
                                  @Named("ingestion-actor") val ingestionActor: ActorRef)(implicit ec: ExecutionContext) {
  Logger.info(s"Default encoding is ${System.getProperty("file.encoding")}." )
  Logger.info(s"Default locale is ${Locale.getDefault.toString}.")

  system.scheduler.schedule(0.microseconds, 10.seconds, ingestionActor, "run-ingestion")
}

/**
  */
@Singleton
class IngestionServiceActor @Inject() (ingestionActions: IngestionActions) extends Actor {

  override def receive: Receive = {
    case msg: String => {
      Logger.debug(s"Ingestion actor called: ${msg}")
      ingestionActions.runAll();
    }
  }
}
