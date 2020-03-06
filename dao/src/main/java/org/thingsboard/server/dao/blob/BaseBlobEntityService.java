/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.TimePaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;

@Service
@Slf4j
public class BaseBlobEntityService extends AbstractEntityService implements BlobEntityService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
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
    public BlobEntity findBlobEntityById(TenantId tenantId, BlobEntityId blobEntityId) {
        log.trace("Executing findBlobEntityById [{}]", blobEntityId);
        validateId(blobEntityId, INCORRECT_BLOB_ENTITY_ID + blobEntityId);
        return blobEntityDao.findById(tenantId, blobEntityId.getId());
    }

    @Override
    public BlobEntityInfo findBlobEntityInfoById(TenantId tenantId, BlobEntityId blobEntityId) {
        log.trace("Executing findBlobEntityInfoById [{}]", blobEntityId);
        validateId(blobEntityId, INCORRECT_BLOB_ENTITY_ID + blobEntityId);
        return blobEntityInfoDao.findById(tenantId, blobEntityId.getId());
    }

    @Override
    public ListenableFuture<BlobEntityInfo> findBlobEntityInfoByIdAsync(TenantId tenantId, BlobEntityId blobEntityId) {
        log.trace("Executing findBlobEntityInfoByIdAsync [{}]", blobEntityId);
        validateId(blobEntityId, INCORRECT_BLOB_ENTITY_ID + blobEntityId);
        return blobEntityInfoDao.findByIdAsync(tenantId, blobEntityId.getId());
    }

    @Override
    public ListenableFuture<List<BlobEntityInfo>> findBlobEntityInfoByIdsAsync(TenantId tenantId, List<BlobEntityId> blobEntityIds) {
        log.trace("Executing findBlobEntityInfoByIdsAsync, tenantId [{}], blobEntityIds [{}]", tenantId, blobEntityIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(blobEntityIds, "Incorrect blobEntityIds " + blobEntityIds);
        return blobEntityInfoDao.findBlobEntitiesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(blobEntityIds));
    }

    @Override
    public PageData<BlobEntityInfo> findBlobEntitiesByTenantId(TenantId tenantId, TimePageLink pageLink) {
        return blobEntityInfoDao.findBlobEntitiesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<BlobEntityInfo> findBlobEntitiesByTenantIdAndType(TenantId tenantId, String type, TimePageLink pageLink) {
        return blobEntityInfoDao.findBlobEntitiesByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<BlobEntityInfo> findBlobEntitiesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TimePageLink pageLink) {
        return blobEntityInfoDao.findBlobEntitiesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<BlobEntityInfo> findBlobEntitiesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TimePageLink pageLink) {
        return blobEntityInfoDao.findBlobEntitiesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public BlobEntity saveBlobEntity(BlobEntity blobEntity) {
        log.trace("Executing saveBlobEntity [{}]", blobEntity);
        blobEntityValidator.validate(blobEntity, BlobEntity::getTenantId);
        return blobEntityDao.save(blobEntity.getTenantId(), blobEntity);
    }

    @Override
    public void deleteBlobEntity(TenantId tenantId, BlobEntityId blobEntityId) {
        log.trace("Executing deleteBlobEntity [{}]", blobEntityId);
        validateId(blobEntityId, INCORRECT_BLOB_ENTITY_ID + blobEntityId);
        deleteEntityRelations(tenantId, blobEntityId);
        blobEntityDao.removeById(tenantId, blobEntityId.getId());
    }

    @Override
    public void deleteBlobEntitiesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteBlobEntitiesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantBlobEntitiesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteBlobEntitiesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteBlobEntitiesByTenantIdAndCustomerId, tenantId [{}], customerId", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerBlobEntitiesRemover.removeEntities(tenantId, customerId);
    }

    private DataValidator<BlobEntity> blobEntityValidator =
            new DataValidator<BlobEntity>() {

                @Override
                protected void validateUpdate(TenantId tenantId, BlobEntity blobEntity) {
                    throw new DataValidationException("Update of BlobEntity is prohibited!");
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, BlobEntity blobEntity) {
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
                        Tenant tenant = tenantDao.findById(tenantId, blobEntity.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("BlobEntity is referencing to non-existent tenant!");
                        }
                    }
                    if (blobEntity.getCustomerId() == null) {
                        blobEntity.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!blobEntity.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(tenantId, blobEntity.getCustomerId().getId());
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
                protected PageData<BlobEntityInfo> findEntities(TenantId tenantId, TenantId id, TimePageLink pageLink) {
                    return blobEntityInfoDao.findBlobEntitiesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, BlobEntityInfo entity) {
                    deleteBlobEntity(tenantId, new BlobEntityId(entity.getId().getId()));
                }
            };

    private TimePaginatedRemover<CustomerId, BlobEntityInfo> customerBlobEntitiesRemover =
            new TimePaginatedRemover<CustomerId, BlobEntityInfo>() {

                @Override
                protected PageData<BlobEntityInfo> findEntities(TenantId tenantId, CustomerId customerId, TimePageLink pageLink) {
                    return blobEntityInfoDao.findBlobEntitiesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, BlobEntityInfo entity) {
                    deleteBlobEntity(tenantId, new BlobEntityId(entity.getId().getId()));
                }
            };
}
