// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cloud;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface TsKvCloudEventDao extends Dao<CloudEvent> {

    ListenableFuture<Void> saveAsync(CloudEvent cloudEvent);

    PageData<CloudEvent> findCloudEvents(UUID tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink);

    void cleanupEvents(long eventsTtl);

}
