package org.thingsboard.server.service.security.permission;

import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class DefaultEntityGroupPageService implements EntityGroupPageService {
    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    @Autowired
    private OwnersCacheService ownersCacheService;

    @Override
    public <E extends SearchTextBased<? extends UUIDBased>, I extends EntityId> TextPageData<E>
    getGroupEntitiesByPageLink(TenantId tenantId, SecurityUser securityUser, EntityType entityType, Operation operation,
                               Function<EntityId, I> toIdFunction, Function<List<I>, List<E>> toEntitiesFunction,
                               List<Predicate<E>> entityFilters, List<I> additionalEntityIds, TextPageLink pageLink)
            throws Exception {
        Resource resource = Resource.resourceFromEntityType(entityType);
        if (securityUser.getAuthority() == Authority.TENANT_ADMIN &&
                securityUser.getUserPermissions().hasGenericPermission(resource, operation)) {
            switch (entityType) {
                case DEVICE:
                    return (TextPageData<E>) deviceService.findDevicesByTenantId(tenantId, pageLink);
                case ASSET:
                    return (TextPageData<E>) assetService.findAssetsByTenantId(tenantId, pageLink);
                case CUSTOMER:
                    return (TextPageData<E>) customerService.findCustomersByTenantId(tenantId, pageLink);
                case USER:
                    return (TextPageData<E>) userService.findUsersByTenantId(tenantId, pageLink);
                case DASHBOARD:
                    return (TextPageData<E>) dashboardService.findDashboardsByTenantId(tenantId, pageLink);
                case ENTITY_VIEW:
                    return (TextPageData<E>) entityViewService.findEntityViewByTenantId(tenantId, pageLink);
                default:
                    throw new RuntimeException("EntityType does not supported: " + entityType);
            }
        } else {
            List<I> entityIds = getEntityIdsFromAllowedGroups(tenantId, securityUser, entityType, operation, toIdFunction);
            entityIds.addAll(additionalEntityIds);

            return loadAndFilterEntities(entityIds, toEntitiesFunction, entityFilters, pageLink);
        }
    }

    @Override
    public <E extends SearchTextBased<? extends UUIDBased>, I extends EntityId> TextPageData<E>
    loadAndFilterEntities(List<I> entityIds, Function<List<I>, List<E>> toEntitiesFunction, List<Predicate<E>> entityFilters,
                          TextPageLink pageLink) {
        List<E> entities;
        if (entityIds.isEmpty()) {
            entities = Collections.emptyList();
        } else {
            entities = toEntitiesFunction.apply(entityIds);
        }
        Stream<E> entitiesStream = entities.stream().sorted(entityComparator());
        for (Predicate<E> entityFilter : entityFilters) {
            entitiesStream = entitiesStream.filter(entityFilter);
        }
        entities = entitiesStream.filter(entityPageLinkFilter(pageLink)).collect(Collectors.toList());
        if (pageLink.getLimit() > 0 && entities.size() > pageLink.getLimit()) {
            int toRemove = entities.size() - pageLink.getLimit();
            entities.subList(entities.size() - toRemove, entities.size()).clear();
        }
        return new TextPageData<>(entities, pageLink);
    }

    @Override
    public Comparator<SearchTextBased<? extends UUIDBased>> entityComparator(){
        return entityComparator;
    }

    @Override
    public Predicate<SearchTextBased<? extends UUIDBased>> entityPageLinkFilter(TextPageLink pageLink) {
        return new EntityPageLinkFilter(pageLink);
    }

    private final Comparator<SearchTextBased<? extends UUIDBased>> entityComparator = (e1, e2) -> {
        int result = e1.getSearchText().compareToIgnoreCase(e2.getSearchText());
        if (result == 0) {
            result = (int)(e2.getCreatedTime() - e1.getCreatedTime());
        }
        return result;
    };

    private static class EntityPageLinkFilter implements Predicate<SearchTextBased<? extends UUIDBased>> {

        private final String textSearch;
        private final String textOffset;
        private final long createdTimeOffset;

        EntityPageLinkFilter(TextPageLink pageLink) {
            if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
                this.textSearch = pageLink.getTextSearch().toLowerCase();
            } else {
                this.textSearch = "";
            }
            if (!StringUtils.isEmpty(pageLink.getTextOffset())) {
                this.textOffset = pageLink.getTextOffset();
            } else {
                this.textOffset = "";
            }
            if (pageLink.getIdOffset() != null) {
                createdTimeOffset = UUIDs.unixTimestamp(pageLink.getIdOffset());
            } else {
                createdTimeOffset = Long.MAX_VALUE;
            }
        }

        @Override
        public boolean test(SearchTextBased<? extends UUIDBased> searchTextBased) {
            if (textOffset.length() > 0) {
                int result = searchTextBased.getSearchText().compareToIgnoreCase(textOffset);
                if (result == 0 && searchTextBased.getCreatedTime() < createdTimeOffset) {
                    return true;
                } else if (result > 0 && searchTextBased.getSearchText().toLowerCase().startsWith(textSearch)) {
                    return true;
                }
            } else if (textSearch.length() > 0) {
                return searchTextBased.getSearchText().toLowerCase().startsWith(textSearch);
            } else {
                return true;
            }
            return false;
        }
    }

    private  <I extends EntityId> List<I> getEntityIdsFromAllowedGroups(TenantId tenantId,
                                                                        SecurityUser securityUser,
                                                                        EntityType entityType,
                                                                        Operation operation,
                                                                        Function<EntityId, I> toIdFunction) throws Exception {
        MergedGroupTypePermissionInfo groupTypePermissionInfo = null;
        if (operation == Operation.READ) {
            groupTypePermissionInfo = securityUser.getUserPermissions().getReadGroupPermissions().get(entityType);
        }
        Resource resource = Resource.resourceFromEntityType(entityType);
        if (securityUser.getUserPermissions().hasGenericPermission(resource, operation) ||
                (groupTypePermissionInfo != null && !groupTypePermissionInfo.getEntityGroupIds().isEmpty())) {

            Set<EntityId> entityIds = new HashSet<>();
            Set<EntityGroupId> groupIds = new HashSet<>();
            if (securityUser.getUserPermissions().hasGenericPermission(resource, operation)) {
                Set<EntityId> ownerIds = ownersCacheService.getChildOwners(tenantId, securityUser.getOwnerId());
                for (EntityId ownerId : ownerIds) {
                    Optional<EntityGroup> entityGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId, ownerId,
                            entityType, EntityGroup.GROUP_ALL_NAME).get();
                    if (entityGroup.isPresent()) {
                        groupIds.add(entityGroup.get().getId());
                    }
                }
            }
            if (groupTypePermissionInfo != null && !groupTypePermissionInfo.getEntityGroupIds().isEmpty()) {
                groupIds.addAll(groupTypePermissionInfo.getEntityGroupIds());
            }
            for (EntityGroupId groupId : groupIds) {
                entityIds.addAll(entityGroupService.findAllEntityIds(tenantId, groupId, new TimePageLink(Integer.MAX_VALUE)).get());
            }
            if (!entityIds.isEmpty()) {
                List<I> entityIdsList = new ArrayList<>();
                entityIds.forEach((entityId) -> entityIdsList.add(toIdFunction.apply(entityId)));
                return entityIdsList;
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

}
