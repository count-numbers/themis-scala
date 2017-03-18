package models

import db.Tables
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Created by simfischer on 3/15/17.
  */
case class Link(id: Int = -1, url: String, title: String, linkType: String)
object Link {

  def of(linkRow: Tables.LinkRow) = Link(id = linkRow.id, url = linkRow.url, title = linkRow.title, linkType = linkRow.linktype)


  // we need to rename linkType since type is a keyword in Scala
  implicit val writesLink: Writes[Link] = (
    (JsPath \ "id").write[Int] and
      (JsPath \ "url").write[String] and
      (JsPath \ "title").write[String] and
      (JsPath \ "type").write[String]
    )(unlift(Link.unapply))

  implicit val readsLink: Reads[Link] = (
    (JsPath \ "id").read[Int] or Reads.pure(-1) and
      (JsPath \ "url").read[String] and
      (JsPath \ "title").read[String] and
      (JsPath \ "type").read[String]
    )(Link.apply _)

}

