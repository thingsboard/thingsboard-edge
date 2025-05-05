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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.secret.SecretUtilService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {DefaultSecretUtilService.class})
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "security.secret.password=7V9OgYiZFDoeMiYMg623EOPDocwOqfFNffF5Ds8Bmsw=",
        "security.secret.salt=randomSalt",
})
public class DefaultSecretUtilTest {

    @Autowired
    protected SecretUtilService secretUtilService;

    @ParameterizedTest
    @MethodSource("encryptionDecryptionProvider")
    public void testEncryptionDecryption(String value) {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        // Encrypt
        byte[] encryptedValue = secretUtilService.encrypt(tenantId, SecretType.TEXT, bytes);
        assertThat(encryptedValue).isNotNull();
        assertThat(new String(encryptedValue, StandardCharsets.UTF_8)).isNotEqualTo(new String(bytes, StandardCharsets.UTF_8));

        // Decrypt
        String decryptedValue = secretUtilService.decryptToString(tenantId, SecretType.TEXT, encryptedValue);
        assertThat(decryptedValue).isNotNull();
        assertThat(decryptedValue).isEqualTo(value);
    }

    @Test
    public void testEncryptionDecryptionSameValue() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        String value = "testEncryption";
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        // encrypt
        byte[] encryptedValue1 = secretUtilService.encrypt(tenantId, SecretType.TEXT, bytes);
        byte[] encryptedValue2 = secretUtilService.encrypt(tenantId, SecretType.TEXT, bytes);
        byte[] encryptedValue3 = secretUtilService.encrypt(tenantId, SecretType.TEXT, bytes);

        assertThat(Base64.getEncoder().encodeToString(encryptedValue1)).isNotEqualTo(Base64.getEncoder().encodeToString(encryptedValue2));
        assertThat(Base64.getEncoder().encodeToString(encryptedValue1)).isNotEqualTo(Base64.getEncoder().encodeToString(encryptedValue3));

        // decrypt
        assertThat(value).isEqualTo(secretUtilService.decryptToString(tenantId, SecretType.TEXT, encryptedValue1));
        assertThat(value).isEqualTo(secretUtilService.decryptToString(tenantId, SecretType.TEXT, encryptedValue2));
        assertThat(value).isEqualTo(secretUtilService.decryptToString(tenantId, SecretType.TEXT, encryptedValue3));
    }

    @Test
    public void testUniqueEncryption() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        byte[] value = "testUniqueEncryption".getBytes(StandardCharsets.UTF_8);

        byte[] encryptedValue1 = secretUtilService.encrypt(tenantId, SecretType.TEXT, value);
        byte[] encryptedValue2 = secretUtilService.encrypt(tenantId, SecretType.TEXT, value);

        assertThat(Base64.getEncoder().encodeToString(encryptedValue1)).isNotEqualTo(Base64.getEncoder().encodeToString(encryptedValue2));
    }

    @ParameterizedTest
    @MethodSource("invalidValueProvider")
    public void testDecryptionInvalidData() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        byte[] invalidEncryptedValue = "invalidEncryptedData".getBytes(StandardCharsets.UTF_8);

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
