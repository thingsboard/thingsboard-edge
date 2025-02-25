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
package org.thingsboard.server.common.data.permission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.server.common.data.id.EntityGroupId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MergedGroupTypePermissionInfoTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ofEmptyGroups(final boolean hasGenericRead) {
        assertThat(MergedGroupTypePermissionInfo.ofEmptyGroups(hasGenericRead))
                .isEqualTo(new MergedGroupTypePermissionInfo(List.of(), hasGenericRead));
    }

    @Test
    void testConstants() {
        assertThat(MergedGroupTypePermissionInfo.MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_TRUE)
                .isEqualTo(new MergedGroupTypePermissionInfo(List.of(), true));
        assertThat(MergedGroupTypePermissionInfo.MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_FALSE)
                .isEqualTo(new MergedGroupTypePermissionInfo(List.of(), false));
    }

    @Test
    void testImmutableEntityGroupIdsEmpty() {
        List<EntityGroupId> entityGroupIds = new ArrayList<>();
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(new ArrayList<>(), true);
        assertThat(mgtpi.getEntityGroupIds()).isEmpty();
        assertThat(mgtpi.isHasGenericRead()).isTrue();
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().add(new EntityGroupId(UUID.randomUUID())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testImmutableEntityGroupIds() {
        List<EntityGroupId> entityGroupIds = new ArrayList<>();
        entityGroupIds.add(new EntityGroupId(UUID.randomUUID()));
        entityGroupIds.add(new EntityGroupId(UUID.randomUUID()));
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(entityGroupIds, true);
        assertThat(mgtpi.getEntityGroupIds()).containsExactlyElementsOf(entityGroupIds);
        assertThat(mgtpi.getEntityGroupIds()).isNotSameAs(entityGroupIds);
        assertThat(mgtpi.isHasGenericRead()).isTrue();
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().add(new EntityGroupId(UUID.randomUUID())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().set(0, new EntityGroupId(UUID.randomUUID())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().remove(entityGroupIds.size() - 1))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testImmutableEntityGroupIdsOfImmutableList() {
        List<EntityGroupId> immutableEntityGroupIds = List.of(
                new EntityGroupId(UUID.randomUUID()), new EntityGroupId(UUID.randomUUID()), new EntityGroupId(UUID.randomUUID()));
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(immutableEntityGroupIds, true);
        assertThat(mgtpi.getEntityGroupIds()).containsExactlyElementsOf(immutableEntityGroupIds);
        assertThat(mgtpi.getEntityGroupIds()).isSameAs(immutableEntityGroupIds); // immutables does not convert to another immutables
        assertThat(mgtpi.isHasGenericRead()).isTrue();
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().add(new EntityGroupId(UUID.randomUUID())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().set(0, new EntityGroupId(UUID.randomUUID())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().remove(0))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testImmutableEntityGroupIdsNullable() {
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(null, false);
        assertThat(mgtpi.getEntityGroupIds()).isNull();
        assertThat(mgtpi.isHasGenericRead()).isFalse();
    }

    @Test
    void testAddIdImmutableWithInitialNullIds() {
        EntityGroupId id = new EntityGroupId(UUID.randomUUID());
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(null, true);
        MergedGroupTypePermissionInfo added = mgtpi.addId(id);
        assertThat(added).isNotSameAs(mgtpi);
        assertThat(added.getEntityGroupIds()).containsExactlyElementsOf(List.of(id));
        assertThat(added.isHasGenericRead()).isTrue();
    }

    @Test
    void testAddIdImmutableWithInitialEmptyIds() {
        EntityGroupId id = new EntityGroupId(UUID.randomUUID());
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(Collections.emptyList(), true);
        MergedGroupTypePermissionInfo added = mgtpi.addId(id);
        assertThat(added).isNotSameAs(mgtpi);
        assertThat(added.getEntityGroupIds()).containsExactlyElementsOf(List.of(id));
        assertThat(added.isHasGenericRead()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAddIdImmutableWithInitialExistedIds(final boolean hasGenericRead) {
        final List<EntityGroupId> ids = List.of(new EntityGroupId(UUID.randomUUID()), new EntityGroupId(UUID.randomUUID()));
        final EntityGroupId id = new EntityGroupId(UUID.randomUUID());
        final List<EntityGroupId> allIds = new ArrayList<>(ids);
        allIds.add(id);

        final MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(ids, hasGenericRead);
        final MergedGroupTypePermissionInfo added = mgtpi.addId(id);

        assertThat(added).isNotSameAs(mgtpi);
        assertThat(added.getEntityGroupIds()).containsExactlyElementsOf(allIds);
        assertThat(added.isHasGenericRead()).isEqualTo(hasGenericRead);
    }

    @Test
    void testAddIdsImmutableWithInitialEmptyIds() {
        EntityGroupId id = new EntityGroupId(UUID.randomUUID());
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(Collections.emptyList(), true);
        MergedGroupTypePermissionInfo added = mgtpi.addIds(List.of(id));
        assertThat(added).isNotSameAs(mgtpi);
        assertThat(added.getEntityGroupIds()).containsExactlyElementsOf(List.of(id));
        assertThat(added.isHasGenericRead()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAddIdsImmutableWithInitialExistedIds(final boolean hasGenericRead) {
        final List<EntityGroupId> ids = List.of(new EntityGroupId(UUID.randomUUID()), new EntityGroupId(UUID.randomUUID()));
        final EntityGroupId id = new EntityGroupId(UUID.randomUUID());
        final List<EntityGroupId> allIds = new ArrayList<>(ids);
        allIds.add(id);

        final MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(ids, hasGenericRead);
        final MergedGroupTypePermissionInfo added = mgtpi.addIds(List.of(id));

        assertThat(added).isNotSameAs(mgtpi);
        assertThat(added.getEntityGroupIds()).containsExactlyElementsOf(allIds);
        assertThat(added.isHasGenericRead()).isEqualTo(hasGenericRead);
    }

}
