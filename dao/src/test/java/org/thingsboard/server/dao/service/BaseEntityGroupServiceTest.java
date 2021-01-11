/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.group.ColumnConfiguration;
import org.thingsboard.server.common.data.group.ColumnType;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
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
import org.thingsboard.server.dao.attributes.AttributesService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class BaseEntityGroupServiceTest extends AbstractBeforeTest {

    @Autowired
    private AttributesService attributesService;

    private MergedUserPermissions mergedUserPermissions;

    class EntityIdComparator implements Comparator<EntityId> {
        @Override
        public int compare(EntityId id1, EntityId id2) {
            return id1.getId().compareTo(id2.getId());
        }
    }

    private TenantId tenantId;

    @Before
    public void beforeRun() {
        tenantId = before();
        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        genericPermissions.put(Resource.resourceFromEntityType(EntityType.DEVICE), Collections.singleton(Operation.ALL));
        mergedUserPermissions = new MergedUserPermissions(genericPermissions, Collections.emptyMap());
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindGroupEntityIds() throws ExecutionException, InterruptedException {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

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
                entityGroupService.findEntityGroupByTypeAndName(tenantId, tenantId, EntityType.DEVICE, EntityGroup.GROUP_ALL_NAME).get();
        Assert.assertTrue(devicesGroupOptional.isPresent());
        EntityGroup devicesGroup = devicesGroupOptional.get();

        PageLink pageLink = new PageLink(Integer.MAX_VALUE);
        List<EntityId> entityIds = entityGroupService.findAllEntityIds(tenantId, devicesGroup.getId(), pageLink).get();
        Assert.assertNotNull(entityIds);

        List<EntityId> sortedIds = entityIds.stream().sorted(new EntityIdComparator()).collect(Collectors.toList());
        List<EntityId> sortedDeviceIds = devices.stream().map(IdBased::getId).sorted(new EntityIdComparator()).collect(Collectors.toList());
        Assert.assertEquals(sortedIds, sortedDeviceIds);

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindGroupEntities() throws ExecutionException, InterruptedException {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

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
                entityGroupService.findEntityGroupByTypeAndName(tenantId, tenantId, EntityType.DEVICE, EntityGroup.GROUP_ALL_NAME).get();
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
        List<EntityId> sortedIds = allGroupEntities.stream().map(ShortEntityView::getId).sorted(new EntityIdComparator()).collect(Collectors.toList());
        List<EntityId> sortedDeviceIds = devices.stream().map(IdBased::getId).sorted(new EntityIdComparator()).collect(Collectors.toList());
        Assert.assertEquals(sortedIds, sortedDeviceIds);

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

        sortedIds = allGroupEntities.stream().map(ShortEntityView::getId).sorted(new EntityIdComparator()).collect(Collectors.toList());
        sortedDeviceIds = testGroupDevices.stream().map(IdBased::getId).sorted(new EntityIdComparator()).collect(Collectors.toList());
        Assert.assertEquals(sortedIds, sortedDeviceIds);

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindGroupEntitiesWithAttributes() throws ExecutionException, InterruptedException {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

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
            Thread.sleep(1);
        }

        List<ListenableFuture<List<Void>>> attributeFutures = new ArrayList<>();
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

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindGroupEntity() throws ExecutionException, InterruptedException {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

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

        List<ListenableFuture<List<Void>>> attributeFutures = new ArrayList<>();
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
        Assert.assertEquals(device.getType(), shortEntityView.properties().get(EntityField.TYPE.name().toLowerCase()));
        Assert.assertEquals(device.getLabel(), shortEntityView.properties().get(EntityField.LABEL.name().toLowerCase()));
        Assert.assertEquals("serverValue1_1", shortEntityView.properties().get("server_serverAttr1"));
        Assert.assertEquals("1", shortEntityView.properties().get("server_serverAttr2"));
        Assert.assertEquals("sharedValue1_1", shortEntityView.properties().get("shared_sharedAttr1"));
        Assert.assertEquals("1", shortEntityView.properties().get("shared_sharedAttr2"));
        Assert.assertEquals("clientValue1_1", shortEntityView.properties().get("client_clientAttr1"));
        Assert.assertEquals("1", shortEntityView.properties().get("client_clientAttr2"));
        Assert.assertEquals("", shortEntityView.properties().get("server_emptyAttr"));

        tenantService.deleteTenant(tenantId);
    }

    private ListenableFuture<List<Void>> saveStringAttribute(EntityId entityId, String key, String value, String scope) {
        KvEntry attrValue = new StringDataEntry(key, value);
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        return attributesService.save(SYSTEM_TENANT_ID, entityId, scope, Collections.singletonList(attr));
    }

    private ListenableFuture<List<Void>> saveLongAttribute(EntityId entityId, String key, long value, String scope) {
        KvEntry attrValue = new LongDataEntry(key, value);
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        return attributesService.save(SYSTEM_TENANT_ID, entityId, scope, Collections.singletonList(attr));
    }
}
