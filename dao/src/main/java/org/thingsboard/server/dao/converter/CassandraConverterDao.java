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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.mapping.Result;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.EntitySubtypeEntity;
import org.thingsboard.server.dao.model.nosql.ConverterEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import javax.annotation.Nullable;
import java.util.*;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.*;

@Component
@Slf4j
@NoSqlDao
public class CassandraConverterDao extends CassandraAbstractSearchTextDao<ConverterEntity, Converter> implements ConverterDao {

    @Override
    protected Class<ConverterEntity> getColumnFamilyClass() {
        return ConverterEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return CONVERTER_COLUMN_FAMILY_NAME;
    }

    @Override
    public Converter save(Converter domain) {
        Converter savedConverter = super.save(domain);
        EntitySubtype entitySubtype = new EntitySubtype(savedConverter.getTenantId(), EntityType.CONVERTER, savedConverter.getType().toString());
        EntitySubtypeEntity entitySubtypeEntity = new EntitySubtypeEntity(entitySubtype);
        Statement saveStatement = cluster.getMapper(EntitySubtypeEntity.class).saveQuery(entitySubtypeEntity);
        executeWrite(saveStatement);
        return savedConverter;
    }

    @Override
    public List<Converter> findConvertersByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find converters by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<ConverterEntity> converterEntities = findPageWithTextSearch(CONVERTER_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(CONVERTER_TENANT_ID_PROPERTY, tenantId)), pageLink);

        log.trace("Found converters [{}] by tenantId [{}] and pageLink [{}]", converterEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(converterEntities);
    }

    @Override
    public List<Converter> findConvertersByTenantIdAndType(UUID tenantId, ConverterType type, TextPageLink pageLink) {
        log.debug("Try to find converters by tenantId [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        List<ConverterEntity> converterEntities = findPageWithTextSearch(CONVERTER_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(CONVERTER_TYPE_PROPERTY, type),
                        eq(CONVERTER_TENANT_ID_PROPERTY, tenantId)), pageLink);
        log.trace("Found converters [{}] by tenantId [{}], type [{}] and pageLink [{}]", converterEntities, tenantId, type, pageLink);
        return DaoUtil.convertDataList(converterEntities);
    }

    @Override
    public ListenableFuture<List<Converter>> findConvertersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> converterIds) {
        log.debug("Try to find converters by tenantId [{}] and converter Ids [{}]", tenantId, converterIds);
        Select select = select().from(getColumnFamilyName());
        Select.Where query = select.where();
        query.and(eq(CONVERTER_TENANT_ID_PROPERTY, tenantId));
        query.and(in(ID_PROPERTY, converterIds));
        return findListByStatementAsync(query);
    }

    @Override
    public Optional<Converter> findConvertersByTenantIdAndName(UUID tenantId, String converterName) {
        Select select = select().from(CONVERTER_BY_TENANT_AND_NAME_VIEW_NAME);
        Select.Where query = select.where();
        query.and(eq(CONVERTER_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(CONVERTER_NAME_PROPERTY, converterName));
        ConverterEntity converterEntity = findOneByStatement(query);
        return Optional.ofNullable(DaoUtil.getData(converterEntity));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantConverterTypesAsync(UUID tenantId) {
        Select select = select().from(ENTITY_SUBTYPE_COLUMN_FAMILY_NAME);
        Select.Where query = select.where();
        query.and(eq(ENTITY_SUBTYPE_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(ENTITY_SUBTYPE_ENTITY_TYPE_PROPERTY, EntityType.CONVERTER));
        query.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
        ResultSetFuture resultSetFuture = getSession().executeAsync(query);
        return Futures.transform(resultSetFuture, new Function<ResultSet, List<EntitySubtype>>() {
            @Nullable
            @Override
            public List<EntitySubtype> apply(@Nullable ResultSet resultSet) {
                Result<EntitySubtypeEntity> result = cluster.getMapper(EntitySubtypeEntity.class).map(resultSet);
                if (result != null) {
                    List<EntitySubtype> entitySubtypes = new ArrayList<>();
                    result.all().forEach((entitySubtypeEntity) ->
                            entitySubtypes.add(entitySubtypeEntity.toEntitySubtype())
                    );
                    return entitySubtypes;
                } else {
                    return Collections.emptyList();
                }
            }
        });
    }
}
