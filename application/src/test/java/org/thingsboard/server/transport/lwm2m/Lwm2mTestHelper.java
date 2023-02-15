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
package org.thingsboard.server.transport.lwm2m;

public class Lwm2mTestHelper {

    // Models
    public static final String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "3.xml", "5.xml", "6.xml", "9.xml", "19.xml", "3303.xml"};
    public static final int BINARY_APP_DATA_CONTAINER = 19;
    public static final int TEMPERATURE_SENSOR = 3303;

    // Ids in Client
    public static final int OBJECT_ID_0 = 0;
    public static final int OBJECT_ID_1 = 1;
    public static final int OBJECT_INSTANCE_ID_0 = 0;
    public static final int OBJECT_INSTANCE_ID_1 = 1;
    public static final int OBJECT_INSTANCE_ID_2 = 2;
    public static final int OBJECT_INSTANCE_ID_12 = 12;
    public static final int RESOURCE_ID_0 = 0;
    public static final int RESOURCE_ID_1 = 1;
    public static final int RESOURCE_ID_2 = 2;
    public static final int RESOURCE_ID_3 = 3;
    public static final int RESOURCE_ID_4 = 4;
    public static final int RESOURCE_ID_7 = 7;
    public static final int RESOURCE_ID_8 = 8;
    public static final int RESOURCE_ID_9 = 9;
    public static final int RESOURCE_ID_11 = 11;
    public static final int RESOURCE_ID_14 = 14;
    public static final int RESOURCE_ID_15 = 15;
    public static final int RESOURCE_INSTANCE_ID_2 = 2;

    public static final String RESOURCE_ID_NAME_3_9 = "batteryLevel";
    public static final String RESOURCE_ID_NAME_3_14 = "UtfOffset";
    public static final String RESOURCE_ID_NAME_19_0_0 = "dataRead";
    public static final String RESOURCE_ID_NAME_19_1_0 = "dataWrite";
    public static final String RESOURCE_ID_NAME_19_0_3 = "dataDescription";

    public enum LwM2MClientState {

        ON_INIT(1, "onInit"),
        ON_BOOTSTRAP_STARTED(1, "onBootstrapStarted"),
        ON_BOOTSTRAP_SUCCESS(2, "onBootstrapSuccess"),
        ON_BOOTSTRAP_FAILURE(3, "onBootstrapFailure"),
        ON_BOOTSTRAP_TIMEOUT(4, "onBootstrapTimeout"),
        ON_REGISTRATION_STARTED(5, "onRegistrationStarted"),
        ON_REGISTRATION_SUCCESS(6, "onRegistrationSuccess"),
        ON_REGISTRATION_FAILURE(7, "onRegistrationFailure"),
        ON_REGISTRATION_TIMEOUT(7, "onRegistrationTimeout"),
        ON_UPDATE_STARTED(8, "onUpdateStarted"),
        ON_UPDATE_SUCCESS(9, "onUpdateSuccess"),
        ON_UPDATE_FAILURE(10, "onUpdateFailure"),
        ON_UPDATE_TIMEOUT(11, "onUpdateTimeout"),
        ON_DEREGISTRATION_STARTED(12, "onDeregistrationStarted"),
        ON_DEREGISTRATION_SUCCESS(13, "onDeregistrationSuccess"),
        ON_DEREGISTRATION_FAILURE(14, "onDeregistrationFailure"),
        ON_DEREGISTRATION_TIMEOUT(15, "onDeregistrationTimeout"),
        ON_EXPECTED_ERROR(16, "onUnexpectedError");

        public int code;
        public String type;

        LwM2MClientState(int code, String type) {
            this.code = code;
            this.type = type;
        }

        public static LwM2MClientState fromLwM2MClientStateByType(String type) {
            for (LwM2MClientState to : LwM2MClientState.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client State type  : %s", type));
        }

        public static LwM2MClientState fromLwM2MClientStateByCode(int code) {
            for (LwM2MClientState to : LwM2MClientState.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client State code : %s", code));
        }
    }

    public enum LwM2MProfileBootstrapConfigType {

        LWM2M_ONLY(1, "only Lwm2m Server"),
        BOOTSTRAP_ONLY(2, "only Bootstrap Server"),
        BOTH(3, "Lwm2m Server and Bootstrap Server"),
        NONE(4, "Without Lwm2m Server and Bootstrap Server");

        public int code;
        public String type;

        LwM2MProfileBootstrapConfigType(int code, String type) {
            this.code = code;
            this.type = type;
        }

        public static LwM2MProfileBootstrapConfigType fromLwM2MBootstrapConfigByType(String type) {
            for (LwM2MProfileBootstrapConfigType to : LwM2MProfileBootstrapConfigType.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Profile Bootstrap Config type  : %s", type));
        }

        public static LwM2MProfileBootstrapConfigType fromLwM2MBootstrapConfigByCode(int code) {
            for (LwM2MProfileBootstrapConfigType to : LwM2MProfileBootstrapConfigType.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Profile Bootstrap Config code : %s", code));
        }
    }
}
