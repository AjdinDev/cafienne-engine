package org.cafienne.actormodel.identity

import org.cafienne.actormodel.exception.{AuthorizationException, MissingTenantException}
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{Value, ValueMap}

final case class PlatformUser(id: String, users: Seq[TenantUser]) extends UserIdentity {
  def tenants: Seq[String] = users.map(u => u.tenant)

  def origin(tenant: String): Origin = {
    if (users.isEmpty) {
      // No tenant users. Means trust level is IDP only.
      Origin.IDP
    } else if (isTenantMember(tenant)) {
      // User is a tenant member, so trust level is Tenant itself
      Origin.Tenant
    } else {
      // User is known in the platform, but not part of the tenant
      Origin.Platform
    }
  }

  def tenantRoles(tenant: String): Set[String] = {
    if (isTenantMember(tenant)) getTenantUser(tenant).roles
    else Set()
  }

  /**
    * If the user is registered in one tenant only, that tenant is returned.
    * Otherwise, the default tenant of the platform is returned, but it fails when the user is not a member of that tenant
    *
    * @return
    */
  def defaultTenant: String = {
    if (tenants.length == 1) {
      tenants.head
    } else {
      val configuredDefaultTenant = Cafienne.config.platform.defaultTenant
      if (configuredDefaultTenant.isEmpty) {
        throw new MissingTenantException("Tenant property must have a value")
      }
      if (!tenants.contains(configuredDefaultTenant)) {
        if (tenants.isEmpty) {
          // Throws an exception that user does not belong to any tenant
          getTenantUser("")
        }
        throw new MissingTenantException("Tenant property must have a value, because user belongs to multiple tenants")
      }
      configuredDefaultTenant
    }
  }

  def resolveTenant(optionalTenant: Option[String]): String = {
    optionalTenant match {
      case None => defaultTenant // This will throw an IllegalArgumentException if the default tenant is not configured
      case Some(tenant) => if (tenant.isBlank) {
        defaultTenant
      } else {
        tenant
      }
    }
  }

  override def toValue: Value[_] = {
    new ValueMap(Fields.userId, id, Fields.tenants, users)
  }

  def shouldBelongTo(tenant: String): Unit = if (!isTenantMember(tenant)) throw AuthorizationException("Tenant '" + tenant + "' does not exist, or user '" + id + "' is not registered in it")

  def isTenantMember(tenant: String): Boolean = users.find(_.tenant == tenant).fold(false)(_.enabled)

  def isPlatformOwner: Boolean = Cafienne.isPlatformOwner(id)

  def getTenantUser(tenant: String): TenantUser = users.find(u => u.tenant == tenant).getOrElse({
    val message = tenants.isEmpty match {
      case true => s"User '$id' is not registered in a tenant"
      case false => s"User '$id' is not registered in tenant '$tenant'; user is registered in ${tenants.size} other tenant(s) "
    }
    throw AuthorizationException(message)
  })
}

object PlatformUser {
  def from(user: UserIdentity) = new PlatformUser(user.id, Seq())
}
