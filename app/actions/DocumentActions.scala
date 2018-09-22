package actions

import javax.inject.Inject
import javax.inject.Singleton

import db._
import models._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by simfischer on 3/17/17.
  */
@Singleton
case class DocumentActions @Inject()(val documentRepository: DocumentRepository,
                                     val activityRepository: ActivityRepository,
                                     val userRepository: UserRepository,
                                     val contactRepository: ContactRepository,
                                     val attachmentRepository: AttachmentRepository,
                                     val linkRepository: LinkRepository,
                                     implicit val exec: ExecutionContext) {

  def rename(docId: Int, name: String, username: String): Future[Option[Document]] = {
    withDocAndUser(docId, username,
      _ => documentRepository.setName(docId, name).map((_, Unit)),
      ActivityType.Renamed, Seq(name))
      .map(_.map(_._1))
  }

  def setDescription(docId: Int, description: String, username: String): Future[Option[Document]] = {
    withDocAndUser(docId, username,
      _ => documentRepository.setDescription(docId, description).map((_, Unit)),
      ActivityType.SetDescription, Seq())
      .map(_.map(_._1))
  }

  def markActionRequired(docId: Int, actionRequired: Boolean, username: String): Future[Option[Document]] = {
    withDocAndUser(docId, username,
      _ => documentRepository.setActionRequired(docId, actionRequired).map((_, Unit)),
      ActivityType.MarkedActionRequired, Seq(actionRequired.toString))
      .map(_.map(_._1))
  }

  def markArchivingComplete(docId: Int, complete: Boolean, username: String) = {
    withDocAndUser(docId, username,
      _ => documentRepository.setArchivingComplete(docId, complete).map((_, Unit)),
      ActivityType.MarkedComplete, Seq(complete.toString))
      .map(_.map(_._1))
  }

  def setFollowup(docId: Int, timestamp: Option[java.util.Date], username: String) = {
    withDocAndUser(docId, username,
      _ => documentRepository.setFollowup(docId, timestamp.map(_.getTime)).map((_, Unit)),
      ActivityType.SetFollowup, timestamp.map(ts => Seq(ts.toString)).getOrElse(Seq()))
      .map(_.map(_._1))
  }

  def addTag(docId: Int, tag: String, username: String): Future[Option[Document]] = {
    withDocAndUser(docId, username,
      _ => documentRepository.addTag(docId, tag).map((t: Option[Tables.DtagRow]) => (t.isDefined, Unit)),
      ActivityType.Tagged, Seq(tag))
      .map(_.map(_._1))
  }

  def deleteTag(docId: Int, tag: String, username: String): Future[Option[Document]] = {
    withDocAndUser(docId, username,
      _ => documentRepository.deleteTag(docId, tag).map((_, Unit)),
      ActivityType.Untagged, Seq(tag))
      .map(_.map(_._1))
  }

  def setContact(docId: Int, contactId: Int, username: String): Future[Option[Document]] = {
    // first, see whether the contact even exists
    for {
      contactOpt: Option[Contact] <- contactRepository.getById(contactId)
      docOpt <- contactOpt match {
        case None => Future.successful(None)
        case Some(contact: Contact) =>
          withDocAndUser(docId, username,
            _ => documentRepository.setContact(docId, Some(contactId)).map((_, Unit)),
            ActivityType.SetContact, Seq(contact.name))
            .map(_.map(_._1))
      }
    } yield docOpt
  }

  def clearContact(docId: Int, username: String): Future[Option[Document]] = {
        withDocAndUser(docId, username,
          _ => documentRepository.setContact(docId, None).map((_, Unit)),
          ActivityType.SetContact, Seq())
          .map(_.map(_._1))
  }

  def addLink(docId: Int, link: Link, username: String): Future[Option[Link]] = {
    withDocAndUser(docId, username,
      _ => linkRepository.persist(link = link, docId = docId).map(_ => (true, link)),
      ActivityType.CreatedLink, Seq(link.linkType, link.url, link.title))
      .map(_.map(_._2))
  }

  def addAttachment(docId: Int, name: String, size: Long, mimeType: String, username: String): Future[Option[Int]] = {
    withDocAndUser(docId, username,
      _ => attachmentRepository.persist(docId, name, size, mimeType).map((attachmentId: Int) => (true, attachmentId)),
      ActivityType.Attached, Seq(name))
      .map(_.map(_._2))
  }

  def createNew(name: String,
                description: Option[String],
                sourceId: String,
                sourceReference: String,
                ownerUsername: String,
                contactId: Option[Int]): Future[Int] = {
    for {
      // first, find user
      user: Option[User]            <- userRepository.getByUsername(ownerUsername)
      // now, do the actual action
      ((docId: Int, userId: Int))   <- user match {
                                        case None => Future.failed(new Exception(s"User ${ownerUsername} not found."))
                                        case Some(user) => documentRepository.persist(name, description, sourceId, sourceReference, user.id, contactId).map(docId => (docId, user.id))
                                      }
      success: Boolean              <- activityRepository.persist(docId = docId, userId = userId, timestamp = System.currentTimeMillis(), activityType = ActivityType.Created.toString, arguments = Seq(name))
    } yield docId
  }

  /** Runs a certain action with a document and user. Action will likely update
    * the document, and the updated document will be returned.
    * @param action the action once we know the user exits. Must return a boolean indicating success and a value
    *     that will later be returned from this method together with the document. */
  private def withDocAndUser[T](docId: Int, username: String,
                             action: Unit => Future[(Boolean, T)],
                             activityType: ActivityType.ActivityType,
                             activityArguments: Seq[String]) : Future[Option[(Document, T)]] = {
    for {
      // first, find user
      user: Option[User]      <- userRepository.getByUsername(username)
      // now, do the actual action
      (success: Boolean, result: T)  <- user match {
        case None => Future.failed(new Exception("user not found")) // this cannot happen since the user is logged in
        case Some(user) => action.apply(Unit)
      }
      // register the activity if successful
      successAction: Boolean  <- if (success) {
        activityRepository.persist(docId = docId, userId = user.get.id, arguments = activityArguments, activityType = activityType.toString, timestamp = System.currentTimeMillis())
      } else {
        Future.successful(false)
      }
      doc: Option[Document]   <- if (successAction) documentRepository.getById(docId) else Future.successful(None)
    } yield {
      doc.map(d => (d, result))
    }
  }
}
