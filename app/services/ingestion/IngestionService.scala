package services.ingestion

import javax.inject.{Named, Singleton}

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.actor.Actor.Receive
import com.google.inject.Inject
import play.api.Configuration
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext

@Singleton
class IngestionService @Inject() (val config: Configuration,
                                  val system: ActorSystem,
                                  @Named("ingestion-actor") val ingestionActor: ActorRef)(implicit ec: ExecutionContext) {

  system.scheduler.schedule(0.microseconds, 5.seconds, ingestionActor, "woohoo")

}

/**
  * Created by simfischer on 3/21/17.
  */
@Singleton
class IngestionServiceActor @Inject() (val config: Configuration) extends Actor {
  override def receive: Receive = {
    case msg: String => println(s"Got: ${msg}")
  }
}
