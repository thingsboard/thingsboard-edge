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
package org.thingsboard.server.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.install.DatabaseEntitiesUpgradeService;
import org.thingsboard.server.service.install.DatabaseSchemaSettingsService;
import org.thingsboard.server.service.install.EntityDatabaseSchemaService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.NoSqlKeyspaceService;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.TsDatabaseSchemaService;
import org.thingsboard.server.service.install.TsLatestDatabaseSchemaService;
import org.thingsboard.server.service.install.migrate.TsLatestMigrateService;
import org.thingsboard.server.service.install.update.CacheCleanupService;
import org.thingsboard.server.service.install.update.DataUpdateService;

@Service
@Profile("install")
@Slf4j
public class ThingsboardInstallService {

    @Value("${install.upgrade:false}")
    private Boolean isUpgrade;

    @Value("${install.upgrade.from_version:}")
    private String upgradeFromVersion;

    @Value("${install.load_demo:false}")
    private Boolean loadDemo;

    @Value("${state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    @Autowired
    private EntityDatabaseSchemaService entityDatabaseSchemaService;

    @Autowired(required = false)
    private NoSqlKeyspaceService noSqlKeyspaceService;

    @Autowired
    private TsDatabaseSchemaService tsDatabaseSchemaService;

    @Autowired(required = false)
    private TsLatestDatabaseSchemaService tsLatestDatabaseSchemaService;

    @Autowired
    private DatabaseEntitiesUpgradeService databaseEntitiesUpgradeService;

    @Autowired
    private ComponentDiscoveryService componentDiscoveryService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private DataUpdateService dataUpdateService;

    @Autowired
    private CacheCleanupService cacheCleanupService;

    @Autowired(required = false)
    private TsLatestMigrateService latestMigrateService;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private DatabaseSchemaSettingsService databaseSchemaVersionService;

    public void performInstall() {
        try {
            if (isUpgrade) {
                if ("cassandra-latest-to-postgres".equals(upgradeFromVersion)) {
                    log.info("Migrating ThingsBoard latest timeseries data from cassandra to SQL database ...");
                    latestMigrateService.migrate();
                } else {
                    // TODO DON'T FORGET to update SUPPORTED_VERSIONS_FROM in DefaultDatabaseSchemaSettingsService
                    var updateFromCE = "CE".equals(upgradeFromVersion);
                    databaseSchemaVersionService.validateSchemaSettings(updateFromCE);
                    if (updateFromCE) {
                        log.info("Upgrading ThingsBoard Edge from version CE to PE ...");
                    } else {
                        String fromVersion = databaseSchemaVersionService.getDbSchemaVersion();
                        String toVersion = databaseSchemaVersionService.getPackageSchemaVersion();
                        log.info("Upgrading ThingsBoard Edge from version {} to {} ...", fromVersion, toVersion);
                    }
                    cacheCleanupService.clearCache();
                    // Apply the schema_update.sql script. The script may include DDL statements to change structure
                    // of *existing* tables and DML statements to manipulate the DB records.
                    databaseEntitiesUpgradeService.upgradeDatabase(updateFromCE);
                    // All new tables that do not have any data will be automatically created here.
                    entityDatabaseSchemaService.createDatabaseSchema(false);
                    // Re-create all views, functions.
                    entityDatabaseSchemaService.createOrUpdateViewsAndFunctions();
                    entityDatabaseSchemaService.createOrUpdateDeviceInfoView(persistToTelemetry);
                    // Creates missing indexes.
                    entityDatabaseSchemaService.createDatabaseIndexes();

                    // TODO: cleanup update code after each release

                    // Runs upgrade scripts that are not possible in plain SQL.
                    dataUpdateService.updateData(updateFromCE);
                    log.info("Updating system data...");
                    // dataUpdateService.upgradeRuleNodes();
                    // systemDataLoaderService.loadSystemWidgets();
                    // installScripts.loadSystemLwm2mResources();
                    installScripts.loadSystemImagesAndResources();
                    // systemDataLoaderService.createDefaultCustomMenu();
                    // installScripts.updateSystemNotificationTemplates();
                    databaseSchemaVersionService.updateSchemaVersion();
                }

                log.info("Upgrade finished successfully!");

            } else {

                log.info("Starting ThingsBoard Edge Installation...");

                log.info("Installing DataBase schema for entities...");

                entityDatabaseSchemaService.createDatabaseSchema();
                databaseSchemaVersionService.createSchemaSettings();

                entityDatabaseSchemaService.createOrUpdateViewsAndFunctions();
                entityDatabaseSchemaService.createOrUpdateDeviceInfoView(persistToTelemetry);

                log.info("Installing DataBase schema for timeseries...");

                if (noSqlKeyspaceService != null) {
                    noSqlKeyspaceService.createDatabaseSchema();
                }

                tsDatabaseSchemaService.createDatabaseSchema();

                if (tsLatestDatabaseSchemaService != null) {
                    tsLatestDatabaseSchemaService.createDatabaseSchema();
                }

                log.info("Loading system data...");

                componentDiscoveryService.discoverComponents();

                // systemDataLoaderService.createSysAdmin();
                systemDataLoaderService.createDefaultTenantProfiles();
                // systemDataLoaderService.createAdminSettings();
                systemDataLoaderService.createRandomJwtSettings();
                // systemDataLoaderService.loadSystemWidgets();
                // systemDataLoaderService.createOAuth2Templates();
                // systemDataLoaderService.createQueues();
                // systemDataLoaderService.createDefaultNotificationConfigs();
                // installScripts.updateSystemNotificationTemplates();
                // systemDataLoaderService.createDefaultCustomMenu();

                // systemDataLoaderService.loadSystemPlugins();
                // systemDataLoaderService.loadSystemRules();
                // installScripts.loadSystemLwm2mResources();
                installScripts.loadSystemImagesAndResources();

                /* loadDemo: not supported on edge
                if (loadDemo) {
                    log.info("Loading demo data...");
                    systemDataLoaderService.loadDemoData();
                }
                 */
                log.info("Installation finished successfully!");
            }
        } catch (Exception e) {
            log.error("Unexpected error during ThingsBoard Edge installation!", e);
            throw new ThingsboardInstallException("Unexpected error during ThingsBoard Edge installation!", e);
        } finally {
            SpringApplication.exit(context);
        }
    }

}
