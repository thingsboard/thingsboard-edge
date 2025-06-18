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
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.EncryptionKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ENCRYPTION_KEY_PASSWORD_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENCRYPTION_KEY_SALT_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENCRYPTION_KEY_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ENCRYPTION_KEY_TABLE_NAME)
public class EncryptionKeyEntity extends BaseSqlEntity<EncryptionKey> {

    @Column(name = TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = ENCRYPTION_KEY_PASSWORD_COLUMN)
    private String password;

    @Column(name = ENCRYPTION_KEY_SALT_COLUMN)
    private String salt;

    public EncryptionKeyEntity() {
        super();
    }

    public EncryptionKeyEntity(EncryptionKey encryptionKey) {
        super(encryptionKey);
        this.tenantId = encryptionKey.getTenantId().getId();
        this.password = encryptionKey.getPassword();
        this.salt = encryptionKey.getSalt();
    }

    @Override
    public EncryptionKey toData() {
        EncryptionKey encryptionKey = new EncryptionKey(new EncryptionKeyId(id));
        encryptionKey.setCreatedTime(createdTime);
        encryptionKey.setTenantId(TenantId.fromUUID(tenantId));
        encryptionKey.setPassword(password);
        encryptionKey.setSalt(salt);
        return encryptionKey;
    }

}
