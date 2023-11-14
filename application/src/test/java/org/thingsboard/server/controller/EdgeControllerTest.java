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
package org.thingsboard.server.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.AbstractMessage;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RoleProto;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.SyncCompletedMsg;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingProto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@TestPropertySource(properties = {
        "edges.enabled=true",
        "queue.rule-engine.stats.enabled=false"
})
@ContextConfiguration(classes = {EdgeControllerTest.Config.class})
@DaoSqlTest
public class EdgeControllerTest extends AbstractControllerTest {

    public static final String EDGE_HOST = "localhost";
    public static final int EDGE_PORT = 7070;

    private final IdComparator<Edge> idComparator = new IdComparator<>();

    ListeningExecutorService executor;

    List<ListenableFuture<Edge>> futures;

    @Autowired
    private EdgeDao edgeDao;

    static class Config {
        @Bean
        @Primary
        public EdgeDao edgeDao(EdgeDao edgeDao) {
            return Mockito.mock(EdgeDao.class, AdditionalAnswers.delegatesTo(edgeDao));
        }
    }

    @Before
    public void setupEdgeTest() throws Exception {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
        loginTenantAdmin();
    }

    @After
    public void teardownEdgeTest() {
        executor.shutdownNow();
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    @Test
    @Ignore
    public void testSaveEdge() throws Exception {
        Edge edge = constructEdge("My edge", "default");

        Mockito.reset(tbClusterService, auditLogService);

        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Assert.assertNotNull(savedEdge);
        Assert.assertNotNull(savedEdge.getId());
        Assert.assertTrue(savedEdge.getCreatedTime() > 0);
        Assert.assertEquals(tenantId, savedEdge.getTenantId());
        Assert.assertNotNull(savedEdge.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedEdge.getCustomerId().getId());
        Assert.assertEquals(edge.getName(), savedEdge.getName());
        Assert.assertTrue(StringUtils.isNoneBlank(savedEdge.getEdgeLicenseKey()));
        Assert.assertTrue(StringUtils.isNoneBlank(savedEdge.getCloudEndpoint()));

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(savedEdge, savedEdge.getId(), savedEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.ADDED);

        savedEdge.setName("My new edge");
        doPost("/api/edge", savedEdge, Edge.class);

        Edge foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(foundEdge.getName(), savedEdge.getName());

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(foundEdge, foundEdge.getId(), foundEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.UPDATED);
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    @Test
    @Ignore
    public void testSaveEdgeWithViolationOfLengthValidation() throws Exception {
        Edge edge = constructEdge(StringUtils.randomAlphabetic(300), "default");
        String msgError = msgErrorFieldLength("name");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, tenantId,
                tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        msgError = msgErrorFieldLength("type");
        edge.setName("normal name");
        edge.setType(StringUtils.randomAlphabetic(300));
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, tenantId,
                tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        msgError = msgErrorFieldLength("label");
        edge.setType("normal type");
        edge.setLabel(StringUtils.randomAlphabetic(300));
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, tenantId,
                tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    @Test
    @Ignore
    public void testFindEdgeById() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);
        Edge foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertNotNull(foundEdge);
        Assert.assertEquals(savedEdge, foundEdge);
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    @Test
    @Ignore
    public void testFindEdgeTypesByTenantId() throws Exception {
        int cntEntity = 3;

        Mockito.reset(tbClusterService, auditLogService);

        for (int i = 0; i < cntEntity; i++) {
            Edge edge = constructEdge("My edge B" + i, "typeB");
            doPost("/api/edge", edge, Edge.class);
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNeverAdditionalInfoAny(new Edge(), new Edge(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.ADDED, cntEntity, 0);

        for (int i = 0; i < 7; i++) {
            Edge edge = constructEdge("My edge C" + i, "typeC");
            doPost("/api/edge", edge, Edge.class);
        }
        for (int i = 0; i < 9; i++) {
            Edge edge = constructEdge("My edge A" + i, "typeA");
            doPost("/api/edge", edge, Edge.class);
        }
        List<EntitySubtype> edgeTypes = doGetTyped("/api/edge/types",
                new TypeReference<>() {
                });

        Assert.assertNotNull(edgeTypes);
        Assert.assertEquals(3, edgeTypes.size());
        Assert.assertEquals("typeA", edgeTypes.get(0).getType());
        Assert.assertEquals("typeB", edgeTypes.get(1).getType());
        Assert.assertEquals("typeC", edgeTypes.get(2).getType());
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    @Test
    @Ignore
    public void testDeleteEdge() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(savedEdge, savedEdge.getId(), savedEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.DELETED, savedEdge.getId().getId().toString());

        doGet("/api/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Edge", savedEdge.getId().getId().toString()))));
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    @Test
    @Ignore
    public void testSaveEdgeWithEmptyType() throws Exception {
        Edge edge = constructEdge("My edge", null);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Edge type " + msgErrorShouldBeSpecified;
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, tenantId,
                tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    @Test
    @Ignore
    public void testSaveEdgeWithEmptyName() throws Exception {
        Edge edge = constructEdge(null, "default");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Edge name " + msgErrorShouldBeSpecified;
        doPost("/api/edge", edge)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(edge, tenantId,
                tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    // keeping CE test for merge compatibility
    @Test
    @Ignore
    public void testAssignUnassignEdgeToCustomer() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        Mockito.reset(tbClusterService, auditLogService);

        Edge assignedEdge = doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(savedCustomer.getId(), assignedEdge.getCustomerId());

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(assignedEdge, assignedEdge.getId(), assignedEdge.getId(),
                tenantId, savedCustomer.getId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.ASSIGNED_TO_CUSTOMER,
                ActionType.ASSIGNED_TO_CUSTOMER, assignedEdge.getId().getId().toString(), savedCustomer.getId().getId().toString(), savedCustomer.getTitle());

        Edge foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(savedCustomer.getId(), foundEdge.getCustomerId());

        Edge unassignedEdge =
                doDelete("/api/customer/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedEdge.getCustomerId().getId());

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(unassignedEdge, unassignedEdge.getId(), unassignedEdge.getId(),
                tenantId, savedCustomer.getId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(), ActionType.UNASSIGNED_FROM_CUSTOMER,
                ActionType.UNASSIGNED_FROM_CUSTOMER, unassignedEdge.getId().getId().toString(), savedCustomer.getId().getId().toString(), savedCustomer.getTitle());

        foundEdge = doGet("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundEdge.getCustomerId().getId());
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    @Test
    @Ignore
    public void testAssignEdgeToNonExistentCustomer() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Mockito.reset(tbClusterService, auditLogService);

        CustomerId customerId = new CustomerId(Uuids.timeBased());
        String customerIdStr = customerId.getId().toString();

        String msgError = msgErrorNoFound("Customer", customerIdStr);
        doPost("/api/customer/" + customerIdStr + "/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityNever(savedEdge.getId(), savedEdge);
        testNotifyEntityNever(customerId, new Customer());
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    // keeping CE test for merge compatibility
    @Test
    @Ignore
    public void testAssignEdgeToCustomerFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Different tenant");
        Tenant savedTenant2 = doPost("/api/tenant", tenant2, Tenant.class);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("tenant3@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");

        createUserAndLogin(tenantAdmin2, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("Different customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        loginTenantAdmin();

        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString("msgErrorPermission")));

        testNotifyEntityNever(savedEdge.getId(), savedEdge);
        testNotifyEntityNever(savedCustomer.getId(), savedCustomer);

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant2.getId().getId().toString())
                .andExpect(status().isOk());
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    @Test
    @Ignore
    public void testFindTenantEdges() throws Exception {
        int cntEntity = 178;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            Edge edge = constructEdge("Edge" + i, "default");
            futures.add(executor.submit(() ->
                    doPost("/api/edge", edge, Edge.class)));
        }
        List<Edge> edges = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));
        List<Edge> loadedEdges = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Edge> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedEdges.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        edges.sort(idComparator);
        loadedEdges.sort(idComparator);

        Assert.assertEquals(edges, loadedEdges);
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    @Test
    @Ignore
    public void testFindTenantEdgesByName() throws Exception {
        String title1 = "Edge title 1";
        int cntEntity = 143;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            futures.add(executor.submit(() ->
                    doPost("/api/edge", edge, Edge.class)));
        }
        List<Edge> edgesTitle1 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        String title2 = "Edge title 2";
        cntEntity = 75;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            futures.add(executor.submit(() ->
                    doPost("/api/edge", edge, Edge.class)));
        }
        List<Edge> edgesTitle2 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        List<Edge> loadedEdgesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Edge> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedEdgesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        edgesTitle1.sort(idComparator);
        loadedEdgesTitle1.sort(idComparator);

        Assert.assertEquals(edgesTitle1, loadedEdgesTitle1);

        List<Edge> loadedEdgesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedEdgesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        edgesTitle2.sort(idComparator);
        loadedEdgesTitle2.sort(idComparator);

        Assert.assertEquals(edgesTitle2, loadedEdgesTitle2);

        for (Edge edge : loadedEdgesTitle1) {
            doDelete("/api/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/edges?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesTitle2) {
            doDelete("/api/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/edges?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    @Test
    @Ignore
    public void testFindTenantEdgesByType() throws Exception {
        String title1 = "Edge title 1";
        String type1 = "typeA";
        int cntEntity = 143;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type1);
            futures.add(executor.submit(() ->
                    doPost("/api/edge", edge, Edge.class)));
        }
        List<Edge> edgesType1 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        String title2 = "Edge title 2";
        String type2 = "typeB";
        cntEntity = 75;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type2);
            futures.add(executor.submit(() ->
                    doPost("/api/edge", edge, Edge.class)));
        }
        List<Edge> edgesType2 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        List<Edge> loadedEdgesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Edge> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?type={type}&",
                    new TypeReference<>() {
                    }, pageLink, type1);
            loadedEdgesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        edgesType1.sort(idComparator);
        loadedEdgesType1.sort(idComparator);

        Assert.assertEquals(edgesType1, loadedEdgesType1);

        List<Edge> loadedEdgesType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/edges?type={type}&",
                    new TypeReference<>() {
                    }, pageLink, type2);
            loadedEdgesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        edgesType2.sort(idComparator);
        loadedEdgesType2.sort(idComparator);

        Assert.assertEquals(edgesType2, loadedEdgesType2);

        for (Edge edge : loadedEdgesType1) {
            doDelete("/api/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/edges?type={type}&",
                new TypeReference<>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesType2) {
            doDelete("/api/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/edges?type={type}&",
                new TypeReference<>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    // keeping CE test for merge compatibility
    @Test
    @Ignore
    public void testFindCustomerEdges() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 128;
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            Edge edge = constructEdge("Edge" + i, "default");
            futures.add(executor.submit(() -> {
                Edge edge1 = doPost("/api/edge", edge, Edge.class);
                return doPost("/api/customer/" + customerId.getId().toString()
                        + "/edge/" + edge1.getId().getId().toString(), Edge.class);
            }));
        }
        List<Edge> edges = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new Edge(), new Edge(),
                tenantId, customerId, tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.ASSIGNED_TO_CUSTOMER, cntEntity, cntEntity, cntEntity * 2, "", "", "");

        List<Edge> loadedEdges = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Edge> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedEdges.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        edges.sort(idComparator);
        loadedEdges.sort(idComparator);

        Assert.assertEquals(edges, loadedEdges);
    }

    // @voba - merge comment
    // keeping CE test for merge compatibility
    // keeping CE test for merge compatibility
    @Test
    @Ignore
    public void testFindCustomerEdgesByName() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        int cntEntity = 125;
        String title1 = "Edge title 1";
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            futures.add(executor.submit(() -> {
                Edge edge1 = doPost("/api/edge", edge, Edge.class);
                return doPost("/api/customer/" + customerId.getId().toString()
                        + "/edge/" + edge1.getId().getId().toString(), Edge.class);
            }));
        }
        List<Edge> edgesTitle1 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        cntEntity = 143;
        String title2 = "Edge title 2";
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, "default");
            futures.add(executor.submit(() -> {
                Edge edge1 = doPost("/api/edge", edge, Edge.class);
                return doPost("/api/customer/" + customerId.getId().toString()
                        + "/edge/" + edge1.getId().getId().toString(), Edge.class);
            }));
        }
        List<Edge> edgesTitle2 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        List<Edge> loadedEdgesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Edge> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedEdgesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        edgesTitle1.sort(idComparator);
        loadedEdgesTitle1.sort(idComparator);

        Assert.assertEquals(edgesTitle1, loadedEdgesTitle1);

        List<Edge> loadedEdgesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedEdgesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgesTitle2, idComparator);
        Collections.sort(loadedEdgesTitle2, idComparator);

        Assert.assertEquals(edgesTitle2, loadedEdgesTitle2);

        Mockito.reset(tbClusterService, auditLogService);

        for (Edge edge : loadedEdgesTitle1) {
            doDelete("/api/customer/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        cntEntity = loadedEdgesTitle1.size();
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(new Edge(), new Edge(),
                tenantId, customerId, tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.UNASSIGNED_FROM_CUSTOMER, ActionType.UNASSIGNED_FROM_CUSTOMER, cntEntity, cntEntity, 3);

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesTitle2) {
            doDelete("/api/customer/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    // keeping CE test for merge compatibility
    @Test
    @Ignore
    public void testFindCustomerEdgesByType() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        int cntEntity = 125;
        String title1 = "Edge title 1";
        String type1 = "typeC";
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type1);
            futures.add(executor.submit(() -> {
                Edge edge1 = doPost("/api/edge", edge, Edge.class);
                return doPost("/api/customer/" + customerId.getId().toString()
                        + "/edge/" + edge1.getId().getId().toString(), Edge.class);
            }));
        }
        List<Edge> edgesType1 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        cntEntity = 143;
        String title2 = "Edge title 2";
        String type2 = "typeD";
        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            Edge edge = constructEdge(name, type2);
            futures.add(executor.submit(() -> {
                Edge edge1 = doPost("/api/edge", edge, Edge.class);
                return doPost("/api/customer/" + customerId.getId().toString()
                        + "/edge/" + edge1.getId().getId().toString(), Edge.class);
            }));
        }
        List<Edge> edgesType2 = new ArrayList<>(Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS));

        List<Edge> loadedEdgesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Edge> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?type={type}&",
                    new TypeReference<>() {
                    }, pageLink, type1);
            loadedEdgesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        edgesType1.sort(idComparator);
        loadedEdgesType1.sort(idComparator);

        Assert.assertEquals(edgesType1, loadedEdgesType1);

        List<Edge> loadedEdgesType2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?type={type}&",
                    new TypeReference<>() {
                    }, pageLink, type2);
            loadedEdgesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        edgesType2.sort(idComparator);
        loadedEdgesType2.sort(idComparator);

        Assert.assertEquals(edgesType2, loadedEdgesType2);

        for (Edge edge : loadedEdgesType1) {
            doDelete("/api/customer/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?type={type}&",
                new TypeReference<>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Edge edge : loadedEdgesType2) {
            doDelete("/api/customer/edge/" + edge.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/edges?type={type}&",
                new TypeReference<>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    // @voba - merge comment
    // edge entities support available in CE/PE
    // keeping CE test for merge compatibility
    @Test
    @Ignore
    public void testSyncEdge() throws Exception {
        loginSysAdmin();
        // get jwt settings from yaml config
        JwtSettings settings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        // save jwt settings into db
        doPost("/api/admin/jwtSettings", settings).andExpect(status().isOk());
        loginTenantAdmin();

        Asset asset = new Asset();
        asset.setName("Test Sync Edge Asset 1");
        asset.setType("test");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Device device = new Device();
        device.setName("Test Sync Edge Device 1");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        Edge edge = doPost("/api/edge", constructEdge("Test Sync Edge", "test"), Edge.class);

        // simulate edge activation
        ObjectNode attributes = JacksonUtil.newObjectNode();
        attributes.put("active", true);
        doPost("/api/plugins/telemetry/EDGE/" + edge.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, attributes);

        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/device/" + savedDevice.getId().getId().toString(), Device.class);
        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);

        EdgeImitator edgeImitator = new EdgeImitator(EDGE_HOST, EDGE_PORT, edge.getRoutingKey(), edge.getSecret());
        edgeImitator.ignoreType(UserCredentialsUpdateMsg.class);

        edgeImitator.expectMessageAmount(24);
        edgeImitator.connect();
        assertThat(edgeImitator.waitForMessages()).as("await for messages on first connect").isTrue();

        verifyFetchersMsgs(edgeImitator);
        // verify queue msgs
        Assert.assertTrue(popDeviceProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popDeviceMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Device 1"));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "test"));
        Assert.assertTrue(popAssetMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Asset 1"));
        Assert.assertTrue(edgeImitator.getDownlinkMsgs().isEmpty());

        edgeImitator.expectMessageAmount(20);
        doPost("/api/edge/sync/" + edge.getId());
        assertThat(edgeImitator.waitForMessages()).as("await for messages after edge sync rest api call").isTrue();

        verifyFetchersMsgs(edgeImitator);
        Assert.assertTrue(edgeImitator.getDownlinkMsgs().isEmpty());

        edgeImitator.allowIgnoredTypes();
        try {
            edgeImitator.disconnect();
        } catch (Exception ignored) {
        }

        doDelete("/api/device/" + savedDevice.getId().getId().toString())
                .andExpect(status().isOk());
        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());
        doDelete("/api/edge/" + edge.getId().getId().toString())
                .andExpect(status().isOk());
    }

    private void verifyFetchersMsgs(EdgeImitator edgeImitator) {
        Assert.assertTrue(popQueueMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Main"));
        Assert.assertTrue(popRuleChainMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Edge Root Rule Chain"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "general"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "mail"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "connectivity"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "jwt"));
        Assert.assertTrue(popDeviceProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popDeviceProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "test"));
        Assert.assertTrue(popUserMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, TENANT_ADMIN_EMAIL, Authority.TENANT_ADMIN));
        Assert.assertTrue(popCustomerMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Public"));
        Assert.assertTrue(popDeviceProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popDeviceMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Device 1"));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "test"));
        Assert.assertTrue(popAssetMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Test Sync Edge Asset 1"));
        Assert.assertTrue(popTenantMsg(edgeImitator.getDownlinkMsgs(), tenantId));
        Assert.assertTrue(popTenantProfileMsg(edgeImitator.getDownlinkMsgs(), tenantProfileId));
        Assert.assertTrue(popSyncCompletedMsg(edgeImitator.getDownlinkMsgs()));
    }

    private boolean popQueueMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof QueueUpdateMsg) {
                QueueUpdateMsg queueUpdateMsg = (QueueUpdateMsg) message;
                Queue queue = JacksonUtil.fromStringIgnoreUnknownProperties(queueUpdateMsg.getEntity(), Queue.class);
                Assert.assertNotNull(queue);
                if (msgType.equals(queueUpdateMsg.getMsgType()) && name.equals(queue.getName())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popRuleChainMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof RuleChainUpdateMsg) {
                RuleChainUpdateMsg ruleChainUpdateMsg = (RuleChainUpdateMsg) message;
                RuleChain ruleChain = JacksonUtil.fromStringIgnoreUnknownProperties(ruleChainUpdateMsg.getEntity(), RuleChain.class);
                Assert.assertNotNull(ruleChain);
                if (msgType.equals(ruleChainUpdateMsg.getMsgType())
                        && name.equals(ruleChain.getName())
                        && ruleChain.isRoot()) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popAdminSettingsMsg(List<AbstractMessage> messages, String key) {
        for (AbstractMessage message : messages) {
            if (message instanceof AdminSettingsUpdateMsg) {
                AdminSettingsUpdateMsg adminSettingsUpdateMsg = (AdminSettingsUpdateMsg) message;
                AdminSettings adminSettings = JacksonUtil.fromStringIgnoreUnknownProperties(adminSettingsUpdateMsg.getEntity(), AdminSettings.class);
                Assert.assertNotNull(adminSettings);
                if (key.equals(adminSettings.getKey())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popDeviceProfileMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof DeviceProfileUpdateMsg) {
                DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) message;
                DeviceProfile deviceProfile = JacksonUtil.fromStringIgnoreUnknownProperties(deviceProfileUpdateMsg.getEntity(), DeviceProfile.class);
                Assert.assertNotNull(deviceProfile);
                if (msgType.equals(deviceProfileUpdateMsg.getMsgType())
                        && name.equals(deviceProfile.getName())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popDeviceMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof DeviceUpdateMsg) {
                DeviceUpdateMsg deviceUpdateMsg = (DeviceUpdateMsg) message;
                Device device = JacksonUtil.fromStringIgnoreUnknownProperties(deviceUpdateMsg.getEntity(), Device.class);
                Assert.assertNotNull(device);
                if (msgType.equals(deviceUpdateMsg.getMsgType())
                        && name.equals(device.getName())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popAssetProfileMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof AssetProfileUpdateMsg) {
                AssetProfileUpdateMsg assetProfileUpdateMsg = (AssetProfileUpdateMsg) message;
                AssetProfile assetProfile = JacksonUtil.fromStringIgnoreUnknownProperties(assetProfileUpdateMsg.getEntity(), AssetProfile.class);
                Assert.assertNotNull(assetProfile);
                if (msgType.equals(assetProfileUpdateMsg.getMsgType())
                        && name.equals(assetProfile.getName())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popAssetMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof AssetUpdateMsg) {
                AssetUpdateMsg assetUpdateMsg = (AssetUpdateMsg) message;
                Asset asset = JacksonUtil.fromStringIgnoreUnknownProperties(assetUpdateMsg.getEntity(), Asset.class);
                Assert.assertNotNull(asset);
                if (msgType.equals(assetUpdateMsg.getMsgType())
                        && name.equals(asset.getName())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popUserMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String email, Authority authority) {
        for (AbstractMessage message : messages) {
            if (message instanceof UserUpdateMsg) {
                UserUpdateMsg userUpdateMsg = (UserUpdateMsg) message;
                User user = JacksonUtil.fromStringIgnoreUnknownProperties(userUpdateMsg.getEntity(), User.class);
                Assert.assertNotNull(user);
                if (msgType.equals(userUpdateMsg.getMsgType())
                        && email.equals(user.getEmail())
                        && authority.equals(user.getAuthority())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popCustomerMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String title) {
        for (AbstractMessage message : messages) {
            if (message instanceof CustomerUpdateMsg) {
                CustomerUpdateMsg customerUpdateMsg = (CustomerUpdateMsg) message;
                Customer customer = JacksonUtil.fromStringIgnoreUnknownProperties(customerUpdateMsg.getEntity(), Customer.class);
                Assert.assertNotNull(customer);
                if (msgType.equals(customerUpdateMsg.getMsgType())
                        && title.equals(customer.getTitle())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popCustomerMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String title, String ownerType, UUID ownerUUID) {
        for (AbstractMessage message : messages) {
            if (message instanceof CustomerUpdateMsg) {
                CustomerUpdateMsg customerUpdateMsg = (CustomerUpdateMsg) message;
                Customer customer = JacksonUtil.fromStringIgnoreUnknownProperties(customerUpdateMsg.getEntity(), Customer.class);
                Assert.assertNotNull(customer);
                if (msgType.equals(customerUpdateMsg.getMsgType())
                        && title.equals(customer.getTitle())
                        && ownerType.equals(customer.getOwnerId().getEntityType().name())
                        && ownerUUID.getMostSignificantBits() == customer.getOwnerId().getId().getMostSignificantBits()
                        && ownerUUID.getLeastSignificantBits() == customer.getOwnerId().getId().getLeastSignificantBits()) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;

    }

    private boolean popRoleMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name, RoleType type) {
        for (AbstractMessage message : messages) {
            if (message instanceof RoleProto) {
                RoleProto roleProto = (RoleProto) message;
                Role role = JacksonUtil.fromStringIgnoreUnknownProperties(roleProto.getEntity(), Role.class);
                Assert.assertNotNull(role);
                if (msgType.equals(roleProto.getMsgType())
                        && name.equals(role.getName())
                        && type.equals(role.getType())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popSyncCompletedMsg(List<AbstractMessage> messages) {
        for (AbstractMessage message : messages) {
            if (message instanceof SyncCompletedMsg) {
                messages.remove(message);
                return true;
            }
        }
        return false;
    }

    private boolean popEntityGroupMsg(List<AbstractMessage> messages, UpdateMsgType msgType, String name, EntityType type, EntityType ownerType) {
        for (AbstractMessage message : messages) {
            if (message instanceof EntityGroupUpdateMsg) {
                EntityGroupUpdateMsg entityGroupUpdateMsg = (EntityGroupUpdateMsg) message;
                EntityGroup entityGroup = JacksonUtil.fromStringIgnoreUnknownProperties(entityGroupUpdateMsg.getEntity(), EntityGroup.class);
                Assert.assertNotNull(entityGroup);
                if (msgType.equals(entityGroupUpdateMsg.getMsgType())
                        && name.equals(entityGroup.getName())
                        && type.equals(entityGroup.getType())
                        && ownerType.equals(entityGroup.getOwnerId().getEntityType())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popEdgeConfigurationMsg(List<AbstractMessage> messages, String name) {
        for (AbstractMessage message : messages) {
            if (message instanceof EdgeConfiguration) {
                EdgeConfiguration edgeConfiguration = (EdgeConfiguration) message;
                if (name.equals(edgeConfiguration.getName())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popTenantProfileMsg(List<AbstractMessage> messages, TenantProfileId tenantProfileId) {
        for (AbstractMessage message : messages) {
            if (message instanceof TenantProfileUpdateMsg) {
                TenantProfileUpdateMsg tenantProfileUpdateMsg = (TenantProfileUpdateMsg) message;
                TenantProfile tenantProfile = JacksonUtil.fromStringIgnoreUnknownProperties(tenantProfileUpdateMsg.getEntity(), TenantProfile.class);
                Assert.assertNotNull(tenantProfile);
                if (UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(tenantProfileUpdateMsg.getMsgType())
                        && tenantProfileId.equals(tenantProfile.getId())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popTenantMsg(List<AbstractMessage> messages, TenantId tenantId1) {
        for (AbstractMessage message : messages) {
            if (message instanceof TenantUpdateMsg) {
                TenantUpdateMsg tenantUpdateMsg = (TenantUpdateMsg) message;
                Tenant tenant = JacksonUtil.fromStringIgnoreUnknownProperties(tenantUpdateMsg.getEntity(), Tenant.class);
                Assert.assertNotNull(tenant);
                if (UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(tenantUpdateMsg.getMsgType())
                        && tenantId1.equals(tenant.getId())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean popWhiteLabeling(List<AbstractMessage> messages, WhiteLabelingType type) {
        for (AbstractMessage message : messages) {
            if (message instanceof WhiteLabelingProto) {
                WhiteLabelingProto whiteLabelingProto = (WhiteLabelingProto) message;
                WhiteLabeling whiteLabeling = JacksonUtil.fromStringIgnoreUnknownProperties(whiteLabelingProto.getEntity(), WhiteLabeling.class);
                Assert.assertNotNull(whiteLabeling);
                if (type.equals(whiteLabeling.getType())) {
                    messages.remove(message);
                    return true;
                }
            }
        }
        return false;
    }

    // @voba - merge comment
    // edge assign functionality only in CE/PE
    @Test
    @Ignore
    public void testSyncEdge_tenantLevel() throws Exception {
        createAdminSettings();
        resetSysAdminWhiteLabelingSettings();
        loginTenantAdmin();

        EntityGroup savedDeviceGroup = new EntityGroup();
        savedDeviceGroup.setType(EntityType.DEVICE);
        savedDeviceGroup.setName("DeviceGroup");
        savedDeviceGroup = doPost("/api/entityGroup", savedDeviceGroup, EntityGroup.class);

        Device device = new Device();
        device.setName("Sync Test EG Edge Device 1");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class, "entityGroupId", savedDeviceGroup.getId().getId().toString());

        EntityGroup savedAssetGroup = new EntityGroup();
        savedAssetGroup.setType(EntityType.ASSET);
        savedAssetGroup.setName("AssetGroup");
        savedAssetGroup = doPost("/api/entityGroup", savedAssetGroup, EntityGroup.class);

        Edge edge = doPost("/api/edge", constructEdge("Sync Test EG Edge", "test"), Edge.class);

        verifyEdgeUserGroups(edge, 2);

        // simulate edge activation
        ObjectNode attributes = JacksonUtil.newObjectNode();
        attributes.put("active", true);
        doPost("/api/plugins/telemetry/EDGE/" + edge.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, attributes);

        Asset asset = new Asset();
        asset.setName("Sync Test EG Edge Asset 1");
        asset.setType("test");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class, "entityGroupId", savedAssetGroup.getId().getId().toString());

        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/entityGroup/" + savedDeviceGroup.getId().getId().toString() + "/DEVICE", EntityGroup.class);

        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/entityGroup/" + savedAssetGroup.getId().getId().toString() + "/ASSET", EntityGroup.class);

        EdgeImitator edgeImitator = new EdgeImitator(EDGE_HOST, EDGE_PORT, edge.getRoutingKey(), edge.getSecret());
        edgeImitator.ignoreType(UserCredentialsUpdateMsg.class);

        edgeImitator.expectMessageAmount(32);
        edgeImitator.connect();
        assertThat(edgeImitator.waitForMessages()).as("await for messages on first connect").isTrue();

        verifyFetchersMsgs_tenantLevel(edgeImitator);
        // verify queue msgs
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "DeviceGroup", EntityType.DEVICE, EntityType.TENANT));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "test"));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "AssetGroup", EntityType.ASSET, EntityType.TENANT));
        Assert.assertTrue("There are some messages: " + edgeImitator.getDownlinkMsgs(), edgeImitator.getDownlinkMsgs().isEmpty());

        edgeImitator.expectMessageAmount(29);
        doPost("/api/edge/sync/" + edge.getId());
        assertThat(edgeImitator.waitForMessages()).as("await for messages after edge sync rest api call").isTrue();

        verifyFetchersMsgs_tenantLevel(edgeImitator);
        Assert.assertTrue(edgeImitator.getDownlinkMsgs().isEmpty());

        edgeImitator.allowIgnoredTypes();
        try {
            edgeImitator.disconnect();
        } catch (Exception ignored) {
        }

        doDelete("/api/device/" + savedDevice.getId().getId().toString())
                .andExpect(status().isOk());
        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());
        doDelete("/api/edge/" + edge.getId().getId().toString())
                .andExpect(status().isOk());
    }

    private void createAdminSettings() throws Exception {
        loginSysAdmin();
        // get jwt settings from yaml config
        JwtSettings settings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        // save jwt settings into db
        doPost("/api/admin/jwtSettings", settings).andExpect(status().isOk());

        CustomMenu sysMenu = new CustomMenu();

        CustomMenuItem sysItem = new CustomMenuItem();
        sysItem.setName("System Menu");
        sysMenu.setMenuItems(new ArrayList<>(List.of(sysItem)));

        doPost("/api/customMenu/customMenu", sysMenu);
    }

    private void verifyFetchersMsgs_tenantLevel(EdgeImitator edgeImitator) {
        verifyFetchersMsgs_bothLevels(edgeImitator);
        Assert.assertTrue(popRoleMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Tenant User", RoleType.GENERIC));
        Assert.assertTrue(popRoleMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Tenant Administrator", RoleType.GENERIC));
        Assert.assertTrue(popRoleMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Customer User", RoleType.GENERIC));
        Assert.assertTrue(popRoleMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Customer Administrator", RoleType.GENERIC));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "DeviceGroup", EntityType.DEVICE, EntityType.TENANT));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "AssetGroup", EntityType.ASSET, EntityType.TENANT));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Tenant Users", EntityType.USER, EntityType.TENANT));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Tenant Administrators", EntityType.USER, EntityType.TENANT));
        Assert.assertTrue(popSyncCompletedMsg(edgeImitator.getDownlinkMsgs()));
    }

    private void verifyFetchersMsgs_bothLevels(EdgeImitator edgeImitator) {
        Assert.assertTrue(popQueueMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Main"));
        Assert.assertTrue(popRuleChainMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Edge Root Rule Chain"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "general"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "mail"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "connectivity"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "jwt"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "customTranslation"));
        Assert.assertTrue(popAdminSettingsMsg(edgeImitator.getDownlinkMsgs(), "customMenu"));
        Assert.assertTrue(popDeviceProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popDeviceProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "default"));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "test"));
        Assert.assertTrue(popCustomerMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Public", "TENANT", tenantId.getId()));
        Assert.assertTrue(popRoleMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Public User", RoleType.GENERIC));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Public Users", EntityType.USER, EntityType.CUSTOMER));
        Assert.assertTrue(popTenantMsg(edgeImitator.getDownlinkMsgs(), tenantId));
        Assert.assertTrue(popTenantProfileMsg(edgeImitator.getDownlinkMsgs(), tenantProfileId));
        Assert.assertTrue(popWhiteLabeling(edgeImitator.getDownlinkMsgs(), WhiteLabelingType.LOGIN));
        Assert.assertTrue(popWhiteLabeling(edgeImitator.getDownlinkMsgs(), WhiteLabelingType.GENERAL));
    }

    // @voba - merge comment
    // edge assign functionality only in CE/PE
    @Test
    @Ignore
    public void testSyncEdge_customerLevel() throws Exception {
        createAdminSettings();
        resetSysAdminWhiteLabelingSettings();
        loginTenantAdmin();

        // create customer
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        EntityGroup savedCustomerDeviceGroup = new EntityGroup();
        savedCustomerDeviceGroup.setType(EntityType.DEVICE);
        savedCustomerDeviceGroup.setName("CustomerDeviceGroup");
        savedCustomerDeviceGroup.setOwnerId(savedCustomer.getId());
        savedCustomerDeviceGroup = doPost("/api/entityGroup", savedCustomerDeviceGroup, EntityGroup.class);

        Device customerDevice = new Device();
        customerDevice.setName("Sync Test EG Edge Customer Device 1");
        customerDevice.setType("default");
        customerDevice.setOwnerId(savedCustomer.getId());
        Device savedDevice = doPost("/api/device", customerDevice, Device.class, "entityGroupId", savedCustomerDeviceGroup.getId().getId().toString());

        EntityGroup savedCustomerAssetGroup = new EntityGroup();
        savedCustomerAssetGroup.setType(EntityType.ASSET);
        savedCustomerAssetGroup.setName("CustomerAssetGroup");
        savedCustomerAssetGroup.setOwnerId(savedCustomer.getId());
        savedCustomerAssetGroup = doPost("/api/entityGroup", savedCustomerAssetGroup, EntityGroup.class);

        Edge edge = doPost("/api/edge", constructEdge("Sync Test EG Edge", "test"), Edge.class);

        verifyEdgeUserGroups(edge, 2);

        // simulate edge activation
        ObjectNode attributes = JacksonUtil.newObjectNode();
        attributes.put("active", true);
        doPost("/api/plugins/telemetry/EDGE/" + edge.getId() + "/attributes/" + DataConstants.SERVER_SCOPE, attributes);

        Asset customerAsset = new Asset();
        customerAsset.setName("Sync Test EG Edge Customer Asset 1");
        customerAsset.setType("test");
        customerAsset.setOwnerId(savedCustomer.getId());
        Asset savedAsset = doPost("/api/asset", customerAsset, Asset.class, "entityGroupId", savedCustomerAssetGroup.getId().getId().toString());

        doPost("/api/owner/CUSTOMER/" + savedCustomer.getId().getId() + "/EDGE/" + edge.getId().getId());

        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/entityGroup/" + savedCustomerDeviceGroup.getId().getId().toString() + "/DEVICE", EntityGroup.class);

        doPost("/api/edge/" + edge.getId().getId().toString()
                + "/entityGroup/" + savedCustomerAssetGroup.getId().getId().toString() + "/ASSET", EntityGroup.class);

        verifyEdgeUserGroups(edge, 4);

        EdgeImitator edgeImitator = new EdgeImitator(EDGE_HOST, EDGE_PORT, edge.getRoutingKey(), edge.getSecret());
        edgeImitator.ignoreType(UserCredentialsUpdateMsg.class);

        edgeImitator.expectMessageAmount(42);
        edgeImitator.connect();
        assertThat(edgeImitator.waitForMessages()).as("await for messages on first connect").isTrue();

        verifyFetchersMsgs_customerLevel(edgeImitator, savedCustomer.getId());
        // verify queue msgs
        Assert.assertTrue(popCustomerMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Edge Customer", "TENANT", tenantId.getId()));
        Assert.assertTrue(popEdgeConfigurationMsg(edgeImitator.getDownlinkMsgs(), edge.getName()));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Customer Users", EntityType.USER, EntityType.CUSTOMER));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Customer Administrators", EntityType.USER, EntityType.CUSTOMER));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "CustomerDeviceGroup", EntityType.DEVICE, EntityType.CUSTOMER));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "CustomerAssetGroup", EntityType.ASSET, EntityType.CUSTOMER));
        Assert.assertTrue(popAssetProfileMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "test"));
        Assert.assertTrue("There are some messages: " + edgeImitator.getDownlinkMsgs(), edgeImitator.getDownlinkMsgs().isEmpty());

        edgeImitator.expectMessageAmount(35);
        doPost("/api/edge/sync/" + edge.getId());
        assertThat(edgeImitator.waitForMessages()).as("await for messages after edge sync rest api call").isTrue();

        verifyFetchersMsgs_customerLevel(edgeImitator, savedCustomer.getId());
        Assert.assertTrue(edgeImitator.getDownlinkMsgs().isEmpty());

        edgeImitator.allowIgnoredTypes();
        try {
            edgeImitator.disconnect();
        } catch (Exception ignored) {
        }

        doDelete("/api/device/" + savedDevice.getId().getId().toString())
                .andExpect(status().isOk());
        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());
        doDelete("/api/edge/" + edge.getId().getId().toString())
                .andExpect(status().isOk());
    }

    private void verifyEdgeUserGroups(Edge edge, int expectedGroupNumber) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EntityGroupInfo> pageData = doGetTypedWithPageLink("/api/entityGroups/edge/" + edge.getId().getId() + "/USER?",
                            new TypeReference<>() {}, new PageLink(1024));
                    if (pageData.getData().isEmpty()) {
                        return false;
                    }
                    return pageData.getTotalElements() == expectedGroupNumber;
                });
    }

    private void verifyFetchersMsgs_customerLevel(EdgeImitator edgeImitator, CustomerId edgeCustomerId) {
        verifyFetchersMsgs_bothLevels(edgeImitator);
        Assert.assertTrue(popCustomerMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Edge Customer", "TENANT", tenantId.getId()));
        Assert.assertTrue(popCustomerMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Public", "CUSTOMER", edgeCustomerId.getId()));
        Assert.assertTrue(popRoleMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Public User", RoleType.GENERIC));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Public Users", EntityType.USER, EntityType.CUSTOMER));
        Assert.assertTrue(popRoleMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Tenant User", RoleType.GENERIC));
        Assert.assertTrue(popRoleMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Tenant Administrator", RoleType.GENERIC));
        Assert.assertTrue(popRoleMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Customer User", RoleType.GENERIC));
        Assert.assertTrue(popRoleMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Customer Administrator", RoleType.GENERIC));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "CustomerDeviceGroup", EntityType.DEVICE, EntityType.CUSTOMER));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "CustomerAssetGroup", EntityType.ASSET, EntityType.CUSTOMER));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Tenant Users", EntityType.USER, EntityType.TENANT));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Tenant Administrators", EntityType.USER, EntityType.TENANT));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Customer Users", EntityType.USER, EntityType.CUSTOMER));
        Assert.assertTrue(popEntityGroupMsg(edgeImitator.getDownlinkMsgs(), UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, "Customer Administrators", EntityType.USER, EntityType.CUSTOMER));
        Assert.assertTrue(popSyncCompletedMsg(edgeImitator.getDownlinkMsgs()));
    }

    @Test
    @Ignore
    public void testDeleteEdgeWithDeleteRelationsOk() throws Exception {
        EdgeId edgeId = savedEdge("Edge for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(tenantId, edgeId, "/api/edge/" + edgeId);
    }

    @Test
    @Ignore
    public void testDeleteEdgeExceptionWithRelationsTransactional() throws Exception {
        EdgeId edgeId = savedEdge("Edge for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(edgeDao, tenantId, edgeId, "/api/edge/" + edgeId);
    }

    private Edge savedEdge(String name) {
        Edge edge = constructEdge(name, "default");
        return doPost("/api/edge", edge, Edge.class);
    }

    @Test
    @Ignore
    public void testGetEdgeInstallInstructions() throws Exception {
        Edge edge = constructEdge(tenantId, "Edge for Test Docker Install Instructions", "default", "7390c3a6-69b0-9910-d155-b90aca4b772e", "l7q4zsjplzwhk16geqxy");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);
        String installInstructions = doGet("/api/edge/instructions/" + savedEdge.getId().getId().toString() + "/docker", String.class);
        Assert.assertTrue(installInstructions.contains("l7q4zsjplzwhk16geqxy"));
        Assert.assertTrue(installInstructions.contains("7390c3a6-69b0-9910-d155-b90aca4b772e"));
    }
}
