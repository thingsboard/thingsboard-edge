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

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.thingsboard.rule.engine.action.TbCreateAlarmNode;
import org.thingsboard.rule.engine.action.TbCreateAlarmNodeConfiguration;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.rule.RuleChainDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {BaseRuleChainControllerTest.Config.class})
public abstract class BaseRuleChainControllerTest extends AbstractControllerTest {

    private IdComparator<RuleChain> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    private RuleChainDao ruleChainDao;

    static class Config {
        @Bean
        @Primary
        public RuleChainDao ruleChainDao(RuleChainDao ruleChainDao) {
            return Mockito.mock(RuleChainDao.class, AdditionalAnswers.delegatesTo(ruleChainDao));
        }
    }

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveRuleChain() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("RuleChain");

        Mockito.reset(tbClusterService, auditLogService);

        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        Assert.assertNotNull(savedRuleChain);
        Assert.assertNotNull(savedRuleChain.getId());
        Assert.assertTrue(savedRuleChain.getCreatedTime() > 0);
        Assert.assertEquals(ruleChain.getName(), savedRuleChain.getName());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedRuleChain, savedRuleChain.getId(), savedRuleChain.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        savedRuleChain.setName("New RuleChain");
        doPost("/api/ruleChain", savedRuleChain, RuleChain.class);
        RuleChain foundRuleChain = doGet("/api/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
        Assert.assertEquals(savedRuleChain.getName(), foundRuleChain.getName());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedRuleChain, savedRuleChain.getId(), savedRuleChain.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void testSaveRuleChainWithViolationOfLengthValidation() throws Exception {

        Mockito.reset(tbClusterService, auditLogService);

        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(StringUtils.randomAlphabetic(300));
        String msgError = msgErrorFieldLength("name");
        doPost("/api/ruleChain", ruleChain)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        ruleChain.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(ruleChain,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindRuleChainById() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("RuleChain");
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        RuleChain foundRuleChain = doGet("/api/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
        Assert.assertNotNull(foundRuleChain);
        Assert.assertEquals(savedRuleChain, foundRuleChain);
    }

    @Test
    public void testDeleteRuleChain() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("RuleChain");
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        Mockito.reset(tbClusterService, auditLogService);

        String entityIdStr = savedRuleChain.getId().getId().toString();
        doDelete("/api/ruleChain/" + savedRuleChain.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(savedRuleChain, savedRuleChain.getId(), savedRuleChain.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedRuleChain.getId().getId().toString());

        doGet("/api/ruleChain/" + entityIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Rule chain", entityIdStr))));
    }

    @Test
    public void testFindEdgeRuleChainsByEdgeId() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);


        List<RuleChain> edgeRuleChains = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<RuleChain> pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/ruleChains?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        edgeRuleChains.addAll(pageData.getData());

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 28;
        for (int i = 0; i < cntEntity; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setName("RuleChain " + i);
            ruleChain.setType(RuleChainType.EDGE);
            RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
            doPost("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/ruleChain/" + savedRuleChain.getId().getId().toString(), RuleChain.class);
            edgeRuleChains.add(savedRuleChain);
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new RuleChain(), new RuleChain(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, ActionType.ADDED, cntEntity, 0, cntEntity * 2);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyWithGroup(new RuleChain(), new RuleChain(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ASSIGNED_TO_EDGE, ActionType.ASSIGNED_TO_EDGE, cntEntity, cntEntity, cntEntity * 2,
                null, null, new String(), new String(), new String());
        Mockito.reset(tbClusterService, auditLogService);

        List<RuleChain> loadedEdgeRuleChains = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/ruleChains?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedEdgeRuleChains.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(edgeRuleChains, idComparator);
        Collections.sort(loadedEdgeRuleChains, idComparator);

        Assert.assertEquals(edgeRuleChains, loadedEdgeRuleChains);

        for (RuleChain ruleChain : loadedEdgeRuleChains) {
            if (!ruleChain.isRoot()) {
                doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                        + "/ruleChain/" + ruleChain.getId().getId().toString(), RuleChain.class);
            }
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAnyWithGroup(new RuleChain(), new RuleChain(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UNASSIGNED_FROM_EDGE, ActionType.UNASSIGNED_FROM_EDGE, cntEntity, cntEntity, 3,
                null, null);

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/ruleChains?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testDeleteRuleChainWithDeleteRelationsOk() throws Exception {
        RuleChainId ruleChainId = createRuleChain("RuleChain for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), ruleChainId, "/api/ruleChain/" + ruleChainId);
    }

    @Ignore
    @Test
    public void testDeleteRuleChainExceptionWithRelationsTransactional() throws Exception {
        RuleChainId ruleChainId = createRuleChain("RuleChain for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(ruleChainDao, savedTenant.getId(), ruleChainId, "/api/ruleChain/" + ruleChainId);
    }

    @Test
    public void givenRuleNodeWithInvalidConfiguration_thenReturnError() throws Exception {
        RuleChain ruleChain = createRuleChain("Rule chain with invalid nodes");
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());

        RuleNode createAlarmNode = new RuleNode();
        createAlarmNode.setName("Create alarm");
        createAlarmNode.setType(TbCreateAlarmNode.class.getName());
        TbCreateAlarmNodeConfiguration invalidCreateAlarmNodeConfiguration = new TbCreateAlarmNodeConfiguration();
        invalidCreateAlarmNodeConfiguration.setSeverity("<script/>");
        invalidCreateAlarmNodeConfiguration.setAlarmType("<script/>");
        createAlarmNode.setConfiguration(mapper.valueToTree(invalidCreateAlarmNodeConfiguration));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(createAlarmNode);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        String error = getErrorMessage(doPost("/api/ruleChain/metadata", ruleChainMetaData)
                .andExpect(status().isBadRequest()));
        assertThat(error).contains("severity is malformed");
        assertThat(error).contains("alarmType is malformed");
    }

    private RuleChain createRuleChain(String name) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        return doPost("/api/ruleChain", ruleChain, RuleChain.class);
    }

}
