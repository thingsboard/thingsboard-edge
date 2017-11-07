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
package org.thingsboard.server.dao.converter;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterSearchQuery;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.*;

@Service
@Slf4j
public class BaseConverterService extends AbstractEntityService implements ConverterService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_CONVERTER_ID = "Incorrect converterId ";

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private EntityService entityService;

    @Autowired
    private ConverterDao converterDao;


    @Override
    public Converter findConverterById(ConverterId converterId) {
        log.trace("Executing findConverterById [{}]", converterId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        return converterDao.findById(converterId.getId());
    }

    @Override
    public ListenableFuture<Converter> findConverterByIdAsync(ConverterId converterId) {
        log.trace("Executing findConverterById [{}]", converterId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        return converterDao.findByIdAsync(converterId.getId());
    }

    @Override
    public Optional<Converter> findConverterByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findConverterByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return converterDao.findConvertersByTenantIdAndName(tenantId.getId(), name);
    }

    @Override
    public Converter saveConverter(Converter converter) {
        log.trace("Executing saveConverter [{}]", converter);
        converterValidator.validate(converter);
        Converter savedConverter = converterDao.save(converter);
        if (converter.getId() == null) {
            entityGroupService.addEntityToEntityGroupAll(savedConverter.getTenantId(), savedConverter.getId());
        }
        return savedConverter;
    }

    @Override
    public void deleteConverter(ConverterId converterId) {
        log.trace("Executing deleteConverter [{}]", converterId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        deleteEntityRelations(converterId);
        converterDao.removeById(converterId.getId());
    }

    @Override
    public TextPageData<Converter> findConvertersByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findConvertersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Converter> converters = converterDao.findConvertersByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(converters, pageLink);
    }

    @Override
    public TextPageData<Converter> findConvertersByTenantIdAndType(TenantId tenantId, String type, TextPageLink pageLink) {
        log.trace("Executing findConvertersByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Converter> converters = converterDao.findConvertersByTenantIdAndType(tenantId.getId(), type, pageLink);
        return new TextPageData<>(converters, pageLink);
    }

    @Override
    public ListenableFuture<List<Converter>> findConvertersByTenantIdAndIdsAsync(TenantId tenantId, List<ConverterId> converterIds) {
        log.trace("Executing findConvertersByTenantIdAndIdsAsync, tenantId [{}], converterIds [{}]", tenantId, converterIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(converterIds, "Incorrect converterIds " + converterIds);
        return converterDao.findConvertersByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(converterIds));
    }

    @Override
    public void deleteConvertersByTenantId(TenantId tenantId) {
        log.trace("Executing deleteConvertersByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantConvertersRemover.removeEntities(tenantId);
    }

    @Override
    public ListenableFuture<List<Converter>> findConvertersByQuery(ConverterSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(query.toEntitySearchQuery());
        ListenableFuture<List<Converter>> converters = Futures.transform(relations, (AsyncFunction<List<EntityRelation>, List<Converter>>) relations1 -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Converter>> futures = new ArrayList<>();
            for (EntityRelation relation : relations1) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.CONVERTER) {
                    futures.add(findConverterByIdAsync(new ConverterId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        });
        converters = Futures.transform(converters, (Function<List<Converter>, List<Converter>>) converterList ->
                converterList == null ? Collections.emptyList() : converterList.stream().filter(converter -> query.getConverterTypes().contains(converter.getType())).collect(Collectors.toList())
        );
        return converters;
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findConverterTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findConverterTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantConverterTypes = converterDao.findTenantConverterTypesAsync(tenantId.getId());
        return Futures.transform(tenantConverterTypes,
                (Function<List<EntitySubtype>, List<EntitySubtype>>) converterTypes -> {
                    converterTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return converterTypes;
                });
    }

    @Override
    public EntityView findGroupConverter(EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing findGroupConverter, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        validateEntityId(entityId, "Incorrect entityId " + entityId);
        return entityGroupService.findGroupEntity(entityGroupId, entityId, converterViewFunction);
    }

    @Override
    public ListenableFuture<TimePageData<EntityView>> findConvertersByEntityGroupId(EntityGroupId entityGroupId, TimePageLink pageLink) {
        log.trace("Executing findConvertersByEntityGroupId, entityGroupId [{}], pageLink [{}]", entityGroupId, pageLink);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        return entityGroupService.findEntities(entityGroupId, pageLink, converterViewFunction);
    }

    private BiFunction<EntityView, List<EntityField>, EntityView> converterViewFunction = ((entityView, entityFields) -> {
        Converter converter = findConverterById(new ConverterId(entityView.getId().getId()));
        entityView.put(EntityField.NAME.name().toLowerCase(), converter.getName());
        for (EntityField field : entityFields) {
            String key = field.name().toLowerCase();
            if (field == EntityField.TYPE) {
                entityView.put(key, converter.getType());
            }
        }
        return entityView;
    });

    private DataValidator<Converter> converterValidator =
            new DataValidator<Converter>() {

                @Override
                protected void validateCreate(Converter converter) {
                    converterDao.findConvertersByTenantIdAndName(converter.getTenantId().getId(), converter.getName()).ifPresent(
                            d -> {
                                throw new DataValidationException("Converter with such name already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(Converter converter) {
                    converterDao.findConvertersByTenantIdAndName(converter.getTenantId().getId(), converter.getName()).ifPresent(
                            d -> {
                                if (!d.getId().equals(converter.getId())) {
                                    throw new DataValidationException("Converter with such name already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(Converter converter) {
                    if (StringUtils.isEmpty(converter.getType())) {
                        throw new DataValidationException("Converter type should be specified!");
                    }
                    if (StringUtils.isEmpty(converter.getName())) {
                        throw new DataValidationException("Converter name should be specified!");
                    }
                    if (converter.getTenantId() == null) {
                        throw new DataValidationException("Converter should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(converter.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Converter is referencing to non-existent tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Converter> tenantConvertersRemover =
            new PaginatedRemover<TenantId, Converter>() {

                @Override
                protected List<Converter> findEntities(TenantId id, TextPageLink pageLink) {
                    return converterDao.findConvertersByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(Converter entity) {
                    deleteConverter(new ConverterId(entity.getId().getId()));
                }
            };
}
