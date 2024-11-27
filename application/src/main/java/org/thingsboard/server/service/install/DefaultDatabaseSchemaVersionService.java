/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.install;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;

import java.util.List;

@Service
@Profile("install")
@Slf4j
@RequiredArgsConstructor
public class DefaultDatabaseSchemaVersionService implements DatabaseSchemaVersionService {

    private static final String CURRENT_PRODUCT = "PE";
    private static final List<String> SUPPORTED_VERSIONS_FOR_UPGRADE = List.of("3.8.0", "3.8.1");

    private final BuildProperties buildProperties;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String validateSchemaSettings(String upgradeFromVersion) {
        //TODO: remove after release (3.9.0)
        createProductIfNotExists();

        if (StringUtils.isNotEmpty(upgradeFromVersion) && DefaultDataUpdateService.getEnv("SKIP_SCHEMA_VERSION_CHECK", false)) {
            log.info("Skipped DB schema version check due to SKIP_SCHEMA_VERSION_CHECK set to 'true'.");
            return upgradeFromVersion;
        }

        String product = getProductFromDb();
        if (!"CE".equals(upgradeFromVersion) && !CURRENT_PRODUCT.equals(product)) {
            onSchemaSettingsError("Upgrade failed: ThingsBoard " + product + " database using ThingsBoard " + CURRENT_PRODUCT);
        }

        Long schemaVersionFromDb = getSchemaVersionFromDb();
        if (schemaVersionFromDb == null) {
            onSchemaSettingsError("Upgrade failed: the database schema version is missing.");
        }

        long currentSchemaVersion = getCurrentSchemaVersion();

        if ("CE".equals(upgradeFromVersion)) {
            if (currentSchemaVersion == schemaVersionFromDb) {
                return upgradeFromVersion;
            } else {
                onSchemaSettingsError("Upgrade failed: transitioning from CE to PE requires the database to first be upgraded to version '"
                        + currentSchemaVersion + "' using ThingsBoard CE.");
            }
        }

        if (currentSchemaVersion == schemaVersionFromDb) {
            onSchemaSettingsError("Upgrade failed: database already upgraded to current version. You can set SKIP_SCHEMA_VERSION_CHECK to 'true' if force re-upgrade needed.");
        }

        long major = schemaVersionFromDb / 1000000;
        long minor = (schemaVersionFromDb % 1000000) / 1000;
        long patch = schemaVersionFromDb % 1000;

        String currentSchemaVersionFromDb = major + "." + minor + "." + patch;

        if (!SUPPORTED_VERSIONS_FOR_UPGRADE.contains(currentSchemaVersionFromDb)) {
            onSchemaSettingsError(String.format("Upgrade failed: database version '%s' is not supported for upgrade. Supported versions are: %s.",
                    currentSchemaVersionFromDb, SUPPORTED_VERSIONS_FOR_UPGRADE
            ));
        }

        if (StringUtils.isEmpty(upgradeFromVersion)) {
            upgradeFromVersion = currentSchemaVersionFromDb;
        } else if (!SUPPORTED_VERSIONS_FOR_UPGRADE.contains(upgradeFromVersion)) {
            onSchemaSettingsError(String.format("Upgrade failed: 'versionFrom' '%s' is not supported for upgrade. Supported versions are: %s.",
                    upgradeFromVersion, SUPPORTED_VERSIONS_FOR_UPGRADE));
        }

        return upgradeFromVersion;
    }

    @Deprecated(forRemoval = true, since = "3.9.0")
    private void createProductIfNotExists() {
        boolean isCommunityEdition;
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM information_schema.tables WHERE table_name = 'integration'", Integer.class);
            isCommunityEdition = false;
        } catch (EmptyResultDataAccessException e) {
            isCommunityEdition = true;
        }
        String product = isCommunityEdition ? "CE" : "PE";
        jdbcTemplate.execute("ALTER TABLE tb_schema_settings ADD COLUMN IF NOT EXISTS product varchar(2) DEFAULT '" + product + "'");
    }

    @Override
    public void createSchemaSettings() {
        Long schemaVersion = getSchemaVersionFromDb();
        if (schemaVersion == null) {
            jdbcTemplate.execute("INSERT INTO tb_schema_settings (schema_version, product) VALUES (" + getCurrentSchemaVersion() + ", '" + CURRENT_PRODUCT + "')");
        }
    }

    @Override
    public void updateSchemaVersion() {
        jdbcTemplate.execute("UPDATE tb_schema_settings SET schema_version = " + getCurrentSchemaVersion() + ", product = '" + CURRENT_PRODUCT + "'");
    }

    private Long getSchemaVersionFromDb() {
        return jdbcTemplate.queryForList("SELECT schema_version FROM tb_schema_settings", Long.class).stream().findFirst().orElse(null);
    }

    private String getProductFromDb() {
        return jdbcTemplate.queryForList("SELECT product FROM tb_schema_settings", String.class).stream().findFirst().orElse(null);
    }

    private long getCurrentSchemaVersion() {
        String[] versionParts = buildProperties.getVersion().replaceAll("[^\\d.]", "").split("\\.");

        long major = Integer.parseInt(versionParts[0]);
        long minor = Integer.parseInt(versionParts[1]);
        long patch = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : 0;

        return major * 1000000 + minor * 1000 + patch;
    }

    private void onSchemaSettingsError(String message) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.info(message)));
        throw new RuntimeException(message);
    }
}
