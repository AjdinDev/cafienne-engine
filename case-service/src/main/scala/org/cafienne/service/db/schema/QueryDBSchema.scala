package org.cafienne.service.db.schema

import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.jdbc.CafienneJDBCConfig
import org.cafienne.service.db.schema.QueryDBSchema._db
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile


/**
  * To quickly DROP all tables (including the flyway tables) from Postgres, run the following script
  * *
   DROP table case_file CASCADE;
   DROP table case_instance CASCADE;
   DROP table case_instance_definition CASCADE;
   DROP table case_instance_role CASCADE;
   DROP table case_instance_team_member CASCADE;
   DROP table flyway_schema_history CASCADE;
   DROP table plan_item CASCADE;
   DROP table plan_item_history CASCADE;
   DROP table task CASCADE;
   DROP table "tenant" CASCADE;
   DROP table "tenant_owners" CASCADE;
   DROP table user_role CASCADE;
   DROP table offset_storage CASCADE;
  */

trait QueryDBSchema extends CafienneJDBCConfig {

  override lazy val dbConfig: DatabaseConfig[JdbcProfile] = _db
}

object QueryDBSchema extends QueryDBSchema {
   lazy val _db: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("", Cafienne.config.queryDB.config)
}
