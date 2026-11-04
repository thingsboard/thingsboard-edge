// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.edge;

import lombok.Data;

@Data
public class EdgeSettings {

    private String edgeId;
    private String tenantId;
    private String name;
    private String type;
    private String routingKey;
    private Long maxDevices;
    private Long maxAssets;
    private boolean fullSyncRequired;
}
