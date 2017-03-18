package util

import play.api.libs.json.Json

/**
  * Created by simfischer on 3/9/17.
  */
object ErrorResponse {

  def apply(status: Int, message: String, description:String) = Json.obj("status" -> status, "message" -> message, "description" -> description)
}
