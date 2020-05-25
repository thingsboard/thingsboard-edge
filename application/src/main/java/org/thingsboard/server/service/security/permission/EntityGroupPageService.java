package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public interface EntityGroupPageService {
    <E extends SearchTextBased<? extends UUIDBased>, I extends EntityId> TextPageData<E>
    getGroupEntitiesByPageLink(TenantId tenantId, SecurityUser securityUser, EntityType entityType, Operation operation,
                               Function<EntityId, I> toIdFunction, Function<List<I>, List<E>> toEntitiesFunction,
                               List<Predicate<E>> entityFilters, List<I> additionalEntityIds, TextPageLink pageLink) throws Exception;

    <E extends SearchTextBased<? extends UUIDBased>, I extends EntityId> TextPageData<E>
    loadAndFilterEntities(List<I> entityIds, Function<List<I>, List<E>> toEntitiesFunction, List<Predicate<E>> entityFilters, TextPageLink pageLink);

    Comparator<SearchTextBased<? extends UUIDBased>> entityComparator();
    Predicate<SearchTextBased<? extends UUIDBased>> entityPageLinkFilter(TextPageLink pageLink);


}
