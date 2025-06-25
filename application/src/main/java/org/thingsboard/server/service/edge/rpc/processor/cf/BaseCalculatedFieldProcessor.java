/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.processor.cf;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseCalculatedFieldProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<CalculatedField> calculatedFieldValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId, CalculatedFieldUpdateMsg calculatedFieldUpdateMsg) {
        boolean isCreated = false;
        boolean isNameUpdated = false;
        try {
            CalculatedField calculatedField = JacksonUtil.fromString(calculatedFieldUpdateMsg.getEntity(), CalculatedField.class, true);
            if (calculatedField == null) {
                throw new RuntimeException("[{" + tenantId + "}] calculatedFieldUpdateMsg {" + calculatedFieldUpdateMsg + " } cannot be converted to calculatedField");
            }

            CalculatedField calculatedFieldById = edgeCtx.getCalculatedFieldService().findById(tenantId, calculatedFieldId);
            if (calculatedFieldById == null) {
                calculatedField.setCreatedTime(Uuids.unixTimestamp(calculatedFieldId.getId()));
                isCreated = true;
                calculatedField.setId(null);
            } else {
                calculatedField.setId(calculatedFieldId);
            }

            String calculatedFieldName = calculatedField.getName();
            CalculatedField calculatedFieldByName = edgeCtx.getCalculatedFieldService().findByEntityIdAndName(calculatedField.getEntityId(), calculatedFieldName);
            if (calculatedFieldByName != null && !calculatedFieldByName.getId().equals(calculatedFieldId)) {
                calculatedFieldName = calculatedFieldName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] calculatedField with name {} already exists. Renaming calculatedField name to {}",
                        tenantId, calculatedField.getName(), calculatedFieldByName.getName());
                isNameUpdated = true;
            }
            calculatedField.setName(calculatedFieldName);

            calculatedFieldValidator.validate(calculatedField, CalculatedField::getTenantId);

            if (isCreated) {
                calculatedField.setId(calculatedFieldId);
            }

            edgeCtx.getCalculatedFieldService().save(calculatedField, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process calculatedField update msg [{}]", tenantId, calculatedFieldUpdateMsg, e);
            throw e;
        }
        return Pair.of(isCreated, isNameUpdated);
    }

}
