// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
public class CloudEventDataValidator extends DataValidator<CloudEvent> {

    @Override
    protected void validateDataImpl(TenantId tenantId, CloudEvent cloudEvent) {
        if (cloudEvent.getAction() == null) {
            throw new DataValidationException("Edge Event action should be specified!");
        }
    }
}
