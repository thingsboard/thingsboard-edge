// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud;

import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

@FunctionalInterface
public interface CloudEventFinder {

    PageData<CloudEvent> find(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink);
}