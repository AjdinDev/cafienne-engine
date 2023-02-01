/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.storage.archival.state

import akka.Done
import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.archival.ActorDataArchiver
import org.cafienne.storage.archival.event.{ModelActorArchived, ProcessArchived}
import org.cafienne.storage.querydb.ProcessStorage

import scala.concurrent.Future

class ProcessArchivalState(override val actor: ActorDataArchiver) extends ArchivalState {
  override val dbStorage: ProcessStorage = new ProcessStorage

  override def findCascadingChildren(): Future[Seq[ActorMetadata]] = Future.successful(Seq())

  override def archiveQueryData(): Future[Done] = Future.successful(Done) // Nothing to archive here, just tell our actor we're done.

  override def createModelActorEvent: ModelActorArchived = new ProcessArchived(metadata)
}