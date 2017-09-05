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
package org.thingsboard.server.dao.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.common.data.rule.RuleMetaData;
import org.thingsboard.server.dao.component.ComponentDescriptorService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.DatabaseException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.plugin.PluginService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class BaseRuleService extends AbstractEntityService implements RuleService {

    private final TenantId systemTenantId = new TenantId(NULL_UUID);

    @Autowired
    public RuleDao ruleDao;

    @Autowired
    public PluginService pluginService;

    @Autowired
    private ComponentDescriptorService componentDescriptorService;

    @Override
    public RuleMetaData saveRule(RuleMetaData rule) {
        ruleValidator.validate(rule);
        if (rule.getTenantId() == null) {
            log.trace("Save system rule metadata with predefined id {}", systemTenantId);
            rule.setTenantId(systemTenantId);
        }
        if (rule.getId() != null) {
            RuleMetaData oldVersion = ruleDao.findById(rule.getId());
            if (rule.getState() == null) {
                rule.setState(oldVersion.getState());
            } else if (rule.getState() != oldVersion.getState()) {
                throw new IncorrectParameterException("Use Activate/Suspend method to control state of the rule!");
            }
        } else {
            if (rule.getState() == null) {
                rule.setState(ComponentLifecycleState.SUSPENDED);
            } else if (rule.getState() != ComponentLifecycleState.SUSPENDED) {
                throw new IncorrectParameterException("Use Activate/Suspend method to control state of the rule!");
            }
        }

        validateFilters(rule.getFilters());
        if (rule.getProcessor() != null && !rule.getProcessor().isNull()) {
            validateComponentJson(rule.getProcessor(), ComponentType.PROCESSOR);
        }
        if (rule.getAction() != null && !rule.getAction().isNull()) {
            validateComponentJson(rule.getAction(), ComponentType.ACTION);
        }
        validateRuleAndPluginState(rule);
        return ruleDao.save(rule);
    }

    private void validateFilters(JsonNode filtersJson) {
        if (filtersJson == null || filtersJson.isNull()) {
            throw new IncorrectParameterException("Rule filters are required!");
        }
        if (!filtersJson.isArray()) {
            throw new IncorrectParameterException("Filters json is not an array!");
        }
        ArrayNode filtersArray = (ArrayNode) filtersJson;
        for (int i = 0; i < filtersArray.size(); i++) {
            validateComponentJson(filtersArray.get(i), ComponentType.FILTER);
        }
    }

    private void validateComponentJson(JsonNode json, ComponentType type) {
        if (json == null || json.isNull()) {
            throw new IncorrectParameterException(type.name() + " is required!");
        }
        String clazz = getIfValid(type.name(), json, "clazz", JsonNode::isTextual, JsonNode::asText);
        String name = getIfValid(type.name(), json, "name", JsonNode::isTextual, JsonNode::asText);
        JsonNode configuration = getIfValid(type.name(), json, "configuration", JsonNode::isObject, node -> node);
        ComponentDescriptor descriptor = componentDescriptorService.findByClazz(clazz);
        if (descriptor == null) {
            throw new IncorrectParameterException(type.name() + " clazz " + clazz + " is not a valid component!");
        }
        if (descriptor.getType() != type) {
            throw new IncorrectParameterException("Clazz " + clazz + " is not a valid " + type.name() + " component!");
        }
        if (!componentDescriptorService.validate(descriptor, configuration)) {
            throw new IncorrectParameterException(type.name() + " configuration is not valid!");
        }
    }

    private void validateRuleAndPluginState(RuleMetaData rule) {
        if (org.springframework.util.StringUtils.isEmpty(rule.getPluginToken())) {
            return;
        }
        PluginMetaData pluginMd = pluginService.findPluginByApiToken(rule.getPluginToken());
        if (pluginMd == null) {
            throw new IncorrectParameterException("Rule points to non-existent plugin!");
        }
        if (!pluginMd.getTenantId().equals(systemTenantId) && !pluginMd.getTenantId().equals(rule.getTenantId())) {
            throw new IncorrectParameterException("Rule access plugin that belongs to different tenant!");
        }
        if (rule.getState() == ComponentLifecycleState.ACTIVE && pluginMd.getState() != ComponentLifecycleState.ACTIVE) {
            throw new IncorrectParameterException("Can't save active rule that points to inactive plugin!");
        }
        ComponentDescriptor pluginDescriptor = componentDescriptorService.findByClazz(pluginMd.getClazz());
        String actionClazz = getIfValid(ComponentType.ACTION.name(), rule.getAction(), "clazz", JsonNode::isTextual, JsonNode::asText);
        if (!Arrays.asList(pluginDescriptor.getActions().split(",")).contains(actionClazz)) {
            throw new IncorrectParameterException("Rule's action is not supported by plugin with token " + rule.getPluginToken() + "!");
        }
    }

    private static <T> T getIfValid(String parentName, JsonNode node, String name, Function<JsonNode, Boolean> validator, Function<JsonNode, T> extractor) {
        if (!node.has(name)) {
            throw new IncorrectParameterException(parentName + "'s " + name + " is not set!");
        } else {
            JsonNode value = node.get(name);
            if (validator.apply(value)) {
                return extractor.apply(value);
            } else {
                throw new IncorrectParameterException(parentName + "'s " + name + " is not valid!");
            }
        }
    }

    @Override
    public RuleMetaData findRuleById(RuleId ruleId) {
        validateId(ruleId, "Incorrect rule id for search rule request.");
        return ruleDao.findById(ruleId.getId());
    }

    @Override
    public ListenableFuture<RuleMetaData> findRuleByIdAsync(RuleId ruleId) {
        validateId(ruleId, "Incorrect rule id for search rule request.");
        return ruleDao.findByIdAsync(ruleId.getId());
    }

    @Override
    public List<RuleMetaData> findPluginRules(String pluginToken) {
        return ruleDao.findRulesByPlugin(pluginToken);
    }

    @Override
    public TextPageData<RuleMetaData> findSystemRules(TextPageLink pageLink) {
        validatePageLink(pageLink, "Incorrect PageLink object for search rule request.");
        List<RuleMetaData> rules = ruleDao.findByTenantIdAndPageLink(systemTenantId, pageLink);
        return new TextPageData<>(rules, pageLink);
    }

    @Override
    public TextPageData<RuleMetaData> findTenantRules(TenantId tenantId, TextPageLink pageLink) {
        validateId(tenantId, "Incorrect tenant id for search rule request.");
        validatePageLink(pageLink, "Incorrect PageLink object for search rule request.");
        List<RuleMetaData> rules = ruleDao.findByTenantIdAndPageLink(tenantId, pageLink);
        return new TextPageData<>(rules, pageLink);
    }

    @Override
    public List<RuleMetaData> findSystemRules() {
        log.trace("Executing findSystemRules");
        List<RuleMetaData> rules = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(300);
        TextPageData<RuleMetaData> pageData = null;
        do {
            pageData = findSystemRules(pageLink);
            rules.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        return rules;
    }

    @Override
    public TextPageData<RuleMetaData> findAllTenantRulesByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findAllTenantRulesByTenantIdAndPageLink, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<RuleMetaData> rules = ruleDao.findAllTenantRulesByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(rules, pageLink);
    }

    @Override
    public List<RuleMetaData> findAllTenantRulesByTenantId(TenantId tenantId) {
        log.trace("Executing findAllTenantRulesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        List<RuleMetaData> rules = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(300);
        TextPageData<RuleMetaData> pageData = null;
        do {
            pageData = findAllTenantRulesByTenantIdAndPageLink(tenantId, pageLink);
            rules.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        return rules;
    }

    @Override
    public void deleteRuleById(RuleId ruleId) {
        validateId(ruleId, "Incorrect rule id for delete rule request.");
        deleteEntityRelations(ruleId);
        ruleDao.deleteById(ruleId);
    }

    @Override
    public void activateRuleById(RuleId ruleId) {
        updateLifeCycleState(ruleId, ComponentLifecycleState.ACTIVE);
    }

    @Override
    public void suspendRuleById(RuleId ruleId) {
        updateLifeCycleState(ruleId, ComponentLifecycleState.SUSPENDED);
    }

    private void updateLifeCycleState(RuleId ruleId, ComponentLifecycleState state) {
        Validator.validateId(ruleId, "Incorrect rule id for state change request.");
        RuleMetaData rule = ruleDao.findById(ruleId);
        if (rule != null) {
            rule.setState(state);
            validateRuleAndPluginState(rule);
            ruleDao.save(rule);
        } else {
            throw new DatabaseException("Plugin not found!");
        }
    }

    @Override
    public void deleteRulesByTenantId(TenantId tenantId) {
        validateId(tenantId, "Incorrect tenant id for delete rules request.");
        tenantRulesRemover.removeEntities(tenantId);
    }

    private DataValidator<RuleMetaData> ruleValidator =
            new DataValidator<RuleMetaData>() {
                @Override
                protected void validateDataImpl(RuleMetaData rule) {
                    if (StringUtils.isEmpty(rule.getName())) {
                        throw new DataValidationException("Rule name should be specified!.");
                    }
                }
            };

    private PaginatedRemover<TenantId, RuleMetaData> tenantRulesRemover =
            new PaginatedRemover<TenantId, RuleMetaData>() {

                @Override
                protected List<RuleMetaData> findEntities(TenantId id, TextPageLink pageLink) {
                    return ruleDao.findByTenantIdAndPageLink(id, pageLink);
                }

                @Override
                protected void removeEntity(RuleMetaData entity) {
                    ruleDao.deleteById(entity.getUuidId());
                }
            };

}
