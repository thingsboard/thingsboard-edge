/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.secret;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.ComponentDescriptorService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
@Service("SecretDaoService")
public class SecretServiceImpl extends AbstractEntityService implements SecretService {

    private static final String INCORRECT_SECRET_ID = "Incorrect secretId ";

    @Autowired
    private SecretDao secretDao;

    @Autowired
    private SecretInfoDao secretInfoDao;

    @Autowired
    private DataValidator<Secret> secretValidator;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private ComponentDescriptorService componentDescriptorService;

    @Autowired(required = false)
    private SecretUtilService secretUtilService;

    @Autowired(required = false)
    private SecretConfigurationService secretConfigurationService;

    @Override
    public Secret saveSecret(TenantId tenantId, Secret secret) {
        log.trace("Executing saveSecret [{}]", secret);
        try {
            Secret old = secretValidator.validate(secret, Secret::getTenantId);

            if (secret.getValue() != null) {
                log.error("before enc {}", JacksonUtil.toString(secret.getValue()));
                byte[] encrypted = secretUtilService.encrypt(tenantId, secret.getType(), secret.getValue());
                log.error("after enc {}", JacksonUtil.toString(encrypted));
                secret.setValue(encrypted);
            } else if (old != null) {
                secret.setValue(old.getValue());
            }

            Secret savedSecret = secretDao.save(tenantId, secret);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedSecret.getId()).entity(savedSecret).created(secret.getId() == null).build());
            return savedSecret;
        } catch (Exception e) {
            checkConstraintViolation(e, "secret_unq_key", "Secret with such name already exists!");
            throw e;
        }
    }

    @Override
    public Secret saveSecretWithoutEncryption(TenantId tenantId, Secret secret) {
        log.trace("Executing saveSecretWithoutEncryption [{}]", secret);
        try {
            log.error("saving value {}", JacksonUtil.toString(secret.getValue()));
            secretValidator.validate(secret, Secret::getTenantId);
            Secret savedSecret = secretDao.save(tenantId, secret);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedSecret.getId()).entity(savedSecret).created(secret.getId() == null).build());
            return savedSecret;
        } catch (Exception e) {
            checkConstraintViolation(e, "secret_unq_key", "Secret with such name already exists!");
            throw e;
        }
    }

    @Override
    public void deleteSecret(TenantId tenantId, SecretInfo secretInfo) {
        SecretId secretId = secretInfo.getId();
        log.trace("Executing deleteSecret [{}]", secretId);
        validateId(secretId, id -> INCORRECT_SECRET_ID + id);
        deleteEntity(tenantId, secretId, false);
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        SecretInfo secretInfo = secretInfoDao.findById(tenantId, id.getId());
        if (secretInfo == null) {
            return;
        }
        if (!force) {
            validateSecretUsage(tenantId, secretInfo.getName());
        }
        secretDao.removeById(tenantId, id.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(secretInfo.getId()).build());
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteSecretsByTenantId, tenantId [{}]", tenantId);
        secretDao.deleteByTenantId(tenantId);
    }

    @Override
    public Secret findSecretById(TenantId tenantId, SecretId secretId) {
        log.trace("Executing findSecretById [{}] [{}]", tenantId, secretId);
        return secretDao.findById(tenantId, secretId.getId());
    }

    @Override
    public SecretInfo findSecretInfoById(TenantId tenantId, SecretId secretId) {
        log.trace("Executing findSecretInfoById [{}] [{}]", tenantId, secretId);
        return secretInfoDao.findById(tenantId, secretId.getId());
    }

    @Override
    public Secret findSecretByName(TenantId tenantId, String name) {
        log.trace("Executing findSecretByName [{}] [{}]", tenantId, name);
        return secretDao.findByName(tenantId, name);
    }

    @Override
    public PageData<SecretInfo> findSecretInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findSecretInfosByTenantId [{}]", tenantId);
        return secretInfoDao.findByTenantId(tenantId, pageLink);
    }

    @Override
    public List<String> findSecretNamesByTenantId(TenantId tenantId) {
        log.trace("Executing findSecretNamesByTenantId [{}]", tenantId);
        return secretInfoDao.findAllNamesByTenantId(tenantId);
    }

    private void validateSecretUsage(TenantId tenantId, String name) {
        validateRuleNodesUsage(tenantId, name);
        validateIntegrationsUsage(tenantId, name);
    }

    private void validateRuleNodesUsage(TenantId tenantId, String name) {
        List<String> ruleNodeClazzes = componentDescriptorService.findClazzesByHasSecret();
        for (String clazz : ruleNodeClazzes) {
            List<RuleNode> ruleNodes = ruleChainService.findRuleNodesByTenantIdAndType(tenantId, clazz);
            for (RuleNode ruleNode : ruleNodes) {
                boolean hasSecret = secretConfigurationService.matchSecretPlaceholder(ruleNode.getConfiguration(), name);
                if (hasSecret) {
                    RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, ruleNode.getRuleChainId());
                    throw new DataValidationException("Secret '" + name + "' cannot be deleted. It is used in the rule node '"
                            + ruleNode.getName() + "' in the rule chain '" + ruleChain.getName() + "'");
                }
            }
        }
    }

    private void validateIntegrationsUsage(TenantId tenantId, String name) {
        List<Integration> integrations = integrationService.findAllIntegrations(tenantId);
        for (Integration integration : integrations) {
            boolean hasSecret = secretConfigurationService.matchSecretPlaceholder(integration.getConfiguration(), name);
            if (hasSecret) {
                throw new DataValidationException("Secret '" + name + "' cannot be deleted. It is used in the integration '" + integration.getName() + "'");
            }
        }
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findSecretById(tenantId, new SecretId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SECRET;
    }

}
