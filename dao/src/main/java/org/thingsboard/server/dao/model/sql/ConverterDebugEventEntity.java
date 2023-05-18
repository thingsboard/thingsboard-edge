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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.ConverterDebugEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.CONVERTER_DEBUG_EVENT_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERROR_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_IN_MSG_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_IN_MSG_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_METADATA_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_OUT_MSG_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_OUT_MSG_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_TYPE_COLUMN_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CONVERTER_DEBUG_EVENT_TABLE_NAME)
@NoArgsConstructor
public class ConverterDebugEventEntity extends EventEntity<ConverterDebugEvent> implements BaseEntity<ConverterDebugEvent> {

    @Column(name = EVENT_TYPE_COLUMN_NAME)
    private String eventType;
    @Column(name = EVENT_IN_MSG_TYPE_COLUMN_NAME)
    private String inMsgType;
    @Column(name = EVENT_IN_MSG_COLUMN_NAME)
    private String inMsg;
    @Column(name = EVENT_OUT_MSG_TYPE_COLUMN_NAME)
    private String outMsgType;
    @Column(name = EVENT_OUT_MSG_COLUMN_NAME)
    private String outMsg;
    @Column(name = EVENT_METADATA_COLUMN_NAME)
    private String metadata;
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    public ConverterDebugEventEntity(ConverterDebugEvent event) {
        super(event);
        this.eventType = event.getEventType();
        this.inMsgType = event.getInMsgType();
        this.inMsg = event.getInMsg();
        this.outMsgType = event.getOutMsgType();
        this.outMsg = event.getOutMsg();
        this.metadata = event.getMetadata();
        this.error = event.getError();
    }

    @Override
    public ConverterDebugEvent toData() {
        var builder = ConverterDebugEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .eventType(eventType)
                .inMsgType(inMsgType)
                .inMsg(inMsg)
                .outMsgType(outMsgType)
                .outMsg(outMsg)
                .metadata(metadata)
                .error(error);
        return builder.build();
    }

}
