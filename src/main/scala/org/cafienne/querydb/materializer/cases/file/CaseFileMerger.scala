package org.cafienne.querydb.materializer.cases.file

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.file.{CaseFileItemChildRemoved, CaseFileItemTransitioned}
import org.cafienne.cmmn.instance.Path
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition
import org.cafienne.json.{Value, ValueMap}

object CaseFileMerger extends LazyLogging {

  def merge(event: CaseFileItemTransitioned, currentCaseFile: ValueMap): Unit = {
    val path: Path = event.getPath
    val parentValue = path.resolveParent(currentCaseFile)
    val itemName = path.getName
    val itemValue = event.getValue
    if (path.isArrayElement) {
      val arrayValue = parentValue.withArray(itemName)
      val itemIndex = path.index
      event.getTransition match { // Matching on transition instead of event class, because classes only introduced in 1.1.9
        case CaseFileItemTransition.Delete => arrayValue.set(itemIndex, Value.NULL)
        case CaseFileItemTransition.Replace => arrayValue.set(itemIndex, itemValue)
        case CaseFileItemTransition.Update => arrayValue.set(itemIndex, itemValue)
        case CaseFileItemTransition.Create => arrayValue.size > itemIndex match {
          case true => arrayValue.set(itemIndex, itemValue)
          case false => arrayValue.add(itemValue)
        }
        case CaseFileItemTransition.RemoveChild => {
          val myValue = arrayValue.get(itemIndex).asInstanceOf[ValueMap]
          handleRemoveChild(event.asInstanceOf[CaseFileItemChildRemoved], myValue)
        }
        case _ => // Ignore other transitions for now
      }
    } else {
      event.getTransition match { // Matching on transition instead of event class, because classes only introduced in 1.1.9
        case CaseFileItemTransition.Delete => parentValue.getValue.put(itemName, Value.NULL)
        case CaseFileItemTransition.Replace => parentValue.put(itemName, itemValue)
        case CaseFileItemTransition.Update => parentValue.put(itemName, itemValue)
        case CaseFileItemTransition.Create => parentValue.put(itemName, itemValue)
        case CaseFileItemTransition.RemoveChild => {
          val myValue = if (path.isEmpty) {
            parentValue // My value is top level is case file itself
          } else {
            parentValue.`with`(itemName)
          }
          handleRemoveChild(event.asInstanceOf[CaseFileItemChildRemoved], myValue)
        }
        case _ => // Ignore other transitions
      }
    }
  }

  private def handleRemoveChild(r: CaseFileItemChildRemoved, myValue: ValueMap): Unit = {
    if (r.getChildPath.isArrayElement) {
      val childList = myValue.withArray(r.getChildPath.getName)
      childList.remove(r.getChildPath.index)
    } else {
      myValue.getValue.remove(r.getChildPath.getName)
    }
  }
}
