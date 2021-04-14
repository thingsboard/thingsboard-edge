///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, ElementRef, Inject, OnInit, Renderer2, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import * as ace from 'ace-builds';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { CloudEvent } from "@shared/models/edge.models";

export interface CloudEventDetailsDialogData {
  content: string;
}

@Component({
  selector: 'tb-cloud-event-details-dialog',
  templateUrl: './cloud-event-details-dialog.component.html',
  styleUrls: ['./cloud-event-details-dialog.component.scss']
})
export class CloudEventDetailsDialogComponent extends DialogComponent<CloudEventDetailsDialogComponent> implements OnInit {

  @ViewChild('actionDataEditor', {static: true})
  actionDataEditorElmRef: ElementRef;
  private actionDataEditor: ace.Ace.Editor;

  actionData: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CloudEventDetailsDialogData,
              public dialogRef: MatDialogRef<CloudEventDetailsDialogComponent>,
              private renderer: Renderer2) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.actionData = JSON.stringify(this.data.content);
    this.actionDataEditor = this.createEditor(this.actionDataEditorElmRef, this.actionData);
  }

  createEditor(editorElementRef: ElementRef, content: string): ace.Ace.Editor {
    const editorElement = editorElementRef.nativeElement;
    let editorOptions: Partial<ace.Ace.EditorOptions> = {
      mode: 'ace/mode/java',
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
    const editor = ace.edit(editorElement, editorOptions);
    editor.session.setUseWrapMode(false);
    editor.setValue(content, -1);
    this.updateEditorSize(editorElement, content, editor);
    return editor;
  }

  updateEditorSize(editorElement: any, content: string, editor: ace.Ace.Editor) {
    let newHeight = 200;
    let newWidth = 600;
    if (content && content.length > 0) {
      const lines = content.split('\n');
      newHeight = 16 * lines.length + 16;
      let maxLineLength = 0;
      lines.forEach((row) => {
        const line = row.replace(/\t/g, '    ').replace(/\n/g, '');
        const lineLength = line.length;
        maxLineLength = Math.max(maxLineLength, lineLength);
      });
      newWidth = 8 * maxLineLength + 16;
    }
    this.renderer.setStyle(editorElement, 'minHeight', newHeight.toString() + 'px');
    this.renderer.setStyle(editorElement, 'height', newHeight.toString() + 'px');
    this.renderer.setStyle(editorElement, 'width', newWidth.toString() + 'px');
    editor.resize();
  }

}
