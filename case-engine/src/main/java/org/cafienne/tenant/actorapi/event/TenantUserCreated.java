package org.cafienne.tenant.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.User;

import java.io.IOException;

@Manifest
public class TenantUserCreated extends TenantUserEvent {
    public final String name;
    public final String email;

    public TenantUserCreated(TenantActor tenant, String userId, String name, String email) {
        super(tenant, userId);
        this.name = name;
        this.email = email;
    }

    public TenantUserCreated(ValueMap json) {
        super(json);
        this.name = json.readString(Fields.name);
        this.email = json.readString(Fields.email);
    }

    @Override
    protected void updateUserState(User user) {

    }

    @Override
    public void updateState(TenantActor tenant) {
        tenant.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.email, email);
    }
}
