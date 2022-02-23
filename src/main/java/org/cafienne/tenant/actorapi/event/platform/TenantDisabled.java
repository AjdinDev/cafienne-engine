package org.cafienne.tenant.actorapi.event.platform;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

@Manifest
public class TenantDisabled extends PlatformBaseEvent {

    public TenantDisabled(TenantActor tenant) {
        super(tenant);
    }

    public TenantDisabled(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(TenantActor tenant) {
        tenant.updateState(this);
    }
}