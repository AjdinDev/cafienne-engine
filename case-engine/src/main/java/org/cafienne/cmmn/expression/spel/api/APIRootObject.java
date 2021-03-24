package org.cafienne.cmmn.expression.spel.api;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.Case;

/**
 * Base context for SPEL expressions, enabling access to the case and it's public members from any expression.
 * <p>Some example expressions:
 * <ul>
 * <li><code>caseInstance.id</code> - The id of the case</li>
 * <li><code>user.id</code> - The unique id of the user executing the current command in the case</li>
 * <li><code>caseInstance.planItems.size()</code> - The number of plan items currently in the case</li>
 * <li><code>caseInstance.definition.name</code> - The name of case definition</li>
 * <li><code>caseInstance.definition.caseRoles</code> - The roles defined in the case</li>
 * </ul>
 * <p>
 * See {@link Case} itself for it's members.
 */
public abstract class APIRootObject<T extends ModelActor> extends APIObject<T> {
    protected APIRootObject(T model) {
        super(model);
    }

    public ValueMap map(Object... args) {
        return this.Map(args);
    }

    public ValueMap Map(Object... args) {
        return new ValueMap(args);
    }

    public ValueList list(Object... args) {
        return this.List(args);
    }

    public ValueList List(Object... args) {
        return new ValueList(args);
    }

    public abstract String getDescription();
}