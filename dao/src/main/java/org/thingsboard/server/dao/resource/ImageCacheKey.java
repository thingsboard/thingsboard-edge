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
package org.thingsboard.server.dao.resource;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.transport.TransportProtos.ImageCacheKeyProto;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageCacheKey {

    private final TenantId tenantId;
    private final String resourceKey;
    @With
    private final boolean preview;

    private final String publicResourceKey;

    public static ImageCacheKey forImage(TenantId tenantId, String key, boolean preview) {
        return new ImageCacheKey(tenantId, key, preview, null);
    }

    public static ImageCacheKey forImage(TenantId tenantId, String key) {
        return forImage(tenantId, key, false);
    }

    public static ImageCacheKey forPublicImage(String publicKey) {
        return new ImageCacheKey(null, null, false, publicKey);
    }

    public ImageCacheKeyProto toProto() {
        var msg = ImageCacheKeyProto.newBuilder();
        if (resourceKey != null) {
            msg.setResourceKey(resourceKey);
        } else {
            msg.setPublicResourceKey(publicResourceKey);
        }
        return msg.build();
    }

    public boolean isPublic() {
        return this.publicResourceKey != null;
    }

}
