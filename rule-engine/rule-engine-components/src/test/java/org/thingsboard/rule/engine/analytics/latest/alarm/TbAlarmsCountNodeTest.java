/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesRelationsQuery;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class TbAlarmsCountNodeTest extends AbstractRuleNodeUpgradeTest {

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
    private RuleEngineAlarmService alarmService;

    private TbAlarmsCountNode node;
    private TbNodeConfiguration nodeConfiguration;

    private RelationsQuery relationsQuery;
    private EntityId rootEntityId;

    private int scheduleCount = 0;

    private Map<EntityId, Integer> expectedAllAlarmsCountMap;
    private Map<EntityId, Integer> expectedActiveAlarmsCountMap;
    private Map<EntityId, Integer> expectedLastDayAlarmsCountMap;
    private Set<Long> alarmCreatedTimes;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void init() {
        TbAlarmsCountNodeConfiguration config = new TbAlarmsCountNodeConfiguration();
        node = spy(new TbAlarmsCountNode());

        lenient().doAnswer((Answer<TbMsg>) invocationOnMock -> {
            TbMsgType type = (TbMsgType) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[3];
            String data = (String) (invocationOnMock.getArguments())[4];
            return TbMsg.newMsg()
                    .type(type)
                    .originator(originator)
                    .copyMetaData(metaData)
                    .data(data)
                    .build();
        }).when(ctx).newMsg(ArgumentMatchers.isNull(), ArgumentMatchers.any(TbMsgType.class), ArgumentMatchers.nullable(EntityId.class),
                ArgumentMatchers.any(TbMsgMetaData.class), ArgumentMatchers.any(String.class));

        scheduleCount = 0;

        lenient().doAnswer((Answer<Void>) invocationOnMock -> {
            scheduleCount++;
            if (scheduleCount == 1) {
                TbMsg msg = (TbMsg) (invocationOnMock.getArguments())[0];
                node.onMsg(ctx, msg);
            }
            return null;
        }).when(ctx).tellSelf(ArgumentMatchers.any(TbMsg.class), ArgumentMatchers.anyLong());

        lenient().when(ctx.getPeContext()).thenReturn(peCtx);

        lenient().when(peCtx.isLocalEntity(ArgumentMatchers.any(EntityId.class))).thenReturn(true);
        lenient().when(ctx.getDbCallbackExecutor()).thenReturn(executor);

        Stubber executorAnswer = lenient().doAnswer(invocationOnMock -> {
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

        executorAnswer.when(executor).execute(ArgumentMatchers.any(Runnable.class));

        lenient().when(ctx.getRelationService()).thenReturn(relationService);

        lenient().doAnswer((Answer<List<Long>>) invocationOnMock -> {
            AlarmQuery query = (AlarmQuery) (invocationOnMock.getArguments())[1];
            List<AlarmFilter> filters = (List<AlarmFilter>) (invocationOnMock.getArguments())[2];
            return findAlarmCounts(alarmService, query, filters);
        }).when(alarmService).findAlarmCounts(ArgumentMatchers.any(), ArgumentMatchers.any(AlarmQuery.class), ArgumentMatchers.any(List.class));

        lenient().when(ctx.getAlarmService()).thenReturn(alarmService);

        relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        RelationEntityTypeFilter entityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setFilters(Collections.singletonList(entityTypeFilter));

        rootEntityId = new TenantId(Uuids.timeBased());

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
        config.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());

        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

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

    // Rule nodes upgrade
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null},\"periodTimeUnit\":\"MINUTES\"," +
                                "\"periodValue\":5,\"countAlarmsForChildEntities\":false,\"queueName\":null," +
                                "\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]}",
                        true,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null},\"periodTimeUnit\":\"MINUTES\"," +
                                "\"periodValue\":5,\"countAlarmsForChildEntities\":false,\"outMsgType\":\"POST_TELEMETRY_REQUEST\"," +
                                "\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]}"),
                // default config for version 0 with queueName
                Arguments.of(0,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null},\"periodTimeUnit\":\"MINUTES\"," +
                                "\"periodValue\":5,\"countAlarmsForChildEntities\":false,\"queueName\":\"Main\"," +
                                "\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]}",
                        true,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null},\"periodTimeUnit\":\"MINUTES\"," +
                                "\"periodValue\":5,\"countAlarmsForChildEntities\":false,\"outMsgType\":\"POST_TELEMETRY_REQUEST\"," +
                                "\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]}"),
                // default config for version 1 with upgrade from version 1
                Arguments.of(1,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null},\"periodTimeUnit\":\"MINUTES\"," +
                                "\"periodValue\":5,\"countAlarmsForChildEntities\":false,\"queueName\":\"Main\",\"outMsgType\":\"POST_TELEMETRY_REQUEST\"," +
                                "\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]}",
                        true,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null},\"periodTimeUnit\":\"MINUTES\"," +
                                "\"periodValue\":5,\"countAlarmsForChildEntities\":false,\"outMsgType\":\"POST_TELEMETRY_REQUEST\"," +
                                "\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]}"),
                // default config for version 2 with upgrade from version 0
                Arguments.of(0,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null},\"periodTimeUnit\":\"MINUTES\"," +
                                "\"periodValue\":5,\"countAlarmsForChildEntities\":false,\"outMsgType\":\"POST_TELEMETRY_REQUEST\"," +
                                "\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]}",
                        false,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null},\"periodTimeUnit\":\"MINUTES\"," +
                                "\"periodValue\":5,\"countAlarmsForChildEntities\":false,\"outMsgType\":\"POST_TELEMETRY_REQUEST\"," +
                                "\"alarmsCountMappings\":[{\"target\":\"alarmsCount\",\"typesList\":null,\"severityList\":null,\"statusList\":null,\"latestInterval\":0}]}")
        );
    }

    private void performAlarmsCountTest(boolean genFailures) throws TbNodeException {
        List<EntityRelation> parentEntityRelations = new ArrayList<>();

        int parentCount = 10 + (int) (Math.random() * 20);

        int totalChildCount = 0;

        int failureCount = 0;

        for (int i = 0; i < parentCount; i++) {
            EntityId parentEntityId = new AssetId(Uuids.timeBased());
            parentEntityRelations.add(createEntityRelation(rootEntityId, parentEntityId));

            boolean shouldFail = genFailures && Math.random() > 0.6;

            List<AlarmInfo> childAlarms = new ArrayList<>();

            if (shouldFail) {
                failureCount++;
                when(relationService.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.eq(buildQuery(parentEntityId, relationsQuery)))).
                        thenReturn(Futures.immediateFailedFuture(new RuntimeException("Failed to fetch entities!")));
            } else {
                List<EntityRelation> childRelations = new ArrayList<>();
                int childCount = 10 + (int) (Math.random() * 20);

                totalChildCount += childCount;

                for (int c = 0; c < childCount; c++) {
                    EntityId childEntityId = new DeviceId(Uuids.timeBased());
                    childRelations.add(createEntityRelation(parentEntityId, childEntityId));
                    List<AlarmInfo> alarms = generateAlarms(childEntityId, Collections.emptyList());
                    expectedAllAlarmsCountMap.put(childEntityId, alarms.size());
                    expectedActiveAlarmsCountMap.put(childEntityId, countActive(alarms));
                    expectedLastDayAlarmsCountMap.put(childEntityId, countLastDay(alarms));
                    childAlarms.addAll(alarms);
                }
                when(relationService.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.eq(buildQuery(parentEntityId, relationsQuery)))).thenReturn(Futures.immediateFuture(childRelations));
            }
            List<AlarmInfo> alarms = generateAlarms(parentEntityId, childAlarms);
            expectedAllAlarmsCountMap.put(parentEntityId, alarms.size());
            expectedActiveAlarmsCountMap.put(parentEntityId, countActive(alarms));
            expectedLastDayAlarmsCountMap.put(parentEntityId, countLastDay(alarms));
        }
        when(relationService.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.eq(buildQuery(rootEntityId, relationsQuery)))).thenReturn(Futures.immediateFuture(parentEntityRelations));

        node.init(ctx, nodeConfiguration);

        int totalEntities = parentCount + totalChildCount;

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, new Times(totalEntities)).enqueueForTellNext(captor.capture(), eq(TbNodeConnectionType.SUCCESS));

        List<TbMsg> messages = captor.getAllValues();
        for (TbMsg msg : messages) {
            verifyMessage(msg);
        }

        if (failureCount > 0) {
            ArgumentCaptor<TbMsg> failureMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            ArgumentCaptor<String> throwableCaptor = ArgumentCaptor.forClass(String.class);

            verify(ctx, new Times(failureCount)).enqueueForTellFailure(failureMsgCaptor.capture(), throwableCaptor.capture());

            List<TbMsg> failedMessages = failureMsgCaptor.getAllValues();
            List<String> throwables = throwableCaptor.getAllValues();
            for (int i = 0; i < failedMessages.size(); i++) {
                TbMsg failedMsg = failedMessages.get(i);
                String t = throwables.get(i);
                Assertions.assertTrue(t.startsWith("Failed to fetch child entities for parent entity"));
                Assertions.assertTrue(t.contains(failedMsg.getOriginator().toString()));
            }
        }
    }

    private int countActive(List<AlarmInfo> alarms) {
        List<AlarmStatus> activeStatuses = Arrays.asList(AlarmStatus.ACTIVE_ACK, AlarmStatus.ACTIVE_UNACK);
        return (int) alarms.stream().filter(alarm -> activeStatuses.contains(alarm.getStatus())).count();
    }

    private int countLastDay(List<AlarmInfo> alarms) {
        long maxTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        return (int) alarms.stream().filter(alarm -> alarm.getCreatedTime() >= maxTime).count();
    }

    private List<AlarmInfo> generateAlarms(EntityId entityId, List<AlarmInfo> childAlarms) {
        int alarmsCount = 10 + (int) (Math.random() * 20);
        List<AlarmInfo> alarms = new ArrayList<>(alarmsCount + childAlarms.size());
        for (int i = 0; i < alarmsCount; i++) {

            AlarmInfo alarm = new AlarmInfo();

            long createdTime;

            do {
                long interval = TimeUnit.DAYS.toMillis((int) (Math.random() * 2)) + TimeUnit.HOURS.toMillis(12)
                        + (int) (Math.random() * 60000);

                createdTime = System.currentTimeMillis() - interval;
            } while (alarmCreatedTimes.contains(createdTime));

            alarmCreatedTimes.add(createdTime);

            alarm.setId(new AlarmId(Uuids.startOf(createdTime)));
            int alarmStatusOrdinal = (int) Math.floor(Math.random() * AlarmStatus.values().length);
            var alarmStatus = AlarmStatus.values()[alarmStatusOrdinal];
            alarm.setCleared(alarmStatus.isCleared());
            alarm.setAcknowledged(alarmStatus.isAck());
            alarm.setStartTs(createdTime);
            alarm.setCreatedTime(createdTime);
            alarm.setSeverity(AlarmSeverity.CRITICAL);
            alarm.setOriginator(entityId);
            alarm.setType(StringUtils.randomAlphanumeric(15));
            alarm.setPropagate(true);
            alarms.add(alarm);
        }
        PageData<AlarmInfo> pageData = new PageData<>(alarms, 1, alarms.size(), false);
        when(alarmService.findAlarms(ArgumentMatchers.any(), argThat(query -> query != null && query.getAffectedEntityId().equals(entityId)))).thenReturn(pageData);
        alarms.addAll(childAlarms);
        return alarms;
    }

    private void verifyMessage(TbMsg msg) {
        Assertions.assertTrue(msg.isTypeOf(TbMsgType.POST_TELEMETRY_REQUEST));
        EntityId entityId = msg.getOriginator();
        Assertions.assertNotNull(entityId);
        String data = msg.getData();
        Assertions.assertNotNull(data);
        JsonObject dataJson = gson.fromJson(data, JsonObject.class);

        Assertions.assertTrue(dataJson.has("allAlarmsCount") ||
                dataJson.has("lastDayAlarmsCount") ||
                dataJson.has("activeAlarmsCount")
        );
        if (dataJson.has("allAlarmsCount")) {
            JsonElement elem = dataJson.get("allAlarmsCount");
            Assertions.assertTrue(elem.isJsonPrimitive());
            int intVal = elem.getAsInt();
            Assertions.assertEquals(expectedAllAlarmsCountMap.get(entityId).intValue(), intVal);
        }
        if (dataJson.has("lastDayAlarmsCount")) {
            JsonElement elem = dataJson.get("lastDayAlarmsCount");
            Assertions.assertTrue(elem.isJsonPrimitive());
            int intVal = elem.getAsInt();
            Assertions.assertEquals(expectedLastDayAlarmsCountMap.get(entityId).intValue(), intVal);
        }
        if (dataJson.has("activeAlarmsCount")) {
            JsonElement elem = dataJson.get("activeAlarmsCount");
            Assertions.assertTrue(elem.isJsonPrimitive());
            int intVal = elem.getAsInt();
            Assertions.assertEquals(expectedActiveAlarmsCountMap.get(entityId).intValue(), intVal);
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

    private static EntityRelationsQuery buildQuery(EntityId originator, RelationsQuery relationsQuery) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        RelationsSearchParameters parameters = new RelationsSearchParameters(originator,
                relationsQuery.getDirection(), relationsQuery.getMaxLevel(), false);
        query.setParameters(parameters);
        query.setFilters(relationsQuery.getFilters());
        return query;
    }

    private static List<Long> findAlarmCounts(RuleEngineAlarmService service, AlarmQuery query, List<AlarmFilter> filters) {
        List<Long> alarmCounts = new ArrayList<>();
        for (AlarmFilter filter : filters) {
            alarmCounts.add(0l);
        }
        PageData<AlarmInfo> alarms;
        do {
            alarms = service.findAlarms(TenantId.SYS_TENANT_ID, query);
            for (int i = 0; i < filters.size(); i++) {
                Predicate<AlarmInfo> filter = matchAlarmFilter(filters.get(i));
                long count = alarms.getData().stream().filter(filter).map(AlarmInfo::getId).distinct().count() + alarmCounts.get(i);
                alarmCounts.set(i, count);
            }
            if (alarms.hasNext()) {
                query = new AlarmQuery(query.getAffectedEntityId(), query.getPageLink(), query.getSearchStatus(), query.getStatus(), null, false);
            }
        } while (alarms.hasNext());
        return alarmCounts;
    }

    private static Predicate<AlarmInfo> matchAlarmFilter(AlarmFilter filter) {
        return alarmInfo -> {
            if (!matches(filter.getTypesList(), alarmInfo.getType())) {
                return false;
            }
            if (!matches(filter.getSeverityList(), alarmInfo.getSeverity())) {
                return false;
            }
            if (!matches(filter.getStatusList(), alarmInfo.getStatus())) {
                return false;
            }
            if (filter.getStartTime() != null) {
                if (alarmInfo.getCreatedTime() <= filter.getStartTime()) {
                    return false;
                }
            }
            return true;
        };
    }

    private static <T> boolean matches(List<T> filterList, T value) {
        if (filterList != null && !filterList.isEmpty()) {
            return filterList.contains(value);
        } else {
            return true;
        }
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }
}
