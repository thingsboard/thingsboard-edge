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
package org.thingsboard.migrator.tenant;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static java.util.List.of;

@Getter
public enum Table {
    TENANT("tenant", "id"),
    CUSTOMER("customer"),
    ADMIN_SETTINGS("admin_settings", tenantId -> {
        return "SELECT * FROM admin_settings WHERE (key LIKE 'loginWhiteLabelDomainNamePrefix%' AND json_value LIKE '%" + tenantId + "%') OR ";
    }),
    QUEUE("queue"),
    RPC("rpc"),
    RULE_CHAIN("rule_chain"),
    DEVICE_PROFILE("device_profile"),
    OTA_PACKAGE("ota_package"),
    RESOURCE("resource"),
    API_USAGE_STATE("api_usage_state"),
    ROLE("role"),
    ENTITY_GROUP("entity_group", Pair.of(
            "owner_id", of(TENANT, CUSTOMER)
    )),
    DEVICE_GROUP_OTA_PACKAGE("device_group_ota_package", Pair.of(
            "ota_package_id", of(OTA_PACKAGE)
    )),
    GROUP_PERMISSION("group_permission", tenantId -> {
        return "SELECT group_permission.*, role.name as role_name FROM group_permission INNER JOIN role " +
                "ON role_id = role.id WHERE ";
    }),
    BLOB_ENTITY("blob_entity", true, "created_time", "blob_entity"),
    SCHEDULER_EVENT("scheduler_event"),
    RULE_CHAIN_DEBUG_EVENT("rule_chain_debug_event", true, "ts", "debug_event"),
    RULE_NODE("rule_node", Pair.of(
            "rule_chain_id", of(RULE_CHAIN)
    )),
    RULE_NODE_DEBUG_EVENT("rule_node_debug_event", true, "ts", "debug_event"),

    CONVERTER("converter"),
    CONVERTER_DEBUG_EVENT("converter_debug_event", true, "ts", "debug_event"),
    INTEGRATION("integration"),
    INTEGRATION_DEBUG_EVENT("integration_debug_event", true, "ts", "debug_event"),
    USER("tb_user"),
    USER_CREDENTIALS("user_credentials", Pair.of(
            "user_id", of(USER)
    )),
    USER_AUTH_SETTINGS("user_auth_settings", Pair.of(
            "user_id", of(USER)
    )),
    EDGE("edge"),
    EDGE_EVENT("edge_event", true, "created_time", "edge_event"),
    WIDGETS_BUNDLE("widgets_bundle"),
    WIDGET_TYPE("widget_type"),
    DASHBOARD("dashboard"),
    DEVICE("device"),
    DEVICE_CREDENTIALS("device_credentials", Pair.of(
            "device_id", of(DEVICE)
    )),
    ASSET_PROFILE("asset_profile"),
    ASSET("asset"),
    ENTITY_VIEW("entity_view"),
    ALARM("alarm"),
    ENTITY_ALARM("entity_alarm", List.of("created_time", "entity_id")),
    ERROR_EVENT("error_event", true, "ts", "event"),
    LC_EVENT("lc_event", true, "ts", "event"),
    RAW_DATA_EVENT("raw_data_event", true, "ts", "event"),
    STATS_EVENT("stats_event", true, "ts", "event"),
    OAUTH2_PARAMS("oauth2_params"),
    OAUTH2_DOMAIN("oauth2_domain", Pair.of(
            "oauth2_params_id", of(OAUTH2_PARAMS)
    )),
    OAUTH2_MOBILE("oauth2_mobile", Pair.of(
            "oauth2_params_id", of(OAUTH2_PARAMS)
    )),
    OAUTH2_REGISTRATION("oauth2_registration", Pair.of(
            "oauth2_params_id", of(OAUTH2_PARAMS)
    )),
    RULE_NODE_STATE("rule_node_state", Pair.of(
            "entity_id", of(DEVICE, TENANT, CUSTOMER, RULE_CHAIN, DEVICE_PROFILE, ROLE, ENTITY_GROUP, RULE_NODE, CONVERTER,
                    INTEGRATION, USER, EDGE, DASHBOARD, ASSET_PROFILE, ASSET, ENTITY_VIEW)
    )),
    AUDIT_LOG("audit_log", true, "created_time", "audit_log"),
    RELATION("relation", Pair.of(
            "from_id", of(TENANT, CUSTOMER, RULE_CHAIN, DEVICE_PROFILE, ROLE, ENTITY_GROUP, RULE_NODE, CONVERTER, OTA_PACKAGE,
                    INTEGRATION, USER, EDGE, DASHBOARD, DEVICE, ASSET_PROFILE, ASSET, ENTITY_VIEW, ALARM, SCHEDULER_EVENT, GROUP_PERMISSION)
    ), List.of("from_type", "from_id", "to_id")),
    ATTRIBUTE("attribute_kv", Pair.of(
            "entity_id", of(TENANT, CUSTOMER, RULE_CHAIN, DEVICE_PROFILE, ROLE, ENTITY_GROUP, RULE_NODE, CONVERTER, OTA_PACKAGE,
                    INTEGRATION, USER, EDGE, DASHBOARD, DEVICE, ASSET_PROFILE, ASSET, ENTITY_VIEW, ALARM, SCHEDULER_EVENT, GROUP_PERMISSION, API_USAGE_STATE)
    ), List.of("last_update_ts", "entity_id", "attribute_key")),

