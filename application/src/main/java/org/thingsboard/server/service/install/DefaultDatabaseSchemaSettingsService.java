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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;

import java.util.List;

@Service
@Profile("install")
@Slf4j
@RequiredArgsConstructor
public class DefaultDatabaseSchemaSettingsService implements DatabaseSchemaSettingsService {

    private static final String CURRENT_PRODUCT = "PE";
    // This list should include all versions which are compatible for the upgrade.
    // The compatibility cycle usually breaks when we have some scripts written in Java that may not work after new release.
    private static final List<String> SUPPORTED_VERSIONS_FOR_UPGRADE = List.of("3.9.0");

    private final BuildProperties buildProperties;
    private final JdbcTemplate jdbcTemplate;

    private String packageSchemaVersion;
    private String schemaVersionFromDb;

    @Override
    public void validateSchemaSettings(boolean updateFromCE) {
        if (DefaultDataUpdateService.getEnv("SKIP_SCHEMA_VERSION_CHECK", false)) {
            log.info("Skipped DB schema version check due to SKIP_SCHEMA_VERSION_CHECK set to 'true'.");
            return;
        }

        String dbSchemaVersion = getDbSchemaVersion();
        if (updateFromCE) {
            if (!dbSchemaVersion.equals(getPackageSchemaVersion())) {
                onSchemaSettingsError("Upgrade failed: transitioning from CE to PE requires the database to first be upgraded to version '"
                        + getPackageSchemaVersion() + "' using ThingsBoard CE.");
            }
        } else {
            String product = getProductFromDb();
            if (!CURRENT_PRODUCT.equals(product)) {
                onSchemaSettingsError(String.format("Upgrade failed: can't upgrade ThingsBoard %s database using ThingsBoard %s.", product, CURRENT_PRODUCT));
            }

            if (dbSchemaVersion.equals(getPackageSchemaVersion())) {
                onSchemaSettingsError("Upgrade failed: database already upgraded to current version. You can set SKIP_SCHEMA_VERSION_CHECK to 'true' if force re-upgrade needed.");
            }

            if (!SUPPORTED_VERSIONS_FOR_UPGRADE.contains(dbSchemaVersion)) {
                onSchemaSettingsError(String.format("Upgrade failed: database version '%s' is not supported for upgrade. Supported versions are: %s.",
                        dbSchemaVersion, SUPPORTED_VERSIONS_FOR_UPGRADE
                ));
            }
        }
    }

    @Override
    public void createSchemaSettings() {
        Long schemaVersion = getSchemaVersionFromDb();
        if (schemaVersion == null) {
            jdbcTemplate.execute("INSERT INTO tb_schema_settings (schema_version, product) VALUES (" + getPackageSchemaVersionForDb() + ", '" + CURRENT_PRODUCT + "')");
        }
    }

    @Override
    public void updateSchemaVersion() {
        jdbcTemplate.execute("UPDATE tb_schema_settings SET schema_version = " + getPackageSchemaVersionForDb() + ", product = '" + CURRENT_PRODUCT + "'");
    }

    @Override
    public String getPackageSchemaVersion() {
        if (packageSchemaVersion == null) {
            packageSchemaVersion = buildProperties.getVersion().replaceAll("[^\\d.]", "");
        }
        return packageSchemaVersion;
    }

    @Override
    public String getDbSchemaVersion() {
        if (schemaVersionFromDb == null) {
            Long version = getSchemaVersionFromDb();
            if (version == null) {
                onSchemaSettingsError("Upgrade failed: the database schema version is missing.");
            }

            @SuppressWarnings("DataFlowIssue")
            long major = version / 1000000;
            long minor = (version % 1000000) / 1000;
            long patch = version % 1000;

            schemaVersionFromDb = major + "." + minor + "." + patch;
        }
        return schemaVersionFromDb;
    }

    private Long getSchemaVersionFromDb() {
        return jdbcTemplate.queryForList("SELECT schema_version FROM tb_schema_settings", Long.class).stream().findFirst().orElse(null);
    }

    private String getProductFromDb() {
        return jdbcTemplate.queryForList("SELECT product FROM tb_schema_settings", String.class).stream().findFirst().orElse(null);
    }

    private long getPackageSchemaVersionForDb() {
        String[] versionParts = getPackageSchemaVersion().split("\\.");

        long major = Integer.parseInt(versionParts[0]);
        long minor = Integer.parseInt(versionParts[1]);
        long patch = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : 0;

        return major * 1000000 + minor * 1000 + patch;
    }

    private void onSchemaSettingsError(String message) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.error(message)));
        throw new RuntimeException(message);
    }
}
