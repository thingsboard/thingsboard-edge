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

import { Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Ace } from 'ace-builds';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { getAce } from '@shared/models/ace/ace.models';

@Component({
  selector: 'tb-markdown-editor',
  templateUrl: './markdown-editor.component.html',
  styleUrls: ['./markdown-editor.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MarkdownEditorComponent),
      multi: true
    }
  ]
})
export class MarkdownEditorComponent implements OnInit, ControlValueAccessor, OnDestroy {

  @Input() label: string;

  @Input() disabled: boolean;

  @Input() readonly: boolean;

  @Input() helpId: string;

  @ViewChild('markdownEditor', {static: true})
  markdownEditorElmRef: ElementRef;

  private markdownEditor: Ace.Editor;

  editorMode = true;

  fullscreen = false;

  markdownValue: string;
  renderValue: string;

  ignoreChange = false;

  private propagateChange = null;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  constructor() {
  }

  ngOnInit(): void {
    if (!this.readonly) {
      const editorElement = this.markdownEditorElmRef.nativeElement;
      let editorOptions: Partial<Ace.EditorOptions> = {
        mode: 'ace/mode/markdown',
        showGutter: true,
        showPrintMargin: false,
        readOnly: false
      };

      const advancedOptions = {
        enableSnippets: true,
        enableBasicAutocompletion: true,
        enableLiveAutocompletion: true
      };

      editorOptions = {...editorOptions, ...advancedOptions};

      getAce().subscribe(
        (ace) => {
          this.markdownEditor = ace.edit(editorElement, editorOptions);
          this.markdownEditor.session.setUseWrapMode(false);
          this.markdownEditor.setValue(this.markdownValue ? this.markdownValue : '', -1);
          this.markdownEditor.on('change', () => {
            if (!this.ignoreChange) {
              this.updateView();
            }
          });
        }
      );

    }
  }

  ngOnDestroy(): void {
    if (this.markdownEditor) {
      this.markdownEditor.destroy();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: string): void {
    this.editorMode = true;
    this.markdownValue = value;
    this.renderValue = this.markdownValue ? this.markdownValue : ' ';
    if (this.markdownEditor) {
      this.ignoreChange = true;
      this.markdownEditor.setValue(this.markdownValue ? this.markdownValue : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.markdownEditor.getValue();
    if (this.markdownValue !== editorValue) {
      this.markdownValue = editorValue;
      this.renderValue = this.markdownValue ? this.markdownValue : ' ';
      this.propagateChange(this.markdownValue);
    }
  }

  onFullscreen() {
    if (this.markdownEditor) {
      setTimeout(() => {
        this.markdownEditor.resize();
      }, 0);
    }
  }

  toggleEditMode() {
    this.editorMode = !this.editorMode;
    if (this.editorMode && this.markdownEditor) {
      setTimeout(() => {
        this.markdownEditor.resize();
      }, 0);
    }
  }
}
