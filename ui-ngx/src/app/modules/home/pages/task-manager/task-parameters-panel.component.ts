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
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { Job, JobStatus } from '@app/shared/models/job.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Ace } from 'ace-builds';
import { getAce } from '@shared/models/ace/ace.models';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-task-parameters-panel',
  templateUrl: './task-parameters-panel.component.html',
  styleUrls: ['./task-parameters-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TaskParametersPanelComponent implements OnInit, OnDestroy {

  @ViewChild('taskContainer', {static: true})
  taskContainerElmRef: ElementRef;

  @ViewChild('taskPanel', {static: true})
  taskPanelElmRef: ElementRef;

  JobStatus = JobStatus;

  @Input()
  job: Job;

  private aceEditor: Ace.Editor;

  constructor(private popover: TbPopoverComponent<TaskParametersPanelComponent>,
              private renderer: Renderer2) {
  }

  ngOnInit() {
    this.createEditor();
  }

  ngOnDestroy() {
    this.aceEditor?.destroy();
  }

  cancel() {
    this.popover.hide();
  }

  private createEditor() {
    const editorElement = this.taskContainerElmRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: `ace/mode/json`,
      theme: 'ace/theme/github',
      showGutter: false,
      showPrintMargin: false,
      readOnly: true
    };

    const advancedOptions = {
      enableSnippets: false,
      enableBasicAutocompletion: false,
      enableLiveAutocompletion: false
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    getAce().subscribe(
      (ace) => {
        this.aceEditor = ace.edit(editorElement, editorOptions);
        this.aceEditor.session.setUseWrapMode(false);
        const cloneConfig = deepClone(this.job.configuration);
        delete cloneConfig.toReprocess;
        delete cloneConfig.tasksKey;
        delete cloneConfig.type;
        const value = JSON.stringify(cloneConfig, null, 2);
        this.aceEditor.setValue(value, -1);
        this.updateEditorSize(editorElement, value, this.aceEditor);
      }
    );
  }

  private updateEditorSize(editorElement: any, content: string, editor: Ace.Editor) {
    let newHeight = 400;
    let newWidth = 200;
    if (content && content.length > 0) {
      const lines = content.split('\n');
      newHeight = 19 * lines.length + 16;
      let maxLineLength = 0;
      lines.forEach((row) => {
        const line = row.replace(/\t/g, '    ').replace(/\n/g, '');
        const lineLength = line.length;
        maxLineLength = Math.max(maxLineLength, lineLength);
      });
      newWidth = Math.max(10 * maxLineLength + 16, 200);
    }
    this.renderer.setStyle(editorElement, 'height', newHeight.toString() + 'px');
    this.renderer.setStyle(this.taskPanelElmRef.nativeElement, 'width', newWidth.toString() + 'px');
    editor.resize();
    this.popover.updatePosition();
  }
}
