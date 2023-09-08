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
package org.thingsboard.server.transport.snmp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class SnmpTestV2 {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        SnmpDeviceSimulatorV2 client = new SnmpDeviceSimulatorV2(1610, "public");

        client.start();
        Map<String, String> mappings = new HashMap<>();
//        for (int i = 1; i <= 500; i++) {
//            String oid = String.format(".1.3.6.1.2.1.%s.1.52", i);
//            mappings.put(oid, "value_" + i);
//        }
        mappings.put("1.3.6.1.2.1.266.1.52", "****");

        client.setUpMappings(mappings);
        inputTraps(client);

        scanner.nextLine();
    }

    private static void inputTraps(SnmpDeviceSimulatorV2 client) throws IOException {
        while (true) {
            String data = scanner.nextLine();
            if (!data.isEmpty()) {
                client.sendTrap("127.0.0.1", 1620, Map.of(
                        "1.3.6.1.2.1.266.1.52", data + " (266)",
                        "1.3.6.1.2.1.267.1.52", data + " (267)"
                ));
            }
        }
    }

}
