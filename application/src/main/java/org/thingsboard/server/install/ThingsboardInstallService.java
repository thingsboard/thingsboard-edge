/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.service.install.DatabaseUpgradeService;
import org.thingsboard.server.service.install.EntityDatabaseSchemaService;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.TsDatabaseSchemaService;
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

    @Autowired
    private DatabaseUpgradeService databaseUpgradeService;

    @Autowired
    private ComponentDiscoveryService componentDiscoveryService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private DataUpdateService dataUpdateService;

    public void performInstall() {
        try {
            if (isUpgrade) {
                log.info("Starting ThingsBoard Upgrade from version {} ...", upgradeFromVersion);

                switch (upgradeFromVersion) {
                    case "1.2.3": //NOSONAR, Need to execute gradual upgrade starting from upgradeFromVersion
                        log.info("Upgrading ThingsBoard from version 1.2.3 to 1.3.0 ...");

                        databaseUpgradeService.upgradeDatabase("1.2.3");

                    case "1.3.0": //NOSONAR, Need to execute gradual upgrade starting from upgradeFromVersion
                        log.info("Upgrading ThingsBoard from version 1.3.0 to 1.3.1 ...");

                        databaseUpgradeService.upgradeDatabase("1.3.0");

                    case "1.3.1": //NOSONAR, Need to execute gradual upgrade starting from upgradeFromVersion
                        log.info("Upgrading ThingsBoard from version 1.3.1 to 1.4.0 ...");

                        databaseUpgradeService.upgradeDatabase("1.3.1");

                    case "1.4.0": //NOSONAR, Need to execute gradual upgrade starting from upgradeFromVersion
                        log.info("Upgrading ThingsBoard from version 1.4.0 to 2.0.0 ...");

                        databaseUpgradeService.upgradeDatabase("1.4.0");

                        dataUpdateService.updateData("1.4.0");

                    case "2.0.0":
                        log.info("Upgrading ThingsBoard from version 2.0.0 to 2.1.1 ...");

                        databaseUpgradeService.upgradeDatabase("2.0.0");

                    case "2.1.1":
                        log.info("Upgrading ThingsBoard from version 2.1.1 to 2.1.2 ...");

                        databaseUpgradeService.upgradeDatabase("2.1.1");
                    case "2.1.3":
                        log.info("Upgrading ThingsBoard from version 2.1.3 to 2.2.0 ...");

                        databaseUpgradeService.upgradeDatabase("2.1.3");

                    case "2.3.0":
                        log.info("Upgrading ThingsBoard from version 2.3.0 to 2.3.1 ...");

                        databaseUpgradeService.upgradeDatabase("2.3.0");

                    case "2.3.1":
                        log.info("Upgrading ThingsBoard from version 2.3.1 to 2.4.0 ...");

                        databaseUpgradeService.upgradeDatabase("2.3.1");

                    case "2.4.0":

                    case "2.4.1": // to 2.4.1PE
                        log.info("Upgrading ThingsBoard from version 2.4.1 to 2.4.1PE ...");

                        databaseUpgradeService.upgradeDatabase("2.4.1");

                        dataUpdateService.updateData("2.4.1");

                        log.info("Updating system data...");

                        systemDataLoaderService.deleteSystemWidgetBundle("charts");
                        systemDataLoaderService.deleteSystemWidgetBundle("cards");
                        systemDataLoaderService.deleteSystemWidgetBundle("maps");
                        systemDataLoaderService.deleteSystemWidgetBundle("analogue_gauges");
                        systemDataLoaderService.deleteSystemWidgetBundle("digital_gauges");
                        systemDataLoaderService.deleteSystemWidgetBundle("gpio_widgets");
                        systemDataLoaderService.deleteSystemWidgetBundle("alarm_widgets");
                        systemDataLoaderService.deleteSystemWidgetBundle("control_widgets");
                        systemDataLoaderService.deleteSystemWidgetBundle("maps_v2");
                        systemDataLoaderService.deleteSystemWidgetBundle("gateway_widgets");
                        systemDataLoaderService.deleteSystemWidgetBundle("scheduling");
                        systemDataLoaderService.deleteSystemWidgetBundle("files");
                        systemDataLoaderService.deleteSystemWidgetBundle("input_widgets");
                        systemDataLoaderService.deleteSystemWidgetBundle("date");

                        systemDataLoaderService.loadSystemWidgets();
                        break;
                    default:
                        throw new RuntimeException("Unable to upgrade ThingsBoard, unsupported fromVersion: " + upgradeFromVersion);

                }
                log.info("Upgrade finished successfully!");

            } else {

                log.info("Starting ThingsBoard Installation...");

                log.info("Installing DataBase schema for entities...");

                entityDatabaseSchemaService.createDatabaseSchema();

                log.info("Installing DataBase schema for timeseries...");

                tsDatabaseSchemaService.createDatabaseSchema();

                log.info("Loading system data...");

                componentDiscoveryService.discoverComponents();

                systemDataLoaderService.createSysAdmin();
                systemDataLoaderService.createAdminSettings();
                systemDataLoaderService.loadSystemWidgets();
//                systemDataLoaderService.loadSystemPlugins();
//                systemDataLoaderService.loadSystemRules();

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
