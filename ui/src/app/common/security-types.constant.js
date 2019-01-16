/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
export default angular.module('thingsboard.securityTypes', [])
    .constant('securityTypes',
        {
            roleType: {
                generic: "GENERIC",
                group: "GROUP"
            },
            resource: {
                all: "ALL",
                profile: "PROFILE",
                adminSettings: "ADMIN_SETTINGS",
                alarm: "ALARM",
                device: "DEVICE",
                asset: "ASSET",
                customer: "CUSTOMER",
                dashboard: "DASHBOARD",
                entityView: "ENTITY_VIEW",
                tenant: "TENANT",
                ruleChain: "RULE_CHAIN",
                user: "USER",
                widgetsBundle: "WIDGETS_BUNDLE",
                widgetType: "WIDGET_TYPE",
                converter: "CONVERTER",
                integration: "INTEGRATION",
                schedulerEvent: "SCHEDULER_EVENT",
                blobEntity: "BLOB_ENTITY",
                customerGroup: "CUSTOMER_GROUP",
                deviceGroup: "DEVICE_GROUP",
                assetGroup: "ASSET_GROUP",
                userGroup: "USER_GROUP",
                entityViewGroup: "ENTITY_VIEW_GROUP",
                dashboardGroup: "DASHBOARD_GROUP",
                role: "ROLE",
                groupPermission: "GROUP_PERMISSION",
                whiteLabeling: "WHITE_LABELING",
                auditLog: "AUDIT_LOG"
            },
            resourceByEntityType: {
                "ALARM": "ALARM",
                "DEVICE": "DEVICE",
                "ASSET": "ASSET",
                "CUSTOMER": "CUSTOMER",
                "DASHBOARD": "DASHBOARD",
                "ENTITY_VIEW": "ENTITY_VIEW",
                "TENANT": "TENANT",
                "RULE_CHAIN": "RULE_CHAIN",
                "USER": "USER",
                "WIDGETS_BUNDLE": "WIDGETS_BUNDLE",
                "WIDGET_TYPE": "WIDGET_TYPE",
                "CONVERTER": "CONVERTER",
                "INTEGRATION": "INTEGRATION",
                "SCHEDULER_EVENT": "SCHEDULER_EVENT",
                "BLOB_ENTITY": "BLOB_ENTITY",
                "ROLE": "ROLE",
                "GROUP_PERMISSION": "GROUP_PERMISSION"
            },
            groupResourceByGroupType: {
                "CUSTOMER": "CUSTOMER_GROUP",
                "DEVICE": "DEVICE_GROUP",
                "ASSET": "ASSET_GROUP",
                "USER": "USER_GROUP",
                "ENTITY_VIEW": "ENTITY_VIEW_GROUP",
                "DASHBOARD": "DASHBOARD_GROUP"
            },
            operation: {
                all: "ALL",
                create: "CREATE",
                read: "READ",
                write: "WRITE",
                delete: "DELETE",
                assignToCustomer: "ASSIGN_TO_CUSTOMER",
                unassignFromCustomer: "UNASSIGN_FROM_CUSTOMER",
                rpcCall: "RPC_CALL",
                readCredentials: "READ_CREDENTIALS",
                writeCredentials: "WRITE_CREDENTIALS",
                readAttributes: "READ_ATTRIBUTES",
                writeAttributes: "WRITE_ATTRIBUTES",
                readTelemetry: "READ_TELEMETRY",
                writeTelemetry: "WRITE_TELEMETRY",
                addToGroup: "ADD_TO_GROUP",
                removeFromGroup: "REMOVE_FROM_GROUP"
            },
            publicGroupTypes: {
                "ASSET": "ASSET",
                "DEVICE": "DEVICE",
                "ENTITY_VIEW": "ENTITY_VIEW",
                "DASHBOARD": "DASHBOARD"
            }
        }
    ).name;