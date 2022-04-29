/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.TsDatabaseSchemaService;
import org.thingsboard.server.service.install.TsLatestDatabaseSchemaService;
import org.thingsboard.server.service.install.migrate.EntitiesMigrateService;
import org.thingsboard.server.service.install.migrate.TsLatestMigrateService;
import org.thingsboard.server.service.install.update.CacheCleanupService;
import org.thingsboard.server.service.install.update.DataUpdateService;

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

    @Autowired
    private EntityDatabaseSchemaService entityDatabaseSchemaService;

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

    public void performInstall() {
        try {
            if (isUpgrade) {
                log.info("Starting ThingsBoard Edge Upgrade from version {} ...", upgradeFromVersion);

                cacheCleanupService.clearCache(upgradeFromVersion);

                switch (upgradeFromVersion) {
                    case "3.3.0-EDGE": // fix because of incorrect upgrade version in 3.3.0 release
                    case "3.3.0":
                    case "3.3.1":
                        log.info("Upgrading ThingsBoard Edge from version 3.3.0 to 3.3.3 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.3.2");
                        dataUpdateService.updateData("3.3.2");
                    case "3.3.3":
                        log.info("Upgrading ThingsBoard Edge from version 3.3.3 to 3.3.3PE ...");
                        dataUpdateService.updateData("3.3.3");
                    case "3.3.4": // to 3.3.4PE
                        log.info("Upgrading ThingsBoard Edge from version 3.3.4 to 3.3.4.1 ...");
                        dataUpdateService.updateData("3.3.4");
                    case "3.3.4.1": // to 3.3.4.1PE
                        log.info("Upgrading ThingsBoard Edge from version 3.3.4 to 3.3.4PE ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.3.4");
                        dataUpdateService.updateData("3.3.4.1");
//                        log.info("Updating system data...");
//                        systemDataLoaderService.updateSystemWidgets();
                        break;
                    //TODO update CacheCleanupService on the next version upgrade

                    default:
                        throw new RuntimeException("Unable to upgrade ThingsBoard Edge, unsupported fromVersion: " + upgradeFromVersion);
                }

                log.info("Upgrade finished successfully!");

            } else {

                log.info("Starting ThingsBoard Edge Installation...");

                log.info("Installing DataBase schema for entities...");

                entityDatabaseSchemaService.createDatabaseSchema();

                log.info("Installing DataBase schema for timeseries...");

                tsDatabaseSchemaService.createDatabaseSchema();

                if (tsLatestDatabaseSchemaService != null) {
                    tsLatestDatabaseSchemaService.createDatabaseSchema();
                }

                log.info("Loading system data...");

                componentDiscoveryService.discoverComponents();

//                systemDataLoaderService.createSysAdmin();
                systemDataLoaderService.createDefaultTenantProfiles();
                systemDataLoaderService.createAdminSettings();
//                systemDataLoaderService.loadSystemWidgets();
//                systemDataLoaderService.createOAuth2Templates();
//                systemDataLoaderService.loadSystemPlugins();
//                systemDataLoaderService.loadSystemRules();

                if (loadDemo) {
//                    log.info("Loading demo data...");
//                    systemDataLoaderService.loadDemoData();
                }
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
