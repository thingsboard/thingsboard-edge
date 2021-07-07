/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.oauth2.deprecated;

import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.deprecated.ExtendedOAuth2ClientRegistrationInfo;
import org.thingsboard.server.common.data.oauth2.deprecated.OAuth2ClientRegistrationInfo;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.deprecated.OAuth2ClientRegistrationInfoEntity;
import org.thingsboard.server.dao.oauth2.deprecated.OAuth2ClientRegistrationInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Deprecated
@Component
@RequiredArgsConstructor
public class JpaOAuth2ClientRegistrationInfoDao extends JpaAbstractDao<OAuth2ClientRegistrationInfoEntity, OAuth2ClientRegistrationInfo> implements OAuth2ClientRegistrationInfoDao {
    private final OAuth2ClientRegistrationInfoRepository repository;

    @Override
    protected Class<OAuth2ClientRegistrationInfoEntity> getEntityClass() {
        return OAuth2ClientRegistrationInfoEntity.class;
    }

    @Override
    protected CrudRepository<OAuth2ClientRegistrationInfoEntity, UUID> getCrudRepository() {
        return repository;
    }

    @Override
    public List<OAuth2ClientRegistrationInfo> findAll() {
        Iterable<OAuth2ClientRegistrationInfoEntity> entities = repository.findAll();
        List<OAuth2ClientRegistrationInfo> result = new ArrayList<>();
        entities.forEach(entity -> {
            result.add(DaoUtil.getData(entity));
        });
        return result;
    }

    @Override
    public List<ExtendedOAuth2ClientRegistrationInfo> findAllExtended() {
        return repository.findAllExtended().stream()
                .map(DaoUtil::getData)
                .collect(Collectors.toList());
    }

    @Override
    public List<OAuth2ClientRegistrationInfo> findByDomainSchemesAndDomainName(List<SchemeType> domainSchemes, String domainName) {
        List<OAuth2ClientRegistrationInfoEntity> entities = repository.findAllByDomainSchemesAndName(domainSchemes, domainName);
        return entities.stream().map(DaoUtil::getData).collect(Collectors.toList());
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }
}
