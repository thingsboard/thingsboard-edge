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

import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { WidgetService } from '@core/http/widget.service';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { AceHighlightRules } from '@shared/models/ace/ace.models';
import {
  scadaSymbolClickActionHighlightRules,
  scadaSymbolRenderFunctionHighlightRules
} from '@home/pages/scada-symbol/scada-symbol-editor.models';
import { JsFuncComponent } from '@shared/components/js-func.component';

@Component({
  selector: 'tb-scada-symbol-metadata-tag-function-panel',
  templateUrl: './scada-symbol-metadata-tag-function-panel.component.html',
  styleUrls: ['./scada-symbol-metadata-tag-function-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolMetadataTagFunctionPanelComponent implements OnInit, AfterViewInit {

  @ViewChild('tagFunctionComponent')
  tagFunctionComponent: JsFuncComponent;

  @Input()
  tagFunction: string;

  @Input()
  tagFunctionType: 'renderFunction' | 'clickAction';

  @Input()
  tag: string;

  @Input()
  completer: TbEditorCompleter;

  @Input()
  disabled: boolean;

  @Input()
  popover: TbPopoverComponent<ScadaSymbolMetadataTagFunctionPanelComponent>;

  @Output()
  tagFunctionApplied = new EventEmitter<string>();

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  tagFunctionFormGroup: UntypedFormGroup;

  panelTitle: string;

  tagFunctionArgs: string[];

  tagFunctionHelpId: string;

  highlightRules: AceHighlightRules;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService) {
  }

  ngOnInit(): void {
    this.tagFunctionFormGroup = this.fb.group(
      {
        tagFunction: [this.tagFunction, []]
      }
    );
    if (this.disabled) {
      this.tagFunctionFormGroup.disable({emitEvent: false});
    }
    if (this.tagFunctionType === 'renderFunction') {
      this.panelTitle = 'scada.state-render-function';
      this.tagFunctionArgs = ['ctx', 'element'];
      this.highlightRules = scadaSymbolRenderFunctionHighlightRules;
      this.tagFunctionHelpId = 'scada/tag_state_render_fn';
    } else if (this.tagFunctionType === 'clickAction') {
      this.panelTitle = 'scada.tag.on-click-action';
      this.tagFunctionArgs = ['ctx', 'element', 'event'];
      this.highlightRules = scadaSymbolClickActionHighlightRules;
      this.tagFunctionHelpId = 'scada/tag_click_action_fn';
    }
  }

  ngAfterViewInit() {
    this.tagFunctionComponent.focus();
  }

  cancel() {
    this.popover?.hide();
  }

  applyTagFunction() {
    const tagFunction: string = this.tagFunctionFormGroup.get('tagFunction').value;
    this.tagFunctionApplied.emit(tagFunction);
  }
}
