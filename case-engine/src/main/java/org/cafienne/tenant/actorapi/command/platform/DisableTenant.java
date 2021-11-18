package org.cafienne.tenant.actorapi.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.PlatformOwner;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.response.TenantResponse;

import java.io.IOException;

@Manifest
public class DisableTenant extends PlatformTenantCommand {
    public DisableTenant(PlatformOwner user, String tenantId) {
        super(user, tenantId);
    }

    public DisableTenant(ValueMap json) {
        super(json);
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.disable();
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
    }
}

