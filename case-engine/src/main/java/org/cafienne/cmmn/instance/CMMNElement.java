/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.definition.DefinitionElement;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;
import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.json.Value;

public class CMMNElement<T extends DefinitionElement> {
    private final Case caseInstance;
    private T definition;
    private T previousDefinition;

    protected CMMNElement() {
        // NOTE: this constructor is added to overcome serialization issues for task parameters. To be reviewed when we review the whole serialization structure again.
        this.caseInstance = null;
        this.definition = null;
    }

    protected CMMNElement(Case caseInstance, T definition) {
        this.caseInstance = caseInstance;
        this.definition = definition;
    }

    protected CMMNElement(CMMNElement<?> parentElement, T definition) {
        this.caseInstance = parentElement.caseInstance;
        this.definition = definition;
    }

    protected void addDebugInfo(DebugStringAppender appender, Criterion<?> criterion) {
        addDebugInfo(appender, criterion.toJson());
    }

    protected void addDebugInfo(DebugStringAppender appender, Exception e) {
        getCaseInstance().addDebugInfo(appender, e);
    }

    protected void addDebugInfo(DebugStringAppender appender, Value<?> v) {
        getCaseInstance().addDebugInfo(appender, v);
    }

    protected void addDebugInfo(DebugStringAppender appender) {
        getCaseInstance().addDebugInfo(appender);
    }

    public T getDefinition() {
        return definition;
    }

    public T getPreviousDefinition() {
        return previousDefinition;
    }

    public void migrateDefinition(T newDefinition) {
        this.previousDefinition = this.definition;
        this.definition = newDefinition;
    }

    protected boolean hasNewDefinition() {
        return this.previousDefinition != null && this.definition.differs(previousDefinition);
    }

    public Case getCaseInstance() {
        return caseInstance;
    }

    public String toString() {
        // TODO: make this print the name
        return super.toString();
    }

    protected <T extends CaseEvent> T addEvent(T event) {
        return getCaseInstance().addEvent(event);
    }
}
