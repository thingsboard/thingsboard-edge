/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.dao.cloud;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;

@Service
@Slf4j
public class BaseCloudEventService implements CloudEventService {

    @Autowired
    public CloudEventDao cloudEventDao;

    @Override
    public ListenableFuture<CloudEvent> saveAsync(CloudEvent cloudEvent) {
        cloudEventValidator.validate(cloudEvent, CloudEvent::getTenantId);
        return cloudEventDao.saveAsync(cloudEvent);
    }

    @Override
    public TimePageData<CloudEvent> findCloudEvents(TenantId tenantId, TimePageLink pageLink) {
        List<CloudEvent> events = cloudEventDao.findCloudEvents(tenantId.getId(), pageLink);
        return new TimePageData<>(events, pageLink);
    }

    private DataValidator<CloudEvent> cloudEventValidator =
            new DataValidator<CloudEvent>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, CloudEvent cloudEvent) {
                    if (StringUtils.isEmpty(cloudEvent.getCloudEventAction())) {
                        throw new DataValidationException("Cloud Event action should be specified!");
                    }
                }
            };
}
