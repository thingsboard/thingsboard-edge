///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import { JsonSettingsSchema } from '@shared/models/widget.models';

export function initSchema(): JsonSettingsSchema {
    return {
        schema: {
            type: 'object',
            properties: {},
            required: []
        },
        form: [],
        groupInfoes: []
    };
}

export function addGroupInfo(schema: JsonSettingsSchema, title: string) {
    schema.groupInfoes.push({
        formIndex: schema.groupInfoes?.length || 0,
        GroupTitle: title
    });
}

export function addToSchema(schema: JsonSettingsSchema, newSchema: JsonSettingsSchema) {
    Object.assign(schema.schema.properties, newSchema.schema.properties);
    schema.schema.required = schema.schema.required.concat(newSchema.schema.required);
    schema.form.push(newSchema.form);
}

export function mergeSchemes(schemes: JsonSettingsSchema[]): JsonSettingsSchema {
    return schemes.reduce((finalSchema: JsonSettingsSchema, schema: JsonSettingsSchema) => {
        return {
            schema: {
                properties: {
                    ...finalSchema.schema.properties,
                    ...schema.schema.properties
                },
                required: [
                    ...finalSchema.schema.required,
                    ...schema.schema.required
                ]
            },
            form: [
                ...finalSchema.form,
                ...schema.form
            ]
        } as JsonSettingsSchema;
    }, initSchema());
}

export function addCondition(schema: JsonSettingsSchema, condition: string): JsonSettingsSchema {
    schema.form = schema.form.map(element => {
        if (typeof element === 'string') {
            return {
                key: element,
                condition
            }
        }
        if (typeof element === 'object') {
            if (element.condition) {
                element.condition += ' && ' + condition
            }
            else element.condition = condition;
        }
        return element;
    });
    return schema;
}
