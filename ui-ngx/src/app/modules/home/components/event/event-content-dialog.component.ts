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

import { Component, ElementRef, Inject, OnDestroy, OnInit, Renderer2, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

import { Ace } from 'ace-builds';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { ContentType, contentTypesMap } from '@shared/models/constants';
import { getAce } from '@shared/models/ace/ace.models';
import { Observable } from 'rxjs/internal/Observable';
import { beautifyJs } from '@shared/models/beautify.models';
import { of } from 'rxjs';
import { base64toString, isLiteralObject } from '@core/utils';

export interface EventContentDialogData {
  content: string;
  title: string;
  contentType: ContentType;
}

@Component({
  selector: 'tb-event-content-dialog',
  templateUrl: './event-content-dialog.component.html',
  styleUrls: ['./event-content-dialog.component.scss']
})
export class EventContentDialogComponent extends DialogComponent<EventContentDialogData> implements OnInit, OnDestroy {

  @ViewChild('eventContentEditor', {static: true})
  eventContentEditorElmRef: ElementRef;

  content: string;
  title: string;
  contentType: ContentType;
  aceEditor: Ace.Editor;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EventContentDialogData,
              public dialogRef: MatDialogRef<EventContentDialogComponent>,
              private renderer: Renderer2) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.content = this.data.content;
    this.title = this.data.title;
    this.contentType = this.data.contentType;

    this.createEditor(this.eventContentEditorElmRef, this.content);
  }

  ngOnDestroy(): void {
    if (this.aceEditor) {
      this.aceEditor.destroy();
    }
    super.ngOnDestroy();
  }

  isJson(str) {
    try {
      return isLiteralObject(JSON.parse(str));
    } catch (e) {
      return false;
    }
  }

  createEditor(editorElementRef: ElementRef, content: string) {
    const editorElement = editorElementRef.nativeElement;
    let mode = 'java';
    let content$: Observable<string> = null;
    if (this.contentType) {
      mode = contentTypesMap.get(this.contentType).code;
      if (this.contentType === ContentType.JSON && content) {
        content$ = beautifyJs(content, {indent_size: 4});
      } else if (this.contentType === ContentType.TEXT && content) {
        try {
          const decodedData = base64toString(content);
          content$ = of(decodedData);
        } catch (e) {}
      }
    }
    if (!content$) {
      content$ = of(content);
    }

    content$.subscribe(
      (processedContent) => {
        let editorOptions: Partial<Ace.EditorOptions> = {
          mode: `ace/mode/${mode}`,
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
            this.aceEditor.setValue(processedContent, -1);
            this.updateEditorSize(editorElement, processedContent, this.aceEditor);
          }
        );
      }
    );
  }

  updateEditorSize(editorElement: any, content: string, editor: Ace.Editor) {
    let newHeight = 400;
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
    // newHeight = Math.min(400, newHeight);
    this.renderer.setStyle(editorElement, 'minHeight', newHeight.toString() + 'px');
    this.renderer.setStyle(editorElement, 'height', newHeight.toString() + 'px');
    this.renderer.setStyle(editorElement, 'width', newWidth.toString() + 'px');
    editor.resize();
  }

}
