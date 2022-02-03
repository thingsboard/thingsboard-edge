package org.thingsboard.server.common.data.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.CloudEventId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CloudEvent extends BaseData<CloudEventId> {

    private TenantId tenantId;
    private String cloudEventAction;
    private UUID entityId;
    private CloudEventType cloudEventType;
    private transient JsonNode entityBody;

    public CloudEvent() {
        super();
    }

    public CloudEvent(CloudEventId id) {
        super(id);
    }

    public CloudEvent(CloudEvent event) {
        super(event);
    }

}