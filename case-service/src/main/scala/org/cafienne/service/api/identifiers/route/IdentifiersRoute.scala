/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.identifiers.route

import akka.http.scaladsl.server.Directives._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.route.QueryRoute
import org.cafienne.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.service.api.cases._
import org.cafienne.service.api.model.BusinessIdentifierFormat
import org.cafienne.service.db.query.IdentifierQueries
import org.cafienne.service.db.query.filter.IdentifierFilter
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/identifiers")
class IdentifiersRoute(val identifierQueries: IdentifierQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem
) extends QueryRoute {
  override val lastModifiedRegistration = CaseReader.lastModifiedRegistration

  override def routes = concat(getIdentifiers, getIdentifierNames)

  @GET
  @Operation(
    summary = "Get a list of identifiers",
    description = "Returns a list of identifiers",
    tags = Array("identifiers"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optionally provide a specific tenant to read the identifiers from", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "name", description = "Optionally provide a name to read only identifiers with that name", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "offset", description = "Starting position", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "0"), required = false),
      new Parameter(name = "numberOfResults", description = "Maximum number of identifiers to fetch", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "100"), required = false),
      new Parameter(name = "sortBy", description = "Field to sort on", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "sortOrder", description = "Sort direction ('asc' or 'desc')", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of identifiers", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[BusinessIdentifierFormat]))))),
    )
  )
  @Produces(Array("application/json"))
  def getIdentifiers = get {
    pathEndOrSingleSlash {
      validUser { platformUser =>
        parameters('tenant ?, 'name ?, 'offset ? 0, 'numberOfResults ? 100, 'sortBy ?, 'sortOrder ?) {
          (tenant, name, offset, numResults, sortBy, sortOrder) =>
            val filter = IdentifierFilter(tenant, name)
            runQuery(identifierQueries.getIdentifiers(platformUser, filter, Area(offset, numResults), Sort.withDefault(sortBy, sortOrder, "name")))
        }
      }
    }
  }

  @Path("/names")
  @GET
  @Operation(
    summary = "Get a list of identifier names",
    description = "Returns a list of identifier names, i.e. names for which identifier values exist",
    tags = Array("identifiers"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optionally provide a specific tenant to read the identifiers from", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of identifier names", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[String]))))),
    )
  )
  @Produces(Array("application/json"))
  def getIdentifierNames = get {
    path("names") {
      pathEndOrSingleSlash {
        validUser { platformUser =>
          parameters('tenant ?) { tenant =>
            runListQuery(identifierQueries.getIdentifierNames(platformUser, tenant))
          }
        }
      }
    }
  }
}