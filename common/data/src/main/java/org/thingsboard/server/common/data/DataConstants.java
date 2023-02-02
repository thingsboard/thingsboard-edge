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
package org.thingsboard.server.common.data;

/**
 * @author Andrew Shvayka
 */
public class DataConstants {

    public static final String TENANT = "TENANT";
    public static final String CUSTOMER = "CUSTOMER";
    public static final String DEVICE = "DEVICE";

    public static final String SCOPE = "scope";
    public static final String CLIENT_SCOPE = "CLIENT_SCOPE";
    public static final String SERVER_SCOPE = "SERVER_SCOPE";
    public static final String SHARED_SCOPE = "SHARED_SCOPE";
    public static final String NOTIFY_DEVICE_METADATA_KEY = "notifyDevice";
    public static final String LATEST_TS = "LATEST_TS";
    public static final String IS_NEW_ALARM = "isNewAlarm";
    public static final String IS_EXISTING_ALARM = "isExistingAlarm";
    public static final String IS_SEVERITY_UPDATED_ALARM = "isSeverityUpdated";
    public static final String IS_CLEARED_ALARM = "isClearedAlarm";
    public static final String ALARM_CONDITION_REPEATS = "alarmConditionRepeats";
    public static final String ALARM_CONDITION_DURATION = "alarmConditionDuration";
    public static final String PERSISTENT = "persistent";
    public static final String TIMEOUT = "timeout";
    public static final String EXPIRATION_TIME = "expirationTime";
    public static final String ADDITIONAL_INFO = "additionalInfo";
    public static final String RETRIES = "retries";
    public static final String EDGE_ID = "edgeId";
    public static final String DEVICE_ID = "deviceId";
    public static final String COAP_TRANSPORT_NAME = "COAP";
    public static final String LWM2M_TRANSPORT_NAME = "LWM2M";
    public static final String MQTT_TRANSPORT_NAME = "MQTT";
    public static final String HTTP_TRANSPORT_NAME = "HTTP";
    public static final String SNMP_TRANSPORT_NAME = "SNMP";


    public static final String[] allScopes() {
        return new String[]{CLIENT_SCOPE, SHARED_SCOPE, SERVER_SCOPE};
    }

    public static final String ALARM = "ALARM";
    public static final String IN = "IN";
    public static final String OUT = "OUT";

    public static final String INACTIVITY_EVENT = "INACTIVITY_EVENT";
    public static final String CONNECT_EVENT = "CONNECT_EVENT";
    public static final String DISCONNECT_EVENT = "DISCONNECT_EVENT";
    public static final String ACTIVITY_EVENT = "ACTIVITY_EVENT";

    public static final String ENTITY_CREATED = "ENTITY_CREATED";
    public static final String ENTITY_UPDATED = "ENTITY_UPDATED";
    public static final String ENTITY_DELETED = "ENTITY_DELETED";
    public static final String ENTITY_ASSIGNED = "ENTITY_ASSIGNED";
    public static final String ENTITY_UNASSIGNED = "ENTITY_UNASSIGNED";
    public static final String ATTRIBUTES_UPDATED = "ATTRIBUTES_UPDATED";
    public static final String ATTRIBUTES_DELETED = "ATTRIBUTES_DELETED";
    public static final String ADDED_TO_ENTITY_GROUP = "ADDED_TO_ENTITY_GROUP";
    public static final String REMOVED_FROM_ENTITY_GROUP = "REMOVED_FROM_ENTITY_GROUP";
    public static final String REST_API_REQUEST = "REST_API_REQUEST";
    public static final String TIMESERIES_UPDATED = "TIMESERIES_UPDATED";
    public static final String TIMESERIES_DELETED = "TIMESERIES_DELETED";
    public static final String ALARM_ACK = "ALARM_ACK";
    public static final String ALARM_CLEAR = "ALARM_CLEAR";
    public static final String ALARM_DELETE = "ALARM_DELETE";
    public static final String ENTITY_ASSIGNED_FROM_TENANT = "ENTITY_ASSIGNED_FROM_TENANT";
    public static final String ENTITY_ASSIGNED_TO_TENANT = "ENTITY_ASSIGNED_TO_TENANT";
    public static final String PROVISION_SUCCESS = "PROVISION_SUCCESS";
    public static final String PROVISION_FAILURE = "PROVISION_FAILURE";
    public static final String OWNER_CHANGED = "OWNER_CHANGED";
    public static final String ENTITY_ASSIGNED_TO_EDGE = "ENTITY_ASSIGNED_TO_EDGE";
    public static final String ENTITY_UNASSIGNED_FROM_EDGE = "ENTITY_UNASSIGNED_FROM_EDGE";

    public static final String RELATION_ADD_OR_UPDATE = "RELATION_ADD_OR_UPDATE";
    public static final String RELATION_DELETED = "RELATION_DELETED";
    public static final String RELATIONS_DELETED = "RELATIONS_DELETED";

    public static final String RPC_CALL_FROM_SERVER_TO_DEVICE = "RPC_CALL_FROM_SERVER_TO_DEVICE";

    public static final String GENERATE_REPORT = "generateReport";

    public static final String RPC_QUEUED = "RPC_QUEUED";
    public static final String RPC_SENT = "RPC_SENT";
    public static final String RPC_DELIVERED = "RPC_DELIVERED";
    public static final String RPC_SUCCESSFUL = "RPC_SUCCESSFUL";
    public static final String RPC_TIMEOUT = "RPC_TIMEOUT";
    public static final String RPC_EXPIRED = "RPC_EXPIRED";
    public static final String RPC_FAILED = "RPC_FAILED";
    public static final String RPC_DELETED = "RPC_DELETED";

    public static final String DEFAULT_SECRET_KEY = "";
    public static final String SECRET_KEY_FIELD_NAME = "secretKey";
    public static final String DURATION_MS_FIELD_NAME = "durationMs";

    public static final String PROVISION = "provision";
    public static final String PROVISION_KEY = "provisionDeviceKey";
    public static final String PROVISION_SECRET = "provisionDeviceSecret";

    public static final String DEVICE_NAME = "deviceName";
    public static final String DEVICE_TYPE = "deviceType";
    public static final String CERT_PUB_KEY = "x509CertPubKey";
    public static final String CREDENTIALS_TYPE = "credentialsType";
    public static final String TOKEN = "token";
    public static final String HASH = "hash";
    public static final String CLIENT_ID = "clientId";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public static final String EDGE_MSG_SOURCE = "edge";
    public static final String MSG_SOURCE_KEY = "source";

    public static final String UPDATE_FIRMWARE = "updateFirmware";
    public static final String UPDATE_SOFTWARE = "updateSoftware";

    public static final String SELF_REGISTRATION_DOMAIN_NAME_PREFIX = "selfRegistrationDomainNamePrefix_";

    public static final String LAST_CONNECTED_GATEWAY = "lastConnectedGateway";

    public static final String MAIN_QUEUE_NAME = "Main";
    public static final String MAIN_QUEUE_TOPIC = "tb_rule_engine.main";
    public static final String HP_QUEUE_NAME = "HighPriority";
    public static final String HP_QUEUE_TOPIC = "tb_rule_engine.hp";
    public static final String SQ_QUEUE_NAME = "SequentialByOriginator";
    public static final String SQ_QUEUE_TOPIC = "tb_rule_engine.sq";

}
