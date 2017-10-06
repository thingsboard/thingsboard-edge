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
package org.thingsboard.server.dao.relation;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;

import java.util.List;

/**
 * Created by ashvayka on 27.04.17.
 */
public interface RelationService {

    ListenableFuture<Boolean> checkRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<EntityRelation> getRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    boolean saveRelation(EntityRelation relation);

    ListenableFuture<Boolean> saveRelationAsync(EntityRelation relation);

    boolean deleteRelation(EntityRelation relation);

    ListenableFuture<Boolean> deleteRelationAsync(EntityRelation relation);

    boolean deleteRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<Boolean> deleteRelationAsync(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    boolean deleteEntityRelations(EntityId entity);

    ListenableFuture<Boolean> deleteEntityRelationsAsync(EntityId entity);

    ListenableFuture<List<EntityRelation>> findByFrom(EntityId from, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelationInfo>> findInfoByFrom(EntityId from, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelation>> findByFromAndType(EntityId from, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelation>> findByTo(EntityId to, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelationInfo>> findInfoByTo(EntityId to, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelation>> findByToAndType(EntityId to, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelation>> findByQuery(EntityRelationsQuery query);

    ListenableFuture<List<EntityRelationInfo>> findInfoByQuery(EntityRelationsQuery query);

//    TODO: This method may be useful for some validations in the future
//    ListenableFuture<Boolean> checkRecursiveRelation(EntityId from, EntityId to);

}
