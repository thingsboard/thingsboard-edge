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
package org.thingsboard.server.service.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.api.NodeDefinition;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.component.ComponentDescriptorService;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class AnnotationComponentDiscoveryService implements ComponentDiscoveryService {

    public static final int MAX_OPTIMISITC_RETRIES = 3;
    @Value("${plugins.scan_packages}")
    private String[] scanPackages;

    @Autowired
    private Environment environment;

    @Autowired
    private ComponentDescriptorService componentDescriptorService;

    private Map<String, ComponentDescriptor> components = new HashMap<>();

    private Map<ComponentType, List<ComponentDescriptor>> coreComponentsMap = new HashMap<>();

    private Map<ComponentType, List<ComponentDescriptor>> edgeComponentsMap = new HashMap<>();

    private ObjectMapper mapper = new ObjectMapper();

    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    @PostConstruct
    public void init() {
        if (!isInstall()) {
            discoverComponents();
        }
    }

    private void registerRuleNodeComponents() {
        Set<BeanDefinition> ruleNodeBeanDefinitions = getBeanDefinitions(RuleNode.class);
        for (BeanDefinition def : ruleNodeBeanDefinitions) {
            int retryCount = 0;
            Exception cause = null;
            while (retryCount < MAX_OPTIMISITC_RETRIES) {
                try {
                    String clazzName = def.getBeanClassName();
                    Class<?> clazz = Class.forName(clazzName);
                    RuleNode ruleNodeAnnotation = clazz.getAnnotation(RuleNode.class);
                    ComponentType type = ruleNodeAnnotation.type();
                    ComponentDescriptor component = scanAndPersistComponent(def, type);
                    components.put(component.getClazz(), component);
                    putComponentIntoMaps(type, ruleNodeAnnotation, component);
                    break;
                } catch (Exception e) {
                    log.trace("Can't initialize component {}, due to {}", def.getBeanClassName(), e.getMessage(), e);
                    cause = e;
                    retryCount++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
            if (cause != null && retryCount == MAX_OPTIMISITC_RETRIES) {
                log.error("Can't initialize component {}, due to {}", def.getBeanClassName(), cause.getMessage(), cause);
                throw new RuntimeException(cause);
            }
        }
    }

    private void putComponentIntoMaps(ComponentType type, RuleNode ruleNodeAnnotation, ComponentDescriptor component) {
        boolean ruleChainTypesMethodAvailable;
        try {
            ruleNodeAnnotation.getClass().getMethod("ruleChainTypes");
            ruleChainTypesMethodAvailable = true;
        } catch (NoSuchMethodException exception) {
            log.warn("[{}] does not have ruleChainTypes. Probably extension class compiled before 3.3 release. " +
                    "Please update your extensions and compile using latest 3.3 release dependency", ruleNodeAnnotation.name());
            ruleChainTypesMethodAvailable = false;
        }
        if (ruleChainTypesMethodAvailable) {
            if (ruleChainTypeContainsArray(RuleChainType.CORE, ruleNodeAnnotation.ruleChainTypes())) {
                coreComponentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
            }
            if (ruleChainTypeContainsArray(RuleChainType.EDGE, ruleNodeAnnotation.ruleChainTypes())) {
                edgeComponentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
            }
        } else {
            coreComponentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
        }
    }

    private boolean ruleChainTypeContainsArray(RuleChainType ruleChainType, RuleChainType[] array) {
        for (RuleChainType tmp : array) {
            if (ruleChainType.equals(tmp)) {
                return true;
            }
        }
        return false;
    }

    private ComponentDescriptor scanAndPersistComponent(BeanDefinition def, ComponentType type) {
        ComponentDescriptor scannedComponent = new ComponentDescriptor();
        String clazzName = def.getBeanClassName();
        try {
            scannedComponent.setType(type);
            Class<?> clazz = Class.forName(clazzName);
            RuleNode ruleNodeAnnotation = clazz.getAnnotation(RuleNode.class);
            scannedComponent.setName(ruleNodeAnnotation.name());
            scannedComponent.setScope(ruleNodeAnnotation.scope());
            NodeDefinition nodeDefinition = prepareNodeDefinition(ruleNodeAnnotation);
            ObjectNode configurationDescriptor = mapper.createObjectNode();
            JsonNode node = mapper.valueToTree(nodeDefinition);
            configurationDescriptor.set("nodeDefinition", node);
            scannedComponent.setConfigurationDescriptor(configurationDescriptor);
            scannedComponent.setClazz(clazzName);
            log.debug("Processing scanned component: {}", scannedComponent);
        } catch (Exception e) {
            log.error("Can't initialize component {}, due to {}", def.getBeanClassName(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
        ComponentDescriptor persistedComponent = componentDescriptorService.findByClazz(TenantId.SYS_TENANT_ID, clazzName);
        if (persistedComponent == null) {
            log.debug("Persisting new component: {}", scannedComponent);
            scannedComponent = componentDescriptorService.saveComponent(TenantId.SYS_TENANT_ID, scannedComponent);
        } else if (scannedComponent.equals(persistedComponent)) {
            log.debug("Component is already persisted: {}", persistedComponent);
            scannedComponent = persistedComponent;
        } else {
            log.debug("Component {} will be updated to {}", persistedComponent, scannedComponent);
            componentDescriptorService.deleteByClazz(TenantId.SYS_TENANT_ID, persistedComponent.getClazz());
            scannedComponent.setId(persistedComponent.getId());
            scannedComponent = componentDescriptorService.saveComponent(TenantId.SYS_TENANT_ID, scannedComponent);
        }
        return scannedComponent;
    }

    private NodeDefinition prepareNodeDefinition(RuleNode nodeAnnotation) throws Exception {
        NodeDefinition nodeDefinition = new NodeDefinition();
        nodeDefinition.setDetails(nodeAnnotation.nodeDetails());
        nodeDefinition.setDescription(nodeAnnotation.nodeDescription());
        nodeDefinition.setInEnabled(nodeAnnotation.inEnabled());
        nodeDefinition.setOutEnabled(nodeAnnotation.outEnabled());
        nodeDefinition.setRelationTypes(getRelationTypesWithFailureRelation(nodeAnnotation));
        nodeDefinition.setCustomRelations(nodeAnnotation.customRelations());
        nodeDefinition.setRuleChainNode(nodeAnnotation.ruleChainNode());
        Class<? extends NodeConfiguration> configClazz = nodeAnnotation.configClazz();
        NodeConfiguration config = configClazz.getDeclaredConstructor().newInstance();
        NodeConfiguration defaultConfiguration = config.defaultConfiguration();
        nodeDefinition.setDefaultConfiguration(mapper.valueToTree(defaultConfiguration));
        nodeDefinition.setUiResources(nodeAnnotation.uiResources());
        nodeDefinition.setConfigDirective(nodeAnnotation.configDirective());
        nodeDefinition.setIcon(nodeAnnotation.icon());
        nodeDefinition.setIconUrl(nodeAnnotation.iconUrl());
        nodeDefinition.setDocUrl(nodeAnnotation.docUrl());
        return nodeDefinition;
    }

    private String[] getRelationTypesWithFailureRelation(RuleNode nodeAnnotation) {
        List<String> relationTypes = new ArrayList<>(Arrays.asList(nodeAnnotation.relationTypes()));
        if (!relationTypes.contains(TbRelationTypes.FAILURE)) {
            relationTypes.add(TbRelationTypes.FAILURE);
        }
        return relationTypes.toArray(new String[relationTypes.size()]);
    }

    private Set<BeanDefinition> getBeanDefinitions(Class<? extends Annotation> componentType) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(componentType));
        Set<BeanDefinition> defs = new HashSet<>();
        for (String scanPackage : scanPackages) {
            defs.addAll(scanner.findCandidateComponents(scanPackage));
        }
        return defs;
    }

    @Override
    public void discoverComponents() {
        registerRuleNodeComponents();
        log.debug("Found following definitions: {}", components.values());
    }

    @Override
    public List<ComponentDescriptor> getComponents(ComponentType type, RuleChainType ruleChainType) {
        if (RuleChainType.CORE.equals(ruleChainType)) {
            if (coreComponentsMap.containsKey(type)) {
                return Collections.unmodifiableList(coreComponentsMap.get(type));
            } else {
                return Collections.emptyList();
            }
        } else if (RuleChainType.EDGE.equals(ruleChainType)) {
            if (edgeComponentsMap.containsKey(type)) {
                return Collections.unmodifiableList(edgeComponentsMap.get(type));
            } else {
                return Collections.emptyList();
            }
        } else {
            log.error("Unsupported rule chain type {}", ruleChainType);
            throw new RuntimeException("Unsupported rule chain type " + ruleChainType);
        }
    }

    @Override
    public List<ComponentDescriptor> getComponents(Set<ComponentType> types, RuleChainType ruleChainType) {
        if (RuleChainType.CORE.equals(ruleChainType)) {
            return getComponents(types, coreComponentsMap);
        } else if (RuleChainType.EDGE.equals(ruleChainType)) {
            return getComponents(types, edgeComponentsMap);
        } else {
            log.error("Unsupported rule chain type {}", ruleChainType);
            throw new RuntimeException("Unsupported rule chain type " + ruleChainType);
        }
    }

    @Override
    public Optional<ComponentDescriptor> getComponent(String clazz) {
        return Optional.ofNullable(components.get(clazz));
    }

    private List<ComponentDescriptor> getComponents(Set<ComponentType> types, Map<ComponentType, List<ComponentDescriptor>> componentsMap) {
        List<ComponentDescriptor> result = new ArrayList<>();
        types.stream().filter(componentsMap::containsKey).forEach(type -> {
            result.addAll(componentsMap.get(type));
        });
        return Collections.unmodifiableList(result);
    }
}
