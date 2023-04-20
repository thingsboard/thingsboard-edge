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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.ColumnConfiguration;
import org.thingsboard.server.common.data.group.ColumnType;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseEntityGroupServiceTest extends AbstractServiceTest {
    static final int TIMEOUT = 30;

    @Autowired
    private AttributesService attributesService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    AssetService assetService;
    @Autowired
    EntityGroupService entityGroupService;
    @Autowired
    EdgeService edgeService;

    private MergedUserPermissions mergedUserPermissions;

    ListeningExecutorService executor;

    @Before
    public void beforeRun() {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        genericPermissions.put(Resource.resourceFromEntityType(EntityType.DEVICE), Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.resourceFromEntityType(EntityType.ASSET), Collections.singleton(Operation.ALL));
        mergedUserPermissions = new MergedUserPermissions(genericPermissions, Collections.emptyMap());
    }

    @After
    public void after() {
        executor.shutdownNow();
    }

    @Test
    public void testFindGroupEntityIds() throws ExecutionException, InterruptedException, TimeoutException {
        List<ListenableFuture<Device>> futures = new ArrayList<>(97);

        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            futures.add(executor.submit(() -> deviceService.saveDevice(device)));
        }

        List<Device> devices = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        Optional<EntityGroup> devicesGroupOptional =
                entityGroupService.findEntityGroupByTypeAndName(tenantId, tenantId, EntityType.DEVICE, EntityGroup.GROUP_ALL_NAME);
        Assert.assertTrue(devicesGroupOptional.isPresent());
        EntityGroup devicesGroup = devicesGroupOptional.get();

        PageLink pageLink = new PageLink(Integer.MAX_VALUE);
        List<EntityId> entityIds = entityGroupService.findAllEntityIdsAsync(tenantId, devicesGroup.getId(), pageLink).get();
        assertThat(entityIds).containsExactlyInAnyOrderElementsOf(
                devices.stream().map(IdBased::getId).collect(Collectors.toList()));

        Futures.allAsList(devices.stream()
                        .map(d -> executor.submit(() -> deviceService.deleteDevice(d.getTenantId(), d.getId())))
                        .collect(Collectors.toList()))
                .get(TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    public void testFindGroupEntities() throws ExecutionException, InterruptedException {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
        }

        Optional<EntityGroup> devicesGroupOptional =
                entityGroupService.findEntityGroupByTypeAndName(tenantId, tenantId, EntityType.DEVICE, EntityGroup.GROUP_ALL_NAME);
        Assert.assertTrue(devicesGroupOptional.isPresent());
        EntityGroup devicesGroup = devicesGroupOptional.get();

        PageLink pageLink = new PageLink(20, 0, "", new SortOrder("label", SortOrder.Direction.DESC));
        PageData<ShortEntityView> groupEntities = entityGroupService.findGroupEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), mergedUserPermissions, devicesGroup.getId(), pageLink);
        Assert.assertNotNull(groupEntities);
        Assert.assertEquals(97, groupEntities.getTotalElements());
        Assert.assertEquals(5, groupEntities.getTotalPages());
        Assert.assertEquals(true, groupEntities.hasNext());
        Assert.assertEquals(20, groupEntities.getData().size());
        List<ShortEntityView> allGroupEntities = new ArrayList<>();
        allGroupEntities.addAll(groupEntities.getData());
        while (groupEntities.hasNext()) {
            pageLink = pageLink.nextPageLink();
            groupEntities = entityGroupService.findGroupEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), mergedUserPermissions, devicesGroup.getId(), pageLink);
            allGroupEntities.addAll(groupEntities.getData());
        }
        Assert.assertEquals(97, allGroupEntities.size());

        assertThat(allGroupEntities.stream().map(ShortEntityView::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(
                        devices.stream().map(IdBased::getId).collect(Collectors.toList()));

        pageLink = new PageLink(20, 0, "device1", new SortOrder("name", SortOrder.Direction.DESC));
        groupEntities = entityGroupService.findGroupEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), mergedUserPermissions, devicesGroup.getId(), pageLink);
        Assert.assertNotNull(groupEntities);
        Assert.assertEquals(11, groupEntities.getTotalElements());
        Assert.assertEquals(1, groupEntities.getTotalPages());
        Assert.assertEquals(false, groupEntities.hasNext());
        Assert.assertEquals(11, groupEntities.getData().size());
        Assert.assertEquals("Device19", groupEntities.getData().get(0).getName());
        Assert.assertEquals("Device1", groupEntities.getData().get(groupEntities.getData().size() - 1).getName());

        EntityGroup testDevicesGroup = new EntityGroup();
        testDevicesGroup.setType(EntityType.DEVICE);
        testDevicesGroup.setName("Test devices");
        testDevicesGroup.setOwnerId(tenantId);

        EntityGroupConfiguration entityGroupConfiguration = new EntityGroupConfiguration();

        entityGroupConfiguration.setColumns(Arrays.asList(
                new ColumnConfiguration(ColumnType.ENTITY_FIELD, EntityField.NAME.name().toLowerCase())
        ));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonConfiguration = mapper.valueToTree(entityGroupConfiguration);
        jsonConfiguration.putObject("settings");
        jsonConfiguration.putObject("actions");
        testDevicesGroup.setConfiguration(jsonConfiguration);
        testDevicesGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, testDevicesGroup);

        List<Device> testGroupDevices = devices.subList(0, 23);

        entityGroupService.addEntitiesToEntityGroup(tenantId,
                testDevicesGroup.getId(),
                testGroupDevices.stream().map(IdBased::getId).collect(Collectors.toList()));

        pageLink = new PageLink(20, 0, "", new SortOrder("name", SortOrder.Direction.ASC));
        groupEntities = entityGroupService.findGroupEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), mergedUserPermissions, testDevicesGroup.getId(), pageLink);
        Assert.assertNotNull(groupEntities);
        Assert.assertEquals(23, groupEntities.getTotalElements());
        Assert.assertEquals(2, groupEntities.getTotalPages());
        Assert.assertEquals(true, groupEntities.hasNext());
        Assert.assertEquals(20, groupEntities.getData().size());

        allGroupEntities = new ArrayList<>();
        allGroupEntities.addAll(groupEntities.getData());
        while (groupEntities.hasNext()) {
            pageLink = pageLink.nextPageLink();
            groupEntities = entityGroupService.findGroupEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), mergedUserPermissions, testDevicesGroup.getId(), pageLink);
            allGroupEntities.addAll(groupEntities.getData());
        }
        Assert.assertEquals(23, allGroupEntities.size());
        Assert.assertEquals("Device0", allGroupEntities.get(0).getName());
        Assert.assertEquals("Device9", allGroupEntities.get(allGroupEntities.size() - 1).getName());

        assertThat(allGroupEntities.stream().map(ShortEntityView::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(
                        testGroupDevices.stream().map(IdBased::getId).collect(Collectors.toList()));
    }

    @Test
    public void testFindGroupEntityProfileType() throws ExecutionException, InterruptedException {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("testDevice");
        device.setType("default");
        device = deviceService.saveDevice(device);

        Optional<EntityGroup> devicesGroupOptional =
                entityGroupService.findEntityGroupByTypeAndName(tenantId, tenantId, EntityType.DEVICE, EntityGroup.GROUP_ALL_NAME);
        Assert.assertTrue(devicesGroupOptional.isPresent());
        EntityGroup devicesGroup = devicesGroupOptional.get();

        ShortEntityView shortDevice = entityGroupService.findGroupEntity(tenantId, new CustomerId(CustomerId.NULL_UUID),
                mergedUserPermissions, devicesGroup.getId(), device.getId());

        Assert.assertNotNull(shortDevice);
        Assert.assertEquals("testDevice", shortDevice.getName());
        Assert.assertEquals("default", shortDevice.properties().get("device_profile"));

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("testAsset");
        asset.setType("default");
        asset = assetService.saveAsset(asset);

        Optional<EntityGroup> assetsGroupOptional =
                entityGroupService.findEntityGroupByTypeAndName(tenantId, tenantId, EntityType.ASSET, EntityGroup.GROUP_ALL_NAME);
        Assert.assertTrue(assetsGroupOptional.isPresent());
        EntityGroup assetGroup = assetsGroupOptional.get();

        ShortEntityView shortAsset = entityGroupService.findGroupEntity(tenantId, new CustomerId(CustomerId.NULL_UUID),
                mergedUserPermissions, assetGroup.getId(), asset.getId());

        Assert.assertNotNull(shortAsset);
        Assert.assertEquals("testAsset", shortAsset.getName());
        Assert.assertEquals("default", shortAsset.properties().get("asset_profile"));
    }

    @Test
    public void testFindGroupEntitiesWithAttributes() throws ExecutionException, InterruptedException {
        EntityGroup testDevicesWithAttributesGroup = new EntityGroup();
        testDevicesWithAttributesGroup.setType(EntityType.DEVICE);
        testDevicesWithAttributesGroup.setName("Test devices with attributes");
        testDevicesWithAttributesGroup.setOwnerId(tenantId);

        EntityGroupConfiguration entityGroupConfiguration = new EntityGroupConfiguration();

        entityGroupConfiguration.setColumns(Arrays.asList(
                new ColumnConfiguration(ColumnType.ENTITY_FIELD, EntityField.CREATED_TIME.name().toLowerCase()),
                new ColumnConfiguration(ColumnType.ENTITY_FIELD, EntityField.NAME.name().toLowerCase()),
                new ColumnConfiguration(ColumnType.ENTITY_FIELD, EntityField.TYPE.name().toLowerCase()),
                new ColumnConfiguration(ColumnType.ENTITY_FIELD, EntityField.LABEL.name().toLowerCase()),
                new ColumnConfiguration(ColumnType.SERVER_ATTRIBUTE, "serverAttr1"),
                new ColumnConfiguration(ColumnType.SERVER_ATTRIBUTE, "serverAttr2"),
                new ColumnConfiguration(ColumnType.SHARED_ATTRIBUTE, "sharedAttr1"),
                new ColumnConfiguration(ColumnType.SHARED_ATTRIBUTE, "sharedAttr2"),
                new ColumnConfiguration(ColumnType.CLIENT_ATTRIBUTE, "clientAttr1"),
                new ColumnConfiguration(ColumnType.CLIENT_ATTRIBUTE, "clientAttr2"),
                new ColumnConfiguration(ColumnType.SERVER_ATTRIBUTE, "emptyAttr")
        ));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonConfiguration = mapper.valueToTree(entityGroupConfiguration);
        jsonConfiguration.putObject("settings");
        jsonConfiguration.putObject("actions");
        testDevicesWithAttributesGroup.setConfiguration(jsonConfiguration);
        testDevicesWithAttributesGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, testDevicesWithAttributesGroup);

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 67; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
        }

        List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveStringAttribute(device.getId(), "serverAttr1", "serverValue1_" + i, DataConstants.SERVER_SCOPE));
            attributeFutures.add(saveLongAttribute(device.getId(), "serverAttr2", i, DataConstants.SERVER_SCOPE));
            attributeFutures.add(saveStringAttribute(device.getId(), "sharedAttr1", "sharedValue1_" + i, DataConstants.SHARED_SCOPE));
            attributeFutures.add(saveLongAttribute(device.getId(), "sharedAttr2", i, DataConstants.SHARED_SCOPE));
            attributeFutures.add(saveStringAttribute(device.getId(), "clientAttr1", "clientValue1_" + i, DataConstants.CLIENT_SCOPE));
            attributeFutures.add(saveLongAttribute(device.getId(), "clientAttr2", i, DataConstants.CLIENT_SCOPE));
        }
        Futures.successfulAsList(attributeFutures).get();

        entityGroupService.addEntitiesToEntityGroup(tenantId,
                testDevicesWithAttributesGroup.getId(),
                devices.stream().map(IdBased::getId).collect(Collectors.toList()));

        PageLink pageLink = new PageLink(20, 0, "", new SortOrder(EntityField.CREATED_TIME.name().toLowerCase(), SortOrder.Direction.ASC));
        PageData<ShortEntityView> groupEntities = entityGroupService.findGroupEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), mergedUserPermissions, testDevicesWithAttributesGroup.getId(), pageLink);
        Assert.assertNotNull(groupEntities);
        Assert.assertEquals(67, groupEntities.getTotalElements());
        Assert.assertEquals(4, groupEntities.getTotalPages());
        Assert.assertEquals(true, groupEntities.hasNext());
        Assert.assertEquals(20, groupEntities.getData().size());
        List<ShortEntityView> allGroupEntities = new ArrayList<>();
        allGroupEntities.addAll(groupEntities.getData());
        while (groupEntities.hasNext()) {
            pageLink = pageLink.nextPageLink();
            groupEntities = entityGroupService.findGroupEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), mergedUserPermissions, testDevicesWithAttributesGroup.getId(), pageLink);
            allGroupEntities.addAll(groupEntities.getData());
        }
        List<EntityId> foundIds = allGroupEntities.stream().map(ShortEntityView::getId).collect(Collectors.toList());
        List<EntityId> deviceIds = devices.stream().map(IdBased::getId).collect(Collectors.toList());
        Assert.assertEquals(foundIds, deviceIds);

        pageLink = new PageLink(20, 0, "serverValue1_1", new SortOrder("serverAttr1", SortOrder.Direction.DESC));
        groupEntities = entityGroupService.findGroupEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), mergedUserPermissions, testDevicesWithAttributesGroup.getId(), pageLink);
        Assert.assertNotNull(groupEntities);
        Assert.assertEquals(11, groupEntities.getTotalElements());
        Assert.assertEquals(1, groupEntities.getTotalPages());
        Assert.assertEquals(false, groupEntities.hasNext());
        Assert.assertEquals(11, groupEntities.getData().size());
        Assert.assertEquals("serverValue1_19", groupEntities.getData().get(0).properties().get("server_serverAttr1"));
    }

    @Test
    public void testFindGroupEntity() throws ExecutionException, InterruptedException {
        EntityGroup testDevicesWithAttributesGroup = new EntityGroup();
        testDevicesWithAttributesGroup.setType(EntityType.DEVICE);
        testDevicesWithAttributesGroup.setName("Test devices with attributes");
        testDevicesWithAttributesGroup.setOwnerId(tenantId);

        EntityGroupConfiguration entityGroupConfiguration = new EntityGroupConfiguration();

        entityGroupConfiguration.setColumns(Arrays.asList(
                new ColumnConfiguration(ColumnType.ENTITY_FIELD, EntityField.CREATED_TIME.name().toLowerCase()),
                new ColumnConfiguration(ColumnType.ENTITY_FIELD, EntityField.NAME.name().toLowerCase()),
                new ColumnConfiguration(ColumnType.ENTITY_FIELD, EntityField.TYPE.name().toLowerCase()),
                new ColumnConfiguration(ColumnType.ENTITY_FIELD, EntityField.LABEL.name().toLowerCase()),
                new ColumnConfiguration(ColumnType.SERVER_ATTRIBUTE, "serverAttr1"),
                new ColumnConfiguration(ColumnType.SERVER_ATTRIBUTE, "serverAttr2"),
                new ColumnConfiguration(ColumnType.SHARED_ATTRIBUTE, "sharedAttr1"),
                new ColumnConfiguration(ColumnType.SHARED_ATTRIBUTE, "sharedAttr2"),
                new ColumnConfiguration(ColumnType.CLIENT_ATTRIBUTE, "clientAttr1"),
                new ColumnConfiguration(ColumnType.CLIENT_ATTRIBUTE, "clientAttr2"),
                new ColumnConfiguration(ColumnType.SERVER_ATTRIBUTE, "emptyAttr")
        ));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonConfiguration = mapper.valueToTree(entityGroupConfiguration);
        jsonConfiguration.putObject("settings");
        jsonConfiguration.putObject("actions");
        testDevicesWithAttributesGroup.setConfiguration(jsonConfiguration);
        testDevicesWithAttributesGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, testDevicesWithAttributesGroup);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Device1");
        device.setType("default");
        device.setLabel("testLabel1");
        device = deviceService.saveDevice(device);

        List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        attributeFutures.add(saveStringAttribute(device.getId(), "serverAttr1", "serverValue1_1", DataConstants.SERVER_SCOPE));
        attributeFutures.add(saveLongAttribute(device.getId(), "serverAttr2", 1, DataConstants.SERVER_SCOPE));
        attributeFutures.add(saveStringAttribute(device.getId(), "sharedAttr1", "sharedValue1_1", DataConstants.SHARED_SCOPE));
        attributeFutures.add(saveLongAttribute(device.getId(), "sharedAttr2", 1, DataConstants.SHARED_SCOPE));
        attributeFutures.add(saveStringAttribute(device.getId(), "clientAttr1", "clientValue1_1", DataConstants.CLIENT_SCOPE));
        attributeFutures.add(saveLongAttribute(device.getId(), "clientAttr2", 1, DataConstants.CLIENT_SCOPE));
        Futures.successfulAsList(attributeFutures).get();

        entityGroupService.addEntityToEntityGroup(tenantId, testDevicesWithAttributesGroup.getId(), device.getId());

        ShortEntityView shortEntityView = entityGroupService.findGroupEntity(tenantId, new CustomerId(CustomerId.NULL_UUID), mergedUserPermissions, testDevicesWithAttributesGroup.getId(), device.getId());
        Assert.assertNotNull(shortEntityView);
        Assert.assertEquals(device.getId(), shortEntityView.getId());
        Assert.assertEquals(device.getCreatedTime() + "", shortEntityView.properties().get(EntityField.CREATED_TIME.name().toLowerCase()));
        Assert.assertEquals(device.getName(), shortEntityView.properties().get(EntityField.NAME.name().toLowerCase()));
        Assert.assertEquals(device.getType(), shortEntityView.properties().get(EntityField.DEVICE_PROFILE.name().toLowerCase()));
        Assert.assertEquals(device.getLabel(), shortEntityView.properties().get(EntityField.LABEL.name().toLowerCase()));
        Assert.assertEquals("serverValue1_1", shortEntityView.properties().get("server_serverAttr1"));
        Assert.assertEquals("1", shortEntityView.properties().get("server_serverAttr2"));
        Assert.assertEquals("sharedValue1_1", shortEntityView.properties().get("shared_sharedAttr1"));
        Assert.assertEquals("1", shortEntityView.properties().get("shared_sharedAttr2"));
        Assert.assertEquals("clientValue1_1", shortEntityView.properties().get("client_clientAttr1"));
        Assert.assertEquals("1", shortEntityView.properties().get("client_clientAttr2"));
        Assert.assertEquals("", shortEntityView.properties().get("server_emptyAttr"));
    }

    @Test
    public void testFindEdgeEntityGroupsByTenantIdAndNameAndType() {
        Edge edge = constructEdge(tenantId, "My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);

        String name1 = "Edge Entity Group name 1";
        List<EntityGroup> entityGroupsName1 = new ArrayList<>();
        for (int i = 0; i < 123; i++) {
            EntityGroup entityGroup = new EntityGroup();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = name1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            entityGroup.setName(name);
            entityGroup.setType(EntityType.DEVICE);
            entityGroupsName1.add(entityGroupService.saveEntityGroup(tenantId, tenantId, entityGroup));
        }
        entityGroupsName1.forEach(entityGroup ->
                entityGroupService.assignEntityGroupToEdge(tenantId, entityGroup.getId(), savedEdge.getId(), EntityType.DEVICE));

        String name2 = "Edge Entity Group name 2";
        List<EntityGroup> entityGroupsName2 = new ArrayList<>();
        for (int i = 0; i < 193; i++) {
            EntityGroup entityGroup = new EntityGroup();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = name2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            entityGroup.setName(name);
            entityGroup.setType(EntityType.ASSET);
            entityGroupsName2.add(entityGroupService.saveEntityGroup(tenantId, tenantId, entityGroup));
        }
        entityGroupsName2.forEach(entityGroup ->
                entityGroupService.assignEntityGroupToEdge(tenantId, entityGroup.getId(), savedEdge.getId(), EntityType.ASSET));

        List<EntityGroup> loadedEntityGroupsName1 = new ArrayList<>();
        PageLink pageLink = new PageLink(19, 0, name1);
        PageData<EntityGroup> pageData = null;
        do {
            pageData = entityGroupService.findEdgeEntityGroupsByType(tenantId, savedEdge.getId(), EntityType.DEVICE, pageLink);
            loadedEntityGroupsName1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(entityGroupsName1).as(name1).containsExactlyInAnyOrderElementsOf(loadedEntityGroupsName1);

        List<EntityGroup> loadedEntityGroupsName2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, name2);
        do {
            pageData = entityGroupService.findEdgeEntityGroupsByType(tenantId, savedEdge.getId(), EntityType.ASSET, pageLink);
            loadedEntityGroupsName2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(entityGroupsName2).as(name2).containsExactlyInAnyOrderElementsOf(loadedEntityGroupsName2);

        for (EntityGroup entityGroup : loadedEntityGroupsName1) {
            entityGroupService.deleteEntityGroup(tenantId, entityGroup.getId());
        }

        pageLink = new PageLink(4, 0, name1);
        pageData = entityGroupService.findEdgeEntityGroupsByType(tenantId, savedEdge.getId(), EntityType.DEVICE, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (EntityGroup entityGroup : loadedEntityGroupsName2) {
            entityGroupService.deleteEntityGroup(tenantId, entityGroup.getId());
        }

        pageLink = new PageLink(4, 0, name2);
        pageData = entityGroupService.findEdgeEntityGroupsByType(tenantId, savedEdge.getId(), EntityType.ASSET, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        edgeService.deleteEdge(tenantId, savedEdge.getId());
    }

    private ListenableFuture<List<String>> saveStringAttribute(EntityId entityId, String key, String value, String scope) {
        KvEntry attrValue = new StringDataEntry(key, value);
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        return attributesService.save(SYSTEM_TENANT_ID, entityId, scope, Collections.singletonList(attr));
    }

    private ListenableFuture<List<String>> saveLongAttribute(EntityId entityId, String key, long value, String scope) {
        KvEntry attrValue = new LongDataEntry(key, value);
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        return attributesService.save(SYSTEM_TENANT_ID, entityId, scope, Collections.singletonList(attr));
    }
}
