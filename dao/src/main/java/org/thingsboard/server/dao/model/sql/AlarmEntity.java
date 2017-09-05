/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ALARM_COLUMN_FAMILY_NAME)
public final class AlarmEntity extends BaseSqlEntity<Alarm> implements BaseEntity<Alarm> {

    @Transient
    private static final long serialVersionUID = -339979717281685984L;

    @Column(name = ALARM_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ALARM_ORIGINATOR_ID_PROPERTY)
    private String originatorId;

    @Column(name = ALARM_ORIGINATOR_TYPE_PROPERTY)
    private EntityType originatorType;

    @Column(name = ALARM_TYPE_PROPERTY)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = ALARM_SEVERITY_PROPERTY)
    private AlarmSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = ALARM_STATUS_PROPERTY)
    private AlarmStatus status;

    @Column(name = ALARM_START_TS_PROPERTY)
    private Long startTs;

    @Column(name = ALARM_END_TS_PROPERTY)
    private Long endTs;

    @Column(name = ALARM_ACK_TS_PROPERTY)
    private Long ackTs;

    @Column(name = ALARM_CLEAR_TS_PROPERTY)
    private Long clearTs;

    @Type(type = "json")
    @Column(name = ModelConstants.ASSET_ADDITIONAL_INFO_PROPERTY)
    private JsonNode details;

    @Column(name = ALARM_PROPAGATE_PROPERTY)
    private Boolean propagate;

    public AlarmEntity() {
        super();
    }

    public AlarmEntity(Alarm alarm) {
        if (alarm.getId() != null) {
            this.setId(alarm.getId().getId());
        }
        if (alarm.getTenantId() != null) {
            this.tenantId = UUIDConverter.fromTimeUUID(alarm.getTenantId().getId());
        }
        this.type = alarm.getType();
        this.originatorId = UUIDConverter.fromTimeUUID(alarm.getOriginator().getId());
        this.originatorType = alarm.getOriginator().getEntityType();
        this.type = alarm.getType();
        this.severity = alarm.getSeverity();
        this.status = alarm.getStatus();
        this.propagate = alarm.isPropagate();
        this.startTs = alarm.getStartTs();
        this.endTs = alarm.getEndTs();
        this.ackTs = alarm.getAckTs();
        this.clearTs = alarm.getClearTs();
        this.details = alarm.getDetails();
    }

    @Override
    public Alarm toData() {
        Alarm alarm = new Alarm(new AlarmId(UUIDConverter.fromString(id)));
        alarm.setCreatedTime(UUIDs.unixTimestamp(UUIDConverter.fromString(id)));
        if (tenantId != null) {
            alarm.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        }
        alarm.setOriginator(EntityIdFactory.getByTypeAndUuid(originatorType, UUIDConverter.fromString(originatorId)));
        alarm.setType(type);
        alarm.setSeverity(severity);
        alarm.setStatus(status);
        alarm.setPropagate(propagate);
        alarm.setStartTs(startTs);
        alarm.setEndTs(endTs);
        alarm.setAckTs(ackTs);
        alarm.setClearTs(clearTs);
        alarm.setDetails(details);
        return alarm;
    }

}