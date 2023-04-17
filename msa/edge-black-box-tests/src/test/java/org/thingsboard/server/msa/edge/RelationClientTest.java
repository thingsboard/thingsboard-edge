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
package org.thingsboard.server.msa.edge;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RelationClientTest extends AbstractContainerTest {

    @Test
    public void testRelations() {
        Device device = saveDeviceAndAssignEntityGroupToEdge(createEntityGroup(EntityType.DEVICE));
        Asset asset = saveAssetAndAssignEntityGroupToEdge(createEntityGroup(EntityType.ASSET));

        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(device.getId());
        relation.setTo(asset.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        cloudRestClient.saveRelation(relation);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isPresent());

        // delete relation
        cloudRestClient.deleteRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isEmpty());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
        cloudRestClient.deleteAsset(asset.getId());
    }

    @Test
    public void sendRelationToCloud() {
        Device device = saveDeviceAndAssignEntityGroupToEdge(createEntityGroup(EntityType.DEVICE));

        Device savedDeviceOnEdge = saveDeviceOnEdge("Test Device 3", "default");
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getDeviceById(savedDeviceOnEdge.getId()).isPresent());

        EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(device.getId());
        relation.setTo(savedDeviceOnEdge.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        edgeRestClient.saveRelation(relation);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isPresent());

        edgeRestClient.deleteRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> cloudRestClient.getRelation(relation.getFrom(), relation.getType(), relation.getTypeGroup(), relation.getTo()).isEmpty());

        // cleanup
        cloudRestClient.deleteDevice(device.getId());
        cloudRestClient.deleteDevice(savedDeviceOnEdge.getId());
    }

}


