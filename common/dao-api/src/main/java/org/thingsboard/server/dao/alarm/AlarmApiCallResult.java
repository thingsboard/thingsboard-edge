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
package org.thingsboard.server.dao.alarm;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;


@Data
public class AlarmApiCallResult {

    private final boolean successful;
    private final boolean created;
    private final boolean modified;
    private final boolean cleared;
    private final AlarmInfo alarm;
    private final Alarm old;
    private final List<EntityId> propagatedEntitiesList;

    @Builder
    private AlarmApiCallResult(boolean successful, boolean created, boolean modified, boolean cleared, AlarmInfo alarm, Alarm old, List<EntityId> propagatedEntitiesList) {
        this.successful = successful;
        this.created = created;
        this.modified = modified;
        this.cleared = cleared;
        this.alarm = alarm;
        this.old = old;
        this.propagatedEntitiesList = propagatedEntitiesList;
    }

    public AlarmApiCallResult(AlarmApiCallResult other, List<EntityId> propagatedEntitiesList) {
        this.successful = other.successful;
        this.created = other.created;
        this.modified = other.modified;
        this.cleared = other.cleared;
        this.alarm = other.alarm;
        this.old = other.old;
        this.propagatedEntitiesList = propagatedEntitiesList;
    }

    public boolean isSeverityChanged() {
        if (alarm == null || old == null) {
            return false;
        } else {
            return !alarm.getSeverity().equals(old.getSeverity());
        }
    }

    public AlarmSeverity getOldSeverity() {
        return isSeverityChanged() ? old.getSeverity() : null;
    }

    public boolean isPropagationChanged() {
        if (created) {
            return true;
        }
        if (alarm == null || old == null) {
            return false;
        }
        return (alarm.isPropagate() != old.isPropagate()) ||
                (alarm.isPropagateToOwner() != old.isPropagateToOwner()) ||
                (alarm.isPropagateToTenant() != old.isPropagateToTenant()) ||
                (!alarm.getPropagateRelationTypes().equals(old.getPropagateRelationTypes()));
    }

}
