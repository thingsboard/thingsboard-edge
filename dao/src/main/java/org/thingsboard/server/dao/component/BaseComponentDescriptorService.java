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
package org.thingsboard.server.dao.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;

import java.util.List;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Service
@Slf4j
public class BaseComponentDescriptorService implements ComponentDescriptorService {

    @Autowired
    private ComponentDescriptorDao componentDescriptorDao;

    @Override
    public ComponentDescriptor saveComponent(ComponentDescriptor component) {
        componentValidator.validate(component);
        Optional<ComponentDescriptor> result = componentDescriptorDao.saveIfNotExist(component);
        if (result.isPresent()) {
            return result.get();
        } else {
            return componentDescriptorDao.findByClazz(component.getClazz());
        }
    }

    @Override
    public ComponentDescriptor findById(ComponentDescriptorId componentId) {
        Validator.validateId(componentId, "Incorrect component id for search request.");
        return componentDescriptorDao.findById(componentId);
    }

    @Override
    public ComponentDescriptor findByClazz(String clazz) {
        Validator.validateString(clazz, "Incorrect clazz for search request.");
        return componentDescriptorDao.findByClazz(clazz);
    }

    @Override
    public TextPageData<ComponentDescriptor> findByTypeAndPageLink(ComponentType type, TextPageLink pageLink) {
        Validator.validatePageLink(pageLink, "Incorrect PageLink object for search plugin components request.");
        List<ComponentDescriptor> components = componentDescriptorDao.findByTypeAndPageLink(type, pageLink);
        return new TextPageData<>(components, pageLink);
    }

    @Override
    public TextPageData<ComponentDescriptor> findByScopeAndTypeAndPageLink(ComponentScope scope, ComponentType type, TextPageLink pageLink) {
        Validator.validatePageLink(pageLink, "Incorrect PageLink object for search plugin components request.");
        List<ComponentDescriptor> components = componentDescriptorDao.findByScopeAndTypeAndPageLink(scope, type, pageLink);
        return new TextPageData<>(components, pageLink);
    }

    @Override
    public void deleteByClazz(String clazz) {
        Validator.validateString(clazz, "Incorrect clazz for delete request.");
        componentDescriptorDao.deleteByClazz(clazz);
    }

    @Override
    public boolean validate(ComponentDescriptor component, JsonNode configuration) {
        JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();
        try {
            if (!component.getConfigurationDescriptor().has("schema")) {
                throw new DataValidationException("Configuration descriptor doesn't contain schema property!");
            }
            JsonNode configurationSchema = component.getConfigurationDescriptor().get("schema");
            ProcessingReport report = validator.validate(configurationSchema, configuration);
            return report.isSuccess();
        } catch (ProcessingException e) {
            throw new IncorrectParameterException(e.getMessage(), e);
        }
    }

    private DataValidator<ComponentDescriptor> componentValidator =
            new DataValidator<ComponentDescriptor>() {
                @Override
                protected void validateDataImpl(ComponentDescriptor plugin) {
                    if (plugin.getType() == null) {
                        throw new DataValidationException("Component type should be specified!.");
                    }
                    if (plugin.getScope() == null) {
                        throw new DataValidationException("Component scope should be specified!.");
                    }
                    if (StringUtils.isEmpty(plugin.getName())) {
                        throw new DataValidationException("Component name should be specified!.");
                    }
                    if (StringUtils.isEmpty(plugin.getClazz())) {
                        throw new DataValidationException("Component clazz should be specified!.");
                    }
                }
            };
}
