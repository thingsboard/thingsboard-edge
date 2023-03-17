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
package org.thingsboard.server.common.data.alarm;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class AlarmStatusFilter {

    private static final AlarmStatusFilter EMPTY = new AlarmStatusFilter(Optional.empty(), Optional.empty());

    private final Optional<Boolean> clearFilter;
    private final Optional<Boolean> ackFilter;

    private AlarmStatusFilter(Optional<Boolean> clearFilter, Optional<Boolean> ackFilter) {
        this.clearFilter = clearFilter;
        this.ackFilter = ackFilter;
    }

    public static AlarmStatusFilter from(AlarmQuery query) {
        if (query.getSearchStatus() != null) {
            return AlarmStatusFilter.from(query.getSearchStatus());
        } else if (query.getStatus() != null) {
            return AlarmStatusFilter.from(query.getStatus());
        }
        return AlarmStatusFilter.empty();
    }

    public static AlarmStatusFilter from(AlarmSearchStatus alarmSearchStatus) {
        switch (alarmSearchStatus) {
            case ACK:
                return new AlarmStatusFilter(Optional.empty(), Optional.of(true));
            case UNACK:
                return new AlarmStatusFilter(Optional.empty(), Optional.of(false));
            case ACTIVE:
                return new AlarmStatusFilter(Optional.of(false), Optional.empty());
            case CLEARED:
                return new AlarmStatusFilter(Optional.of(true), Optional.empty());
            default:
                return EMPTY;
        }
    }

    public static AlarmStatusFilter from(AlarmStatus alarmStatus) {
        switch (alarmStatus) {
            case ACTIVE_UNACK:
                return new AlarmStatusFilter(Optional.of(false), Optional.of(false));
            case ACTIVE_ACK:
                return new AlarmStatusFilter(Optional.of(false), Optional.of(true));
            case CLEARED_UNACK:
                return new AlarmStatusFilter(Optional.of(true), Optional.of(false));
            case CLEARED_ACK:
                return new AlarmStatusFilter(Optional.of(true), Optional.of(true));
            default:
                return EMPTY;
        }
    }

    public static AlarmStatusFilter empty() {
        return EMPTY;
    }

    public boolean hasAnyFilter() {
        return clearFilter.isPresent() || ackFilter.isPresent();
    }

    public boolean hasClearFilter() {
        return clearFilter.isPresent();
    }

    public boolean hasAckFilter() {
        return ackFilter.isPresent();
    }

    public boolean getClearFilter() {
        return clearFilter.orElseThrow(() -> new RuntimeException("Clear filter is not set! Use `hasClearFilter` to check."));
    }

    public boolean getAckFilter() {
        return ackFilter.orElseThrow(() -> new RuntimeException("Ack filter is not set! Use `hasAckFilter` to check."));
    }


    public static AlarmStatusFilter fromList(List<AlarmSearchStatus> list) {
        if (list == null || list.isEmpty() || list.contains(AlarmSearchStatus.ANY)) {
            return EMPTY;
        }
        boolean clearFilter = list.contains(AlarmSearchStatus.CLEARED);
        boolean activeFilter = list.contains(AlarmSearchStatus.ACTIVE);
        Optional<Boolean> clear = Optional.empty();
        if (clearFilter && !activeFilter || !clearFilter && activeFilter) {
            clear = Optional.of(clearFilter);
        }

        boolean ackFilter = list.contains(AlarmSearchStatus.ACK);
        boolean unackFilter = list.contains(AlarmSearchStatus.UNACK);
        Optional<Boolean> ack = Optional.empty();
        if (ackFilter && !unackFilter || !ackFilter && unackFilter) {
            ack = Optional.of(ackFilter);
        }
        return new AlarmStatusFilter(clear, ack);
    }

    public static AlarmStatusFilter from(List<AlarmStatus> filter) {
        if (filter == null || filter.isEmpty()) {
            return AlarmStatusFilter.EMPTY;
        }
        boolean clearFilter = filter.stream().anyMatch(AlarmStatus::isCleared);
        boolean activeFilter = filter.stream().anyMatch(Predicate.not(AlarmStatus::isCleared));
        Optional<Boolean> clear = Optional.empty();
        if (clearFilter && !activeFilter || !clearFilter && activeFilter) {
            clear = Optional.of(clearFilter);
        }

        boolean ackFilter = filter.stream().anyMatch(AlarmStatus::isAck);
        boolean unackFilter = filter.stream().anyMatch(Predicate.not(AlarmStatus::isAck));
        Optional<Boolean> ack = Optional.empty();
        if (ackFilter && !unackFilter || !ackFilter && unackFilter) {
            ack = Optional.of(ackFilter);
        }
        return new AlarmStatusFilter(clear, ack);
    }


}
