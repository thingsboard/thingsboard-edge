/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.analytics.latest.alarm;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.internal.matchers.Any;
import org.mockito.internal.verification.Times;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesRelationsQuery;
import org.thingsboard.server.common.data.alarm.*;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.*;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class TbAlarmsCountNodeTest {

    private final Gson gson = new Gson();

    @Mock
    private TbContext ctx;

    @Mock
    private TbPeContext peCtx;

    @Mock
    private ListeningExecutor executor;

    @Mock
    private RelationService relationService;

    @Mock
    private AlarmService alarmService;

    private TbAlarmsCountNode node;
    private TbNodeConfiguration nodeConfiguration;

    private RuleChainId ruleChainId = new RuleChainId(UUIDs.timeBased());
    private RuleNodeId ruleNodeId = new RuleNodeId(UUIDs.timeBased());

    private RelationsQuery relationsQuery;
    private EntityId rootEntityId;

    private int scheduleCount = 0;

    private Map<EntityId, Integer> expectedAllAlarmsCountMap;
    private Map<EntityId, Integer> expectedActiveAlarmsCountMap;
    private Map<EntityId, Integer> expectedLastDayAlarmsCountMap;
    private Set<Long> alarmCreatedTimes;

    @Before
    public void init() {
        TbAlarmsCountNodeConfiguration config = new TbAlarmsCountNodeConfiguration();
        node = new TbAlarmsCountNode();

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            String type = (String) (invocationOnMock.getArguments())[0];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[1];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[2];
            String data = (String) (invocationOnMock.getArguments())[3];
            return new TbMsg(UUIDs.timeBased(), type, originator, metaData.copy(), data,
                    ruleChainId, ruleNodeId, 0);
        }).when(ctx).newMsg(Matchers.any(String.class), Matchers.any(EntityId.class),
                Matchers.any(TbMsgMetaData.class), Matchers.any(String.class));

        scheduleCount = 0;

        doAnswer((Answer<Void>) invocationOnMock -> {
            scheduleCount++;
            if (scheduleCount == 1) {
                TbMsg msg = (TbMsg) (invocationOnMock.getArguments())[0];
                node.onMsg(ctx, msg);
            }
            return null;
        }).when(ctx).tellSelf(Matchers.any(TbMsg.class), Matchers.anyLong());

        when(ctx.getPeContext()).thenReturn(peCtx);

        when(peCtx.isLocalEntity(Matchers.any(EntityId.class))).thenReturn(true);
        when(ctx.getDbCallbackExecutor()).thenReturn(executor);

        Stubber executorAnswer = doAnswer(invocationOnMock -> {
            try {
                Object arg = (invocationOnMock.getArguments())[0];
                Object result = null;
                if (arg instanceof Callable) {
                    Callable task = (Callable) arg;
                    result = task.call();
                } else if (arg instanceof Runnable) {
                    Runnable task = (Runnable) arg;
                    task.run();
                }
                return Futures.immediateFuture(result);
            } catch (Throwable th) {
                return Futures.immediateFailedFuture(th);
            }
        });

        executorAnswer.when(executor).executeAsync(Matchers.any(Callable.class));
        executorAnswer.when(executor).execute(Matchers.any(Runnable.class));

        when(ctx.getRelationService()).thenReturn(relationService);

        doAnswer((Answer<List<Long>>) invocationOnMock -> {
            AlarmQuery query = (AlarmQuery) (invocationOnMock.getArguments())[1];
            List<Predicate<AlarmInfo>> filters = (List<Predicate<AlarmInfo>>) (invocationOnMock.getArguments())[2];
            return findAlarmCounts(alarmService, query, filters);
        }).when(alarmService).findAlarmCounts(Matchers.any(), Matchers.any(AlarmQuery.class), Matchers.any(List.class));

        when(ctx.getAlarmService()).thenReturn(alarmService);

        relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        EntityTypeFilter entityTypeFilter = new EntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setFilters(Collections.singletonList(entityTypeFilter));

        rootEntityId = new TenantId(UUIDs.timeBased());

        ParentEntitiesRelationsQuery parentEntitiesRelationsQuery = new ParentEntitiesRelationsQuery();
        parentEntitiesRelationsQuery.setRootEntityId(rootEntityId);

        parentEntitiesRelationsQuery.setRelationsQuery(relationsQuery);
        parentEntitiesRelationsQuery.setChildRelationsQuery(relationsQuery);

        config.setParentEntitiesQuery(parentEntitiesRelationsQuery);
        config.setCountAlarmsForChildEntities(true);

        List<AlarmsCountMapping> alarmsCountMappings = new ArrayList<>();

        AlarmsCountMapping allAlarmsCountMapping = new AlarmsCountMapping();
        allAlarmsCountMapping.setTarget("allAlarmsCount");
        alarmsCountMappings.add(allAlarmsCountMapping);

        AlarmsCountMapping lastDayAlarmsCountMapping = new AlarmsCountMapping();
        lastDayAlarmsCountMapping.setTarget("lastDayAlarmsCount");
        lastDayAlarmsCountMapping.setLatestInterval(TimeUnit.DAYS.toMillis(1));
        alarmsCountMappings.add(lastDayAlarmsCountMapping);

        AlarmsCountMapping activeAlarmsCountMapping = new AlarmsCountMapping();
        activeAlarmsCountMapping.setStatusList(Arrays.asList(AlarmStatus.ACTIVE_ACK, AlarmStatus.ACTIVE_UNACK));
        activeAlarmsCountMapping.setTarget("activeAlarmsCount");
        alarmsCountMappings.add(activeAlarmsCountMapping);

        config.setAlarmsCountMappings(alarmsCountMappings);

        config.setPeriodTimeUnit(TimeUnit.MILLISECONDS);
        config.setPeriodValue(0);

        ObjectMapper mapper = new ObjectMapper();
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

        expectedAllAlarmsCountMap = new HashMap<>();
        expectedActiveAlarmsCountMap = new HashMap<>();
        expectedLastDayAlarmsCountMap = new HashMap<>();
        alarmCreatedTimes = new HashSet<>();
    }

    @Test
    public void parentEntitiesByRelationQueryAlarmsCount() throws TbNodeException {
        performAlarmsCountTest(false);
    }

    @Test
    public void childEntitiesFailedByRelationQueryAlarmsCount() throws TbNodeException {
        performAlarmsCountTest(true);
    }

    private void performAlarmsCountTest(boolean genFailures) throws TbNodeException {
        List<EntityRelation> parentEntityRelations = new ArrayList<>();

        int parentCount = 10 + (int)(Math.random()*20);

        int totalChildCount = 0;

        int failureCount = 0;

        for (int i=0;i<parentCount;i++) {
            EntityId parentEntityId = new AssetId(UUIDs.timeBased());
            parentEntityRelations.add(createEntityRelation(rootEntityId, parentEntityId));

            boolean shouldFail = genFailures && Math.random() > 0.6;

            List<AlarmInfo> childAlarms = new ArrayList<>();

            if (shouldFail) {
                failureCount++;
                when(relationService.findByQuery(Matchers.any(), Matchers.eq(buildQuery(parentEntityId, relationsQuery)))).
                        thenReturn(Futures.immediateFailedFuture(new RuntimeException("Failed to fetch entities!")));
            } else {
                List<EntityRelation> childRelations = new ArrayList<>();
                int childCount = 10 + (int) (Math.random() * 20);

                totalChildCount += childCount;

                for (int c = 0; c < childCount; c++) {
                    EntityId childEntityId = new DeviceId(UUIDs.timeBased());
                    childRelations.add(createEntityRelation(parentEntityId, childEntityId));
                    List<AlarmInfo> alarms = generateAlarms(childEntityId, Collections.emptyList());
                    expectedAllAlarmsCountMap.put(childEntityId, alarms.size());
                    expectedActiveAlarmsCountMap.put(childEntityId, countActive(alarms));
                    expectedLastDayAlarmsCountMap.put(childEntityId, countLastDay(alarms));
                    childAlarms.addAll(alarms);
                }
                when(relationService.findByQuery(Matchers.any(), Matchers.eq(buildQuery(parentEntityId, relationsQuery)))).thenReturn(Futures.immediateFuture(childRelations));
            }
            List<AlarmInfo> alarms = generateAlarms(parentEntityId, childAlarms);
            expectedAllAlarmsCountMap.put(parentEntityId, alarms.size());
            expectedActiveAlarmsCountMap.put(parentEntityId, countActive(alarms));
            expectedLastDayAlarmsCountMap.put(parentEntityId, countLastDay(alarms));
        }
        when(relationService.findByQuery(Matchers.any(), Matchers.eq(buildQuery(rootEntityId, relationsQuery)))).thenReturn(Futures.immediateFuture(parentEntityRelations));

        node.init(ctx, nodeConfiguration);

        int totalEntities = parentCount + totalChildCount;

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, new Times(totalEntities)).tellNext(captor.capture(), eq(SUCCESS));

        List<TbMsg> messages = captor.getAllValues();
        for (TbMsg msg : messages) {
            verifyMessage(msg);
        }

        if (failureCount > 0) {
            ArgumentCaptor<TbMsg> failureMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

            verify(ctx, new Times(failureCount)).tellFailure(failureMsgCaptor.capture(), throwableCaptor.capture());

            List<TbMsg> failedMessages = failureMsgCaptor.getAllValues();
            List<Throwable> throwables = throwableCaptor.getAllValues();
            for (int i=0;i<failedMessages.size();i++) {
                TbMsg failedMsg = failedMessages.get(i);
                Throwable t = throwables.get(i);
                Assert.assertTrue(t instanceof RuntimeException);
                Assert.assertTrue(t.getMessage().startsWith("Failed to fetch child entities for parent entity"));
                Assert.assertTrue(t.getMessage().contains(failedMsg.getOriginator().toString()));
            }
        }
    }

    private int countActive(List<AlarmInfo> alarms) {
        List<AlarmStatus> activeStatuses = Arrays.asList(AlarmStatus.ACTIVE_ACK, AlarmStatus.ACTIVE_UNACK);
        return (int)alarms.stream().filter(alarm -> activeStatuses.contains(alarm.getStatus())).count();
    }

    private int countLastDay(List<AlarmInfo> alarms) {
        long maxTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        return (int)alarms.stream().filter(alarm -> alarm.getCreatedTime() >= maxTime ).count();
    }

    private List<AlarmInfo> generateAlarms(EntityId entityId, List<AlarmInfo> childAlarms) {
        int alarmsCount = 10 + (int) (Math.random() * 20);
        List<AlarmInfo> alarms = new ArrayList<>(alarmsCount+childAlarms.size());
        List<EntityRelation> alarmRelations = new ArrayList<>(alarmsCount+childAlarms.size());
        for (int i=0;i<alarmsCount;i++) {

            AlarmInfo alarm = new AlarmInfo();

            long createdTime;

            do {
                long interval = TimeUnit.DAYS.toMillis((int) (Math.random() * 2)) + TimeUnit.HOURS.toMillis(12)
                        + (int) (Math.random() * 60000);

                createdTime = System.currentTimeMillis() - interval;
            } while (alarmCreatedTimes.contains(createdTime));

            alarmCreatedTimes.add(createdTime);

            alarm.setId(new AlarmId(UUIDs.startOf(createdTime)));
            int alarmStatusOrdinal = (int)Math.floor(Math.random() * AlarmStatus.values().length);
            alarm.setStatus(AlarmStatus.values()[alarmStatusOrdinal]);
            alarm.setStartTs(createdTime);
            alarm.setCreatedTime(createdTime);
            alarm.setSeverity(AlarmSeverity.CRITICAL);
            alarm.setOriginator(entityId);
            alarm.setType(RandomStringUtils.randomAlphanumeric(15));
            alarm.setPropagate(true);
            alarms.add(alarm);
            alarmRelations.add(createAlarmRelation(entityId, alarm.getId()));
        }
        for (int i=0;i<childAlarms.size();i++) {
            alarmRelations.add(createAlarmRelation(entityId, childAlarms.get(i).getId()));
        }
        when(relationService.findByFromAsync(Matchers.any(), Matchers.eq(entityId), Matchers.eq(RelationTypeGroup.ALARM))).thenReturn(Futures.immediateFuture(alarmRelations));
        TimePageData<AlarmInfo> pageData = new TimePageData<>(alarms, new TimePageLink(alarms.size()+1));
        when(alarmService.findAlarms(Matchers.any(), argThat(new ArgumentMatcher<AlarmQuery>() {
                                                 @Override
                                                 public boolean matches(Object query) {
                                                     return query != null && ((AlarmQuery) query).getAffectedEntityId().equals(entityId);
                                                 }
                                             }))).thenReturn(Futures.immediateFuture(pageData));
        alarms.addAll(childAlarms);
        return alarms;
    }

    private void verifyMessage(TbMsg msg) {
        Assert.assertEquals(SessionMsgType.POST_TELEMETRY_REQUEST.name(), msg.getType());
        EntityId entityId = msg.getOriginator();
        Assert.assertNotNull(entityId);
        String data = msg.getData();
        Assert.assertNotNull(data);
        JsonObject dataJson = gson.fromJson(data, JsonObject.class);

        Assert.assertTrue(dataJson.has("allAlarmsCount") ||
                dataJson.has("lastDayAlarmsCount") ||
                dataJson.has("activeAlarmsCount")
        );
        if (dataJson.has("allAlarmsCount")) {
            JsonElement elem = dataJson.get("allAlarmsCount");
            Assert.assertTrue(elem.isJsonPrimitive());
            int intVal = elem.getAsInt();
            Assert.assertEquals(expectedAllAlarmsCountMap.get(entityId).intValue(), intVal);
        }
        if (dataJson.has("lastDayAlarmsCount")) {
            JsonElement elem = dataJson.get("lastDayAlarmsCount");
            Assert.assertTrue(elem.isJsonPrimitive());
            int intVal = elem.getAsInt();
            Assert.assertEquals(expectedLastDayAlarmsCountMap.get(entityId).intValue(), intVal);
        }
        if (dataJson.has("activeAlarmsCount")) {
            JsonElement elem = dataJson.get("activeAlarmsCount");
            Assert.assertTrue(elem.isJsonPrimitive());
            int intVal = elem.getAsInt();
            Assert.assertEquals(expectedActiveAlarmsCountMap.get(entityId).intValue(), intVal);
        }
    }


    private static EntityRelation createEntityRelation(EntityId from, EntityId to) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(from);
        relation.setTo(to);
        relation.setType(EntityRelation.CONTAINS_TYPE);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        return relation;
    }

    private static EntityRelation createAlarmRelation(EntityId from, EntityId to) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(from);
        relation.setTo(to);
        relation.setType(EntityRelation.CONTAINS_TYPE);
        relation.setTypeGroup(RelationTypeGroup.ALARM);
        return relation;
    }

    private static EntityRelationsQuery buildQuery(EntityId originator, RelationsQuery relationsQuery) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        RelationsSearchParameters parameters = new RelationsSearchParameters(originator,
                relationsQuery.getDirection(), relationsQuery.getMaxLevel());
        query.setParameters(parameters);
        query.setFilters(relationsQuery.getFilters());
        return query;
    }

    private static List<Long> findAlarmCounts(AlarmService service, AlarmQuery query, List<Predicate<AlarmInfo>> filters) {
        List<Long> alarmCounts = new ArrayList<>();
        for (Predicate filter : filters) {
            alarmCounts.add(0l);
        }
        TimePageData<AlarmInfo> alarms;
        do {
            try {
                alarms = service.findAlarms(TenantId.SYS_TENANT_ID, query).get();
                for (int i = 0; i < filters.size(); i++) {
                    Predicate<AlarmInfo> filter = filters.get(i);
                    long count = alarms.getData().stream().filter(filter).map(AlarmInfo::getId).distinct().count() + alarmCounts.get(i);
                    alarmCounts.set(i, count);
                }
                if (alarms.hasNext()) {
                    query = new AlarmQuery(query.getAffectedEntityId(),
                            alarms.getNextPageLink(),
                            query.getSearchStatus(), query.getStatus(), false);
                }
            } catch (ExecutionException | InterruptedException e) {
                log.warn("Failed to find alarms by query. Query: [{}]", query);
                throw new RuntimeException(e);
            }
        } while (alarms.hasNext());
        return alarmCounts;
    }


}
