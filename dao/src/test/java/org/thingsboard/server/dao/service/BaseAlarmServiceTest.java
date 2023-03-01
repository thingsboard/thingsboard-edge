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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.alarm.AlarmOperationResult;
import org.thingsboard.common.util.JacksonUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public abstract class BaseAlarmServiceTest extends AbstractBeforeTest {

    public static final String TEST_ALARM = "TEST_ALARM";
    private TenantId tenantId;
    private MergedUserPermissions mergedUserPermissions;

    @Before
    public void beforeRun() {
        tenantId = before();
        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        genericPermissions.put(Resource.resourceFromEntityType(EntityType.DEVICE), Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.resourceFromEntityType(EntityType.ASSET), Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.resourceFromEntityType(EntityType.ALARM), Collections.singleton(Operation.ALL));
        mergedUserPermissions = new MergedUserPermissions(genericPermissions, Collections.emptyMap());
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }


    @Test
    public void testSaveAndFetchAlarm() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        Alarm alarm = Alarm.builder().tenantId(tenantId).originator(childId)
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();

        AlarmOperationResult result = alarmService.createOrUpdateAlarm(alarm);
        Alarm created = result.getAlarm();

        Assert.assertNotNull(created);
        Assert.assertNotNull(created.getId());
        Assert.assertNotNull(created.getOriginator());
        Assert.assertNotNull(created.getSeverity());
        Assert.assertNotNull(created.getStatus());

        Assert.assertEquals(tenantId, created.getTenantId());
        Assert.assertEquals(childId, created.getOriginator());
        Assert.assertEquals(TEST_ALARM, created.getType());
        Assert.assertEquals(AlarmSeverity.CRITICAL, created.getSeverity());
        Assert.assertEquals(AlarmStatus.ACTIVE_UNACK, created.getStatus());
        Assert.assertEquals(ts, created.getStartTs());
        Assert.assertEquals(ts, created.getEndTs());
        Assert.assertEquals(0L, created.getAckTs());
        Assert.assertEquals(0L, created.getClearTs());

        Alarm fetched = alarmService.findAlarmByIdAsync(tenantId, created.getId()).get();
        Assert.assertEquals(created, fetched);
    }

    @Test
    public void testFindAlarm() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        Alarm alarm = Alarm.builder().tenantId(tenantId).originator(childId)
                .type(TEST_ALARM)
                .propagate(false)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();

        AlarmOperationResult result = alarmService.createOrUpdateAlarm(alarm);
        Alarm created = result.getAlarm();

        // Check child relation
        PageData<AlarmInfo> alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        created.setPropagate(true);
        result = alarmService.createOrUpdateAlarm(created);
        created = result.getAlarm();

        // Check child relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        alarmService.ackAlarm(tenantId, created.getId(), System.currentTimeMillis()).get();
        created = alarmService.findAlarmByIdAsync(tenantId, created.getId()).get();

        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_ACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check not existing relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        alarmService.clearAlarm(tenantId, created.getId(), null, System.currentTimeMillis()).get();
        created = alarmService.findAlarmByIdAsync(tenantId, created.getId()).get();

        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.CLEARED_ACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));
    }

    @Test
    public void testFindAlarmCounts() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId id1 = new AssetId(Uuids.timeBased());
        AssetId id2 = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, id1, EntityRelation.CONTAINS_TYPE);
        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());
        relation = new EntityRelation(parentId, id2, EntityRelation.CONTAINS_TYPE);
        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        Alarm alarm = Alarm.builder().tenantId(tenantId).originator(id1)
                .type(TEST_ALARM)
                .propagate(true)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();

        alarmService.createOrUpdateAlarm(alarm);
        alarm = Alarm.builder().tenantId(tenantId).originator(id2)
                .type(TEST_ALARM)
                .propagate(true)
                .severity(AlarmSeverity.WARNING).status(AlarmStatus.ACTIVE_ACK)
                .startTs(ts).build();
        alarmService.createOrUpdateAlarm(alarm);

        List<Long> alarmCounts = alarmService.findAlarmCounts(tenantId, AlarmQuery.builder()
                        .affectedEntityId(parentId)
                        .pageLink(
                                new TimePageLink(new PageLink(Integer.MAX_VALUE), null, null)
                        ).build(), Arrays.asList(
                AlarmFilter.builder().severityList(Arrays.asList(AlarmSeverity.CRITICAL)).build(),
                AlarmFilter.builder().severityList(Arrays.asList(AlarmSeverity.MAJOR)).build(),
                AlarmFilter.builder().severityList(Arrays.asList(AlarmSeverity.WARNING))
                        .typesList(Arrays.asList(TEST_ALARM)).statusList(Arrays.asList(AlarmStatus.ACTIVE_ACK)).build(),
                AlarmFilter.builder().typesList(Arrays.asList(TEST_ALARM)).build()
                )
        );

        Assert.assertNotNull(alarmCounts);
        Assert.assertEquals(4, alarmCounts.size());
        Assert.assertEquals(1, alarmCounts.get(0).longValue());
        Assert.assertEquals(0, alarmCounts.get(1).longValue());
        Assert.assertEquals(1, alarmCounts.get(2).longValue());
        Assert.assertEquals(2, alarmCounts.get(3).longValue());
    }

    public void testFindCustomerAlarm() throws ExecutionException, InterruptedException {
        Customer customer = new Customer();
        customer.setTitle("TestCustomer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        Device tenantDevice = new Device();
        tenantDevice.setName("TestTenantDevice");
        tenantDevice.setType("default");
        tenantDevice.setTenantId(tenantId);
        tenantDevice = deviceService.saveDevice(tenantDevice);

        Device customerDevice = new Device();
        customerDevice.setName("TestCustomerDevice");
        customerDevice.setType("default");
        customerDevice.setTenantId(tenantId);
        customerDevice.setCustomerId(customer.getId());
        customerDevice = deviceService.saveDevice(customerDevice);

        long ts = System.currentTimeMillis();
        Alarm tenantAlarm = Alarm.builder().tenantId(tenantId)
                .originator(tenantDevice.getId())
                .type(TEST_ALARM)
                .propagate(true)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();
        AlarmOperationResult result = alarmService.createOrUpdateAlarm(tenantAlarm);
        tenantAlarm = result.getAlarm();

        Alarm deviceAlarm = Alarm.builder().tenantId(tenantId)
                .originator(customerDevice.getId())
                .type(TEST_ALARM)
                .propagate(true)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();
        result = alarmService.createOrUpdateAlarm(deviceAlarm);
        deviceAlarm = result.getAlarm();

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        PageData<AlarmData> tenantAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Arrays.asList(tenantDevice.getId(), customerDevice.getId()));
        Assert.assertEquals(2, tenantAlarms.getData().size());

        PageData<AlarmData> customerAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Collections.singletonList(customerDevice.getId()));
        Assert.assertEquals(1, customerAlarms.getData().size());
        Assert.assertEquals(deviceAlarm, customerAlarms.getData().get(0));

        PageData<AlarmInfo> alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(tenantDevice.getId())
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(10, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(tenantAlarm, alarms.getData().get(0));
    }

    @Test
    public void testFindPropagatedCustomerAssetAlarm() throws ExecutionException, InterruptedException {
        Customer customer = new Customer();
        customer.setTitle("TestCustomer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        Device tenantDevice = new Device();
        tenantDevice.setName("TestTenantDevice");
        tenantDevice.setType("default");
        tenantDevice.setTenantId(tenantId);
        tenantDevice = deviceService.saveDevice(tenantDevice);

        Asset customerAsset = new Asset();
        customerAsset.setName("TestCustomerDevice");
        customerAsset.setType("default");
        customerAsset.setTenantId(tenantId);
        customerAsset.setCustomerId(customer.getId());
        customerAsset = assetService.saveAsset(customerAsset);

        EntityRelation relation = new EntityRelation();
        relation.setFrom(customerAsset.getId());
        relation.setTo(tenantDevice.getId());
        relation.setAdditionalInfo(JacksonUtil.newObjectNode());
        relation.setType("Contains");
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relationService.saveRelation(tenantId, relation);

        long ts = System.currentTimeMillis();
        Alarm tenantAlarm = Alarm.builder().tenantId(tenantId)
                .originator(tenantDevice.getId())
                .type("Not Propagated")
                .propagate(false)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();
        AlarmOperationResult result = alarmService.createOrUpdateAlarm(tenantAlarm);
        tenantAlarm = result.getAlarm();

        Alarm customerAlarm = Alarm.builder().tenantId(tenantId)
                .originator(tenantDevice.getId())
                .type("Propagated")
                .propagate(true)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();
        result = alarmService.createOrUpdateAlarm(customerAlarm);
        customerAlarm = result.getAlarm();

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        //TEST that propagated alarms are visible on the asset level.
        PageData<AlarmData> customerAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Collections.singletonList(customerAsset.getId()));
        Assert.assertEquals(1, customerAlarms.getData().size());
        Assert.assertEquals(customerAlarm, customerAlarms.getData().get(0));
    }

    @Test
    public void testFindPropagatedToOwnerAndTenantAlarm() {
        Customer customer = new Customer();
        customer.setTitle("TestCustomer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        Device device = new Device();
        device.setName("TestTenantDevice");
        device.setType("default");
        device.setTenantId(tenantId);
        device.setCustomerId(customer.getId());
        device = deviceService.saveDevice(device);

        long ts = System.currentTimeMillis();
        Alarm tenantAlarm = Alarm.builder().tenantId(tenantId)
                .originator(device.getId())
                .type("Propagated To Tenant")
                .propagateToTenant(true)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();
        AlarmOperationResult result = alarmService.createOrUpdateAlarm(tenantAlarm);
        tenantAlarm = result.getAlarm();

        Alarm customerAlarm = Alarm.builder().tenantId(tenantId)
                .originator(device.getId())
                .type("Propagated to Customer")
                .propagate(false)
                .propagateToOwner(true)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();
        result = alarmService.createOrUpdateAlarm(customerAlarm);
        customerAlarm = result.getAlarm();

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        //TEST that propagated alarms are visible on the asset level.
        PageData<AlarmData> tenantAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Collections.singletonList(tenantId));
        Assert.assertEquals(1, tenantAlarms.getData().size());
        Assert.assertEquals(tenantAlarm, tenantAlarms.getData().get(0));

        //TEST that propagated alarms are visible on the asset level.
        PageData<AlarmData> customerAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Collections.singletonList(customer.getId()));
        Assert.assertEquals(1, customerAlarms.getData().size());
        Assert.assertEquals(customerAlarm, customerAlarms.getData().get(0));
    }

    private AlarmDataQuery toQuery(AlarmDataPageLink pageLink){
        return toQuery(pageLink, Collections.emptyList());
    }

    private AlarmDataQuery toQuery(AlarmDataPageLink pageLink, List<EntityKey> alarmFields){
        return new AlarmDataQuery(new DeviceTypeFilter(), pageLink, null, null, null, alarmFields);
    }

    @Test
    public void testFindHighestAlarmSeverity() throws ExecutionException, InterruptedException {
        Customer customer = new Customer();
        customer.setTitle("TestCustomer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        Device customerDevice = new Device();
        customerDevice.setName("TestCustomerDevice");
        customerDevice.setType("default");
        customerDevice.setTenantId(tenantId);
        customerDevice.setCustomerId(customer.getId());
        customerDevice = deviceService.saveDevice(customerDevice);

        // no one alarms was created
        Assert.assertNull(alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), null, null));

        Alarm alarm1 = Alarm.builder()
                .tenantId(tenantId)
                .originator(customerDevice.getId())
                .type(TEST_ALARM)
                .severity(AlarmSeverity.MAJOR)
                .status(AlarmStatus.ACTIVE_UNACK)
                .startTs(System.currentTimeMillis())
                .build();
        alarm1 = alarmService.createOrUpdateAlarm(alarm1).getAlarm();
        alarmService.clearAlarm(tenantId, alarm1.getId(), null, System.currentTimeMillis()).get();

        Alarm alarm2 = Alarm.builder()
                .tenantId(tenantId)
                .originator(customerDevice.getId())
                .type(TEST_ALARM)
                .severity(AlarmSeverity.MINOR)
                .status(AlarmStatus.ACTIVE_ACK)
                .startTs(System.currentTimeMillis())
                .build();
        alarm2 = alarmService.createOrUpdateAlarm(alarm2).getAlarm();
        alarmService.clearAlarm(tenantId, alarm2.getId(), null, System.currentTimeMillis()).get();

        Alarm alarm3 = Alarm.builder()
                .tenantId(tenantId)
                .originator(customerDevice.getId())
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .status(AlarmStatus.ACTIVE_ACK)
                .startTs(System.currentTimeMillis())
                .build();
        alarm3 = alarmService.createOrUpdateAlarm(alarm3).getAlarm();

        Assert.assertEquals(AlarmSeverity.MAJOR, alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), AlarmSearchStatus.UNACK, null));
        Assert.assertEquals(AlarmSeverity.CRITICAL, alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), null, null));
        Assert.assertEquals(AlarmSeverity.MAJOR, alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), null, AlarmStatus.CLEARED_UNACK));
        Assert.assertEquals(AlarmSeverity.CRITICAL, alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), AlarmSearchStatus.ACTIVE, null));
        Assert.assertEquals(AlarmSeverity.MINOR, alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), null, AlarmStatus.CLEARED_ACK));
    }

    @Test
    public void testFindAlarmUsingAlarmDataQuery() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId parentId2 = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relation2 = new EntityRelation(parentId2, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());
        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation2).get());

        long ts = System.currentTimeMillis();
        Alarm alarm = Alarm.builder().tenantId(tenantId).originator(childId)
                .type(TEST_ALARM)
                .propagate(false)
                .severity(AlarmSeverity.CRITICAL)
                .status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();

        AlarmOperationResult result = alarmService.createOrUpdateAlarm(alarm);
        Alarm created = result.getAlarm();

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(false);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        PageData<AlarmData> alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Collections.singletonList(childId));

        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(false);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Collections.singletonList(childId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new Alarm(alarms.getData().get(0)));

        pageLink.setSearchPropagatedAlarms(true);
        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Collections.singletonList(childId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new Alarm(alarms.getData().get(0)));

        // Check child relation
        created.setPropagate(true);
        result = alarmService.createOrUpdateAlarm(created);
        created = result.getAlarm();

        // Check child relation
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Collections.singletonList(childId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check parent relation
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Collections.singletonList(parentId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        PageData<AlarmInfo> alarmsInfoData = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(10, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarmsInfoData.getData());
        Assert.assertEquals(1, alarmsInfoData.getData().size());
        Assert.assertEquals(created, alarmsInfoData.getData().get(0));

        alarmsInfoData = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(10, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarmsInfoData.getData());
        Assert.assertEquals(1, alarmsInfoData.getData().size());
        Assert.assertEquals(created, alarmsInfoData.getData().get(0));

        alarmsInfoData = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId2)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(10, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarmsInfoData.getData());
        Assert.assertEquals(1, alarmsInfoData.getData().size());
        Assert.assertEquals(created, alarmsInfoData.getData().get(0));

        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Collections.singletonList(parentId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        alarmService.ackAlarm(tenantId, created.getId(), System.currentTimeMillis()).get();
        created = alarmService.findAlarmByIdAsync(tenantId, created.getId()).get();

        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, mergedUserPermissions, toQuery(pageLink), Collections.singletonList(childId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));
    }

    @Test
    public void testDeleteAlarm() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        Alarm alarm = Alarm.builder().tenantId(tenantId).originator(childId)
                .type(TEST_ALARM)
                .propagate(true)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();

        AlarmOperationResult result = alarmService.createOrUpdateAlarm(alarm);
        Alarm created = result.getAlarm();

        PageData<AlarmInfo> alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        Assert.assertTrue("Alarm was not deleted when expected", alarmService.deleteAlarm(tenantId, created.getId()).isSuccessful());

        Alarm fetched = alarmService.findAlarmByIdAsync(tenantId, created.getId()).get();

        Assert.assertNull("Alarm was returned when it was expected to be null", fetched);

        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

    }
}
