/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class DaoUtil {
    private static final int MAX_IN_VALUE = Short.MAX_VALUE / 2;

    private DaoUtil() {
    }

    public static <T> PageData<T> toPageData(Page<? extends ToData<T>> page) {
        List<T> data = convertDataList(page.getContent());
        return new PageData(data, page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    public static <T> PageData<T> pageToPageData(Page<T> page) {
        return new PageData(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    public static Pageable toPageable(PageLink pageLink) {
        return toPageable(pageLink, Collections.emptyMap());
    }

    public static Pageable toPageable(PageLink pageLink, Map<String,String> columnMap) {
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), toSort(pageLink.getSortOrder(), columnMap));
    }

    public static Sort toSort(SortOrder sortOrder) {
        return toSort(sortOrder, Collections.emptyMap());
    }

    public static Sort toSort(SortOrder sortOrder, Map<String,String> columnMap) {
        if (sortOrder == null) {
            return Sort.unsorted();
        } else {
            String property = sortOrder.getProperty();
            if (columnMap.containsKey(property)) {
                property = columnMap.get(property);
            }
            return Sort.by(Sort.Direction.fromString(sortOrder.getDirection().name()), property);
        }
    }

    public static <T> List<T> convertDataList(Collection<? extends ToData<T>> toDataList) {
        List<T> list = Collections.emptyList();
        if (toDataList != null && !toDataList.isEmpty()) {
            list = new ArrayList<>();
            for (ToData<T> object : toDataList) {
                if (object != null) {
                    list.add(object.toData());
                }
            }
        }
        return list;
    }

    public static <T> T getData(ToData<T> data) {
        T object = null;
        if (data != null) {
            object = data.toData();
        }
        return object;
    }

    public static <T> T getData(Optional<? extends ToData<T>> data) {
        T object = null;
        if (data.isPresent()) {
            object = data.get().toData();
        }
        return object;
    }

    public static UUID getId(UUIDBased idBased) {
        UUID id = null;
        if (idBased != null) {
            id = idBased.getId();
        }
        return id;
    }

    public static List<UUID> toUUIDs(List<? extends UUIDBased> idBasedIds) {
        List<UUID> ids = new ArrayList<>();
        for (UUIDBased idBased : idBasedIds) {
            ids.add(getId(idBased));
        }
        return ids;
    }

    public static <T> ListenableFuture<List<T>> getEntitiesByTenantIdAndIdIn(List<UUID> entityIds,
                                                                             Function<List<UUID>, Collection<? extends ToData<T>>> daoConsumer,
                                                                             JpaExecutorService service) {
        int size = entityIds.size();
        List<ListenableFuture<List<T>>> resultList = new ArrayList<>();
        if (size > MAX_IN_VALUE) {
            int startIndex = 0;
            int currentSize = 0;
            while (startIndex + currentSize < size) {
                startIndex += currentSize;
                currentSize = Math.min(size - startIndex, MAX_IN_VALUE);

                List<UUID> currentEntityIds = entityIds.subList(startIndex, startIndex + currentSize);
                resultList.add(service.submit(() -> convertDataList(daoConsumer.apply(currentEntityIds))));
            }
            return Futures.transform(Futures.allAsList(resultList), list -> {
                if (!CollectionUtils.isEmpty(list)) {
                    return list.stream().flatMap(List::stream).collect(Collectors.toList());
                }

                return Collections.emptyList();
            }, service);

        } else {
            return service.submit(() -> convertDataList(daoConsumer.apply(entityIds)));
        }
    }
}
