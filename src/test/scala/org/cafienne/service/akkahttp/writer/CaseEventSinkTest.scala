package org.cafienne.service.akkahttp.writer

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.testkit.{TestKit, TestProbe}
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.test.TestScript
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.querydb.materializer.cases.CaseEventSink
import org.cafienne.querydb.schema.QueryDB
import org.cafienne.system.CaseSystem
import org.cafienne.util.Guid
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class CaseEventSinkTest
  extends TestKit(ActorSystem("testsystem", TestConfig.config))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with Eventually {

  implicit val logger: LoggingAdapter = Logging(system, getClass)

  // This is needed to get an offset
  QueryDB.verifyConnectivity()

  val caseInstanceId: String = new Guid().toString

  lazy val storeEventsActor: ActorRef = system.actorOf(Props(classOf[CreateEventsInStoreActor]), caseInstanceId)
  val tp: TestProbe = TestProbe()

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(2, Seconds)),
    interval = scaled(Span(5, Millis)))

  def sendEvent(evt: CaseEvent): CaseEvent = {
    within(10.seconds) {
      tp.send(storeEventsActor, evt)
      tp.expectMsg(evt)
    }
  }

  val user: TenantUser = TestIdentityFactory.createTenantUser("test")
  val caseDefinition: CaseDefinition = TestScript.getCaseDefinition("testdefinition/helloworld.xml")
  val eventFactory = new EventFactory(caseInstanceId, caseDefinition, user)

  new CaseEventSink(new CaseSystem(system), TestQueryDB).start()
}
