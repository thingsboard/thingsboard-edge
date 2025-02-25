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
package org.thingsboard.migrator;

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
    ADMIN_SETTINGS("admin_settings"),
    CUSTOM_MENU("custom_menu"),
    QUEUE("queue"),
    RPC("rpc"),
    RULE_CHAIN("rule_chain"),
    OTA_PACKAGE("ota_package"),
    RESOURCE("resource"),
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
    WIDGETS_BUNDLE_WIDGET("widgets_bundle_widget", Pair.of(
            "widgets_bundle_id", of(WIDGETS_BUNDLE)
    ), of("widget_type_id")),
    DASHBOARD("dashboard"),
    DEVICE_PROFILE("device_profile"),
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
    DOMAIN("domain"),
    MOBILE_APP("mobile_app"),
    MOBILE_APP_BUNDLE("mobile_app_bundle"),
    OAUTH2_CLIENT("oauth2_client"),
    DOMAIN_OAUTH2_CLIENT("domain_oauth2_client", Pair.of(
            "oauth2_client_id", of(OAUTH2_CLIENT)
    ), of("domain_id", "oauth2_client_id")),
    MOBILE_APP_BUNDLE_OAUTH2_CLIENT("mobile_app_bundle_oauth2_client", Pair.of(
            "oauth2_client_id", of(OAUTH2_CLIENT)
    ), of("mobile_app_bundle_id", "oauth2_client_id")),
    RULE_NODE_STATE("rule_node_state", Pair.of(
            "entity_id", of(DEVICE)
    )),
    AUDIT_LOG("audit_log", true, "created_time", "audit_log"),
    USER_SETTINGS("user_settings", Pair.of(
            "user_id", of(USER)
    ), of("user_id")),
    NOTIFICATION_TARGET("notification_target"),
    NOTIFICATION_TEMPLATE("notification_template"),
    NOTIFICATION_RULE("notification_rule"),
    WHITE_LABELING("white_labeling", List.of("tenant_id", "customer_id", "type")),
    ALARM_TYPES("alarm_types", null, of("type")),
    CUSTOM_TRANSLATION("custom_translation", List.of("tenant_id", "customer_id", "locale_code")),
    QR_CODE_SETTINGS("qr_code_settings"),

    /*
     * data from tables below is exported for each entity separately
     * */
    RELATION("relation", Pair.of(
            "from_id", of(TENANT, CUSTOMER, RULE_CHAIN, DEVICE_PROFILE, ROLE, ENTITY_GROUP, RULE_NODE, CONVERTER,
                    INTEGRATION, USER, EDGE, DASHBOARD, DEVICE, ASSET_PROFILE, ASSET, ENTITY_VIEW)
    ), List.of("to_id")),
    ATTRIBUTE("attribute_kv", Pair.of(
            "entity_id", of(TENANT, CUSTOMER, RULE_CHAIN, DEVICE_PROFILE, ROLE, ENTITY_GROUP, RULE_NODE, CONVERTER, OTA_PACKAGE,
                    INTEGRATION, USER, EDGE, DASHBOARD, DEVICE, ASSET_PROFILE, ASSET, ENTITY_VIEW, ALARM, SCHEDULER_EVENT, GROUP_PERMISSION)
    ), List.of("last_update_ts", "attribute_key"), tenantId -> {
        return "SELECT attribute_kv.*, dict.key as key_name FROM attribute_kv " +
                "INNER JOIN key_dictionary dict ON attribute_kv.attribute_key = dict.key_id WHERE ";
    }),
    LATEST_KV("ts_kv_latest", Pair.of(
            "entity_id", of(TENANT, CUSTOMER, RULE_CHAIN, DEVICE_PROFILE, ROLE, ENTITY_GROUP, RULE_NODE, CONVERTER, OTA_PACKAGE,
                    INTEGRATION, USER, EDGE, DASHBOARD, DEVICE, ASSET_PROFILE, ASSET, ENTITY_VIEW, ALARM, SCHEDULER_EVENT, GROUP_PERMISSION)
    ), List.of("key", "ts"), tenantId -> {
        return "SELECT ts_kv_latest.*, dict.key as key_name FROM ts_kv_latest " +
                "INNER JOIN key_dictionary dict ON ts_kv_latest.key = dict.key_id WHERE ";
    });

    private final String name;
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
        this.sortColumns = List.of(partitionColumn, "id");
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

    Table(String name, Pair<String, List<Table>> reference, List<String> sortColumns, Function<UUID, String> customSelect) {
        this.name = name;
        this.reference = reference;
        this.sortColumns = sortColumns;
        this.customSelect = customSelect;
    }

}
