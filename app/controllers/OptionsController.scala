package controllers

import play.api.mvc._

/**
  * This controller sends cors headers in response to all OPTIONS requests.
  *
  * Created by simfischer on 3/11/17.
  */
class OptionsController extends Controller {

  def headers = List(
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, DELETE, PUT",
    "Access-Control-Max-Age" -> "3600",
    "Access-Control-Allow-Headers" -> "Origin, Content-Type, Accept, Authorization",
    "Access-Control-Allow-Credentials" -> "true"
  )

  def rootOptions = options("/")
  def options(url: String) = Action { request =>
    NoContent.withHeaders(headers : _*)
  }
}
