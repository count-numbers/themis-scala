package util

import java.util.Collections

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.{AbstractDataStore, AbstractDataStoreFactory}
import com.google.api.services.drive.model.{File, FileList}
import com.google.api.services.drive.{Drive, DriveScopes}
import db.ConfigRepository
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json

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
    Logger.info(s"Deleting credentials for ${key}.")
    val f: Future[Int] = configRepo.deleteKey("datastore:"+id+":"+key)
    Await.ready(f, Duration.Inf).value.get
    this
  }

  override def get(key: String) = {
    val value: Try[Option[String]] = Await.ready(configRepo.getForKey("datastore:"+id+":"+key), Duration.Inf).value.get
    //Logger.debug(s"Existing credentials: ${value}")
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
    Logger.debug(s"Creating data store for ${id}")
    new ConfigRepoDataStore(this, id, configRepository)
  }
}


@Singleton
class GDriveClientFactory @Inject()(val configRepo: ConfigRepository, implicit val executionContext: ExecutionContext) {

  def clientId: String = configRepo.getForKeySync("google.oauth.client_id").get
  def clientSecret: String = configRepo.getForKeySync("google.oauth.client_secret").get

    /** Builds a new auth flow */
  def build(user: String): GDriveClient = {
      Logger.debug(s"Creating GDrive client with ID ${clientId} and secret ${clientSecret}.")
      GDriveClient(user, clientId, clientSecret, configRepo, executionContext)
  }
}

case class GDriveFile(mimeType: String, id: String, name: String, embedLink: String, size: Option[Long])
object GDriveFile {
  implicit val writesGDriveFile = Json.writes[GDriveFile]
  def of(f: File) = GDriveFile(f.getMimeType, f.getId, f.getTitle, f.getEmbedLink, Option(f.getFileSize).map(_.longValue()))
}

case class GDriveClient(user: String, clientId: String, clientSecret: String, val configRepo: ConfigRepository, implicit val executionContext: ExecutionContext) {

  lazy val authCodeFlow = new GoogleAuthorizationCodeFlow.Builder(
    new NetHttpTransport(),
    JacksonFactory.getDefaultInstance(),
    clientId,
    clientSecret,
    Collections.singletonList(DriveScopes.DRIVE))
    .setDataStoreFactory(new ConfigRepoDataStoreFactory(configRepo))
    .setAccessType("offline")
    .setApprovalPrompt("force")
    .build()


  lazy val driveTry: Try[Drive] = {
    val credentialOpt = Option(authCodeFlow.loadCredential(user))
    credentialOpt match {
      case None => {
        Failure(new IllegalStateException("Not yet authorized with Google."))
      }
      case Some(credential) =>
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport
        Success(new Drive.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential).setApplicationName("themis-server").build())
    }
  }

  def listFolder(folderId: Option[String]): Try[Seq[GDriveFile]] = {
    import collection.JavaConverters._
    val id = folderId.getOrElse("root");
    val q = s"\'${id}\' in parents";
    Logger.debug(s"GDrive query: ${q}.")
    driveTry match {
      case Success(drive: Drive) => {
        val children: FileList = drive.files().list().setQ(q).execute()
        Success(for {
          child: File <- children.getItems().asScala
        } yield {
          Logger.debug(s"Found file: ${child}")
          GDriveFile.of(child)
        })
      }
      case Failure(ex) => Failure(ex)
    }
  }
}
