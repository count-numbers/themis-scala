package controllers

import db.ActivityRepository
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext
import javax.inject.{Inject, Singleton}

import auth.AuthAction
import models.Activity
import play.api.libs.json.Json

/**
  * Created by simfischer on 3/12/17.
  */
@Singleton
class ActivitiesController @Inject()(activityRepository: ActivityRepository, implicit val exec: ExecutionContext) extends Controller {

  def latest() = AuthAction().async {
    request => {
      for {
        activities: Seq[Activity] <- activityRepository.latest(0, 10)
      } yield {
        Ok(Json.toJson(activities))
      }
    }
  }
}