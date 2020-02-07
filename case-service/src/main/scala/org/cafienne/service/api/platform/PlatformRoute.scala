/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.platform

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import io.swagger.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.response.{CommandFailure, SecurityFailure}
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.Main
import org.cafienne.service.api.AuthenticatedRoute
import org.cafienne.service.api.cases.TestResponse
import org.cafienne.service.api.tenant.model.TenantAPI
import org.cafienne.tenant.akka.command.TenantCommand
import org.cafienne.tenant.akka.command.platform.{CreateTenant, DisableTenant, EnableTenant}
import org.cafienne.tenant.akka.command.response.{TenantOwnersResponse, TenantResponse}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

@Api(value = "platform", tags = Array("platform"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/platform")
class PlatformRoute()(override implicit val userCache: IdentityProvider) extends AuthenticatedRoute {

  override def routes = {
    pathPrefix("platform") {
      createTenant ~
      disableTenant ~
      enableTenant ~
      getUserInformation
    }
  }

  @Path("/platform")
  @POST
  @Operation(
    summary = "Register a new tenant",
    description = "Register a new tenant with it's owners",
    tags = Array("platform"),
    responses = Array(
      new ApiResponse(description = "Tenant registered successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  @RequestBody(description = "Tenant", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.Tenant]))))
  @Consumes(Array("application/json"))
  def createTenant = post {
    pathEndOrSingleSlash {
      validUser { platformOwner =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._

        implicit val userFormat = jsonFormat4(TenantAPI.User)
        implicit val tenantFormat = jsonFormat2(TenantAPI.Tenant)
        entity(as[TenantAPI.Tenant]) { newTenant =>
          val newTenantName = newTenant.name
          val owners = newTenant.owners.map(owner => TenantUser(owner.userId, owner.roles.toSeq, newTenantName, owner.name.getOrElse(""), owner.email.getOrElse(""), true))
          askPlatform(new CreateTenant(platformOwner, newTenantName, newTenantName, owners.asJava))
        }
      }
    }
  }

  @Path("/{tenant}/disable")
  @PUT
  @Operation(
    summary = "Disable a tenant",
    description = "Disabling a tenant can only be done by platform owners",
    tags = Array("platform"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to disable", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Tenant registered successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  def disableTenant = put {
    validUser { platformOwner =>
      path(Segment / "disable") { tenant =>
        askPlatform(new DisableTenant(platformOwner, tenant.name))
      }
    }
  }

  @Path("/{tenant}/enable")
  @PUT
  @Operation(
    summary = "Enable a tenant",
    description = "Enabling a tenant can only be done by platform owners",
    tags = Array("platform"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to enable", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Tenant registered successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  def enableTenant = put {
    validUser { platformOwner =>
      path(Segment / "enable") { tenant =>
        askPlatform(new EnableTenant(platformOwner, tenant.name))
      }
    }
  }

  @Path("/user")
  @GET
  @Operation(
    summary = "Get user information of current user",
    description = "Retrieves the user information of current user",
    tags = Array("platform"),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "All user information known within the platform", content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.PlatformUser])))),
      new ApiResponse(responseCode = "400", description = "Invalid request"),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Produces(Array("application/json"))
  def getUserInformation = get {
    pathPrefix("user") {
      pathEndOrSingleSlash {
        validUser { user =>
          val value = HttpEntity(ContentTypes.`application/json`, user.toJSON)
          complete(StatusCodes.OK, value)
        }
      }
    }
  }

  private def askPlatform(command: TenantCommand) = {
    import akka.pattern.ask
    implicit val timeout = Main.caseSystemTimeout

    onComplete(CaseSystem.router ? command) {
      case Success(value) =>
        value match {
          case s: SecurityFailure => complete(StatusCodes.Unauthorized, s.exception.getMessage)
          case e: CommandFailure => complete(StatusCodes.BadRequest, e.exception.getMessage)
          case o: TenantOwnersResponse => {

            // TODO: akkhttp will support some form of json serialization of a set of strings. To be found :).
            val sb = new StringBuilder("[")
            val owners = o.owners
            var first = true
            owners.forEach(o => {
              if (! first) {
                sb.append(", ")
              }
              sb.append("\"" + o + "\"")
              if (first) first = false
            })
            sb.append("]")


            complete(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, sb.toString))
          }
          case _: TenantResponse => complete(StatusCodes.NoContent)
          case TestResponse => complete(StatusCodes.NoContent)
        }
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
    }
  }
}