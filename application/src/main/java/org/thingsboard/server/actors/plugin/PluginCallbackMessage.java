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
package org.thingsboard.server.actors.plugin;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@ToString
public final class PluginCallbackMessage<V> {
    @Getter
    private final PluginCallback<V> callback;
    @Getter
    private final boolean success;
    @Getter
    private final V v;
    @Getter
    private final Exception e;

    public static <V> PluginCallbackMessage<V> onSuccess(PluginCallback<V> callback, V data) {
        return new PluginCallbackMessage<V>(true, callback, data, null);
    }

    public static <V> PluginCallbackMessage<V> onError(PluginCallback<V> callback, Exception e) {
        return new PluginCallbackMessage<V>(false, callback, null, e);
    }

    private PluginCallbackMessage(boolean success, PluginCallback<V> callback, V v, Exception e) {
        this.success = success;
        this.callback = callback;
        this.v = v;
        this.e = e;
    }
}
