// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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