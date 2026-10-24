// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.thingsboard.server.common.data.cloud.CloudEvent;

import static org.thingsboard.server.dao.model.ModelConstants.TS_KV_CLOUD_EVENT_COLUMN_FAMILY_NAME;

@Entity
@Table(name = TS_KV_CLOUD_EVENT_COLUMN_FAMILY_NAME)
public class TsKvCloudEventEntity extends AbstractCloudEventEntity {

    public TsKvCloudEventEntity() {
        super();
    }

    public TsKvCloudEventEntity(CloudEvent cloudEvent) {
        super(cloudEvent);
    }

    @Override
    public CloudEvent toData() {
        return super.toData();
    }

}
