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
package org.thingsboard.server.msa.connectivity;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Optional;

public class EdgeClientTest extends AbstractContainerTest {

    @Test
    public void edgeCreateAndConnect() {
        restClient.login("tenant@thingsboard.org", "tenant");
        Edge edge = createEdge("test", "280629c7-f853-ee3d-01c0-fffbb6f2ef38", "g9ta4soeylw6smqkky8g");

        boolean loginSuccessful = false;
        int attempt = 0;
        do {
            try {
                edgeRestClient.login("tenant@thingsboard.org", "tenant");
                loginSuccessful = true;
            } catch (Exception ignored1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored2) {}
            }
            attempt++;
            if (attempt > 50) {
                break;
            }
        } while (!loginSuccessful);
        Assert.assertTrue(loginSuccessful);
        Optional<Tenant> tenant = edgeRestClient.getTenantById(edge.getTenantId());
        Assert.assertTrue(tenant.isPresent());
        Assert.assertEquals(edge.getTenantId(), tenant.get().getId());
    }
}

