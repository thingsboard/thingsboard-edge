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
package org.thingsboard.server.dao.sql.oauth2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.dao.model.sql.OAuth2RegistrationEntity;

import java.util.List;
import java.util.UUID;

public interface OAuth2RegistrationRepository extends JpaRepository<OAuth2RegistrationEntity, UUID> {

    @Query("SELECT reg " +
            "FROM OAuth2RegistrationEntity reg " +
            "LEFT JOIN OAuth2ParamsEntity params on reg.oauth2ParamsId = params.id " +
            "LEFT JOIN OAuth2DomainEntity domain on reg.oauth2ParamsId = domain.oauth2ParamsId " +
            "WHERE params.enabled = true " +
            "AND domain.domainName = :domainName " +
            "AND domain.domainScheme IN (:domainSchemes) " +
            "AND (:pkgName IS NULL OR EXISTS (SELECT mobile FROM OAuth2MobileEntity mobile WHERE mobile.oauth2ParamsId = reg.oauth2ParamsId AND mobile.pkgName = :pkgName)) " +
            "AND (:platformFilter IS NULL OR reg.platforms IS NULL OR reg.platforms = '' OR reg.platforms LIKE :platformFilter)")
    List<OAuth2RegistrationEntity> findEnabledByDomainSchemesDomainNameAndPkgNameAndPlatformType(@Param("domainSchemes") List<SchemeType> domainSchemes,
                                                                                                 @Param("domainName") String domainName,
                                                                                                 @Param("pkgName") String pkgName,
                                                                                                 @Param("platformFilter") String platformFilter);

    List<OAuth2RegistrationEntity> findByOauth2ParamsId(UUID oauth2ParamsId);

    @Query("SELECT mobile.appSecret " +
            "FROM OAuth2MobileEntity mobile " +
            "LEFT JOIN OAuth2RegistrationEntity reg on mobile.oauth2ParamsId = reg.oauth2ParamsId " +
            "WHERE reg.id = :registrationId " +
            "AND mobile.pkgName = :pkgName")
    String findAppSecret(@Param("registrationId") UUID id,
                         @Param("pkgName") String pkgName);

}
