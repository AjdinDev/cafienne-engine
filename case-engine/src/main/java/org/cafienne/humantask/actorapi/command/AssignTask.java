/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.actorapi.response.HumanTaskResponse;
import org.cafienne.humantask.instance.TaskState;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class AssignTask extends WorkflowCommand {
    private final String assignee;

    public AssignTask(CaseUserIdentity user, String caseInstanceId, String taskId, UserIdentity assignee) {
        super(user, caseInstanceId, taskId);
        this.assignee = assignee.id();
    }

    public AssignTask(ValueMap json) {
        super(json);
        this.assignee = json.readString(Fields.assignee);
    }

    @Override
    public void validate(HumanTask task) {
        super.validateCaseOwnership(task);
        TaskState currentTaskState = task.getImplementation().getCurrentState();
        if (! currentTaskState.isActive()) {
            raiseException("Cannot be done because the task is in " + currentTaskState + " state, but must be in an active state (Unassigned or Assigned)");
        }
        super.validateCaseTeamMembership(task, assignee);
    }

    @Override
    public HumanTaskResponse process(WorkflowTask workflowTask) {
        workflowTask.assign(this.assignee);
        return new HumanTaskResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.assignee, assignee);
    }
}
