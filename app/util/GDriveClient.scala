package util

import java.util
import java.util.Collections
import javax.inject.{Inject, Singleton}

import com.google.api.client.auth.oauth2.{Credential, StoredCredential, TokenResponse}
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleAuthorizationCodeRequestUrl, GoogleAuthorizationCodeTokenRequest, GoogleTokenResponse}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.{AbstractDataStore, AbstractDataStoreFactory, MemoryDataStoreFactory}
import com.google.api.services.drive.model.{ChildList, ChildReference, File}
import com.google.api.services.drive.{Drive, DriveScopes}
import db.ConfigRepository
import play.api.Logger
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/** Representation of an oauth credential to be saved in a DB.  */
case class DBCredential(accessToken: String, refreshToken: String, expirationTime: Long) {
  def toStoredCredential() = {
    val sc = new StoredCredential()
    sc.setAccessToken(accessToken)
    sc.setRefreshToken(refreshToken)
    sc.setExpirationTimeMilliseconds(expirationTime)
  }
  implicit val writesContact = Json.writes[DBCredential]

  def serialize(): String = {
    Json.toJson(this).toString()
  }
}
object DBCredential {
  implicit val readsContact = Json.reads[DBCredential]

  def of(sc: StoredCredential) = DBCredential(sc.getAccessToken, sc.getRefreshToken, sc.getExpirationTimeMilliseconds)
  def of(json: String) = Json.parse(json).as[DBCredential]
}

/** Stores oauth credentials in a ConfigRepository. */
case class ConfigRepoDataStore[V <: java.io.Serializable](configRepoDataStoreFactory: ConfigRepoDataStoreFactory, id: String, configRepo: ConfigRepository)
  extends AbstractDataStore[V](configRepoDataStoreFactory, id) {

  // as a matter of fact, these four methods are never called.
  override def keySet() = ??? //Collections.emptySet()
  override def clear() = ??? // { this }
  override def values() = ??? //Collections.emptyList()
  override def delete(key: String) = {
    Logger.warn(s"Deleting credentials for ${key}.")
    val f: Future[Int] = configRepo.deleteKey("datastore:"+id+":"+key)
    Await.ready(f, Duration.Inf).value.get
    this
  }

  override def get(key: String) = {
    val value: Try[Option[String]] = Await.ready(configRepo.getForKey("datastore:"+id+":"+key), Duration.Inf).value.get
    Logger.debug(s"Existing credentials: ${value}")
    value match {
      case Failure(ex) => { Logger.warn(s"Failed to load config key: ${ex}",ex); null.asInstanceOf[V] }
      case Success(None) => null.asInstanceOf[V]
      case Success(Some(serialized: String)) => DBCredential.of(serialized).toStoredCredential().asInstanceOf[V]
    }
  }

  override def set(key: String, value: V) = {
    val cred = value.asInstanceOf[StoredCredential]
    Logger.info(s"Persisting credential for ${key}")
    val f: Future[Int] = configRepo.persist("datastore:"+id+":"+key, DBCredential.of(cred).serialize())
    Await.ready(f, Duration.Inf).value.get
    this
  }
}
class ConfigRepoDataStoreFactory(configRepository: ConfigRepository) extends AbstractDataStoreFactory {
  override def createDataStore[V <: java.io.Serializable](id: String) = {
    Logger.info(s"Creating data store for ${id}")
    new ConfigRepoDataStore(this, id, configRepository)
  }
}


@Singleton
class GDriveClientFactory @Inject()(val configRepo: ConfigRepository) {

  def clientId: String = configRepo.getForKeySync("google.oauth.client_id").get
  def clientSecret: String = configRepo.getForKeySync("google.oauth.client_secret").get

    /** Builds a new auth flow */
  def build(user: String): GDriveClient = {
      Logger.debug(s"Creating GDrive client with ID ${clientId} and secret ${clientSecret}.")
      GDriveClient(user, clientId, clientSecret, configRepo)
  }
}

case class GDriveFile(mimeType: String, id: String, name: String, size: Option[Long])
object GDriveFile {
  implicit val writesGDriveFile = Json.writes[GDriveFile]
  def of(f: File) = GDriveFile(f.getMimeType, f.getId, f.getTitle, Option(f.getFileSize).map(_.longValue()))
}

case class GDriveClient(user: String, clientId: String, clientSecret: String, val configRepo: ConfigRepository) {

  lazy val authCodeFlow = new GoogleAuthorizationCodeFlow.Builder(
    new NetHttpTransport(),
    JacksonFactory.getDefaultInstance(),
    clientId,
    clientSecret,
    Collections.singletonList(DriveScopes.DRIVE_READONLY))
    .setDataStoreFactory(new ConfigRepoDataStoreFactory(configRepo))
    .setAccessType("offline")
    .setApprovalPrompt("force")
    .build()


  // TODO: Make this a promise
  def listFolder(folderId: Option[String]): Try[Seq[GDriveFile]] = {
    val credentialOpt = Option(authCodeFlow.loadCredential(user))

    import collection.JavaConverters._

    credentialOpt match {
      case None => Failure(new IllegalStateException("Not yet authorized with Google"))
      case Some(credential) =>
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport
        val drive = new Drive.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential).setApplicationName("themis-server").build()
        val rawChildren: ChildList = drive.children().list(folderId.getOrElse("root")).execute()
        val children: Seq[GDriveFile] = for {
          child <- rawChildren.getItems.asScala
        } yield {
          val f: File = drive.files().get(child.getId).execute()
          Logger.info(s"Child: ${f}")
          GDriveFile.of(f)
        }
        Success(children)
    }
  }
}
