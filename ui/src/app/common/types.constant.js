/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
export default angular.module('thingsboard.types', [])
    .constant('types',
        {
            serverErrorCode: {
                general: 2,
                authentication: 10,
                jwtTokenExpired: 11,
                credentialsExpired: 15,
                permissionDenied: 20,
                invalidArguments: 30,
                badRequestParams: 31,
                itemNotFound: 32,
                tooManyRequests: 33,
                tooManyUpdates: 34
            },
            entryPoints: {
                login: "/api/auth/login",
                tokenRefresh: "/api/auth/token",
                nonTokenBased: "/api/noauth"
            },
            id: {
                nullUid: "13814000-1dd2-11b2-8080-808080808080",
            },
            aggregation: {
                min: {
                    value: "MIN",
                    name: "aggregation.min"
                },
                max: {
                    value: "MAX",
                    name: "aggregation.max"
                },
                avg: {
                    value: "AVG",
                    name: "aggregation.avg"
                },
                sum: {
                    value: "SUM",
                    name: "aggregation.sum"
                },
                count: {
                    value: "COUNT",
                    name: "aggregation.count"
                },
                none: {
                    value: "NONE",
                    name: "aggregation.none"
                }
            },
            alarmFields: {
                createdTime: {
                    keyName: 'createdTime',
                    value: "createdTime",
                    name: "alarm.created-time",
                    time: true
                },
                startTime: {
                    keyName: 'startTime',
                    value: "startTs",
                    name: "alarm.start-time",
                    time: true
                },
                endTime: {
                    keyName: 'endTime',
                    value: "endTs",
                    name: "alarm.end-time",
                    time: true
                },
                ackTime: {
                    keyName: 'ackTime',
                    value: "ackTs",
                    name: "alarm.ack-time",
                    time: true
                },
                clearTime: {
                    keyName: 'clearTime',
                    value: "clearTs",
                    name: "alarm.clear-time",
                    time: true
                },
                originator: {
                    keyName: 'originator',
                    value: "originatorName",
                    name: "alarm.originator"
                },
                originatorType: {
                    keyName: 'originatorType',
                    value: "originator.entityType",
                    name: "alarm.originator-type"
                },
                type: {
                    keyName: 'type',
                    value: "type",
                    name: "alarm.type"
                },
                severity: {
                    keyName: 'severity',
                    value: "severity",
                    name: "alarm.severity"
                },
                status: {
                    keyName: 'status',
                    value: "status",
                    name: "alarm.status"
                }
            },
            alarmStatus: {
                activeUnack: "ACTIVE_UNACK",
                activeAck: "ACTIVE_ACK",
                clearedUnack: "CLEARED_UNACK",
                clearedAck: "CLEARED_ACK"
            },
            alarmSearchStatus: {
                any: "ANY",
                active: "ACTIVE",
                cleared: "CLEARED",
                ack: "ACK",
                unack: "UNACK"
            },
            alarmSeverity: {
                "CRITICAL": {
                    name: "alarm.severity-critical",
                    class: "tb-critical",
                    color: "red"
                },
                "MAJOR": {
                    name: "alarm.severity-major",
                    class: "tb-major",
                    color: "orange"
                },
                "MINOR": {
                    name: "alarm.severity-minor",
                    class: "tb-minor",
                    color: "#ffca3d"
                },
                "WARNING": {
                    name: "alarm.severity-warning",
                    class: "tb-warning",
                    color: "#abab00"
                },
                "INDETERMINATE": {
                    name: "alarm.severity-indeterminate",
                    class: "tb-indeterminate",
                    color: "green"
                }
            },
            auditLogActionType: {
                "ADDED": {
                    name: "audit-log.type-added"
                },
                "DELETED": {
                    name: "audit-log.type-deleted"
                },
                "UPDATED": {
                    name: "audit-log.type-updated"
                },
                "ATTRIBUTES_UPDATED": {
                    name: "audit-log.type-attributes-updated"
                },
                "ATTRIBUTES_DELETED": {
                    name: "audit-log.type-attributes-deleted"
                },
                "RPC_CALL": {
                    name: "audit-log.type-rpc-call"
                },
                "CREDENTIALS_UPDATED": {
                    name: "audit-log.type-credentials-updated"
                },
                "ASSIGNED_TO_CUSTOMER": {
                    name: "audit-log.type-assigned-to-customer"
                },
                "UNASSIGNED_FROM_CUSTOMER": {
                    name: "audit-log.type-unassigned-from-customer"
                },
                "ACTIVATED": {
                    name: "audit-log.type-activated"
                },
                "SUSPENDED": {
                    name: "audit-log.type-suspended"
                },
                "CREDENTIALS_READ": {
                    name: "audit-log.type-credentials-read"
                },
                "ATTRIBUTES_READ": {
                    name: "audit-log.type-attributes-read"
                },
                "ADDED_TO_ENTITY_GROUP": {
                    name: "audit-log.type-added-to-entity-group"
                },
                "REMOVED_FROM_ENTITY_GROUP": {
                    name: "audit-log.type-removed-from-entity-group"
                },
                "RELATION_ADD_OR_UPDATE": {
                    name: "audit-log.type-relation-add-or-update"
                },
                "RELATION_DELETED": {
                    name: "audit-log.type-relation-delete"
                },
                "RELATIONS_DELETED": {
                    name: "audit-log.type-relations-delete"
                },
                "ALARM_ACK": {
                    name: "audit-log.type-alarm-ack"
                },
                "ALARM_CLEAR": {
                    name: "audit-log.type-alarm-clear"
                },
                "REST_API_RULE_ENGINE_CALL": {
                    name: "audit-log.type-rest-api-rule-engine-call"
                },
                "MADE_PUBLIC": {
                    name: "audit-log.type-made-public"
                },
                "MADE_PRIVATE": {
                    name: "audit-log.type-made-private"
                },
                "LOGIN": {
                    name: "audit-log.type-login"
                },
                "LOGOUT": {
                    name: "audit-log.type-logout"
                },
                "LOCKOUT": {
                    name: "audit-log.type-lockout"
                }
            },
            auditLogActionStatus: {
                "SUCCESS": {
                    value: "SUCCESS",
                    name: "audit-log.status-success"
                },
                "FAILURE": {
                    value: "FAILURE",
                    name: "audit-log.status-failure"
                }
            },
            auditLogMode: {
                tenant: "tenant",
                entity: "entity",
                user: "user",
                customer: "customer"
            },
            aliasFilterType: {
                singleEntity: {
                    value: 'singleEntity',
                    name: 'alias.filter-type-single-entity'
                },
                entityGroup: {
                    value: 'entityGroup',
                    name: 'alias.filter-type-entity-group'
                },
                entityList: {
                    value: 'entityList',
                    name: 'alias.filter-type-entity-list'
                },
                entityName: {
                    value: 'entityName',
                    name: 'alias.filter-type-entity-name'
                },
                entityGroupList: {
                    value: 'entityGroupList',
                    name: 'alias.filter-type-entity-group-list'
                },
                entityGroupName: {
                    value: 'entityGroupName',
                    name: 'alias.filter-type-entity-group-name'
                },
                entitiesByGroupName: {
                    value: 'entitiesByGroupName',
                    name: 'alias.filter-type-entities-by-group-name'
                },
                stateEntity: {
                    value: 'stateEntity',
                    name: 'alias.filter-type-state-entity'
                },
                stateEntityOwner: {
                    value: 'stateEntityOwner',
                    name: 'alias.filter-type-state-entity-owner'
                },
                assetType: {
                    value: 'assetType',
                    name: 'alias.filter-type-asset-type'
                },
                deviceType: {
                    value: 'deviceType',
                    name: 'alias.filter-type-device-type'
                },
                entityViewType: {
                    value: 'entityViewType',
                    name: 'alias.filter-type-entity-view-type'
                },
                relationsQuery: {
                    value: 'relationsQuery',
                    name: 'alias.filter-type-relations-query'
                },
                assetSearchQuery: {
                    value: 'assetSearchQuery',
                    name: 'alias.filter-type-asset-search-query'
                },
                deviceSearchQuery: {
                    value: 'deviceSearchQuery',
                    name: 'alias.filter-type-device-search-query'
                },
                entityViewSearchQuery: {
                    value: 'entityViewSearchQuery',
                    name: 'alias.filter-type-entity-view-search-query'
                }
            },
            direction: {
                column: {
                    value: "column",
                    name: "direction.column"
                },
                row: {
                    value: "row",
                    name: "direction.row"
                }
            },
            position: {
                top: {
                    value: "top",
                    name: "position.top"
                },
                bottom: {
                    value: "bottom",
                    name: "position.bottom"
                },
                left: {
                    value: "left",
                    name: "position.left"
                },
                right: {
                    value: "right",
                    name: "position.right"
                }
            },
            datasourceType: {
                function: "function",
                entity: "entity"
            },
            dataKeyType: {
                timeseries: "timeseries",
                attribute: "attribute",
                function: "function",
                alarm: "alarm",
                entityField: "entityField"
            },
            converterType: {
                "UPLINK": {
                    uplink: true,
                    name: "converter.type-uplink",
                    value: "UPLINK"
                },
                "DOWNLINK": {
                    downlink: true,
                    name: "converter.type-downlink",
                    value: "DOWNLINK"
                }
            },
            contentType: {
                "JSON": {
                    value: "JSON",
                    name: "content-type.json",
                    code: "json"
                },
                "TEXT": {
                    value: "TEXT",
                    name: "content-type.text",
                    code: "text"
                },
                "BINARY": {
                    value: "BINARY",
                    name: "content-type.binary",
                    code: "text"
                }
            },
            integrationType: {
                "HTTP": {
                    name: "integration.type-http",
                    value: "HTTP",
                    http: true
                },
                "OCEANCONNECT": {
                    name: "integration.type-ocean-connect",
                    value: "OCEANCONNECT",
                    http: true
                },
                "SIGFOX": {
                    name: "integration.type-sigfox",
                    value: "SIGFOX",
                    http: true
                },
                "THINGPARK": {
                    name: "integration.type-thingpark",
                    value: "THINGPARK",
                    http: true
                },
                "TPE": {
                    name: "integration.type-thingpark-enterprise",
                    value: "TPE",
                    http: true
                },
                "TMOBILE_IOT_CDP": {
                    name: "integration.type-tmobile-iot-cdp",
                    value: "TMOBILE_IOT_CDP",
                    http: true
                },
                "MQTT": {
                    name: "integration.type-mqtt",
                    value: "MQTT",
                    mqtt: true
                },
                "AWS_IOT": {
                    name: "integration.type-aws-iot",
                    value: "AWS_IOT",
                    mqtt: true
                },
                "AWS_SQS": {
                    name: "integration.type-aws-sqs",
                    value: "AWS_SQS",
                    mqtt: true
                },
                "AWS_KINESIS": {
                    name: "integration.type-aws-kinesis",
                    value: "AWS_KINESIS"
                },
                "IBM_WATSON_IOT": {
                    name: "integration.type-ibm-watson-iot",
                    value: "IBM_WATSON_IOT",
                    mqtt: true
                },
                "TTN": {
                    name: "integration.type-ttn",
                    value: "TTN",
                    mqtt: true
                },
                "AZURE_EVENT_HUB": {
                    name: "integration.type-azure-event-hub",
                    value: "AZURE_EVENT_HUB"
                },
                "OPC_UA": {
                    name: "integration.type-opc-ua",
                    value: "OPC_UA"
                },
                "UDP": {
                    name: "integration.type-udp",
                    value: "UDP",
                    remote: true
                },
                "TCP": {
                    name: "integration.type-tcp",
                    value: "TCP",
                    remote: true
                },
                "KAFKA": {
                    name: "integration.type-kafka",
                    value: "KAFKA"
                },
                "CUSTOM": {
                    name: "integration.type-custom",
                    value: "CUSTOM",
                    remote: true
                }
            },
            componentType: {
                enrichment: "ENRICHMENT",
                filter: "FILTER",
                transformation: "TRANSFORMATION",
                action: "ACTION",
                external: "EXTERNAL"
            },
            entityType: {
                device: "DEVICE",
                asset: "ASSET",
                tenant: "TENANT",
                customer: "CUSTOMER",
                user: "USER",
                dashboard: "DASHBOARD",
                alarm: "ALARM",
                entityGroup: "ENTITY_GROUP",
                converter: "CONVERTER",
                integration: "INTEGRATION",
                rulechain: "RULE_CHAIN",
                rulenode: "RULE_NODE",
                schedulerEvent: "SCHEDULER_EVENT",
                blobEntity: "BLOB_ENTITY",
                entityView: "ENTITY_VIEW",
                role: "ROLE"
            },
            entityGroup: {
                sortOrder: {
                    asc: {
                        name: 'entity-group.sort-order.asc',
                        value: 'ASC'
                    },
                    desc: {
                        name: 'entity-group.sort-order.desc',
                        value: 'DESC'
                    },
                    none: {
                        name: 'entity-group.sort-order.none',
                        value: 'NONE'
                    }
                },
                columnType: {
                    clientAttribute: {
                        name: 'entity-group.column-type.client-attribute',
                        value: 'CLIENT_ATTRIBUTE'
                    },
                    sharedAttribute: {
                        name: 'entity-group.column-type.shared-attribute',
                        value: 'SHARED_ATTRIBUTE'
                    },
                    serverAttribute: {
                        name: 'entity-group.column-type.server-attribute',
                        value: 'SERVER_ATTRIBUTE'
                    },
                    timeseries: {
                        name: 'entity-group.column-type.timeseries',
                        value: 'TIMESERIES'
                    },
                    entityField: {
                        name: 'entity-group.column-type.entity-field',
                        value: 'ENTITY_FIELD'
                    }
                },
                entityField: {
                    created_time: {
                        name: 'entity-group.entity-field.created-time',
                        value: 'created_time',
                        time: true
                    },
                    name: {
                        name: 'entity-group.entity-field.name',
                        value: 'name'
                    },
                    type: {
                        name: 'entity-group.entity-field.type',
                        value: 'type'
                    },
                    assigned_customer: {
                        name: 'entity-group.entity-field.assigned_customer',
                        value: 'assigned_customer'
                    },
                    authority: {
                        name: 'entity-group.entity-field.authority',
                        value: 'authority'
                    },
                    first_name: {
                        name: 'entity-group.entity-field.first_name',
                        value: 'first_name'
                    },
                    last_name: {
                        name: 'entity-group.entity-field.last_name',
                        value: 'last_name'
                    },
                    email: {
                        name: 'entity-group.entity-field.email',
                        value: 'email'
                    },
                    title: {
                        name: 'entity-group.entity-field.title',
                        value: 'title'
                    },
                    country: {
                        name: 'entity-group.entity-field.country',
                        value: 'country'
                    },
                    state: {
                        name: 'entity-group.entity-field.state',
                        value: 'state'
                    },
                    city: {
                        name: 'entity-group.entity-field.city',
                        value: 'city'
                    },
                    address: {
                        name: 'entity-group.entity-field.address',
                        value: 'address'
                    },
                    address2: {
                        name: 'entity-group.entity-field.address2',
                        value: 'address2'
                    },
                    zip: {
                        name: 'entity-group.entity-field.zip',
                        value: 'zip'
                    },
                    phone: {
                        name: 'entity-group.entity-field.phone',
                        value: 'phone'
                    },
                    label: {
                        name: 'entity-group.entity-field.label',
                        value: 'label'
                    }
                },
                detailsMode: {
                    onRowClick: {
                        name: 'entity-group.details-mode.on-row-click',
                        value: 'onRowClick'
                    },
                    onActionButtonClick: {
                        name: 'entity-group.details-mode.on-action-button-click',
                        value: 'onActionButtonClick'
                    },
                    disabled: {
                        name: 'entity-group.details-mode.disabled',
                        value: 'disabled'
                    }
                }
            },
            entityTypeResources: {
                "DEVICE": {
                    helpId: 'devices'
                },
                "ASSET": {
                    helpId: 'assets'
                },
                "RULE": {
                    helpId: 'rules'
                },
                "PLUGIN": {
                    helpId: 'plugins'
                },
                "TENANT": {
                    helpId: 'tenants'
                },
                "CUSTOMER": {
                    helpId: 'customers'
                },
                "USER": {
                    helpId: 'users'
                },
                "DASHBOARD": {
                    helpId: 'dashboards'
                },
                "ALARM": {
                    helpId: 'docs'
                },
                "CONVERTER": {
                    helpId: 'converters'
                },
                "INTEGRATION": {
                    helpId: 'integrations'
                },
                "RULE_CHAIN": {
                    helpId: 'rulechains'
                },
                "ENTITY_VIEW": {
                    helpId: 'entityViews'
                }
            },
            importEntityColumnType: {
                name: {
                    name: 'import.column-type.name',
                    value: 'name'
                },
                type: {
                    name: 'import.column-type.type',
                    value: 'type'
                },
                label: {
                    name: 'import.column-type.label',
                    value: 'label'
                },
                clientAttribute: {
                    name: 'import.column-type.client-attribute',
                    value: 'CLIENT_ATTRIBUTE'
                },
                sharedAttribute: {
                    name: 'import.column-type.shared-attribute',
                    value: 'SHARED_ATTRIBUTE'
                },
                serverAttribute: {
                    name: 'import.column-type.server-attribute',
                    value: 'SERVER_ATTRIBUTE'
                },
                timeseries: {
                    name: 'import.column-type.timeseries',
                    value: 'TIMESERIES'
                },
                entityField: {
                    name: 'import.column-type.entity-field',
                    value: 'ENTITY_FIELD'
                },
                accessToken: {
                    name: 'import.column-type.access-token',
                    value: 'ACCESS_TOKEN'
                },
                isGateway: {
                    name: 'import.column-type.isgateway',
                    value: 'gateway'
                },
                description: {
                    name: 'import.column-type.description',
                    value: 'description'
                }
            },
            aliasEntityType: {
                current_customer: "CURRENT_CUSTOMER"
            },
            entityTypeTranslations: {
                "DEVICE": {
                    type: 'entity.type-device',
                    typePlural: 'entity.type-devices',
                    list: 'entity.list-of-devices',
                    nameStartsWith: 'entity.device-name-starts-with',
                    details: 'device.device-details',
                    add: 'device.add',
                    noEntities: 'device.no-devices-text',
                    selectedEntities: 'device.selected-devices',
                    search: 'device.search',
                    selectGroupToAdd: 'device.select-group-to-add',
                    selectGroupToMove: 'device.select-group-to-move',
                    removeFromGroup: 'device.remove-devices-from-group',
                    group: 'device.group',
                    groupList: 'device.list-of-groups',
                    groupNameStartsWith: 'device.group-name-starts-with'
                },
                "ASSET": {
                    type: 'entity.type-asset',
                    typePlural: 'entity.type-assets',
                    list: 'entity.list-of-assets',
                    nameStartsWith: 'entity.asset-name-starts-with',
                    details: 'asset.asset-details',
                    add: 'asset.add',
                    noEntities: 'asset.no-assets-text',
                    selectedEntities: 'asset.selected-assets',
                    search: 'asset.search',
                    selectGroupToAdd: 'asset.select-group-to-add',
                    selectGroupToMove: 'asset.select-group-to-move',
                    removeFromGroup: 'asset.remove-assets-from-group',
                    group: 'asset.group',
                    groupList: 'asset.list-of-groups',
                    groupNameStartsWith: 'asset.group-name-starts-with'
                },
                "ENTITY_VIEW": {
                    type: 'entity.type-entity-view',
                    typePlural: 'entity.type-entity-views',
                    list: 'entity.list-of-entity-views',
                    nameStartsWith: 'entity.entity-view-name-starts-with',
                    details: 'entity-view.entity-view-details',
                    add: 'entity-view.add',
                    noEntities: 'entity-view.no-entity-views-text',
                    selectedEntities: 'entity-view.selected-entity-views',
                    search: 'entity-view.search',
                    selectGroupToAdd: 'entity-view.select-group-to-add',
                    selectGroupToMove: 'entity-view.select-group-to-move',
                    removeFromGroup: 'entity-view.remove-entity-views-from-group',
                    group: 'entity-view.group',
                    groupList: 'entity-view.list-of-groups',
                    groupNameStartsWith: 'entity-view.group-name-starts-with'
                },
                "TENANT": {
                    type: 'entity.type-tenant',
                    typePlural: 'entity.type-tenants',
                    list: 'entity.list-of-tenants',
                    nameStartsWith: 'entity.tenant-name-starts-with',
                    details: 'tenant.tenant-details',
                    add: 'tenant.add',
                    noEntities: 'tenant.no-tenants-text',
                    selectedEntities: 'tenant.selected-tenants',
                    search: 'tenant.search'
                },
                "CUSTOMER": {
                    type: 'entity.type-customer',
                    typePlural: 'entity.type-customers',
                    list: 'entity.list-of-customers',
                    nameStartsWith: 'entity.customer-name-starts-with',
                    details: 'customer.customer-details',
                    add: 'customer.add',
                    noEntities: 'customer.no-customers-text',
                    selectedEntities: 'customer.selected-customers',
                    search: 'customer.search',
                    selectGroupToAdd: 'customer.select-group-to-add',
                    selectGroupToMove: 'customer.select-group-to-move',
                    removeFromGroup: 'customer.remove-customers-from-group',
                    group: 'customer.group',
                    groupList: 'customer.list-of-groups',
                    groupNameStartsWith: 'customer.group-name-starts-with'
                },
                "USER": {
                    type: 'entity.type-user',
                    typePlural: 'entity.type-users',
                    list: 'entity.list-of-users',
                    nameStartsWith: 'entity.user-name-starts-with',
                    details: 'user.user-details',
                    add: 'user.add',
                    noEntities: 'user.no-users-text',
                    selectedEntities: 'user.selected-users',
                    search: 'user.search',
                    selectGroupToAdd: 'user.select-group-to-add',
                    selectGroupToMove: 'user.select-group-to-move',
                    removeFromGroup: 'user.remove-users-from-group',
                    group: 'user.group',
                    groupList: 'user.list-of-groups',
                    groupNameStartsWith: 'user.group-name-starts-with'
                },
                "DASHBOARD": {
                    type: 'entity.type-dashboard',
                    typePlural: 'entity.type-dashboards',
                    list: 'entity.list-of-dashboards',
                    nameStartsWith: 'entity.dashboard-name-starts-with',
                    details: 'dashboard.dashboard-details',
                    add: 'dashboard.add',
                    noEntities: 'dashboard.no-dashboards-text',
                    selectedEntities: 'dashboard.selected-dashboards',
                    search: 'dashboard.search',
                    selectGroupToAdd: 'dashboard.select-group-to-add',
                    selectGroupToMove: 'dashboard.select-group-to-move',
                    removeFromGroup: 'dashboard.remove-dashboards-from-group',
                    group: 'dashboard.group',
                    groupList: 'dashboard.list-of-groups',
                    groupNameStartsWith: 'dashboard.group-name-starts-with'
                },
                "ALARM": {
                    type: 'entity.type-alarm',
                    typePlural: 'entity.type-alarms',
                    list: 'entity.list-of-alarms',
                    nameStartsWith: 'entity.alarm-name-starts-with',
                    details: 'alarm.alarm-details',
                    add: 'alarm.alarm'
                },
                "ENTITY_GROUP": {
                    type: 'entity.type-entity-group'
                },
                "CONVERTER": {
                    type: 'entity.type-converter',
                    typePlural: 'entity.type-converters',
                    list: 'entity.list-of-converters',
                    nameStartsWith: 'entity.converter-name-starts-with',
                    details: 'converter.converter-details',
                    add: 'converter.add'
                },
                "INTEGRATION": {
                    type: 'entity.type-integration',
                    typePlural: 'entity.type-integrations',
                    list: 'entity.list-of-integrations',
                    nameStartsWith: 'entity.integration-name-starts-with',
                    details: 'integration.integration-details',
                    add: 'integration.add'
                },
                "RULE_CHAIN": {
                    type: 'entity.type-rulechain',
                    typePlural: 'entity.type-rulechains',
                    list: 'entity.list-of-rulechains',
                    nameStartsWith: 'entity.rulechain-name-starts-with'
                },
                "RULE_NODE": {
                    type: 'entity.type-rulenode',
                    typePlural: 'entity.type-rulenodes',
                    list: 'entity.list-of-rulenodes',
                    nameStartsWith: 'entity.rulenode-name-starts-with'
                },
                "CURRENT_CUSTOMER": {
                    type: 'entity.type-current-customer',
                    list: 'entity.type-current-customer'
                },
                "SCHEDULER_EVENT": {
                    type: 'entity.type-scheduler-event',
                    typePlural: 'entity.type-scheduler-events',
                    list: 'entity.list-of-scheduler-events',
                    nameStartsWith: 'entity.scheduler-event-name-starts-with'
                },
                "BLOB_ENTITY": {
                    type: 'entity.type-blob-entity',
                    typePlural: 'entity.type-blob-entities',
                    list: 'entity.list-of-blob-entities',
                    nameStartsWith: 'entity.blob-entity-name-starts-with'
                },
                "ROLE": {
                    type: 'entity.type-role',
                    typePlural: 'entity.type-roles',
                    list: 'entity.list-of-roles',
                    nameStartsWith: 'entity.role-name-starts-with'
                },
                "GROUP_PERMISSION": {
                    type: 'entity.type-group-permission'
                }
            },
            entityField: {
                createdTime: {
                    keyName: 'createdTime',
                    name: 'entity-field.created-time',
                    value: 'createdTime',
                    time: true
                },
                name: {
                    keyName: 'name',
                    name: 'entity-field.name',
                    value: 'name'
                },
                type: {
                    keyName: 'type',
                    name: 'entity-field.type',
                    value: 'type'
                },
                firstName: {
                    keyName: 'firstName',
                    name: 'entity-field.first-name',
                    value: 'firstName'
                },
                lastName: {
                    keyName: 'lastName',
                    name: 'entity-field.last-name',
                    value: 'lastName'
                },
                email: {
                    keyName: 'email',
                    name: 'entity-field.email',
                    value: 'email'
                },
                title: {
                    keyName: 'title',
                    name: 'entity-field.title',
                    value: 'title'
                },
                country: {
                    keyName: 'country',
                    name: 'entity-field.country',
                    value: 'country'
                },
                state: {
                    keyName: 'state',
                    name: 'entity-field.state',
                    value: 'state'
                },
                city: {
                    keyName: 'city',
                    name: 'entity-field.city',
                    value: 'city'
                },
                address: {
                    keyName: 'address',
                    name: 'entity-field.address',
                    value: 'address'
                },
                address2: {
                    keyName: 'address2',
                    name: 'entity-field.address2',
                    value: 'address2'
                },
                zip: {
                    keyName: 'zip',
                    name: 'entity-field.zip',
                    value: 'zip'
                },
                phone: {
                    keyName: 'phone',
                    name: 'entity-field.phone',
                    value: 'phone'
                },
                label: {
                    keyName: 'label',
                    name: 'entity-field.label',
                    value: 'label'
                }
            },
            entitySearchDirection: {
                from: "FROM",
                to: "TO"
            },
            entityRelationType: {
                contains: "Contains",
                manages: "Manages"
            },
            eventType: {
                error: {
                    value: "ERROR",
                    name: "event.type-error"
                },
                lcEvent: {
                    value: "LC_EVENT",
                    name: "event.type-lc-event"
                },
                stats: {
                    value: "STATS",
                    name: "event.type-stats"
                },
                rawData: {
                    value: "RAW_DATA",
                    name: "event.type-rw-event"
                }
            },
            debugEventType: {
                debugConverter: {
                    value: "DEBUG_CONVERTER",
                    name: "event.type-debug-converter"
                },
                debugIntegration: {
                    value: "DEBUG_INTEGRATION",
                    name: "event.type-debug-integration"
                },
                debugRuleNode: {
                    value: "DEBUG_RULE_NODE",
                    name: "event.type-debug-rule-node"
                },
                debugRuleChain: {
                    value: "DEBUG_RULE_CHAIN",
                    name: "event.type-debug-rule-chain"
                }
            },
            extensionType: {
                http: "HTTP",
                mqtt: "MQTT",
                opc: "OPC UA",
                modbus: "MODBUS"
            },
            gatewayConfigType: {
                mqtt:  {
                    value: "mqtt",
                    name: "MQTT"
                },
                modbus:  {
                    value: "modbus",
                    name: "Modbus"
                },
                opc_ua:  {
                    value: "opcua",
                    name: "OPC-UA"
                },
                ble:  {
                    value: "ble",
                    name: "BLE"
                }
            },
            gatewayLogLevel: {
                none: "NONE",
                critical: "CRITICAL",
                error: "ERROR",
                warning: "WARNING",
                info: "INFO",
                debug: "DEBUG"
            },
            extensionValueType: {
                string: 'value.string',
                long: 'value.long',
                double: 'value.double',
                boolean: 'value.boolean'
            },
            extensionTransformerType: {
                toDouble: 'extension.to-double',
                custom: 'extension.custom'
            },
            mqttConverterTypes: {
                json: 'extension.converter-json',
                custom: 'extension.custom'
            },
            mqttCredentialTypes: {
                anonymous:  {
                    value: "anonymous",
                    name: "extension.anonymous"
                },
                basic: {
                    value: "basic",
                    name: "extension.basic"
                },
                'cert.PEM': {
                    value: "cert.PEM",
                    name: "extension.pem"
                }
            },
            mqttQoS: {
                0: {
                    value: 0,
                    name: 'integration.mqtt-qos-at-most-once'
                },
                1: {
                    value: 1,
                    name: 'integration.mqtt-qos-at-least-once'
                },
                2: {
                    value: 2,
                    name: 'integration.mqtt-qos-exactly-once'
                }
            },
            opcSecurityTypes: {
                Basic128Rsa15: "Basic128Rsa15",
                Basic256: "Basic256",
                Basic256Sha256: "Basic256Sha256",
                None: "None"
            },
            handlerConfigurationTypes: {
                text:  {
                    value: "TEXT",
                    name: "extension.text"
                },
                binary: {
                    value: "BINARY",
                    name: "extension.binary"
                },
                json: {
                    value: "JSON",
                    name: "extension.json"
                },
                hex: {
                    value: "HEX",
                    name: "extension.hex"
                }
            },
            tcpBinaryByteOrder: {
                littleEndian: {
                    value: "LITTLE_ENDIAN"
                },
                bigEndian: {
                    value: "BIG_ENDIAN"
                }
            },
            tcpTextMessageSeparator: {
                systemLineSeparator: {
                    value: "SYSTEM_LINE_SEPARATOR"
                },
                nulDelimiter: {
                    value: "NUL_DELIMITER"
                }
            },
            identityType: {
                anonymous: "extension.anonymous",
                username: "extension.username"
            },
            initialPositionInStream: {
                latest: "LATEST",
                trim_horizon: "TRIM_HORIZON",
                at_timestamp: "AT_TIMESTAMP"
            },
            extensionKeystoreType: {
                PKCS12: "PKCS12",
                JKS: "JKS"
            },
            extensionModbusFunctionCodes: {
                1: "Read Coils (1)",
                2: "Read Discrete Inputs (2)",
                3: "Read Multiple Holding Registers (3)",
                4: "Read Input Registers (4)"
            },
            extensionModbusTransports: {
                tcp: "TCP",
                udp: "UDP",
                rtu: "RTU"
            },
            extensionModbusRtuParities: {
                none: "none",
                even: "even",
                odd: "odd"
            },
            extensionModbusRtuEncodings: {
                ascii: "ascii",
                rtu: "rtu"
            },
            opcUaMappingType: {
                ID: "ID",
                FQN: "Fully Qualified Name"
            },
            latestTelemetry: {
                value: "LATEST_TELEMETRY",
                name: "attribute.scope-latest-telemetry",
                clientSide: true
            },
            attributesScope: {
                client: {
                    value: "CLIENT_SCOPE",
                    name: "attribute.scope-client",
                    clientSide: true
                },
                server: {
                    value: "SERVER_SCOPE",
                    name: "attribute.scope-server",
                    clientSide: false
                },
                shared: {
                    value: "SHARED_SCOPE",
                    name: "attribute.scope-shared",
                    clientSide: false
                }
            },
            ruleNodeTypeComponentTypes: ["FILTER", "ENRICHMENT", "TRANSFORMATION", "ACTION", "ANALYTICS", "EXTERNAL"],
            ruleChainNodeComponent: {
                type: 'RULE_CHAIN',
                name: 'rule chain',
                clazz: 'tb.internal.RuleChain',
                configurationDescriptor: {
                    nodeDefinition: {
                        description: "",
                        details: "Forwards incoming messages to specified Rule Chain",
                        inEnabled: true,
                        outEnabled: false,
                        relationTypes: [],
                        customRelations: false,
                        defaultConfiguration: {}
                    }
                }
            },
            unknownNodeComponent: {
                type: 'UNKNOWN',
                name: 'unknown',
                clazz: 'tb.internal.Unknown',
                configurationDescriptor: {
                    nodeDefinition: {
                        description: "",
                        details: "",
                        inEnabled: true,
                        outEnabled: true,
                        relationTypes: [],
                        customRelations: false,
                        defaultConfiguration: {}
                    }
                }
            },
            inputNodeComponent: {
                type: 'INPUT',
                name: 'Input',
                clazz: 'tb.internal.Input'
            },
            ruleNodeType: {
                FILTER: {
                    value: "FILTER",
                    name: "rulenode.type-filter",
                    details: "rulenode.type-filter-details",
                    nodeClass: "tb-filter-type",
                    icon: "filter_list"
                },
                ENRICHMENT: {
                    value: "ENRICHMENT",
                    name: "rulenode.type-enrichment",
                    details: "rulenode.type-enrichment-details",
                    nodeClass: "tb-enrichment-type",
                    icon: "playlist_add"
                },
                TRANSFORMATION: {
                    value: "TRANSFORMATION",
                    name: "rulenode.type-transformation",
                    details: "rulenode.type-transformation-details",
                    nodeClass: "tb-transformation-type",
                    icon: "transform"
                },
                ACTION: {
                    value: "ACTION",
                    name: "rulenode.type-action",
                    details: "rulenode.type-action-details",
                    nodeClass: "tb-action-type",
                    icon: "flash_on"
                },
                ANALYTICS: {
                    value: "ANALYTICS",
                    name: "rulenode.type-analytics",
                    details: "rulenode.type-analytics-details",
                    nodeClass: "tb-analytics-type",
                    icon: "timeline"
                },
                EXTERNAL: {
                    value: "EXTERNAL",
                    name: "rulenode.type-external",
                    details: "rulenode.type-external-details",
                    nodeClass: "tb-external-type",
                    icon: "cloud_upload"
                },
                RULE_CHAIN: {
                    value: "RULE_CHAIN",
                    name: "rulenode.type-rule-chain",
                    details: "rulenode.type-rule-chain-details",
                    nodeClass: "tb-rule-chain-type",
                    icon: "settings_ethernet"
                },
                INPUT: {
                    value: "INPUT",
                    name: "rulenode.type-input",
                    details: "rulenode.type-input-details",
                    nodeClass: "tb-input-type",
                    icon: "input",
                    special: true
                },
                UNKNOWN: {
                    value: "UNKNOWN",
                    name: "rulenode.type-unknown",
                    details: "rulenode.type-unknown-details",
                    nodeClass: "tb-unknown-type",
                    icon: "help_outline"
                }
            },
            messageType: {
                'POST_ATTRIBUTES_REQUEST': {
                    name: 'Post attributes',
                    value: 'POST_ATTRIBUTES_REQUEST'
                },
                'POST_TELEMETRY_REQUEST': {
                    name: 'Post telemetry',
                    value: 'POST_TELEMETRY_REQUEST'
                },
                'TO_SERVER_RPC_REQUEST': {
                    name: 'RPC Request from Device',
                    value: 'TO_SERVER_RPC_REQUEST'
                },
                'RPC_CALL_FROM_SERVER_TO_DEVICE': {
                    name: 'RPC Request to Device',
                    value: 'RPC_CALL_FROM_SERVER_TO_DEVICE'
                },
                'ACTIVITY_EVENT': {
                    name: 'Activity Event',
                    value: 'ACTIVITY_EVENT'
                },
                'INACTIVITY_EVENT': {
                    name: 'Inactivity Event',
                    value: 'INACTIVITY_EVENT'
                },
                'CONNECT_EVENT': {
                    name: 'Connect Event',
                    value: 'CONNECT_EVENT'
                },
                'DISCONNECT_EVENT': {
                    name: 'Disconnect Event',
                    value: 'DISCONNECT_EVENT'
                },
                'ENTITY_CREATED': {
                    name: 'Entity Created',
                    value: 'ENTITY_CREATED'
                },
                'ENTITY_UPDATED': {
                    name: 'Entity Updated',
                    value: 'ENTITY_UPDATED'
                },
                'ENTITY_DELETED': {
                    name: 'Entity Deleted',
                    value: 'ENTITY_DELETED'
                },
                'ENTITY_ASSIGNED': {
                    name: 'Entity Assigned',
                    value: 'ENTITY_ASSIGNED'
                },
                'ENTITY_UNASSIGNED': {
                    name: 'Entity Unassigned',
                    value: 'ENTITY_UNASSIGNED'
                },
                'ATTRIBUTES_UPDATED': {
                    name: 'Attributes Updated',
                    value: 'ATTRIBUTES_UPDATED'
                },
                'ATTRIBUTES_DELETED': {
                    name: 'Attributes Deleted',
                    value: 'ATTRIBUTES_DELETED'
                },
                'ADDED_TO_ENTITY_GROUP': {
                    name: 'Added to Group',
                    value: 'ADDED_TO_ENTITY_GROUP'
                },
                'REMOVED_FROM_ENTITY_GROUP': {
                    name: 'Removed from Group',
                    value: 'REMOVED_FROM_ENTITY_GROUP'
                },
                'REST_API_REQUEST': {
                    name: 'REST API request',
                    value: 'REST_API_REQUEST'
                },
                'generateReport': {
                    name: 'Generate Report',
                    value: 'generateReport'
                }
            },
            valueType: {
                string: {
                    value: "string",
                    name: "value.string",
                    icon: "mdi:format-text"
                },
                integer: {
                    value: "integer",
                    name: "value.integer",
                    icon: "mdi:numeric"
                },
                double: {
                    value: "double",
                    name: "value.double",
                    icon: "mdi:numeric"
                },
                boolean: {
                    value: "boolean",
                    name: "value.boolean",
                    icon: "mdi:checkbox-marked-outline"
                },
                json: {
                    value: "json",
                    name: "value.json",
                    icon: "mdi:json"
                }
            },
            mailTemplate: {
                test: {
                    value: "test",
                    name: "admin.mail-template.test"
                },
                activation: {
                    value: "activation",
                    name: "admin.mail-template.activation"
                },
                accountActivated: {
                    value: "accountActivated",
                    name: "admin.mail-template.account-activated"
                },
                accountLockout: {
                    value: "accountLockout",
                    name: "admin.mail-template.account-lockout"
                },
                resetPassword: {
                    value: "resetPassword",
                    name: "admin.mail-template.reset-password"
                },
                passwordWasReset: {
                    value: "passwordWasReset",
                    name: "admin.mail-template.password-was-reset"
                },
                userActivated: {
                    value: "userActivated",
                    name: "admin.mail-template.user-activated"
                },
                userRegistered: {
                    value: "userRegistered",
                    name: "admin.mail-template.user-registered"
                }
            },
            widgetType: {
                timeseries: {
                    value: "timeseries",
                    name: "widget.timeseries",
                    template: {
                        bundleAlias: "charts",
                        alias: "basic_timeseries"
                    }
                },
                latest: {
                    value: "latest",
                    name: "widget.latest-values",
                    template: {
                        bundleAlias: "cards",
                        alias: "attributes_card"
                    }
                },
                rpc: {
                    value: "rpc",
                    name: "widget.rpc",
                    template: {
                        bundleAlias: "gpio_widgets",
                        alias: "basic_gpio_control"
                    }
                },
                alarm: {
                    value: "alarm",
                    name: "widget.alarm",
                    template: {
                        bundleAlias: "alarm_widgets",
                        alias: "alarms_table"
                    }
                },
                static: {
                    value: "static",
                    name: "widget.static",
                    template: {
                        bundleAlias: "cards",
                        alias: "html_card"
                    }
                }
            },
            widgetExportType: {
                csv: {
                    name: 'widget.export-to-csv',
                    value: 'csv'
                },
                xls: {
                    name: 'widget.export-to-excel',
                    value: 'xls'
                },
                xlsx: {
                    name: 'widget.export-to-excel-xlsx',
                    value: 'xlsx'
                }
            },
            widgetActionSources: {
                headerButton: {
                    name: 'widget-action.header-button',
                    value: 'headerButton',
                    multiple: true
                }
            },
            widgetActionTypes: {
                openDashboardState: {
                    name: 'widget-action.open-dashboard-state',
                    value: 'openDashboardState'
                },
                updateDashboardState: {
                    name: 'widget-action.update-dashboard-state',
                    value: 'updateDashboardState'
                },
                openDashboard: {
                    name: 'widget-action.open-dashboard',
                    value: 'openDashboard'
                },
                custom: {
                    name: 'widget-action.custom',
                    value: 'custom'
                },
                customPretty: {
                    name: 'widget-action.custom-pretty',
                    value: 'customPretty'
                }
            },
            entityGroupActionTypes: {
                openDashboard: {
                    name: 'widget-action.open-dashboard',
                    value: 'openDashboard'
                },
                custom: {
                    name: 'widget-action.custom',
                    value: 'custom'
                }
            },
            schedulerRepeat: {
                daily: {
                    value: 'DAILY',
                    name: 'scheduler.daily'
                },
                weekly: {
                    value: 'WEEKLY',
                    name: 'scheduler.weekly'
                },
                monthly: {
                    value: 'MONTHLY',
                    type:'month',
                    name: 'scheduler.monthly'
                },
                yearly: {
                    value: 'YEARLY',
                    type:'year',
                    name: 'scheduler.yearly'
                },
                timer: {
                    value: 'TIMER',
                    name: 'scheduler.timer'
                }
            },
            schedulerTimeUnit: [
                {
                    value: 'HOURS',
                    name: 'scheduler.hours'
                },
                {
                    value: 'MINUTES',
                    name: 'scheduler.minutes'
                }/*,
                {
                    value: 'SECONDS',
                    name: 'scheduler.seconds'
                },*/
            ],
            schedulerWeekday: [
                'scheduler.sunday',
                'scheduler.monday',
                'scheduler.tuesday',
                'scheduler.wednesday',
                'scheduler.thursday',
                'scheduler.friday',
                'scheduler.saturday'
            ],
            schedulerCalendarView: {
                'month': {
                    name: 'scheduler.month',
                    value: 'month'
                },
                'basicWeek': {
                    name: 'scheduler.week',
                    value: 'basicWeek'
                },
                'basicDay': {
                    name: 'scheduler.day',
                    value: 'basicDay'
                },
                'listYear': {
                    name: 'scheduler.list-year',
                    value: 'listYear'
                },
                'listMonth': {
                    name: 'scheduler.list-month',
                    value: 'listMonth'
                },
                'listWeek': {
                    name: 'scheduler.list-week',
                    value: 'listWeek'
                },
                'listDay': {
                    name: 'scheduler.list-day',
                    value: 'listDay'
                },
                'agendaWeek': {
                    name: 'scheduler.agenda-week',
                    value: 'agendaWeek'
                },
                'agendaDay': {
                    name: 'scheduler.agenda-day',
                    value: 'agendaDay'
                }
            },
            schedulerEventConfigTypes: [
                {
                    name: 'Generate Report',
                    value: 'generateReport',
                    directive: 'tbGenerateReportEventConfig',
                    originator: false,
                    msgType: false,
                    metadata: false
                },
                {
                    name: 'Update Attributes',
                    value: 'updateAttributes',
                    directive: 'tbUpdateAttributesEventConfig',
                    originator: false,
                    msgType: false,
                    metadata: false
                },
                {
                    name: 'Send RPC Request to Device',
                    value: 'sendRpcRequest',
                    directive: 'tbSendRpcRequestEventConfig',
                    originator: false,
                    msgType: false,
                    metadata: false
                }
            ],
            reportType: {
                'pdf': {
                    name: 'PDF',
                    value: 'pdf'
                },
                'png': {
                    name: 'PNG',
                    value: 'png'
                },
                'jpeg': {
                    name: 'JPEG',
                    value: 'jpeg'
                }
            },
            blobEntityType: {
                "report": {
                    name: "blob-entity.report"
                }
            },
            systemBundleAlias: {
                charts: "charts",
                cards: "cards"
            },
            translate: {
                customTranslationsPrefix: "custom.",
                i18nPrefix: "i18n"
            }
        }
    ).name;
