package org.cafienne.humantask.actorapi.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.humantask.actorapi.command.WorkflowCommand;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class HumanTaskValidationResponse extends HumanTaskResponse {
    private final ValueMap value;

    public HumanTaskValidationResponse(WorkflowCommand command, ValueMap value) {
        super(command);
        this.value = value;
    }

    public HumanTaskValidationResponse(ValueMap json) {
        super(json);
        this.value = json.readMap(Fields.value);
    }

    @Override
    public ValueMap toJson() {
        return value;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.value, value);
    }
}
