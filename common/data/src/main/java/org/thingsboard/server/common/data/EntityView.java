/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;

import java.util.HashMap;
import java.util.Map;

public class EntityView<T extends BaseData<I> & HasName, I extends UUIDBased & EntityId> extends BaseData<I> implements HasName {

    private final T entity;
    private Map<String, String> attributes = new HashMap<>();

    public EntityView(T entity) {
        super(entity);
        this.entity = entity;
    }

    public T getEntity() {
        return entity;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    @Override
    public String getName() {
        return this.entity.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EntityView<?, ?> that = (EntityView<?, ?>) o;

        return entity != null ? entity.equals(that.entity) : that.entity == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (entity != null ? entity.hashCode() : 0);
        return result;
    }
}
