/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.OAuth2ClientEntity;

import java.util.List;
import java.util.UUID;

public interface OAuth2ClientRepository extends JpaRepository<OAuth2ClientEntity, UUID> {

    @Query("SELECT с FROM OAuth2ClientEntity с WHERE с.tenantId = :tenantId AND " +
            "(:searchText is NULL OR ilike(с.title, concat('%', :searchText, '%')) = true)")
    Page<OAuth2ClientEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                            @Param("searchText") String searchText,
                                            Pageable pageable);

    @Query("SELECT c " +
            "FROM OAuth2ClientEntity c " +
            "LEFT JOIN DomainOauth2ClientEntity dc on c.id = dc.oauth2ClientId " +
            "LEFT JOIN DomainEntity domain on dc.domainId = domain.id " +
            "WHERE domain.name = :domainName AND domain.oauth2Enabled = true " +
            "AND (:platformFilter IS NULL OR c.platforms IS NULL OR c.platforms = '' OR c.platforms LIKE :platformFilter)")
    List<OAuth2ClientEntity> findEnabledByDomainNameAndPlatformType(@Param("domainName") String domainName,
                                                                    @Param("platformFilter") String platformFilter);

    @Query("SELECT c " +
            "FROM OAuth2ClientEntity c " +
            "LEFT JOIN MobileAppOauth2ClientEntity mc on c.id = mc.oauth2ClientId " +
            "LEFT JOIN MobileAppEntity app on mc.mobileAppId = app.id " +
            "WHERE app.pkgName = :pkgName AND app.oauth2Enabled = true " +
            "AND (:platformFilter IS NULL OR c.platforms IS NULL OR c.platforms = '' OR ilike(c.platforms, CONCAT('%', :platformFilter, '%')) = true)")
    List<OAuth2ClientEntity> findEnabledByPkgNameAndPlatformType(@Param("pkgName") String pkgName,
                                                                 @Param("platformFilter") String platformFilter);

    @Query("SELECT c " +
            "FROM OAuth2ClientEntity c " +
            "LEFT JOIN DomainOauth2ClientEntity dc on dc.oauth2ClientId = c.id " +
            "WHERE dc.domainId = :domainId ")
    List<OAuth2ClientEntity> findByDomainId(@Param("domainId") UUID domainId);

    @Query("SELECT c " +
            "FROM OAuth2ClientEntity c " +
            "LEFT JOIN MobileAppOauth2ClientEntity mc on mc.oauth2ClientId = c.id " +
            "WHERE mc.mobileAppId = :mobileAppId ")
    List<OAuth2ClientEntity> findByMobileAppId(@Param("mobileAppId") UUID mobileAppId);

    @Query("SELECT m.appSecret " +
            "FROM MobileAppEntity m " +
            "LEFT JOIN MobileAppOauth2ClientEntity mp on m.id = mp.mobileAppId " +
            "LEFT JOIN OAuth2ClientEntity p on mp.oauth2ClientId = p.id " +
            "WHERE p.id = :clientId " +
            "AND m.pkgName = :pkgName")
    String findAppSecret(@Param("clientId") UUID id,
                         @Param("pkgName") String pkgName);

    @Transactional
    @Modifying
    @Query("DELETE FROM OAuth2ClientEntity t WHERE t.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    List<OAuth2ClientEntity> findByTenantIdAndIdIn(UUID tenantId, List<UUID> uuids);

}
