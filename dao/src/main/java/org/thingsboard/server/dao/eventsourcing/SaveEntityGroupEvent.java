package org.thingsboard.server.dao.eventsourcing;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

@Builder
@Data
public class SaveEntityGroupEvent {
    private final TenantId tenantId;
    private final EntityId entityId;
    private final Boolean added;
    private final Boolean entityGroupIsAll;
    private final Boolean entityEdgeGroupIsAll;
}
