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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.TenantId;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service("EncryptionKeyDaoService")
public class EncryptionKeyServiceImpl implements EncryptionKeyService {

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private EncryptionKeyDao encryptionKeyDao;

    @Override
    public void createEncryptionKey(TenantId tenantId) {
        if (findByTenantId(tenantId) != null) {
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

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        encryptionKeyDao.deleteByTenantId(tenantId);
    }

    @Override
    public EncryptionKey findByTenantId(TenantId tenantId) {
        return encryptionKeyDao.findByTenantId(tenantId);
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

}
