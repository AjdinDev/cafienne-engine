package org.cafienne.service.api.projection.query

case class Sort(on: Option[String], direction: Option[String] = Some("desc")) {
  lazy val ascending = direction.fold(false)(d => if (d matches "(?i)asc")  true else false)
}

object Sort {
  def NoSort: Sort = Sort(None, None)

  def asc(field: String) = Sort(Some(field), Some("asc"))

  def on(field: String) = Sort(Some(field))

}
