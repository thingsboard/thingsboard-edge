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
package org.thingsboard.server.service.converter.js;

import com.eclipsesource.v8.*;
import com.eclipsesource.v8.utils.V8ObjectUtils;
import org.thingsboard.server.service.converter.UplinkMetaData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class V8JsConverterEvaluator {

    private final V8 runtime;

    V8JsConverterEvaluator(String script) {
        runtime = V8.createV8Runtime();
        runtime.executeScript(script, "Decoder", 0);
    }

    String execute(byte[] data, UplinkMetaData metadata) {
        V8Array v8array = new V8Array(runtime);
        for (byte b : data) {
            v8array.push(b);
        }
        //V8ArrayBuffer v8byteArray = new V8ArrayBuffer(runtime, makeByteBuffer(data));
        V8Object v8metadata = V8ObjectUtils.toV8Object(runtime, metadata.getKvMap());
        V8Object object = null;
        String result = "";
        V8Array parameterArray = new V8Array(runtime);
        parameterArray.push(v8array);
        parameterArray.push(v8metadata);
        try {
            object = runtime.executeObjectFunction("Decoder", parameterArray);
            if (object != null) {
                result = stringify(object);
            }
        } finally {
            parameterArray.release();
            v8array.release();
            v8metadata.release();
            if (object != null) {
                object.release();
            }
        }
        return result;
    }

    String stringify(V8Object object) {
        V8Object json = runtime.getObject("JSON");
        V8Array parameters = new V8Array(runtime).push(object);
        try {
            return json.executeStringFunction("stringify", parameters);
        } finally {
            parameters.release();
            json.release();
        }
    }

    private static ByteBuffer makeByteBuffer(byte[] arr) {
        ByteBuffer bb = ByteBuffer.allocateDirect(arr.length);
        bb.order(ByteOrder.nativeOrder());
        bb.put(arr);
        //bb.position(0);
        return bb;
    }

    public void destroy() {
        runtime.release();
    }

}
