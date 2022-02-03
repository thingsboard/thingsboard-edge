package org.thingsboard.server.common.data.edge;

import lombok.Data;

@Data
public class EdgeSettings {

    private String edgeId;
    private String tenantId;
    private String name;
    private String type;
    private String routingKey;
    private boolean fullSyncRequired;
}