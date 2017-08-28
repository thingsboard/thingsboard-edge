/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
export default angular.module('thingsboard.types', [])
    .constant('types',
        {
            serverErrorCode: {
                general: 2,
                authentication: 10,
                jwtTokenExpired: 11,
                permissionDenied: 20,
                invalidArguments: 30,
                badRequestParams: 31,
                itemNotFound: 32
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
                stateEntity: {
                    value: 'stateEntity',
                    name: 'alias.filter-type-state-entity'
                },
                assetType: {
                    value: 'assetType',
                    name: 'alias.filter-type-asset-type'
                },
                deviceType: {
                    value: 'deviceType',
                    name: 'alias.filter-type-device-type'
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
                alarm: "alarm"
            },
            componentType: {
                filter: "FILTER",
                processor: "PROCESSOR",
                action: "ACTION",
                plugin: "PLUGIN"
            },
            entityType: {
                device: "DEVICE",
                asset: "ASSET",
                rule: "RULE",
                plugin: "PLUGIN",
                tenant: "TENANT",
                customer: "CUSTOMER",
                user: "USER",
                dashboard: "DASHBOARD",
                alarm: "ALARM",
                entityGroup: "ENTITY_GROUP"
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
                }
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
                "RULE": {
                    type: 'entity.type-rule',
                    typePlural: 'entity.type-rules',
                    list: 'entity.list-of-rules',
                    nameStartsWith: 'entity.rule-name-starts-with',
                    details: 'rule.rule-details',
                    add: 'rule.add'
                },
                "PLUGIN": {
                    type: 'entity.type-plugin',
                    typePlural: 'entity.type-plugins',
                    list: 'entity.list-of-plugins',
                    nameStartsWith: 'entity.plugin-name-starts-with',
                    details: 'plugin.plugin-details',
                    add: 'plugin.add'
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
                    search: 'user.search'
                },
                "DASHBOARD": {
                    type: 'entity.type-dashboard',
                    typePlural: 'entity.type-dashboards',
                    list: 'entity.list-of-dashboards',
                    nameStartsWith: 'entity.dashboard-name-starts-with',
                    details: 'dashboard.dashboard-details',
                    add: 'dashboard.add'
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
                }
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
            systemBundleAlias: {
                charts: "charts",
                cards: "cards"
            },
            translate: {
                customTranslationsPrefix: "custom."
            }
        }
    ).name;
