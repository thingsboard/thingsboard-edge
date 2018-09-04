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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import lombok.Data;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Data
public class AlarmsCountMapping {

    private String target;
    private List<String> typesList;
    private List<AlarmSeverity> severityList;
    private List<AlarmStatus> statusList;
    private long latestInterval;

    public ListenableFuture<Optional<JsonObject>> countAlarms(TbContext ctx, EntityId entityId) {
        ListenableFuture<List<EntityRelation>> relationsFuture =
                ctx.getRelationService().findByFromAsync(entityId, RelationTypeGroup.ALARM);
        boolean executeFilter = (typesList != null && !typesList.isEmpty()) ||
                (severityList != null && !severityList.isEmpty()) ||
                (statusList != null && !statusList.isEmpty());
        boolean executeTimeFilter = latestInterval > 0;
        if (executeFilter) {
            ListenableFuture<List<AlarmInfo>> alarmsFuture = Futures.transformAsync(relationsFuture, relations -> {
                List<ListenableFuture<AlarmInfo>> alarmFutures = new ArrayList<>(relations.size());
                relations.forEach(relation -> alarmFutures.add(ctx.getAlarmService().findAlarmInfoByIdAsync(new AlarmId(relation.getTo().getId()))));
                return Futures.successfulAsList(alarmFutures);
            }, ctx.getDbCallbackExecutor());
            return Futures.transform(alarmsFuture,
                    alarms -> Optional.of(prepareResult(alarms.stream().filter(createAlarmFilter()).map(AlarmInfo::getId).distinct().count())),
                    ctx.getDbCallbackExecutor());
        } else if (executeTimeFilter) {
            long maxTime = System.currentTimeMillis() - latestInterval;
            return Futures.transform(relationsFuture, relations -> Optional.of(prepareResult(relations.stream().filter(relation ->
                    UUIDs.unixTimestamp(relation.getTo().getId()) >= maxTime
            ).map(EntityRelation::getTo).distinct().count())), ctx.getDbCallbackExecutor());
        } else {
            return Futures.transform(relationsFuture, relations -> Optional.of(prepareResult(relations.stream().map(EntityRelation::getTo).distinct().count())), ctx.getDbCallbackExecutor());
        }
    }

    private JsonObject prepareResult(Number number) {
        JsonObject obj = new JsonObject();
        obj.addProperty(target, number);
        return obj;
    }

    private Predicate<AlarmInfo> createAlarmFilter() {
        long maxTime = System.currentTimeMillis() - latestInterval;
        return alarmInfo -> {
            if (!matches(typesList, alarmInfo.getType())) {
                return false;
            }
            if (!matches(severityList, alarmInfo.getSeverity())) {
                return false;
            }
            if (!matches(statusList, alarmInfo.getStatus())) {
                return false;
            }
            if (latestInterval > 0) {
                if (alarmInfo.getCreatedTime() >= maxTime) {
                    return false;
                }
            }
            return true;
        };
    }

    private <T> boolean matches(List<T> filterList, T value) {
        if (filterList != null && !filterList.isEmpty()) {
            return filterList.contains(value);
        } else {
            return true;
        }
    }
}
