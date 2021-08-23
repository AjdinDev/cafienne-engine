package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.XMLElementDefinition;
import org.cafienne.cmmn.definition.sentry.CriterionDefinition;
import org.cafienne.cmmn.instance.PlanItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;

public abstract class CriteriaListener {
    protected final PlanItem<?> item;
    protected final Collection<Criterion<?>> criteria = new ArrayList<>();
    protected final Collection<? extends CriterionDefinition> definitions;
    private final String logDescription;

    protected CriteriaListener(PlanItem<?> item, Collection<? extends CriterionDefinition> definitions) {
        this.item = item;
        this.definitions = definitions;
        this.logDescription = getClass().getSimpleName().substring(8).toLowerCase(Locale.ROOT);
    }

    /**
     * Start listening to the sentry network
     */
    public void connect() {
        this.definitions.forEach(criterionDefinition -> criteria.add(criterionDefinition.createInstance(this)));
        if (!criteria.isEmpty()) {
            item.getCaseInstance().addDebugInfo(() -> "Connected " + item + " to " + criteria.size() + " " + logDescription + " criteria");
        }
    }

    /**
     * Stop listening to the sentry network, typically when the criterion is satisfied.
     */
    public void release() {
        if (!criteria.isEmpty()) {
            item.getCaseInstance().addDebugInfo(() -> "Disconnecting " + item + " from " + criteria.size() + " " + logDescription + " criteria");
        }
        criteria.forEach(Criterion::release);
    }

    public abstract void satisfy(Criterion<?> criterion);

    protected abstract void migrateCriteria(ItemDefinition newItemDefinition);

    protected <T extends CriterionDefinition> void migrateCriteria(Collection<T> newCriteria, Stream<Criterion<T>> existingCriteria) {
        item.MigDevConsole(getClass().getSimpleName() + "["+item.getName()+"]: migrating " + criteria.size() + " criteria");
        existingCriteria.forEach(criterion -> {
            T oldDefinition = criterion.getDefinition();
            T newDefinition = XMLElementDefinition.findDefinition(oldDefinition, newCriteria);
            if (newDefinition != null) {
                criterion.migrateDefinition(newDefinition);
            } else {
                // Not sure what to do here. Remove the criterion?
                // Search for a 'nearby' alternative?
                item.MigDevConsole("Cannot migrate criterion");
            }
        });
    }
}
