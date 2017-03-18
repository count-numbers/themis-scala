package actions

import javax.inject.Inject
import javax.inject.Singleton

import db._
import models.{ActivityType, Document, Link, User}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by simfischer on 3/17/17.
  */
@Singleton
case class DocumentActions @Inject()(val documentRepository: DocumentRepository,
                                     val activityRepository: ActivityRepository,
                                     val userRepository: UserRepository,
                                     val linkRepository: LinkRepository,
                                     implicit val exec: ExecutionContext) {

  def rename(docId: Int, name: String, username: String): Future[Option[Document]] = {
    withDocAndUser(docId, username,
      _ => documentRepository.setName(docId, name).map((_, Unit)),
      ActivityType.MarkedActionRequired, Seq(name))
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

  def addLink(docId: Int, link: Link, username: String): Future[Option[Link]] = {
    withDocAndUser(docId, username,
      _ => linkRepository.persist(link = link, docId = docId).map(_ => (true, link)),
      ActivityType.CreatedLink, Seq(link.linkType, link.url, link.title))
      .map(_.map(_._2))
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
        case None => Future.successful(false)
        case Some(user) => action()
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
