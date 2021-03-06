package org.cafienne.akka.actor.handler;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.event.ModelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecoveryEventHandler<C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends ValidMessageHandler<E, C, E, A> {
    private final static Logger logger = LoggerFactory.getLogger(RecoveryEventHandler.class);

    public RecoveryEventHandler(A actor, E msg) {
        super(actor, msg);
    }

    protected void process() {
        msg.recover(actor);
    }

    @Override
    public <EV extends E> EV addEvent(EV event) {
        // NOTE: This commit makes it possible to handle actor state updates from events only.
        //  Such has been implemented for TenantActor and ProcessTaskActor, and partly for Case.
        //  Enabling the logging will showcase where this pattern has not been completely done.
//        System.out.println("Recovering " + actor + " generates event of type " + event.getClass().getSimpleName());

        logger.debug("Not storing internally generated event because recovery is running. Event is of type " + event.getClass().getSimpleName());
        return event;
    }

    protected void complete() {
    }

}
