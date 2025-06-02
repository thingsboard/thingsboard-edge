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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SECRET_DESCRIPTION_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.SECRET_NAME_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractSecretInfoEntity<T extends SecretInfo> extends BaseSqlEntity<T> implements BaseEntity<T> {

    @Column(name = TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = SECRET_NAME_COLUMN)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.SECRET_TYPE_COLUMN)
    private SecretType type;

    @Column(name = SECRET_DESCRIPTION_COLUMN)
    private String description;

    public AbstractSecretInfoEntity() {
        super();
    }

    public AbstractSecretInfoEntity(SecretInfo secretInfo) {
        super(secretInfo);
        this.tenantId = secretInfo.getTenantId().getId();
        this.name = secretInfo.getName();
        this.type = secretInfo.getType();
        this.description = secretInfo.getDescription();
    }

    protected SecretInfo toSecretInfo() {
        SecretInfo secretInfo = new SecretInfo(new SecretId(getUuid()));
        secretInfo.setCreatedTime(createdTime);
        secretInfo.setTenantId(TenantId.fromUUID(tenantId));
        secretInfo.setName(name);
        secretInfo.setType(type);
        secretInfo.setDescription(description);
        return secretInfo;
    }

}
