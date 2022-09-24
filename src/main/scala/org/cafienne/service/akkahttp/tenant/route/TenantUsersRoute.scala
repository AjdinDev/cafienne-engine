
/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.tenant.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.actormodel.exception.AuthorizationException
import org.cafienne.querydb.query.exception.UserSearchFailure
import org.cafienne.service.akkahttp.tenant.model.TenantAPI.TenantUserResponseFormat
import org.cafienne.system.CaseSystem

import javax.ws.rs._
import scala.util.{Failure, Success}


@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tenant")
class TenantUsersRoute(override val caseSystem: CaseSystem) extends TenantRoute {
  override def routes: Route = concat(getTenantUsers, getTenantUser)

  @Path("/{tenant}/users")
  @GET
  @Operation(
    summary = "Get tenant users",
    description = "Retrieves the list of tenant users",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to retrieve users from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of user ids of that are registered in the tenant", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[TenantUserResponseFormat]))))),
      new ApiResponse(responseCode = "404", description = "Tenant not found"),
    )
  )
  @Produces(Array("application/json"))
  def getTenantUsers: Route = get {
    tenantUser { platformUser =>
      path("users") {
        runListQuery(userQueries.getTenantUsers(platformUser))
      }
    }
  }

  @Path("/{tenant}/users/{userId}")
  @GET
  @Operation(
    summary = "Get a tenant user",
    description = "Gets information about the tenant user with the specified id",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to retrieve users from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "userId", description = "The user id to read", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of user ids of that are registered in the tenant", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[TenantUserResponseFormat]))))),
      new ApiResponse(responseCode = "404", description = "User (or tenant) not found"),
    )
  )
  @Produces(Array("application/json"))
  def getTenantUser: Route = get {
    tenantUser { tenantUser =>
      path("users" / Segment) { userId =>
        onComplete(userQueries.getTenantUser(tenantUser, userId)) {
          case Success(tenantUserInformation) =>
            if (tenantUserInformation.enabled) {
              completeJsonValue(tenantUserInformation.toValue)
            } else {
              // TODO: perhaps this should be allowed for tenant owners?
              if (tenantUser.isOwner) {
                logger.warn(s"Tenant owner '${tenantUser.id}' tries to fetch tenant user '$userId' but that account has been disabled, hence no response is given")
              } else {
                logger.warn(s"User with id '${tenantUser.id}' tries to fetch tenant user '$userId' but that account has been disabled")
              }
              complete(StatusCodes.NotFound)
            }
          case Failure(failure) =>
            failure match {
              case u: UserSearchFailure => complete(StatusCodes.NotFound, u.getLocalizedMessage)
              case err: AuthorizationException => complete(StatusCodes.Unauthorized, err.getMessage)
              case _ =>
                logger.warn(s"Ran into an exception while getting user '$userId' in tenant '${tenantUser.tenant}'", failure)
                complete(StatusCodes.InternalServerError)
            }
        }
      }
    }
  }
}
