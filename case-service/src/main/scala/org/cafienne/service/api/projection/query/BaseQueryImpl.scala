package org.cafienne.service.api.projection.query

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.service.api.projection.record.CaseBusinessIdentifierRecord
import org.cafienne.service.api.projection.table.{CaseTables, TaskTables, TenantTables}

trait BaseQueryImpl
  extends CaseTables
    with TaskTables
    with TenantTables
    with LazyLogging {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val caseInstanceQuery = TableQuery[CaseInstanceTable]
  val caseFileQuery = TableQuery[CaseFileTable]

  val planItemTableQuery = TableQuery[PlanItemTable]
  val taskQuery = TableQuery[TaskTable]
  val caseInstanceRoleQuery = TableQuery[CaseInstanceRoleTable]
  val caseInstanceTeamMemberQuery = TableQuery[CaseInstanceTeamMemberTable]
  val rolesQuery = TableQuery[UserRoleTable]


  /**
    * Query that validates that the user belongs to the team of the specified case, either by explicit
    * membership of the user id, or by one of the tenant roles of the user that are bound to the team of the case
    * @param user
    * @param caseInstanceId
    * @param tenant
    * @return
    */
  def membershipQuery(user: PlatformUser, caseInstanceId: Rep[String], tenant: Rep[String], identifiers: Option[String]) = {
    val query = for {
      // Validate tenant membership
      tenantMembership <- TableQuery[UserRoleTable].filter(_.userId === user.userId).filter(_.tenant === tenant)
      // Validate case team membership: either user is explicit member or has a matching tenant role
      teamMembership <- TableQuery[CaseInstanceTeamMemberTable]
        .filter(_.caseInstanceId === caseInstanceId)
        .filter(_.active === true) // Only search in active team members
        .filter(_.caseRole === "") // Only search by base membership, not in certain roles
        .filter(member => { // Search by user id or by one of the user's tenant roles
          (member.isTenantUser === true && member.memberId === user.userId) ||
            (member.isTenantUser === false && member.memberId === tenantMembership.role_name)
        })
      _ <- {
        val query = {
          if (identifiers.isEmpty) blankIdentifierFilterQuery(caseInstanceId)
          else addBusinessIdentifiersFilter(identifiers, caseInstanceId)
        }
        query
      }
    } yield (tenantMembership, teamMembership)

    query
  }

  /**
    * Membership query is extended with business identifier filters, if any.
    * If no identifiers are passed, then we need to have a base query applied (either on Case or on Task)
    * @param caseInstanceId
    * @return
    */
  def blankIdentifierFilterQuery(caseInstanceId: Rep[String]): Query[_, _, Seq]

  def addBusinessIdentifiersFilter(filterString: Option[String], caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq]  = {
    new BusinessIdentifierFilterParser(filterString).asQuery(caseInstanceId)
  }

  class BusinessIdentifierFilterParser(string: Option[String]) {
    private val filters: Seq[ParsedFilter] = string.fold(Seq[ParsedFilter]()) {
      parseFilters
    }

    def asQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq]  = {
      val topLevelQuery = filters.length match {
        case 0 => {
          // If no filter is specified, then there must be at least something in the business identifier table, i.e.,
          //  at least one business identifier must be filled in the case.
          TableQuery[CaseBusinessIdentifierTable].filter(_.caseInstanceId === caseInstanceId)
        }
        case 1 => {
          logger.whenDebugEnabled{logger.debug(s"Simple filter: [$string]")}
          filters(0).toQuery(caseInstanceId)
        }
        case moreThanOne => {
          logger.whenDebugEnabled{logger.debug(s"Composite filter on $moreThanOne fields: [$string]")}
          for {
            topQuery <- filters(0).toQuery(caseInstanceId)
            _ <- createCompositeQuery(1, topQuery.caseInstanceId)
          } yield topQuery
        }
      }
      topLevelQuery
    }

    /**
      * Note: this method is recursive, iterating into the depth of the filter list to create a structure like below
            f0 <- getQ(0, caseInstanceId)
            q1 <- for {
              f1 <- getQ(1, f0.caseInstanceId)
              q2 <- for {
                f2 <- getQ(2, f1.caseInstanceId)
                q3 <- for {
                        f3 <- getQ(3, f2.caseInstanceId)
                        q4 <- for {
                            f4 <- getQ(4, f3.caseInstanceId)
                            q5 <- for {
                                f5 <- getQ(5, q4.caseInstanceId)
                                q6 <- for {
                                    f6 <- getQ(6, f5.caseInstanceId)
                                } yield f6
                            } yield f5
                        } yield f4
                    } yield f3
                } yield f2
            } yield f1      *
      * @param current
      * @param caseInstanceId
      * @return
      */
    def createCompositeQuery(current: Int, caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = {
      val next = current + 1
      if (filters.size <= next) {
        for {
          finalQuery <- filters(current).toQuery(caseInstanceId)
        } yield finalQuery
      } else {
        for {
          thisFilterQuery <- filters(current).toQuery(caseInstanceId)
          _ <- createCompositeQuery(next, thisFilterQuery.caseInstanceId)
        } yield thisFilterQuery
      }
    }

    override def toString: String = {
      s"====================== Filter[${string}]\n${filters.map(filter => s"Filter[${filter.field}]: $filter").mkString("\n")}\n========================"
    }

    def parseFilters(query: String): Seq[ParsedFilter] = {
      // First, create a raw list of all filters given.
      val rawFilters: Seq[RawFilter] = query.split(',').map(rawFilter => {
        if (rawFilter.isBlank) NoFilter()
        else if (rawFilter.startsWith("!")) NotFieldFilter(rawFilter.substring(1))
        else if (rawFilter.indexOf("!=") > 0) NotValueFilter(rawFilter)
        else if (rawFilter.indexOf("=") > 0) ValueFilter(rawFilter)
        else FieldFilter(rawFilter) // Well, with all options coming
      })

      // Next, collect and merge filters that work on the same field
      val filtersPerField = scala.collection.mutable.LinkedHashMap[String, scala.collection.mutable.ArrayBuffer[RawFilter]]()
      rawFilters.map(filter => filtersPerField.getOrElseUpdate(filter.field, scala.collection.mutable.ArrayBuffer[RawFilter]()) += filter)

      // Next, join filters on the same field to one new BasicFilter for that field
      //  Combination logic:
      //  1. NotFieldFilter takes precedence over all other filters for that field
      //  2. Any NotValueFilter in combination with ValueFilter can be discarded
      def combineToBasicFilter(field: String, filters: Seq[RawFilter]): ParsedFilter = {
        val filter: ParsedFilter = {
          filters.find(f => f.isInstanceOf[NotFieldFilter]).getOrElse({
            val notFieldValues = JoinedNotFilter(field, filters.filter(f => f.isInstanceOf[NotValueFilter]).map(f => f.asInstanceOf[NotValueFilter].value))
            if (notFieldValues.values.nonEmpty) { // There are NotValueFilters; but they are only relevant if there are not also ValueFilters, otherwise ValueFilters take precedence
              val valueFilters = filters.filter(f => f.isInstanceOf[ValueFilter])
              if (valueFilters.nonEmpty) {
                OrFilter(field, valueFilters.map(f => f.asInstanceOf[ValueFilter].value))
              } else {
                // There are no ValueFilters; there might be a FieldFilter, but that can be safely ignored
                notFieldValues
              }
            } else {
              // Check to see if there is a generic FieldFilter, that takes precedence over any ValueFilters for that field
              filters.find(f => f.isInstanceOf[FieldFilter]).getOrElse(OrFilter(field, filters.map(f => f.asInstanceOf[ValueFilter].value)))
            }
          })
        }.asInstanceOf[ParsedFilter]
        filter
      }

      // TODO: for performance reasons we can sort the array to have the "NOT" filters at the end
      filtersPerField.toArray[(String, Seq[RawFilter])].map(fieldFilter => combineToBasicFilter(fieldFilter._1, fieldFilter._2))
    }
  }

  trait RawFilter {
    protected val rawFieldName: String // Raw field name should NOT be used, only the trimmed version should be used.
    lazy val field = rawFieldName.trim() // Always trim field names.
  }

  trait BasicValueFilter extends RawFilter {
    private lazy val splittedRawFilter = rawFilter.split(splitter)
    val splitter: String
    val rawFilter: String
    val rawFieldName = getContent(0)
    val value = getContent(1)

    private def getContent(index: Int): String = {
      if (splittedRawFilter.length > index) splittedRawFilter(index)
      else ""
    }
  }

  case class NotValueFilter(rawFilter: String, splitter: String = "!=") extends BasicValueFilter

  case class ValueFilter(rawFilter: String, splitter: String = "=") extends BasicValueFilter

  trait ParsedFilter extends RawFilter {
    def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq]
  }

  case class NoFilter(rawFieldName: String = "") extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]) = TableQuery[CaseBusinessIdentifierTable].filter(_.caseInstanceId === caseInstanceId)
  }

  case class NotFieldFilter(rawFieldName: String) extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = {
      logger.warn(s"OPERATION NOT YET SUPPORTED: 'Field-must-NOT-be-set' filter for field $field")
      logger.whenDebugEnabled{logger.debug(s"Adding 'Field-must-NOT-be-set' filter for field $field")}
      // TODO: this must be refactored to support some form of not exists like below. Currently unclear how to achieve this in Slick
      //select case-id
      //from business-identifier 'bi-1'
      //where (bi-1.name === 'd' && bi-1.value in ('java', 'sql')
      //and not exists (select * from business-identifier 'bi-2'
      //                where bi-2.case-id = bi-1.case-id
      //                and bi-2.name = 'u')
      TableQuery[CaseBusinessIdentifierTable].filterNot(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field)
    }
  }

  case class FieldFilter(rawFieldName: String) extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = {
      logger.whenDebugEnabled{logger.debug(s"Adding 'Field-must-be-set' filter for field $field")}
      TableQuery[CaseBusinessIdentifierTable].filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field && identifier.value.nonEmpty)
    }
  }

  case class JoinedNotFilter(rawFieldName: String, values: Seq[String]) extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = values.length match {
      case 1 => {
        logger.whenDebugEnabled{logger.debug(s"Adding 'Field-does-not-have-value' filter $field == ${values(0)}")}
        TableQuery[CaseBusinessIdentifierTable]
          .filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field)
          .filterNot(_.value === values(0))
      }
      case _ => {
        logger.whenDebugEnabled{logger.debug(s"Adding 'Value-NOT-in-set' filter for field $field on values $values")}
        TableQuery[CaseBusinessIdentifierTable].filterNot(record => record.caseInstanceId === caseInstanceId && record.active === true && record.name === field && record.value.inSet(values))
      }
    }
  }

  case class OrFilter(rawFieldName: String, values: Seq[String]) extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = values.length match {
      case 1 => {
        logger.whenDebugEnabled{logger.debug(s"Adding 'Field-has-value' filter $field == ${values(0)}")}
        TableQuery[CaseBusinessIdentifierTable].filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field && identifier.value === values(0))
      }
      case _ => {
        logger.whenDebugEnabled{logger.debug(s"Adding 'Value-in-set' filter for field $field on values $values")}
        TableQuery[CaseBusinessIdentifierTable].filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field && identifier.value.inSet(values))
      }
    }
  }

}

