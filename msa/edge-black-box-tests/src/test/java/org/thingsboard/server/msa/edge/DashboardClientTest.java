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
package org.thingsboard.server.msa.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DashboardClientTest extends AbstractContainerTest {

    @Test
    public void testDashboards() throws Exception {
        verifyEntityGroups(EntityType.DASHBOARD, 1);

        EntityGroup dashboardEntityGroup = new EntityGroup();
        dashboardEntityGroup.setType(EntityType.DASHBOARD);
        dashboardEntityGroup.setName("DashboardGroup");
        EntityGroupInfo savedDashboardEntityGroup = cloudRestClient.saveEntityGroup(dashboardEntityGroup);
        Dashboard savedDashboardOnCloud = saveDashboardOnCloud("Edge Dashboard 1", savedDashboardEntityGroup.getId());

        cloudRestClient.assignEntityGroupToEdge(edge.getId(), savedDashboardEntityGroup.getId(), EntityType.DASHBOARD);

        verifyEntityGroups(EntityType.DASHBOARD, 2);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboardOnCloud.getId()).isPresent());

        JsonNode dashboardAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"dashboardKey\":\"dashboardValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedDashboardOnCloud.getId(), DataConstants.SERVER_SCOPE, dashboardAttributes);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnEdge(savedDashboardOnCloud.getId(),
                        DataConstants.SERVER_SCOPE, "dashboardKey", "dashboardValue"));

        cloudRestClient.unassignEntityGroupFromEdge(edge.getId(), savedDashboardEntityGroup.getId(), EntityType.DASHBOARD);

        verifyEntityGroups(EntityType.DASHBOARD, 1);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboardOnCloud.getId()).isEmpty());

        cloudRestClient.deleteDashboard(savedDashboardOnCloud.getId());
    }

    private Dashboard saveDashboardOnCloud(String dashboardTitle, EntityGroupId entityGroupId) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(dashboardTitle);
        Dashboard savedDashboard = cloudRestClient.saveDashboard(dashboard);
        if (entityGroupId != null) {
            cloudRestClient.addEntitiesToEntityGroup(entityGroupId, Arrays.asList(savedDashboard.getId()));
        }
        return savedDashboard;
    }
}

