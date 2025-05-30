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
package org.thingsboard.server.common.data.encryptionkey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EncryptionKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class EncryptionKey extends BaseData<EncryptionKeyId> implements HasTenantId {

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the secret cannot be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @NoXss
    @NotBlank
    @Length(fieldName = "password")
    @Schema(description = "Encryption key password", requiredMode = Schema.RequiredMode.REQUIRED, example = "Password")
    private String password;

    @NoXss
    @NotBlank
    @Length(fieldName = "salt")
    @Schema(description = "Encryption key salt", requiredMode = Schema.RequiredMode.REQUIRED, example = "Salt")
    private String salt;

    public EncryptionKey() {
        super();
    }

    public EncryptionKey(EncryptionKeyId id) {
        super(id);
    }

    public EncryptionKey(EncryptionKey encryptionKey) {
        super(encryptionKey);
        this.tenantId = encryptionKey.getTenantId();
        this.password = encryptionKey.getPassword();
        this.salt = encryptionKey.getSalt();
    }

}
