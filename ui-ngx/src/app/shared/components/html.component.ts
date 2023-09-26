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

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, UntypedFormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { Ace } from 'ace-builds';
import { getAce } from '@shared/models/ace/ace.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { ResizeObserver } from '@juggle/resize-observer';
import { beautifyHtml } from '@shared/models/beautify.models';

@Component({
  selector: 'tb-html',
  templateUrl: './html.component.html',
  styleUrls: ['./html.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => HtmlComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => HtmlComponent),
      multi: true,
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class HtmlComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  @ViewChild('htmlEditor', {static: true})
  htmlEditorElmRef: ElementRef;

  private htmlEditor: Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;
  private ignoreChange = false;

  @Input() label: string;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() minHeight = '200px';

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  fullscreen = false;

  modelValue: string;

  hasErrors = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              private utils: UtilsService,
              private translate: TranslateService,
              protected store: Store<AppState>,
              private raf: RafService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    const editorElement = this.htmlEditorElmRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: 'ace/mode/html',
      showGutter: true,
      showPrintMargin: true,
      readOnly: this.disabled
    };

    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    getAce().subscribe(
      (ace) => {
        this.htmlEditor = ace.edit(editorElement, editorOptions);
        this.htmlEditor.session.setUseWrapMode(true);
        this.htmlEditor.setValue(this.modelValue ? this.modelValue : '', -1);
        this.htmlEditor.setReadOnly(this.disabled);
        this.htmlEditor.on('change', () => {
          if (!this.ignoreChange) {
            this.updateView();
          }
        });
        // @ts-ignore
        this.htmlEditor.session.on('changeAnnotation', () => {
          const annotations = this.htmlEditor.session.getAnnotations();
          const hasErrors = annotations.filter(annotation => annotation.type === 'error').length > 0;
          if (this.hasErrors !== hasErrors) {
            this.hasErrors = hasErrors;
            this.propagateChange(this.modelValue);
            this.cd.markForCheck();
          }
        });
        this.editorResize$ = new ResizeObserver(() => {
          this.onAceEditorResize();
        });
        this.editorResize$.observe(editorElement);
      }
    );
  }

  ngOnDestroy(): void {
    if (this.editorResize$) {
      this.editorResize$.disconnect();
    }
    if (this.htmlEditor) {
      this.htmlEditor.destroy();
    }
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.htmlEditor.resize();
      this.htmlEditor.renderer.updateFull();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.htmlEditor) {
      this.htmlEditor.setReadOnly(this.disabled);
    }
  }

  public validate(c: UntypedFormControl) {
    return (!this.hasErrors) ? null : {
      html: {
        valid: false,
      },
    };
  }

  beautifyHtml() {
    beautifyHtml(this.modelValue, {indent_size: 4}).subscribe(
      (res) => {
        if (this.modelValue !== res) {
          this.htmlEditor.setValue(res ? res : '', -1);
          this.updateView();
        }
      }
    );
  }

  writeValue(value: string): void {
    this.modelValue = value;
    if (this.htmlEditor) {
      this.ignoreChange = true;
      this.htmlEditor.setValue(this.modelValue ? this.modelValue : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.htmlEditor.getValue();
    if (this.modelValue !== editorValue) {
      this.modelValue = editorValue;
      this.propagateChange(this.modelValue);
      this.cd.markForCheck();
    }
  }
}
