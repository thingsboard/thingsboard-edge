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
package org.thingsboard.server.extensions.core.plugin.telemetry.handlers;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.PluginContext;

/**
 * Created by ashvayka on 21.02.17.
 */
@Slf4j
public abstract class BiPluginCallBack<V1, V2> {

    private V1 v1;
    private V2 v2;

    public PluginCallback<V1> getV1Callback() {
        return new PluginCallback<V1>() {
            @Override
            public void onSuccess(PluginContext ctx, V1 value) {
                synchronized (BiPluginCallBack.this) {
                    BiPluginCallBack.this.v1 = value;
                    if (v2 != null) {
                        BiPluginCallBack.this.onSuccess(ctx, v1, v2);
                    }
                }
            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                BiPluginCallBack.this.onFailure(ctx, e);
            }
        };
    }

    public PluginCallback<V2> getV2Callback() {
        return new PluginCallback<V2>() {
            @Override
            public void onSuccess(PluginContext ctx, V2 value) {
                synchronized (BiPluginCallBack.this) {
                    BiPluginCallBack.this.v2 = value;
                    if (v1 != null) {
                        BiPluginCallBack.this.onSuccess(ctx, v1, v2);
                    }
                }

            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                BiPluginCallBack.this.onFailure(ctx, e);
            }
        };
    }

    abstract public void onSuccess(PluginContext ctx, V1 v1, V2 v2);

    abstract public void onFailure(PluginContext ctx, Exception e);

}
