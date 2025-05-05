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
package org.thingsboard.server.service.secret;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.secret.SecretUtilService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSecretUtilService implements SecretUtilService {

    private final Map<TenantId, BytesEncryptor> encryptorMap = new ConcurrentReferenceHashMap<>();

    private final AttributesService attributesService;

    @Override
    public byte[] encrypt(TenantId tenantId, SecretType type, byte[] value) {
        try {
            return getEncryptor(tenantId).encrypt(value);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public byte[] decrypt(TenantId tenantId, byte[] encryptedValue) {
        return doDecrypt(tenantId, encryptedValue);
    }

    @Override
    public String decryptToString(TenantId tenantId, SecretType type, byte[] encryptedValue) {
        byte[] decrypted = doDecrypt(tenantId, encryptedValue);
        return Base64.getEncoder().encodeToString(decrypted);
    }

    private byte[] doDecrypt(TenantId tenantId, byte[] encryptedValue) {
        try {
            return getEncryptor(tenantId).decrypt(encryptedValue);
        } catch (Exception e) {
            log.error("[{}] Failed to process decryption", tenantId, e);
            throw new RuntimeException("Failed to process decryption", e);
        }
    }

    private BytesEncryptor getEncryptor(TenantId tenantId) {
        return encryptorMap.computeIfAbsent(tenantId, id -> {
            AttributeKvEntry attribute;
            try {
                // TO CHANGE for separate table, IT WAS FOR TESTING
                attribute = attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, "encryption_key").get().orElse(null);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (attribute == null) {
                throw new RuntimeException("No encryption key found for tenant: " + tenantId);
            }
            JsonNode json = JacksonUtil.toJsonNode(attribute.getJsonValue().orElse(null));
            if (json == null) {
                throw new RuntimeException("Value of attribute key 'encryption_key' is not present for tenant: " + tenantId);
            }
            String password = json.get("password").asText();
            String salt = ensureHexFormat(json.get("salt").asText());
            log.error("[{}] Using password '{}' and salt '{}'", tenantId, password, salt);
            return Encryptors.stronger(password, salt);
        });
    }

    private String ensureHexFormat(String salt) {
        if (isHex(salt)) {
            return salt;
        }
        return HexFormat.of().formatHex(salt.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isHex(String value) {
        return value.matches("[0-9A-Fa-f]+");
    }

}
