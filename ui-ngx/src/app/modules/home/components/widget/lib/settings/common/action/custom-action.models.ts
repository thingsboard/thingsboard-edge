///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import { entityIdHref, entityTypeHref, serviceCompletions } from '@shared/models/ace/service-completion.models';
import { CustomActionDescriptor, WidgetAction } from '@shared/models/widget.models';
import { deepClone, isDefined, isUndefined } from '@core/utils';
import customSampleJs from './custom-sample-js.raw';
import customSampleCss from './custom-sample-css.raw';
import customSampleHtml from './custom-sample-html.raw';

const customActionCompletions: TbEditorCompletions = {
  ...{
    $event: {
      meta: 'argument',
      type: 'Event',
      description: 'The DOM event that triggered this action.'
    },
    widgetContext: widgetContextCompletions.ctx,
    entityId: {
      meta: 'argument',
      type: entityIdHref,
      description: 'Id of the entity for which the action was triggered.',
      children: {
        id: {
          meta: 'property',
          type: 'string',
          description: 'UUID Id string'
        },
        entityType: {
          meta: 'property',
          type: entityTypeHref,
          description: 'Entity type'
        }
      }
    },
    entityName: {
      meta: 'argument',
      type: 'string',
      description: 'Name of the entity for which the action was triggered.'
    },
    additionalParams: {
      meta: 'argument',
      type: 'object',
      description: 'Optional object holding additional information.'
    },
    entityLabel: {
      meta: 'argument',
      type: 'string',
      description: 'Label of the entity for which the action was triggered.'
    }
  }
};

const customPrettyActionCompletions: TbEditorCompletions = {
  ...{
    htmlTemplate: {
      meta: 'argument',
      type: 'string',
      description: 'HTML template used to render custom dialog.'
    }
  },
  ...customActionCompletions
};

export const toCustomAction = (action: WidgetAction): CustomActionDescriptor => {
  let result: CustomActionDescriptor;
  if (!action || (isUndefined(action.customFunction) && isUndefined(action.customHtml) && isUndefined(action.customCss))) {
    result = {
      customHtml: customSampleHtml,
      customCss: customSampleCss,
      customFunction: customSampleJs
    };
  } else {
    result = {
      customHtml: action.customHtml,
      customCss: action.customCss,
      customFunction: action.customFunction
    };
  }
  result.customResources = action && isDefined(action.customResources) ? deepClone(action.customResources) : [];
  return result;
};

export const CustomActionEditorCompleter = new TbEditorCompleter(customActionCompletions);
export const CustomPrettyActionEditorCompleter = new TbEditorCompleter(customPrettyActionCompletions);
