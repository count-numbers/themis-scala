package models

import db.Tables
import play.api.libs.json.Json

/**
  * Created by simfischer on 3/18/17.
  */
case class Contact(id: Option[Int],
                   identifier: String,
                   name: String,
                   address1: Option[String],
                   address2: Option[String],
                   zip: Option[String],
                   city: Option[String],
                   region: Option[String],
                   country: Option[String],
                   email: Option[String])

object Contact {

  def of(row: Tables.ContactRow) = Contact(
      id = Some(row.id),
      identifier = row.identifier,
      name = row.name,
      address1 = row.address1,
      address2 = row.address2,
      zip = row.zip,
      city =  row.city,
      region = row.region,
      country = row.country,
      email = row.email
  )


  implicit val formatContact = Json.format[Contact]
  implicit val readsContact  = Json.reads[Contact]
  implicit val writesContact = Json.writes[Contact]

}
