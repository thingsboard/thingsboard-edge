///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import { TbEditorCompleter, TbEditorCompletions } from '@shared/models/ace/completion.models';
import { widgetContextCompletions } from '@shared/models/ace/widget-completion.models';
import { serviceCompletions } from '@shared/models/ace/service-completion.models';

const widgetEditorCompletions: TbEditorCompletions = {
  ... {self: {
    description: 'Built-in variable <b>self</b> that is a reference to the widget instance',
    type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L350">WidgetTypeInstance</a>',
    meta: 'object',
    children: {
      ...{
        onInit: {
          description: 'The first function which is called when widget is ready for initialization.<br>Should be used to prepare widget DOM, process widget settings and initial subscription information.',
          meta: 'function'
        },
        onDataUpdated: {
          description: 'Called when the new data is available from the widget subscription.<br>Latest data can be accessed from ' +
            'the <code>defaultSubscription</code> property of <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L83">widget context (<code>ctx</code>)</a>.',
          meta: 'function'
        },
        onResize: {
          description: 'Called when widget container is resized. Latest <code>width</code> and <code>height</code> can be obtained from <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L83">widget context (<code>ctx</code>)</a>.',
          meta: 'function'
        },
        onEditModeChanged: {
          description: 'Called when dashboard editing mode is changed. Latest mode is handled by <code>isEdit</code> property of <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L83">widget context (<code>ctx</code>)</a>.',
          meta: 'function'
        },
        onMobileModeChanged: {
          description: 'Called when dashboard view width crosses mobile breakpoint. Latest state is handled by <code>isMobile</code> property of <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/modules/home/models/widget-component.models.ts#L83">widget context (<code>ctx</code>)</a>.',
          meta: 'function'
        },
        onDestroy: {
          description: 'Called when widget element is destroyed. Should be used to cleanup all resources if necessary.',
          meta: 'function'
        },
        getSettingsSchema: {
          description: 'Optional function returning widget settings schema json as alternative to <b>Settings tab</b> of <a href="https://thingsboard.io/docs/user-guide/contribution/widgets-development/#settings-schema-section">Settings schema section</a>.',
          meta: 'function',
          return: {
            description: 'An widget settings schema json',
            type: 'object'
          }
        },
        getDataKeySettingsSchema: {
          description: 'Optional function returning particular data key settings schema json as alternative to <b>Data key settings schema</b> of <a href="https://thingsboard.io/docs/user-guide/contribution/widgets-development/#settings-schema-section">Settings schema section</a>.',
          meta: 'function',
          return: {
            description: 'A particular data key settings schema json',
            type: 'object'
          }
        },
        typeParameters: {
          description: 'Returns object describing widget datasource parameters.',
          meta: 'function',
          return: {
            description: 'An object describing widget datasource parameters.',
            type: '<a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L146">WidgetTypeParameters</a>'
          }
        },
        actionSources: {
          description: 'Returns map describing available widget action sources used to define user actions.',
          meta: 'function',
          return: {
            description: 'A map of action sources by action source id.',
            type: '{[actionSourceId: string]: <a href="https://github.com/thingsboard/thingsboard/blob/13e6b10b7ab830e64d31b99614a9d95a1a25928a/ui-ngx/src/app/shared/models/widget.models.ts#L118">WidgetActionSource</a>}'
          }
        }
      },
      ...widgetContextCompletions
    }
  }},
  ...widgetContextCompletions,
  ...serviceCompletions
};

export const widgetEditorCompleter = new TbEditorCompleter(widgetEditorCompletions);
