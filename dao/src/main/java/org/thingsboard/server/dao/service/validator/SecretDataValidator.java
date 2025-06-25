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
package org.thingsboard.server.dao.service.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SecretDataValidator extends DataValidator<Secret> {

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[^{};\\p{Cntrl}]+$");

    private static final int MAX_FILE_SIZE_BYTES = 512 * 1024; // 0.5 MB
    private static final int MAX_TEXT_SIZE_LENGTH = 255;

    @Lazy
    private final SecretService secretService;

    @Override
    protected void validateDataImpl(TenantId tenantId, Secret secret) {
        if (!VALID_NAME_PATTERN.matcher(secret.getName()).matches()) {
            throw new DataValidationException("Secret name contains unsupported characters. It must not include '{', '}', ';' or control characters.");
        }
        String value = secret.getValue();
        if (value != null) {
            if (SecretType.TEXT.equals(secret.getType())) {
                if (value.length() > MAX_TEXT_SIZE_LENGTH) {
                    throw new DataValidationException(String.format("Secret value is %d characters; exceeds maximum of %d characters", value.length(), MAX_TEXT_SIZE_LENGTH));
                }
            } else {
                int fileLength = secret.getRawValue().length;
                if (fileLength > MAX_FILE_SIZE_BYTES) {
                    throw new DataValidationException(String.format("Secret file size is %d bytes; exceeds the maximum of %d bytes", fileLength, MAX_FILE_SIZE_BYTES));
                }
            }
        }
    }

    @Override
    protected void validateCreate(TenantId tenantId, Secret secret) {
        if (secret.getValue() == null) {
            throw new DataValidationException("Secret must contain value");
        }
    }

    @Override
    protected Secret validateUpdate(TenantId tenantId, Secret secret) {
        Secret old = secretService.findSecretById(tenantId, secret.getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing secret!");
        }
        if (!old.getName().equals(secret.getName())) {
            throw new DataValidationException("Can't update secret name!");
        }
        if (!old.getType().equals(secret.getType())) {
            throw new DataValidationException("Can't update secret type!");
        }
        return old;
    }

}
