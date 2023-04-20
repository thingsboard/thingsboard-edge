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

import { Directive } from '@angular/core';
import { BREAKPOINT, LayoutAlignDirective, LayoutDirective, LayoutGapDirective, ShowHideDirective } from '@angular/flex-layout';

const TB_BREAKPOINTS = [
  {
    alias: 'md-lg',
    mediaQuery: 'screen and (min-width: 960px) and (max-width: 1819px)',
    priority: 750
  },
  {
    alias: 'gt-md-lg',
    mediaQuery: 'screen and (min-width: 1820px)',
    priority: -600
  }
];

export const TbBreakPointsProvider = {
  provide: BREAKPOINT,
  useValue: TB_BREAKPOINTS,
  multi: true
};

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayout.md-lg]', inputs: ['fxLayout.md-lg']})
export class MdLgLayoutDirective extends LayoutDirective {
  protected inputs = ['fxLayout.md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayoutAlign.md-lg]', inputs: ['fxLayoutAlign.md-lg']})
export class MdLgLayoutAlignDirective extends LayoutAlignDirective {
  protected inputs = ['fxLayoutAlign.md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayoutGap.md-lg]', inputs: ['fxLayoutGap.md-lg']})
export class MdLgLayoutGapDirective extends LayoutGapDirective {
  protected inputs = ['fxLayoutGap.md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxHide.md-lg]', inputs: ['fxHide.md-lg']})
export class MdLgShowHideDirective extends ShowHideDirective {
  protected inputs = ['fxHide.md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayout.gt-md-lg]', inputs: ['fxLayout.gt-md-lg']})
export class GtMdLgLayoutDirective extends LayoutDirective {
  protected inputs = ['fxLayout.gt-md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayoutAlign.gt-md-lg]', inputs: ['fxLayoutAlign.gt-md-lg']})
export class GtMdLgLayoutAlignDirective extends LayoutAlignDirective {
  protected inputs = ['fxLayoutAlign.gt-md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxLayoutGap.gt-md-lg]', inputs: ['fxLayoutGap.gt-md-lg']})
export class GtMdLgLayoutGapDirective extends LayoutGapDirective {
  protected inputs = ['fxLayoutGap.gt-md-lg'];
}

// eslint-disable-next-line @angular-eslint/no-inputs-metadata-property,@angular-eslint/directive-selector
@Directive({selector: '[fxHide.gt-md-lg]', inputs: ['fxHide.gt-md-lg']})
export class GtMdLgShowHideDirective extends ShowHideDirective {
  protected inputs = ['fxHide.gt-md-lg'];
}
