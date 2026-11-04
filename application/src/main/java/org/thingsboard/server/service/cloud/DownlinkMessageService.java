// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.cloud;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;

import java.util.List;

public interface DownlinkMessageService {

    ListenableFuture<List<Void>> processDownlinkMsg(TenantId tenantId,
                                                    CustomerId edgeCustomerId,
                                                    DownlinkMsg downlinkMsg,
                                                    EdgeSettings currentEdgeSettings);

}