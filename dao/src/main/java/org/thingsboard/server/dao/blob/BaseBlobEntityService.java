/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.blob;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.TimePaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseBlobEntityService extends AbstractEntityService implements BlobEntityService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_BLOB_ENTITY_ID = "Incorrect blobEntityId ";


    @Autowired
    private BlobEntityDao blobEntityDao;

    @Autowired
    private BlobEntityInfoDao blobEntityInfoDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Override
    public BlobEntity findBlobEntityById(BlobEntityId blobEntityId) {
        log.trace("Executing findBlobEntityById [{}]", blobEntityId);
        validateId(blobEntityId, INCORRECT_BLOB_ENTITY_ID + blobEntityId);
        return blobEntityDao.findById(blobEntityId.getId());
    }

    @Override
    public BlobEntityInfo findBlobEntityInfoById(BlobEntityId blobEntityId) {
        log.trace("Executing findBlobEntityInfoById [{}]", blobEntityId);
        validateId(blobEntityId, INCORRECT_BLOB_ENTITY_ID + blobEntityId);
        return blobEntityInfoDao.findById(blobEntityId.getId());
    }

    @Override
    public ListenableFuture<BlobEntityInfo> findBlobEntityInfoByIdAsync(BlobEntityId blobEntityId) {
        log.trace("Executing findBlobEntityInfoByIdAsync [{}]", blobEntityId);
        validateId(blobEntityId, INCORRECT_BLOB_ENTITY_ID + blobEntityId);
        return blobEntityInfoDao.findByIdAsync(blobEntityId.getId());
    }

    @Override
    public TimePageData<BlobEntityInfo> findBlobEntitiesByTenantId(TenantId tenantId, TimePageLink pageLink) {
        List<BlobEntityInfo> entities = blobEntityInfoDao.findBlobEntitiesByTenantId(tenantId.getId(), pageLink);
        return new TimePageData<>(entities, pageLink);
    }

    @Override
    public TimePageData<BlobEntityInfo> findBlobEntitiesByTenantIdAndType(TenantId tenantId, String type, TimePageLink pageLink) {
        List<BlobEntityInfo> entities = blobEntityInfoDao.findBlobEntitiesByTenantIdAndType(tenantId.getId(), type, pageLink);
        return new TimePageData<>(entities, pageLink);
    }

    @Override
    public TimePageData<BlobEntityInfo> findBlobEntitiesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TimePageLink pageLink) {
        List<BlobEntityInfo> entities = blobEntityInfoDao.findBlobEntitiesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        return new TimePageData<>(entities, pageLink);
    }

    @Override
    public TimePageData<BlobEntityInfo> findBlobEntitiesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TimePageLink pageLink) {
        List<BlobEntityInfo> entities = blobEntityInfoDao.findBlobEntitiesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
        return new TimePageData<>(entities, pageLink);
    }

    @Override
    public BlobEntity saveBlobEntity(BlobEntity blobEntity) {
        log.trace("Executing saveBlobEntity [{}]", blobEntity);
        blobEntityValidator.validate(blobEntity);
        BlobEntity savedBlobEntity = blobEntityDao.save(blobEntity);
        return savedBlobEntity;
    }

    @Override
    public void deleteBlobEntity(BlobEntityId blobEntityId) {
        log.trace("Executing deleteBlobEntity [{}]", blobEntityId);
        validateId(blobEntityId, INCORRECT_BLOB_ENTITY_ID + blobEntityId);
        deleteEntityRelations(blobEntityId);
        blobEntityDao.removeById(blobEntityId.getId());
    }

    @Override
    public void deleteBlobEntitiesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteBlobEntitiesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantBlobEntitiesRemover.removeEntities(tenantId);
    }

    private DataValidator<BlobEntity> blobEntityValidator =
            new DataValidator<BlobEntity>() {

                @Override
                protected void validateUpdate(BlobEntity blobEntity) {
                    throw new DataValidationException("Update of BlobEntity is prohibited!");
                }

                @Override
                protected void validateDataImpl(BlobEntity blobEntity) {
                    if (org.springframework.util.StringUtils.isEmpty(blobEntity.getType())) {
                        throw new DataValidationException("BlobEntity type should be specified!");
                    }
                    if (org.springframework.util.StringUtils.isEmpty(blobEntity.getName())) {
                        throw new DataValidationException("BlobEntity name should be specified!");
                    }
                    if (org.springframework.util.StringUtils.isEmpty(blobEntity.getContentType())) {
                        throw new DataValidationException("BlobEntity content type should be specified!");
                    }
                    if (blobEntity.getData() == null) {
                        throw new DataValidationException("BlobEntity data should be specified!");
                    }
                    if (blobEntity.getTenantId() == null) {
                        throw new DataValidationException("BlobEntity should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(blobEntity.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("BlobEntity is referencing to non-existent tenant!");
                        }
                    }
                    if (blobEntity.getCustomerId() == null) {
                        blobEntity.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!blobEntity.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(blobEntity.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign blobEntity to non-existent customer!");
                        }
                        if (!customer.getTenantId().equals(blobEntity.getTenantId())) {
                            throw new DataValidationException("Can't assign blobEntity to customer from different tenant!");
                        }
                    }
                }
            };

    private TimePaginatedRemover<TenantId, BlobEntityInfo> tenantBlobEntitiesRemover =
            new TimePaginatedRemover<TenantId, BlobEntityInfo>() {

                @Override
                protected List<BlobEntityInfo> findEntities(TenantId id, TimePageLink pageLink) {
                    return blobEntityInfoDao.findBlobEntitiesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(BlobEntityInfo entity) {
                    deleteBlobEntity(new BlobEntityId(entity.getId().getId()));
                }
            };
}
