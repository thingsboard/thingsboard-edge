/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.service.install.DatabaseTsUpgradeService;
import org.thingsboard.server.service.install.EntityDatabaseSchemaService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.NoSqlKeyspaceService;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.TsDatabaseSchemaService;
import org.thingsboard.server.service.install.TsLatestDatabaseSchemaService;
import org.thingsboard.server.service.install.migrate.EntitiesMigrateService;
import org.thingsboard.server.service.install.migrate.TsLatestMigrateService;
import org.thingsboard.server.service.install.update.CacheCleanupService;
import org.thingsboard.server.service.install.update.DataUpdateService;

import static org.thingsboard.server.service.install.update.DefaultDataUpdateService.getEnv;

@Service
@Profile("install")
@Slf4j
public class ThingsboardInstallService {

    @Value("${install.upgrade:false}")
    private Boolean isUpgrade;

    @Value("${install.upgrade.from_version:1.3.0}")
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

    @Autowired(required = false)
    private DatabaseTsUpgradeService databaseTsUpgradeService;

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
    private EntitiesMigrateService entitiesMigrateService;

    @Autowired(required = false)
    private TsLatestMigrateService latestMigrateService;

    @Autowired
    private InstallScripts installScripts;

    public void performInstall() {
        try {
            if (isUpgrade) {
                log.info("Starting ThingsBoard Upgrade from version {} ...", upgradeFromVersion);

                cacheCleanupService.clearCache(upgradeFromVersion);

                if ("2.5.0PE-cassandra".equals(upgradeFromVersion)) {
                    log.info("Migrating ThingsBoard entities data from cassandra to SQL database ...");
                    entitiesMigrateService.migrate();

                    dataUpdateService.updateData("3.0.0");

                    log.info("Updating system data...");
                    systemDataLoaderService.updateSystemWidgets();
                } else if ("3.0.1-cassandra".equals(upgradeFromVersion)) {
                    log.info("Migrating ThingsBoard latest timeseries data from cassandra to SQL database ...");
                    latestMigrateService.migrate();
                } else {
                    switch (upgradeFromVersion) {
                        case "1.2.3": //NOSONAR, Need to execute gradual upgrade starting from upgradeFromVersion
                            log.info("Upgrading ThingsBoard from version 1.2.3 to 1.3.0 ...");

                            databaseEntitiesUpgradeService.upgradeDatabase("1.2.3");

                        case "1.3.0":  //NOSONAR, Need to execute gradual upgrade starting from upgradeFromVersion
                            log.info("Upgrading ThingsBoard from version 1.3.0 to 1.3.1 ...");

                            databaseEntitiesUpgradeService.upgradeDatabase("1.3.0");

                        case "1.3.1": //NOSONAR, Need to execute gradual upgrade starting from upgradeFromVersion
                            log.info("Upgrading ThingsBoard from version 1.3.1 to 1.4.0 ...");

                            databaseEntitiesUpgradeService.upgradeDatabase("1.3.1");

                        case "1.4.0":
                            log.info("Upgrading ThingsBoard from version 1.4.0 to 2.0.0 ...");

                            databaseEntitiesUpgradeService.upgradeDatabase("1.4.0");

                            dataUpdateService.updateData("1.4.0");

                        case "2.0.0":
                            log.info("Upgrading ThingsBoard from version 2.0.0 to 2.1.1 ...");

                            databaseEntitiesUpgradeService.upgradeDatabase("2.0.0");

                        case "2.1.1":
                            log.info("Upgrading ThingsBoard from version 2.1.1 to 2.1.2 ...");

                            databaseEntitiesUpgradeService.upgradeDatabase("2.1.1");
                        case "2.1.3":
                            log.info("Upgrading ThingsBoard from version 2.1.3 to 2.2.0 ...");

                            databaseEntitiesUpgradeService.upgradeDatabase("2.1.3");

                        case "2.3.0":
                            log.info("Upgrading ThingsBoard from version 2.3.0 to 2.3.1 ...");

                            databaseEntitiesUpgradeService.upgradeDatabase("2.3.0");

                        case "2.3.1":
                            log.info("Upgrading ThingsBoard from version 2.3.1 to 2.4.0 ...");

                            databaseEntitiesUpgradeService.upgradeDatabase("2.3.1");

                        case "2.4.0":
                            log.info("Upgrading ThingsBoard from version 2.4.0 to 2.4.1 ...");

                        case "2.4.1":
                            log.info("Upgrading ThingsBoard from version 2.4.1 to 2.4.2 ...");

                            databaseEntitiesUpgradeService.upgradeDatabase("2.4.1");
                        case "2.4.2":
                            log.info("Upgrading ThingsBoard from version 2.4.2 to 2.4.3 ...");

                            databaseEntitiesUpgradeService.upgradeDatabase("2.4.2");

                        case "2.4.3":
                            log.info("Upgrading ThingsBoard from version 2.4.3 to 2.5 ...");

                            if (databaseTsUpgradeService != null) {
                                databaseTsUpgradeService.upgradeDatabase("2.4.3");
                            }
                            databaseEntitiesUpgradeService.upgradeDatabase("2.4.3");

                        case "2.5.0":
                            log.info("Upgrading ThingsBoard from version 2.5.0 to 2.5.1 ...");
                            if (databaseTsUpgradeService != null) {
                                databaseTsUpgradeService.upgradeDatabase("2.5.0");
                            }
                        case "2.5.1":
                            log.info("Upgrading ThingsBoard from version 2.5.1 to 3.0.0 ...");
                        case "3.0.1":
                            log.info("Upgrading ThingsBoard from version 3.0.1 to 3.1.0 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.0.1");
                            dataUpdateService.updateData("3.0.1");
                        case "3.1.0":
                            log.info("Upgrading ThingsBoard from version 3.1.0 to 3.1.1 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.1.0");
                        case "3.1.1":
                            log.info("Upgrading ThingsBoard from version 3.1.1 to 3.2.0 ...");
                            if (databaseTsUpgradeService != null) {
                                databaseTsUpgradeService.upgradeDatabase("3.1.1");
                            }
                            databaseEntitiesUpgradeService.upgradeDatabase("3.1.1");
                            dataUpdateService.updateData("3.1.1");
                            systemDataLoaderService.createOAuth2Templates();
                        case "3.2.0":
                            log.info("Upgrading ThingsBoard from version 3.2.0 to 3.2.1 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.2.0");
                        case "3.2.1":
                            log.info("Upgrading ThingsBoard from version 3.2.1 to 3.2.2 ...");
                            if (databaseTsUpgradeService != null) {
                                databaseTsUpgradeService.upgradeDatabase("3.2.1");
                            }
                            databaseEntitiesUpgradeService.upgradeDatabase("3.2.1");
                        case "3.2.2":
                            log.info("Upgrading ThingsBoard from version 3.2.2 to 3.3.0 ...");
                            if (databaseTsUpgradeService != null) {
                                databaseTsUpgradeService.upgradeDatabase("3.2.2");
                            }
                            databaseEntitiesUpgradeService.upgradeDatabase("3.2.2");

                            dataUpdateService.updateData("3.2.2");
                            systemDataLoaderService.createOAuth2Templates();
                        case "3.3.0":
                            log.info("Upgrading ThingsBoard from version 3.3.0 to 3.3.1 ...");
                        case "3.3.1":
                            log.info("Upgrading ThingsBoard from version 3.3.1 to 3.3.2 ...");
                        case "3.3.2":
                            log.info("Upgrading ThingsBoard from version 3.3.2 to 3.3.3 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.3.2");
                            dataUpdateService.updateData("3.3.2");
                        case "3.3.3":
                            log.info("Upgrading ThingsBoard from version 3.3.3 to 3.3.4 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.3.3");
                        case "3.3.4":
                            log.info("Upgrading ThingsBoard from version 3.3.4 to 3.4.0 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.3.4");
                            dataUpdateService.updateData("3.3.4");
                        case "3.4.0":
                            log.info("Upgrading ThingsBoard from version 3.4.0 to 3.4.1 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.4.0");
                            dataUpdateService.updateData("3.4.0");
                        case "3.4.1":
                            log.info("Upgrading ThingsBoard from version 3.4.1 to 3.4.2 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.4.1");
                            dataUpdateService.updateData("3.4.1");
                        case "3.4.2":
                            log.info("Upgrading ThingsBoard from version 3.4.2 to 3.4.3 ...");
                        case "3.4.3":
                            log.info("Upgrading ThingsBoard from version 3.4.3 to 3.4.4 ...");
                        case "3.4.4":
                            log.info("Upgrading ThingsBoard from version 3.4.4 to 3.5.0 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.4.4");
                            if (!getEnv("SKIP_DEFAULT_NOTIFICATION_CONFIGS_CREATION", false)) {
                                systemDataLoaderService.createDefaultNotificationConfigs();
                            } else {
                                log.info("Skipping default notification configs creation");
                            }
                        case "3.5.0":
                            log.info("Upgrading ThingsBoard from version 3.5.0 to 3.5.1 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.5.0");
                            dataUpdateService.updateData("3.5.0");
                        case "3.5.1": // to 3.5.1PE
                            log.info("Upgrading ThingsBoard from version 3.5.1 to 3.5.1PE ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.5.1");
                            entityDatabaseSchemaService.createOrUpdateViewsAndFunctions();
                            entityDatabaseSchemaService.createOrUpdateDeviceInfoView(persistToTelemetry);
                            dataUpdateService.updateData("3.5.1");
                            log.info("Updating system data...");
                            systemDataLoaderService.updateSystemWidgets();
                            installScripts.loadSystemLwm2mResources();
                            break;
                            //TODO update CacheCleanupService on the next version upgrade
                        default:
                            throw new RuntimeException("Unable to upgrade ThingsBoard, unsupported fromVersion: " + upgradeFromVersion);

                    }
                }
                log.info("Upgrade finished successfully!");

            } else {

                log.info("Starting ThingsBoard Installation...");

                log.info("Installing DataBase schema for entities...");

                entityDatabaseSchemaService.createDatabaseSchema();

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

                systemDataLoaderService.createSysAdmin();
                systemDataLoaderService.createDefaultTenantProfiles();
                systemDataLoaderService.createAdminSettings();
                systemDataLoaderService.createRandomJwtSettings();
                systemDataLoaderService.loadSystemWidgets();
                systemDataLoaderService.createOAuth2Templates();
                systemDataLoaderService.createQueues();
                systemDataLoaderService.createDefaultNotificationConfigs();

//                systemDataLoaderService.loadSystemPlugins();
//                systemDataLoaderService.loadSystemRules();
                installScripts.loadSystemLwm2mResources();

                if (loadDemo) {
                    log.info("Loading demo data...");
                    systemDataLoaderService.loadDemoData();
                }
                log.info("Installation finished successfully!");
            }


        } catch (Exception e) {
            log.error("Unexpected error during ThingsBoard installation!", e);
            throw new ThingsboardInstallException("Unexpected error during ThingsBoard installation!", e);
        } finally {
            SpringApplication.exit(context);
        }
    }

}

