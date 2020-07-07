package org.cafienne.infrastructure.akka.http

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.cafienne.akka.actor.serialization.{ValueMapJacksonDeserializer, ValueMapJacksonSerializer}
import org.cafienne.cmmn.akka.command.CaseCommandModels
import org.cafienne.cmmn.instance.casefile.ValueMap
import org.cafienne.service.api.model.{BackwardCompatibleTeam, BackwardCompatibleTeamMember, StartCaseAPI}

/**
  * This file contains some marshallers and unmarshallers for the engine
  */
object CommandMarshallers {

  implicit val StartCaseUnMarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JsonUtil.fromJson[StartCaseAPI](data)
  })

  implicit val StartCaseMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: StartCaseAPI =>
    val startCaseJson = JsonUtil.toJson(value)
    HttpEntity(ContentTypes.`application/json`,  startCaseJson)
  }

  implicit val CaseTeamUnMarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JsonUtil.fromJson[BackwardCompatibleTeam](data)
  })

  implicit val CaseTeamMemberUnMarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JsonUtil.fromJson[BackwardCompatibleTeamMember](data)
  })

  implicit val DiscretionaryItemUnMarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JsonUtil.fromJson[CaseCommandModels.PlanDiscretionaryItem](data)
  })
}

object JsonUtil {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  val valueMapModule = new SimpleModule
  valueMapModule.addSerializer(classOf[ValueMap], new ValueMapJacksonSerializer)
  valueMapModule.addDeserializer(classOf[ValueMap], new ValueMapJacksonDeserializer)
  mapper.registerModule(valueMapModule)

  def toJson(value: Any): String = {
    mapper.writeValueAsString(value)
  }

  def fromJson[T](json: String)(implicit m : Manifest[T]): T = {
    mapper.readValue[T](json)
  }
}