package org.cafienne.querydb.materializer.cases

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.cmmn.actorapi.event._
import org.cafienne.cmmn.actorapi.event.file._
import org.cafienne.cmmn.actorapi.event.plan._
import org.cafienne.cmmn.actorapi.event.team._
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.querydb.materializer.{EventBatchTransaction, QueryDBStorage}
import org.cafienne.querydb.materializer.cases.file.CaseFileProjection
import org.cafienne.querydb.materializer.cases.plan.CasePlanProjection
import org.cafienne.querydb.materializer.cases.team.CaseTeamProjection

import scala.concurrent.Future

class CaseTransaction(val batch: CaseEventBatch, val storage: QueryDBStorage) extends EventBatchTransaction with LazyLogging  {
  import scala.concurrent.ExecutionContext.Implicits.global

  val caseInstanceId: String = batch.persistenceId
  val tenant: String = batch.events.head.event.tenant()
  val dBTransaction: CaseStorageTransaction = storage.createCaseTransaction(batch.persistenceId)

  private val caseTeamProjection = new CaseTeamProjection(dBTransaction)
  private val caseFileProjection = new CaseFileProjection(dBTransaction, caseInstanceId, tenant)
  private val casePlanProjection = new CasePlanProjection(dBTransaction)
  private val caseProjection = new CaseProjection(dBTransaction, caseFileProjection)

  def createOffsetRecord(offset: Offset): OffsetRecord = OffsetRecord(CaseEventSink.offsetName, offset)

  override def handleEvent(envelope: ModelEventEnvelope): Future[Done] = {
    logger.whenDebugEnabled(logger.debug("Handling event of type " + envelope.event.getClass.getSimpleName + " in case " + caseInstanceId))
    envelope.event match {
      case event: CasePlanEvent[_] => casePlanProjection.handleCasePlanEvent(event)
      case event: CaseFileEvent => caseFileProjection.handleCaseFileEvent(event)
      case event: CaseTeamEvent => caseTeamProjection.handleCaseTeamEvent(event)
      case event: CaseEvent => caseProjection.handleCaseEvent(event)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  override def commit(envelope: ModelEventEnvelope, transactionEvent: CommitEvent): Future[Done] = {
    transactionEvent match {
      case caseModified: CaseModified => commitCaseRecords(envelope, caseModified)
      case event: CaseAppliedPlatformUpdate => updateUserIds(event, envelope)
      case _ =>
        logger.warn(s"CaseTransaction unexpectedly receives a commit event of type ${transactionEvent.getClass.getName}. This event is ignored.")
        Future.successful(Done)
    }
  }

  private def commitCaseRecords(envelope: ModelEventEnvelope, caseModified: CaseModified): Future[Done] = {
    // Tell the projections to prepare for commit, i.e. let them update the persistence.
    caseProjection.prepareCommit()
    caseTeamProjection.prepareCommit()
    caseFileProjection.prepareCommit()
    casePlanProjection.prepareCommit(caseModified)
    // Update the offset storage with the latest & greatest offset we handled
    dBTransaction.upsert(createOffsetRecord(envelope.offset))

    // Commit and then inform the last modified registration
    dBTransaction.commit().andThen(_ => CaseReader.lastModifiedRegistration.handle(caseModified))
  }

  private def updateUserIds(event: CaseAppliedPlatformUpdate, envelope: ModelEventEnvelope): Future[Done] = {
    dBTransaction.updateCaseUserInformation(event.getCaseInstanceId, event.newUserInformation.info, createOffsetRecord(envelope.offset))
  }
}
