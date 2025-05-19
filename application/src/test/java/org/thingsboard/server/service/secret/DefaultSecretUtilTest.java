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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.encryptionkey.EncryptionKeyService;
import org.thingsboard.server.dao.secret.SecretUtilService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

public class DefaultSecretUtilTest {

    private EncryptionKeyService encryptionKeyService;
    private SecretUtilService secretUtilService;

    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        encryptionKeyService = Mockito.mock(EncryptionKeyService.class);

        EncryptionKey key = new EncryptionKey();
        key.setPassword("pass");
        key.setSalt("321321");
        Mockito.when(encryptionKeyService.findByTenantId(any())).thenReturn(key);
        secretUtilService = new DefaultSecretUtilService(encryptionKeyService);

        tenantId = TenantId.fromUUID(UUID.randomUUID());
    }

    @ParameterizedTest
    @MethodSource("encryptionDecryptionProvider")
    void testEncryptAndDecryptString(String original) {
        byte[] originalBytes = original.getBytes(StandardCharsets.UTF_8);

        // Encrypt
        byte[] encrypted = secretUtilService.encrypt(tenantId, SecretType.TEXT, originalBytes);
        assertThat(encrypted).isNotNull();
        assertThat(new String(encrypted)).isNotEqualTo(original);

        // Decrypt
        String decrypted = secretUtilService.decryptToString(tenantId, SecretType.TEXT, encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void testDecryptToBase64IfBinaryFile() {
        byte[] fileBytes = "binary-content".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = secretUtilService.encrypt(tenantId, SecretType.BINARY_FILE, fileBytes);
        String decryptedBase64 = secretUtilService.decryptToString(tenantId, SecretType.BINARY_FILE, encrypted);

        assertThat(decryptedBase64).isEqualTo(Base64.getEncoder().encodeToString(fileBytes));
    }

    @Test
    void testEncryptProducesDifferentOutputs() {
        String plain = "SameInput";
        byte[] bytes = plain.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted1 = secretUtilService.encrypt(tenantId, SecretType.TEXT, bytes);
        byte[] encrypted2 = secretUtilService.encrypt(tenantId, SecretType.TEXT, bytes);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @ParameterizedTest
    @MethodSource("invalidValueProvider")
    public void testDecryptionInvalidData(String invalid) {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        byte[] invalidEncryptedValue = invalid.getBytes(StandardCharsets.UTF_8);

        try {
            secretUtilService.decrypt(tenantId, invalidEncryptedValue);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Failed to process decryption");
        }
    }

    private static Stream<Arguments> encryptionDecryptionProvider() {
        return Stream.of(
                Arguments.of("testEncryptionDecryption1"),
                Arguments.of("testEncryptionDecryption2"),
                Arguments.of("testEncryptionDecryption3")
        );
    }

    private static Stream<Arguments> invalidValueProvider() {
        return Stream.of(
                Arguments.of("invalidEncryptedData1"),
                Arguments.of("invalidEncryptedData2"),
                Arguments.of("invalidEncryptedData3")
        );
    }

}
