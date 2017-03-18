package controllers

import play.api.mvc.{Action, Controller}

/**
  * Created by simfischer on 3/12/17.
  */
class ActivitiesController extends Controller {

  def activities() = Action {
    Ok("[]")
  }
}
