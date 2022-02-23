package org.cafienne.cmmn.instance.casefile;

import org.cafienne.cmmn.actorapi.event.file.CaseFileItemTransitioned;

import java.util.ArrayList;
import java.util.List;

public class BootstrapCaseFileTransitionPublisher extends CaseFileTransitionPublisher {
    private final List<CaseFileItemTransitioned> bootstrapEvents = new ArrayList<>();

    BootstrapCaseFileTransitionPublisher(CaseFileItem item) {
        super(item);
        addDebugInfo(() -> "Creating delayed publisher for item " + item);
    }

    @Override
    public void addEvent(CaseFileItemTransitioned event) {
        addDebugInfo(() -> "Adding delayed event " + event.getTransition() + " to myself");
        bootstrapEvents.add(event);
        super.updateItemState(event);
    }

    @Override
    public void releaseBootstrapEvents() {
        addDebugInfo(() -> "Releasing " + bootstrapEvents.size() + " events from case file publisher of item " + item);
        bootstrapEvents.forEach(super::informSentryNetwork);
    }
}