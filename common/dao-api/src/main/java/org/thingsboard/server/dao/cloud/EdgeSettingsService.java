// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cloud;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;

import java.util.List;

public interface EdgeSettingsService {

    EdgeSettings findEdgeSettings();

    ListenableFuture<AttributesSaveResult> saveEdgeSettings(TenantId tenantId, EdgeSettings edgeSettings);
}
