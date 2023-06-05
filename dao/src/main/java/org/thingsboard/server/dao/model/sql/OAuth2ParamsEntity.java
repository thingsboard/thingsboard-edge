/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Params;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.OAUTH2_PARAMS_TABLE_NAME)
@NoArgsConstructor
public class OAuth2ParamsEntity extends BaseSqlEntity<OAuth2Params> {

    @Column(name = ModelConstants.OAUTH2_PARAMS_ENABLED_PROPERTY)
    private Boolean enabled;

    @Column(name = ModelConstants.OAUTH2_PARAMS_TENANT_ID_PROPERTY)
    private UUID tenantId;

    public OAuth2ParamsEntity(OAuth2Params oauth2Params) {
        if (oauth2Params.getId() != null) {
            this.setUuid(oauth2Params.getUuidId());
        }
        this.setCreatedTime(oauth2Params.getCreatedTime());
        this.enabled = oauth2Params.isEnabled();
        if (oauth2Params.getTenantId() != null) {
            this.tenantId = oauth2Params.getTenantId().getId();
        }
    }

    @Override
    public OAuth2Params toData() {
        OAuth2Params oauth2Params = new OAuth2Params();
        oauth2Params.setId(new OAuth2ParamsId(id));
        oauth2Params.setCreatedTime(createdTime);
        oauth2Params.setTenantId(TenantId.fromUUID(tenantId));
        oauth2Params.setEnabled(enabled);
        return oauth2Params;
    }
}
