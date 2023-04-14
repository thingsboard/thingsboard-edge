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
import org.thingsboard.server.common.data.kv.AggTsKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.BOOLEAN_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.DOUBLE_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.JSON_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.LONG_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.STRING_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.TS_COLUMN;

@Data
@MappedSuperclass
public abstract class AbstractTsKvEntity implements ToData<TsKvEntry> {

    protected static final String SUM = "SUM";
    protected static final String AVG = "AVG";
    protected static final String MIN = "MIN";
    protected static final String MAX = "MAX";

    @Id
    @Column(name = ENTITY_ID_COLUMN, columnDefinition = "uuid")
    protected UUID entityId;

    @Id
    @Column(name = KEY_COLUMN)
    protected int key;

    @Id
    @Column(name = TS_COLUMN)
    protected Long ts;

    @Column(name = BOOLEAN_VALUE_COLUMN)
    protected Boolean booleanValue;

    @Column(name = STRING_VALUE_COLUMN)
    protected String strValue;

    @Column(name = LONG_VALUE_COLUMN)
    protected Long longValue;

    @Column(name = DOUBLE_VALUE_COLUMN)
    protected Double doubleValue;

    @Column(name = JSON_VALUE_COLUMN)
    protected String jsonValue;

    @Transient
    protected String strKey;

    @Transient
    protected Long aggValuesLastTs;
    @Transient
    protected Long aggValuesCount;

    public AbstractTsKvEntity() {
    }

    public AbstractTsKvEntity(Long aggValuesLastTs) {
        this.aggValuesLastTs = aggValuesLastTs;
    }

    public abstract boolean isNotEmpty();

    protected static boolean isAllNull(Object... args) {
        for (Object arg : args) {
            if (arg != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public TsKvEntry toData() {
        KvEntry kvEntry = null;
        if (strValue != null) {
            kvEntry = new StringDataEntry(strKey, strValue);
        } else if (longValue != null) {
            kvEntry = new LongDataEntry(strKey, longValue);
        } else if (doubleValue != null) {
            kvEntry = new DoubleDataEntry(strKey, doubleValue);
        } else if (booleanValue != null) {
            kvEntry = new BooleanDataEntry(strKey, booleanValue);
        } else if (jsonValue != null) {
            kvEntry = new JsonDataEntry(strKey, jsonValue);
        }

        if (aggValuesCount == null) {
            return new BasicTsKvEntry(ts, kvEntry);
        } else {
            return new AggTsKvEntry(ts, kvEntry, aggValuesCount);
        }
    }

}