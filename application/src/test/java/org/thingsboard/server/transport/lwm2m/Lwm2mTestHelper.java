/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.client.object.Security;

import static org.eclipse.leshan.client.object.Security.noSec;

public class Lwm2mTestHelper {

    // Server
    public static final int SECURE_PORT = 5686;
    public static final int SECURE_PORT_BS = 5688;
    public static final int PORT = 5685;
    public static final int PORT_BS = 5687;
    public static final String HOST = "localhost";
    public static final String HOST_BS = "localhost";
    public static final int SHORT_SERVER_ID = 123;
    public static final int SHORT_SERVER_ID_BS = 111;

    public static final NetworkConfig SECURE_COAP_CONFIG = new NetworkConfig().setString("COAP_SECURE_PORT", Integer.toString(SECURE_PORT));
    public static final String SECURE_URI = "coaps://" + HOST + ":" + SECURE_PORT;
    public static final Security SECURITY = noSec("coap://"+ HOST +":" + PORT, SHORT_SERVER_ID);
    public static final NetworkConfig COAP_CONFIG = new NetworkConfig().setString("COAP_PORT", Integer.toString(PORT));

    // Models
    public static final String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "3.xml", "5.xml", "6.xml", "9.xml", "19.xml", "3303.xml"};
    public static final int BINARY_APP_DATA_CONTAINER = 19;
    public static final int TEMPERATURE_SENSOR = 3303;

    // Ids in Client
    public static final int OBJECT_ID_0 = 0;
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
}