    LATEST_KV("ts_kv_latest", Pair.of(
            "entity_id", of(TENANT, CUSTOMER, RULE_CHAIN, DEVICE_PROFILE, ROLE, ENTITY_GROUP, RULE_NODE, CONVERTER, OTA_PACKAGE,
                    INTEGRATION, USER, EDGE, DASHBOARD, DEVICE, ASSET_PROFILE, ASSET, ENTITY_VIEW, ALARM, SCHEDULER_EVENT, GROUP_PERMISSION, API_USAGE_STATE)
    ), List.of("entity_id", "key"), tenantId -> {
        return "SELECT ts_kv_latest.*, dict.key as key_name FROM ts_kv_latest " +
                "INNER JOIN ts_kv_dictionary dict ON ts_kv_latest.key = dict.key_id WHERE ";
    });

    private String name;
    private List<String> sortColumns = List.of("id");
    private String tenantIdColumn = "tenant_id";
    private Pair<String, List<Table>> reference;
    private Function<UUID, String> customSelect;
    private boolean partitioned = false;
    private String partitionColumn;
    private String partitionSizeSettingsKey;

    Table(String name) {
        this.name = name;
    }

    Table(String name, Function<UUID, String> customSelect) {
        this.name = name;
        this.customSelect = customSelect;
    }

    Table(String name, boolean partitioned, String partitionColumn, String settingsKey) {
        this.name = name;
        this.partitioned = partitioned;
        this.partitionColumn = partitionColumn;
        this.partitionSizeSettingsKey = settingsKey;
        this.sortColumns = List.of(partitionColumn + " DESC", "id");
    }

    Table(String name, List<String> sortColumns) {
        this.name = name;
        this.sortColumns = sortColumns;
    }

    Table(String name, String tenantIdColumn) {
        this.name = name;
        this.tenantIdColumn = tenantIdColumn;
    }

    Table(String name, Pair<String, List<Table>> reference) {
        this.name = name;
        this.reference = reference;
    }

    Table(String name, Pair<String, List<Table>> reference, List<String> sortColumns) {
        this.name = name;
        this.reference = reference;
        this.sortColumns = sortColumns;
    }

    Table(String name, Pair<String, List<Table>> reference, List<String> sortColumns,
          Function<UUID, String> customSelect) {
        this.name = name;
        this.reference = reference;
        this.sortColumns = sortColumns;
        this.customSelect = customSelect;
    }

}
