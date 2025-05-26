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
package org.thingsboard.server.dao.encryptionkey;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Service("EncryptionDaoService")
@RequiredArgsConstructor
public class EncryptionServiceImpl implements EncryptionService {

    private final Map<TenantId, BytesEncryptor> encryptorMap = new ConcurrentReferenceHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    private final EncryptionKeyDao encryptionKeyDao;

    @Override
    public byte[] encrypt(TenantId tenantId, SecretType type, byte[] value) {
        try {
            return getEncryptor(tenantId).encrypt(value);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String decryptToString(TenantId tenantId, SecretType type, byte[] encryptedValue) {
        byte[] decrypted = doDecrypt(tenantId, encryptedValue);
        return new String(decrypted, StandardCharsets.UTF_8);
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
            EncryptionKey key = encryptionKeyDao.findByTenantId(tenantId);
            return Encryptors.stronger(key.getPassword(), key.getSalt());
        });
    }

    @Override
    public void createEncryptionKey(TenantId tenantId) {
        if (encryptionKeyDao.findByTenantId(tenantId) != null) {
            return;
        }
        String password = generateSecurePassword();
        String salt = generateSalt();

        EncryptionKey key = new EncryptionKey();
        key.setTenantId(tenantId);
        key.setPassword(password);
        key.setSalt(salt);

        encryptionKeyDao.save(tenantId, key);
    }

    private String generateSecurePassword() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String generateSalt() {
        byte[] randomSalt = new byte[8];
        secureRandom.nextBytes(randomSalt);
        return HexFormat.of().formatHex(randomSalt);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        encryptionKeyDao.deleteByTenantId(tenantId);
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void handleComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (ComponentLifecycleEvent.DELETED.equals(event.getEvent())) {
            EntityId entityId = event.getEntityId();
            if (EntityType.TENANT.equals(entityId.getEntityType())) {
                encryptorMap.remove(TenantId.fromUUID(entityId.getId()));
            }
        }
    }

}
