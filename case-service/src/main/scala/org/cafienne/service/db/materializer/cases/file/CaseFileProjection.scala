package org.cafienne.service.db.materializer.cases.file

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.file._
import org.cafienne.json.{JSONReader, ValueMap}
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.record._

import scala.concurrent.{ExecutionContext, Future}

class CaseFileProjection(persistence: RecordsPersistence, caseInstanceId: String, tenant: String)(implicit val executionContext: ExecutionContext) extends LazyLogging {
  private val businessIdentifiers = scala.collection.mutable.Set[CaseBusinessIdentifierRecord]()
  private val bufferedCaseFileEvents = new CaseFileEventBuffer()
  private var caseFile: Option[ValueMap] = None

  def handleCaseCreation(): Unit = {
    this.caseFile = Some(new ValueMap()) // Always create an empty case file
  }

  def handleCaseFileEvent(event: CaseFileEvent): Future[Done] = {
    event match {
      case itemEvent: CaseFileItemTransitioned => handleCaseFileItemEvent(itemEvent)
      case identifierEvent: BusinessIdentifierEvent => handleBusinessIdentifierEvent(identifierEvent)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  private def handleBusinessIdentifierEvent(event: BusinessIdentifierEvent): Future[Done] = {
    event match {
      case event: BusinessIdentifierSet => businessIdentifiers.add(CaseIdentifierMerger.merge(event))
      case event: BusinessIdentifierCleared => businessIdentifiers.add(CaseIdentifierMerger.merge(event))
      case _ => // Ignore other events
    }
    Future.successful(Done)
  }

  private def handleCaseFileItemEvent(event: CaseFileItemTransitioned): Future[Done] = {
    bufferedCaseFileEvents.addEvent(event)
    // Fetch the existing case file data, so that we can apply the events to it later on
    getCaseFile(caseInstanceId).map(_ => Done)
  }

  private def getCaseFile(caseInstanceId: String): Future[ValueMap] = {
    if (this.caseFile.isEmpty) {
      logger.whenDebugEnabled(logger.debug("Retrieving casefile caseInstanceId={} from database", caseInstanceId))
      persistence.getCaseFile(caseInstanceId).map {
        case Some(record) => JSONReader.parse(record.data)
        case None => new ValueMap()
      }.map {
        data =>
          this.caseFile = Some(data)
          data
      }
    } else {
      Future.successful(this.caseFile.get)
    }
  }

  def prepareCommit(): Unit = {
    // Update case file and identifiers
    this.caseFile.map(getUpdatedCaseFile).foreach(caseFile => persistence.upsert(caseFile))
    this.businessIdentifiers.toSeq.foreach(item => persistence.upsert(item))
  }

  /**
    * Depending on the presence of CaseFileEvents this will add a new CaseFileRecord
    *
    * @param caseFileInProgress
    * @return
    */
  private def getUpdatedCaseFile(caseFileInProgress: ValueMap): CaseFileRecord = {
    bufferedCaseFileEvents.events.forEach(event => CaseFileMerger.merge(event, caseFileInProgress))
    CaseFileRecord(caseInstanceId, tenant, caseFileInProgress.toString)
  }
}
