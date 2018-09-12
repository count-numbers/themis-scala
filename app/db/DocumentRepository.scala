package db

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import db.Tables._
import db.Tables.profile.api._
import models.{Activity, Attachment, Comment, Document, Link}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json
import slick.backend.DatabaseConfig
import slick.dbio.DBIOAction
import slick.dbio.Effect.{Read, Write}
import slick.driver.JdbcProfile
import slick.lifted.SimpleExpression

import scala.concurrent.{ExecutionContext, Future}

/** Holds earliest ald latest possible timestamp in the DB. To be used to define min and max of slider. */
case class DocumentBaseMetaData(earliestTimestamp: Option[Long], latestTimestamp: Option[Long])
object DocumentBaseMetaData {
  implicit val writesDocumentBaseMetadata = Json.writes[DocumentBaseMetaData]
}

/**
  * Created by simfischer on 3/9/17.
  */
@Singleton
class DocumentRepository @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                   val commentRepository: CommentRepository,
                                   val linkRepository: LinkRepository,
                                   val attachmentRepository: AttachmentRepository,
                                   val activityRepository: ActivityRepository,
                                   implicit val exec: ExecutionContext) {

  val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  /** Returns a single document by id.
    * The returned document will contain the following linked entities: user (owner), tags and comments. */
  def getById(id: Int): Future[Option[Document]] = {

    // 1. query docs, join with one user and many tags (via join table)
    val docQuery: Query[(Tables.Document, Rep[Option[Tables.User]], Rep[Option[Tables.Contact]], Rep[Option[Tables.Dtag]]), (Tables.DocumentRow, Option[Tables.UserRow], Option[Tables.ContactRow], Option[Tables.DtagRow]), Seq] = for {
      ((((doc, user), contact), tagging), tag) <- (db.Tables.Document
                      joinLeft db.Tables.User on (_.owner === _.id)
                      joinLeft db.Tables.Contact on (_._1.contact === _.id)
                      joinLeft db.Tables.Tagging on (_._1._1.id === _.docid)
                      joinLeft db.Tables.Dtag on (_._2.map(_.tagid) === _.id)) if (doc.id === id)
    } yield (doc, user, contact, tag)

    val docResultFuture: Future[Seq[(DocumentRow, Option[UserRow], Option[ContactRow], Option[DtagRow])]] = dbConfig.db.run(docQuery.result)

    // 2. separately (to avoid an inefficient join on both tags and other entities), load comments and links
    val commentsFuture: Future[Seq[Comment]] = commentRepository.getForDocument(id)
    val linksFuture       = linkRepository.getForDocument(id)
    val activitiesFuture  = activityRepository.getForDocument(id)
    val attachmentsFuture = attachmentRepository.getForDocument(id)

    for {
      docRows: Seq[(DocumentRow, Option[UserRow], Option[ContactRow], Option[DtagRow])] <- docResultFuture
      comments: Seq[Comment]       <- commentsFuture
      links: Seq[Link]             <- linksFuture
      activities: Seq[Activity]    <- activitiesFuture
      attachments: Seq[Attachment] <- attachmentsFuture
    } yield {
      // group by document and user (which is the same as grouping by document id - we do this only
      // so (doc,user) becomes the key and Seq[..., Tag] becomes the value
      // and then pick the first, if exists
      val singleHit: Option[((DocumentRow, Option[UserRow], Option[ContactRow]), Seq[(DocumentRow, Option[UserRow], Option[ContactRow], Option[DtagRow])])] =
        docRows
          .groupBy(x => (x._1, x._2, x._3))
          .headOption

      singleHit.map({
        // split into Doc, Option[User] and Seq[..., ..., Tag]
        case ((doc: DocumentRow, userOpt: Option[UserRow], contactOpt: Option[ContactRow]), tagSeq: Seq[(DocumentRow, Option[UserRow], Option[ContactRow], Option[DtagRow])])
        =>
        {
          val tags: Seq[DtagRow] = for {
            rowTriple    <- tagSeq
            tag: DtagRow <- rowTriple._4 // take only 3rd element from triple (= tag)
          } yield { tag }
          Document.of(doc = doc, owner = userOpt, contact = contactOpt, tags = Some(tags), attachments = Some(attachments), comments = Some(comments), links = Some(links), activityHistory = Some(activities))
        }
      })
    }
  }

  /** Currently unused method. Identical to {@link #search()}, but also contains tags.
    * These are not displayed by the UI though. */
  def searchWithTags(searchTerm: String,
                fromArchiveTimestamp: Option[Long],
                toArchiveTimestamp: Option[Long],
                fromModificationTimestamp: Option[Long],
                toModificationTimestamp: Option[Long],
                offset: Int,
                limit: Int): Future[Seq[Document]] = {

    val searchExpression = "%" + searchTerm + "%"

    val fulltextMatch = SimpleExpression.binary[Option[String],String,Boolean] { (col,search,qb) =>
      //qb.sqlBuilder += "match("
      qb.expr(col)
      qb.sqlBuilder += " @@ to_tsquery('pg_catalog.english', "
      qb.expr(search)
      qb.sqlBuilder += ")"
    }

    val query: Query[(Tables.Document, Rep[Option[Tables.User]], Rep[Option[Dtag]]), (Tables.DocumentRow, Option[Tables.UserRow], Option[Tables.DtagRow]), Seq] = for {
      (((doc, user), tagging), tag) <- (db.Tables.Document
        joinLeft db.Tables.User on (_.owner === _.id)
        joinLeft db.Tables.Tagging on (_._1.id === _.docid)
        joinLeft db.Tables.Dtag on (_._2.map(_.tagid) === _.id)) if fulltextMatch(doc.fulltext, searchTerm)
    } yield (doc, user, tag)

    val resultFuture: Future[Seq[(DocumentRow, Option[UserRow], Option[DtagRow])]] = dbConfig.db.run(query.drop(offset).take(limit).result)

    for {
      allRows: Seq[(DocumentRow, Option[Tables.UserRow], Option[DtagRow])] <- resultFuture
    } yield {
      val groupedByDocRows: Map[(DocumentRow, Option[UserRow]), Seq[(DocumentRow, Option[UserRow], Option[DtagRow])]] =
        allRows
          .groupBy(x => (x._1, x._2))

      val documentIterator = for {
        ((doc: DocumentRow, userOpt : Option[UserRow]), tagSeq: Seq[(_, _, Option[DtagRow])]) <- groupedByDocRows
      } yield {
        val tags: Seq[DtagRow] = for {
          rowTriple    <- tagSeq
          tag: DtagRow <- rowTriple._3 // take only 3rd element from triple (= tag)
        } yield { tag }
        Document.of(doc = doc, owner = userOpt, tags = Some(tags))
      }
      documentIterator.toSeq
    }
  }

  /** Searches for documents matching a given search term within a  given time frame.
    * The returned document collection is linked to the user, but does not contain tags
    * or other linked entities (which would require more database queries). */
  def search(searchTerm: String,
             fromArchiveTimestamp: Option[Long],
             toArchiveTimestamp: Option[Long],
             fromModificationTimestamp: Option[Long],
             toModificationTimestamp: Option[Long],
             offset: Int,
             limit: Int): Future[Seq[Document]] = {
    val searchExpression = "%" + searchTerm + "%"
    val query = for {
      (doc, user) <- db.Tables.Document joinLeft db.Tables.User on (_.owner === _.id)
                     if (doc.name like searchExpression) || (doc.description like searchExpression)
    } yield (doc, user)

    val resultFuture: Future[Seq[(DocumentRow, Option[UserRow])]] = dbConfig.db.run(query.drop(offset).take(limit).result)

    for (result: Seq[(DocumentRow, Option[UserRow])] <- resultFuture)
      yield {
        for ((doc, user) <- result)
          yield Document.of(doc, user)
      }
  }

  def attentionRequired(offset: Int, limit: Int): Future[Seq[Document]] = {
    val query: Query[(Tables.Document, Rep[Option[Tables.User]], Rep[Option[Dtag]]), (Tables.DocumentRow, Option[Tables.UserRow], Option[Tables.DtagRow]), Seq] = for {
      (((doc, user), tagging), tag) <- (db.Tables.Document
        joinLeft db.Tables.User on (_.owner === _.id)
        joinLeft db.Tables.Tagging on (_._1.id === _.docid)
        joinLeft db.Tables.Dtag on (_._2.map(_.tagid) === _.id)).sortBy(_._1._1._1.archivetimestamp.asc) if (!doc.archivingcomplete || doc.actionrequired)
    } yield (doc, user, tag)

    val resultFuture: Future[Seq[(DocumentRow, Option[UserRow], Option[DtagRow])]] = dbConfig.db.run(query.drop(offset).result) //take(limit).result)

    for {
      allRows: Seq[(DocumentRow, Option[Tables.UserRow], Option[DtagRow])] <- resultFuture
    } yield {
      val groupedByDocRows: Map[(DocumentRow, Option[UserRow]), Seq[(DocumentRow, Option[UserRow], Option[DtagRow])]] =
        allRows
          .groupBy(x => (x._1, x._2))

      val documentIterator = for {
        ((doc: DocumentRow, userOpt : Option[UserRow]), tagSeq: Seq[(_, _, Option[DtagRow])]) <- groupedByDocRows
      } yield {
        val tags: Seq[DtagRow] = for {
          rowTriple    <- tagSeq
          tag: DtagRow <- rowTriple._3 // take only 3rd element from triple (= tag)
        } yield { tag }
        Document.of(doc = doc, owner = userOpt, tags = Some(tags))
      }
      documentIterator.toSeq
    }
  }

  def getDocumentsForContact(contactId: Int, offset: Int, limit: Int) = {
    val query = for {
      (doc, user) <- db.Tables.Document joinLeft db.Tables.User on (_.owner === _.id) if (doc.contact === contactId)
    } yield (doc, user)

    val resultFuture: Future[Seq[(DocumentRow, Option[UserRow])]] = dbConfig.db.run(query.drop(offset).take(limit).result)

    for {
      result: Seq[(DocumentRow, Option[UserRow])] <- resultFuture
    } yield {
      for {
        (doc, user) <- result
      } yield Document.of(doc, user)
    }

    dbConfig.db
      .run(query.drop(offset).take(limit).result)
      .map((rows: Seq[(DocumentRow, Option[UserRow])]) =>
        rows.map{ case (doc: DocumentRow, userOpt: Option[UserRow]) => Document.of(doc, userOpt)})
  }

  def persist(name: String,
              description: Option[String],
              sourceId: String,
              sourceReference: String,
              ownerId: Int): Future[Int] = {
    val now = new java.sql.Timestamp(System.currentTimeMillis());
    // we're projecting to only the columns we want to insert because we must avoid inserting fulltext - it would
    // cause an SQL type error since Strings do not match to Postgres tsvectors. fulltext is populated
    // via a DB trigger
    val action = (Tables.Document.map(dr => (dr.name, dr.description, dr.sourceid, dr.sourcereference, dr.owner, dr.archivingcomplete, dr.actionrequired, dr.archivetimestamp, dr.modificationtimestamp))
      returning Tables.Document.map(_.id)) += (name, description, sourceId, sourceReference, ownerId, false, false, now, now)

    val f: Future[Int] = dbConfig.db.run(action)
    f.onSuccess({ case id: Int => Logger.info(s"Inserted document with id ${id}")})
    f.onFailure({ case ex: Throwable => Logger.warn(s"Failed to insert document: ${ex}!", ex)})
    return f;
  }


  /** Links a single tag to a document, creating the tag if needed. */
  def addTag(docId: Int, tag: String): Future[Option[DtagRow]] = {
    Logger.debug(s"Adding tag ${tag}.")

    val insertQ = Dtag returning Dtag

    // see whether we find the document in the first place
    val documentFound: Future[Boolean] = dbConfig.db.run(Tables.Document.filter(_.id === docId).exists.result)
    documentFound.flatMap {
      case false => Future(None)
      case true =>
        val insertAction: DBIOAction[Option[DtagRow], NoStream, Read with Write with Write] = for {
        // see whether we find an existing tag. return all as a sequence
          existingTags: Seq[DtagRow] <- Dtag.filter(_.name === tag).result
          tagRow: DtagRow <- existingTags match {
            // got none? insert new
            case Seq() => insertQ += DtagRow(-1, tag)
            // got one? use it, return its id
            case Seq(tagRow: DtagRow) => DBIO.successful(tagRow)
            // got > 1? panic.
            case _ => DBIO.failed(new RuntimeException(s"Found multiple copies of tag ${tag}1"))
          }
          // now that we know id, link it to the doc
          rowsAffected <- Tagging += TaggingRow(docid = docId, tagid = tagRow.id)
        } yield {
          Some(tagRow)
        }

        dbConfig.db.run(insertAction)
    }
  }

  /** Unlinks a given tag from a given document. Orphaned tags are not deleted. */
  def deleteTag(docId: Int, tag: String): Future[Boolean] = {
    val toDelete = Tagging.filter(tagging => (tagging.docid === docId) && (tagging.tagid in (Dtag.filter(_.name === tag).map(_.id))))
    dbConfig.db.run(toDelete.delete).map(_ >= 1) // return true if exactly one row deleted
  }

  /** Updates the description of the document with the given ID. Returns false if it does not exist. */
  def setDescription(docId: Int, description: String): Future[Boolean] = {
    val updateAction = Tables.Document.filter(_.id === docId).map(_.description).update(Some(description))
    dbConfig.db.run(updateAction).map(_ == 1)
  }

  /** Updates the name of the document with the given ID. Returns false if it does not exist. */
  def setName(docId: Int, name: String): Future[Boolean] = {
    val updateAction = Tables.Document.filter(_.id === docId).map(_.name).update(name)
    dbConfig.db.run(updateAction).map(_ == 1)
  }

  /** Updates the name of the document with the given ID. Returns false if it does not exist. */
  def setContact(docId: Int, contactId: Option[Int]): Future[Boolean] = {
    val updateAction = Tables.Document.filter(_.id === docId).map(_.contact).update(contactId)
    dbConfig.db.run(updateAction).map(_ == 1)
  }

  def setArchivingComplete(docId: Int, complete: Boolean): Future[Boolean] = {
    val updateAction = Tables.Document.filter(_.id === docId).map(_.archivingcomplete).update(complete)
    dbConfig.db.run(updateAction).map(_ == 1)
  }

  def setActionRequired(docId: Int, actionRequired: Boolean): Future[Boolean] = {
    val updateAction = Tables.Document.filter(_.id === docId).map(_.actionrequired).update(actionRequired)
    dbConfig.db.run(updateAction).map(_ == 1)
  }

  def setFollowup(docId: Int, timestamp: Option[Long]): Future[Boolean] = {
    val updateAction = Tables.Document.filter(_.id === docId).map(_.followuptimestamp).update(timestamp.map(new java.sql.Timestamp(_)))
    dbConfig.db.run(updateAction).map(_ == 1)
  }

  /** Returns meta data on first and last timestamp. Used for min and max of respective filter sliders in the UI. */
  def meta(): Future[DocumentBaseMetaData] = {
    val earliestQ = db.Tables.Document.map(_.archivetimestamp).min    // will always be <= first modification
    val latestQ = db.Tables.Document.map(_.modificationtimestamp).max // will always be >= latest archive

    val q = Query(earliestQ, latestQ)

    val resultFuture: Future[Seq[(Option[Timestamp], Option[Timestamp])]] = dbConfig.db.run(q.result)
    for {
      result: Seq[(Option[Timestamp], Option[Timestamp])] <- resultFuture
    } yield {
      for {
        (singleMin: Option[Timestamp], singleMax: Option[Timestamp]) <- result.headOption
      } yield {
        DocumentBaseMetaData(earliestTimestamp = singleMin.map(_.getTime), latestTimestamp = singleMax.map(_.getTime))
      }
    }.getOrElse(DocumentBaseMetaData(None, None)) // Seq is empty -> no result found
  }
}
