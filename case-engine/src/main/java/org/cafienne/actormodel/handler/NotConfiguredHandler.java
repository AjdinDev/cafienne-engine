package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.BootstrapCommand;

/**
 * Handler for messages that are sent to a ModelActor before the ModelActor has received a {@link BootstrapCommand}.
 */
public class NotConfiguredHandler extends InvalidMessageHandler {
    public NotConfiguredHandler(ModelActor actor, Object msg) {
        super(actor, msg);
    }

    protected String errorMsg() {
        return this.actor + " cannot handle message '" + msg.getClass().getSimpleName() + "' because it has not been initialized properly";
    }
}
