package services.ingestion

import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.services.drive.model.{ChildList, ChildReference, File}

class GDriveSource {


  def scan() = {
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport
    val credential: _root_.com.google.api.client.http.HttpRequestInitializer = null
    val drive = new Drive.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential).setApplicationName("themis-server").build()
    //val x: File = drive.files().get("kjhkjhjkh").execute()
    val x: ChildList = drive.children().list("folderid").execute()

  }
}
