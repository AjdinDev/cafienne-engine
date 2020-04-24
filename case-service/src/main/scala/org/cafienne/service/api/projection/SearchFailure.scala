package org.cafienne.service.api.projection

class SearchFailure(msg: String) extends RuntimeException(msg)

case class CaseSearchFailure(caseId: String) extends SearchFailure(s"A case with id '$caseId' cannot be not found.")
case class TaskSearchFailure(taskId: String) extends SearchFailure(s"A task with id '$taskId' cannot be not found.")
case class UserSearchFailure(userId: String) extends SearchFailure(s"A user with id '$userId' cannot be not found.")