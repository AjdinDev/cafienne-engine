package org.cafienne.processtask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.CafienneVersion;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.command.StartProcess;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class ProcessStarted extends BaseProcessEvent {
    public final String parentActorId;
    public final String rootActorId;
    public final String name;
    public final ValueMap inputParameters;
    public transient ProcessDefinition definition;
    public final boolean debugMode;
    public final CafienneVersion engineVersion;

    public ProcessStarted(ProcessTaskActor actor, StartProcess command) {
        super(actor);
        this.debugMode = command.debugMode();
        this.definition = command.getDefinition();
        this.name = command.getName();
        this.parentActorId = command.getParentActorId();
        this.rootActorId = command.getRootActorId();
        this.inputParameters = command.getInputParameters();
        this.engineVersion = Cafienne.version();
    }

    public ProcessStarted(ValueMap json) {
        super(json);
        this.engineVersion = json.readObject(Fields.engineVersion, CafienneVersion::new);
        this.name = json.readString(Fields.name);
        this.parentActorId = json.readString(Fields.parentActorId);
        this.rootActorId = json.readString(Fields.rootActorId);
        this.inputParameters = json.readMap(Fields.input);
        this.definition = json.readDefinition(Fields.processDefinition, ProcessDefinition.class);
        this.debugMode = json.readBoolean(Fields.debugMode);
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        actor.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.input, inputParameters);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.parentActorId, parentActorId);
        writeField(generator, Fields.rootActorId, rootActorId);
        writeField(generator, Fields.debugMode, debugMode);
        writeField(generator, Fields.processDefinition, definition);
        writeField(generator, Fields.engineVersion, engineVersion.json());
    }
}
