package org.cafienne.service.api.tenant

import org.cafienne.infrastructure.jdbc.QueryDbConfig

final case class UserRole(userId: String, tenant: String, role_name: String, name: String, email: String = "", enabled: Boolean = true)

final case class User(id: String, tenant: String, name: String, email: String = "", enabled: Boolean = true)

final case class Tenant(name: String, enabled: Boolean = true)

final case class TenantOwner(tenant: String, userId: String, enabled: Boolean = true)

trait TenantTables extends QueryDbConfig {

  import dbConfig.profile.api._

  // Schema for the "tenant-owner" table:
  final class TenantOwnersTable(tag: Tag) extends CafienneTable[TenantOwner](tag, "tenant_owners") {

    def * = (tenant, userId, enabled) <> (TenantOwner.tupled, TenantOwner.unapply)

    def enabled = column[Boolean]("enabled", O.Default(true))

    def pk = primaryKey("pk_tenant_owner", (tenant, userId))

    def tenant = idColumn[String]("tenant")

    def userId = idColumn[String]("userId")
  }

  // Schema for the "tenant" table:
  final class TenantTable(tag: Tag) extends CafienneTable[Tenant](tag, "tenant") {

    // Columsn
    def name = idColumn[String]("name", O.PrimaryKey)
    def enabled = column[Boolean]("enabled", O.Default(true))

    // Constraints
    def pk = primaryKey("pk_tenant", (name))
    def * = (name, enabled) <> (Tenant.tupled, Tenant.unapply)
  }

  final class UserRoleTable(tag: Tag) extends CafienneTable[UserRole](tag, "user_role") {
    def pk = primaryKey("pk_userrole", (userId, tenant, role_name))

    def * = (userId, tenant, role_name, name, email, enabled) <> (UserRole.tupled, UserRole.unapply)

    def userId = idColumn[String]("user_id")

    def tenant = idColumn[String]("tenant")

    def role_name = idColumn[String]("role_name")

    def name = column[String]("name")

    def email = column[String]("email")

    // By default when inserting a user or role, enabled is true;
    //  right now, it does not make sense to enter a user without enabling it
    def enabled = column[Boolean]("enabled", O.Default(true))
  }

}