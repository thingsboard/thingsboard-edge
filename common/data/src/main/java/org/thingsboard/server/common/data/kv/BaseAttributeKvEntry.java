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
package org.thingsboard.server.common.data.kv;

import javax.validation.Valid;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
public class BaseAttributeKvEntry implements AttributeKvEntry {

    private static final long serialVersionUID = -6460767583563159407L;

    private final long lastUpdateTs;
    @Valid
    private final KvEntry kv;

    public BaseAttributeKvEntry(KvEntry kv, long lastUpdateTs) {
        this.kv = kv;
        this.lastUpdateTs = lastUpdateTs;
    }

    public BaseAttributeKvEntry(long lastUpdateTs, KvEntry kv) {
        this(kv, lastUpdateTs);
    }

    @Override
    public long getLastUpdateTs() {
        return lastUpdateTs;
    }

    @Override
    public String getKey() {
        return kv.getKey();
    }

    @Override
    public DataType getDataType() {
        return kv.getDataType();
    }

    @Override
    public Optional<String> getStrValue() {
        return kv.getStrValue();
    }

    @Override
    public Optional<Long> getLongValue() {
        return kv.getLongValue();
    }

    @Override
    public Optional<Boolean> getBooleanValue() {
        return kv.getBooleanValue();
    }

    @Override
    public Optional<Double> getDoubleValue() {
        return kv.getDoubleValue();
    }

    @Override
    public Optional<String> getJsonValue() {
        return kv.getJsonValue();
    }

    @Override
    public String getValueAsString() {
        return kv.getValueAsString();
    }

    @Override
    public Object getValue() {
        return kv.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseAttributeKvEntry that = (BaseAttributeKvEntry) o;

        if (lastUpdateTs != that.lastUpdateTs) return false;
        return kv.equals(that.kv);

    }

    @Override
    public int hashCode() {
        int result = (int) (lastUpdateTs ^ (lastUpdateTs >>> 32));
        result = 31 * result + kv.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BaseAttributeKvEntry{" +
                "lastUpdateTs=" + lastUpdateTs +
                ", kv=" + kv +
                '}';
    }
}
