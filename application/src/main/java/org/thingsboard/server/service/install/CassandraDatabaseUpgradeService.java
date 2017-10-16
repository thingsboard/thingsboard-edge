/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import com.datastax.driver.core.KeyspaceMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cassandra.CassandraInstallCluster;
import org.thingsboard.server.dao.util.NoSqlDao;
import org.thingsboard.server.service.install.cql.CQLStatementsParser;
import org.thingsboard.server.service.install.cql.CassandraDbHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@NoSqlDao
@Profile("install")
@Slf4j
public class CassandraDatabaseUpgradeService implements DatabaseUpgradeService {

    private static final String SCHEMA_UPDATE_CQL = "schema_update.cql";
    public static final String DEVICE = "device";
    public static final String TENANT_ID = "tenant_id";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String SEARCH_TEXT = "search_text";
    public static final String ADDITIONAL_INFO = "additional_info";
    public static final String ASSET = "asset";

    @Value("${install.data_dir}")
    private String dataDir;

    @Autowired
    private CassandraCluster cluster;

    @Autowired
    private CassandraInstallCluster installCluster;

    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {

        switch (fromVersion) {
            case "1.2.3":

                log.info("Upgrading Cassandara DataBase from version {} to 1.3.0 ...", fromVersion);

                //Dump devices, assets and relations

                KeyspaceMetadata ks = cluster.getCluster().getMetadata().getKeyspace(cluster.getKeyspaceName());

                log.info("Dumping devices ...");
                Path devicesDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), DEVICE,
                        new String[]{"id", TENANT_ID, CUSTOMER_ID, "name", SEARCH_TEXT, ADDITIONAL_INFO, "type"},
                        new String[]{"", "", "", "", "", "", "default"},
                        "tb-devices");
                log.info("Devices dumped.");

                log.info("Dumping assets ...");
                Path assetsDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), ASSET,
                        new String[]{"id", TENANT_ID, CUSTOMER_ID, "name", SEARCH_TEXT, ADDITIONAL_INFO, "type"},
                        new String[]{"", "", "", "", "", "", "default"},
                        "tb-assets");
                log.info("Assets dumped.");

                log.info("Dumping relations ...");
                Path relationsDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), "relation",
                        new String[]{"from_id", "from_type", "to_id", "to_type", "relation_type", ADDITIONAL_INFO, "relation_type_group"},
                        new String[]{"", "", "", "", "", "", "COMMON"},
                        "tb-relations");
                log.info("Relations dumped.");

                log.info("Updating schema ...");
                Path schemaUpdateFile = Paths.get(this.dataDir, "upgrade", "1.3.0", SCHEMA_UPDATE_CQL);
                loadCql(schemaUpdateFile);
                log.info("Schema updated.");

                //Restore devices, assets and relations

                log.info("Restoring devices ...");
                if (devicesDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), DEVICE,
                            new String[]{"id", TENANT_ID, CUSTOMER_ID, "name", SEARCH_TEXT, ADDITIONAL_INFO, "type"}, devicesDump);
                    Files.deleteIfExists(devicesDump);
                }
                log.info("Devices restored.");

                log.info("Dumping device types ...");
                Path deviceTypesDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), DEVICE,
                        new String[]{TENANT_ID, "type"},
                        new String[]{"", ""},
                        "tb-device-types");
                if (deviceTypesDump != null) {
                    CassandraDbHelper.appendToEndOfLine(deviceTypesDump, "DEVICE");
                }
                log.info("Device types dumped.");
                log.info("Loading device types ...");
                if (deviceTypesDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), "entity_subtype",
                            new String[]{TENANT_ID, "type", "entity_type"}, deviceTypesDump);
                    Files.deleteIfExists(deviceTypesDump);
                }
                log.info("Device types loaded.");

                log.info("Restoring assets ...");
                if (assetsDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), ASSET,
                            new String[]{"id", TENANT_ID, CUSTOMER_ID, "name", SEARCH_TEXT, ADDITIONAL_INFO, "type"}, assetsDump);
                    Files.deleteIfExists(assetsDump);
                }
                log.info("Assets restored.");

                log.info("Dumping asset types ...");
                Path assetTypesDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), ASSET,
                        new String[]{TENANT_ID, "type"},
                        new String[]{"", ""},
                        "tb-asset-types");
                if (assetTypesDump != null) {
                    CassandraDbHelper.appendToEndOfLine(assetTypesDump, "ASSET");
                }
                log.info("Asset types dumped.");
                log.info("Loading asset types ...");
                if (assetTypesDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), "entity_subtype",
                            new String[]{TENANT_ID, "type", "entity_type"}, assetTypesDump);
                    Files.deleteIfExists(assetTypesDump);
                }
                log.info("Asset types loaded.");

                log.info("Restoring relations ...");
                if (relationsDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), "relation",
                            new String[]{"from_id", "from_type", "to_id", "to_type", "relation_type", ADDITIONAL_INFO, "relation_type_group"}, relationsDump);
                    Files.deleteIfExists(relationsDump);
                }
                log.info("Relations restored.");

                break;
            case "1.4.0":

                log.info("Updating schema ...");
                schemaUpdateFile = Paths.get(this.dataDir, "upgrade", "1.4.0ee", SCHEMA_UPDATE_CQL);
                loadCql(schemaUpdateFile);
                log.info("Schema updated.");

                break;
            default:
                throw new RuntimeException("Unable to upgrade Cassandra database, unsupported fromVersion: " + fromVersion);
        }

    }

    private void loadCql(Path cql) throws Exception {
        List<String> statements = new CQLStatementsParser(cql).getStatements();
        statements.forEach(statement -> installCluster.getSession().execute(statement));
    }

}
