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
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subscription } from 'rxjs';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { FlowDirective } from '@flowjs/ngx-flow';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';

@Component({
  selector: 'tb-file-input',
  templateUrl: './file-input.component.html',
  styleUrls: ['./file-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FileInputComponent),
      multi: true
    }
  ]
})
export class FileInputComponent extends PageComponent implements AfterViewInit, OnDestroy, ControlValueAccessor, OnChanges {

  @Input()
  label: string;

  @Input()
  accept = '*/*';

  @Input()
  noFileText = 'import.no-file';

  @Input()
  inputId = this.utils.guid();

  @Input()
  allowedExtensions: string;

  @Input()
  dropLabel: string;

  @Input()
  contentConvertFunction: (content: string) => any;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
    }
  }

  private requiredAsErrorValue: boolean;

  get requiredAsError(): boolean {
    return this.requiredAsErrorValue;
  }

  @Input()
  set requiredAsError(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredAsErrorValue !== newVal) {
      this.requiredAsErrorValue = newVal;
    }
  }

  @Input()
  disabled: boolean;

  @Input()
  existingFileName: string;

  @Input()
  readAsBinary = false;

  @Input()
  workFromFileObj = false;

  private multipleFileValue = false;

  @Input()
  set multipleFile(value: boolean) {
    this.multipleFileValue = value;
    if (this.flow?.flowJs) {
      this.updateMultipleFileMode(this.multipleFile);
    }
  }

  get multipleFile(): boolean {
    return this.multipleFileValue;
  }

  @Output()
  fileNameChanged = new EventEmitter<string|string[]>();

  fileName: string | string[];
  fileContent: any;
  files: File[];

  @ViewChild('flow', {static: true})
  flow: FlowDirective;

  @ViewChild('flowInput', {static: true})
  flowInput: ElementRef;

  autoUploadSubscription: Subscription;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              public translate: TranslateService) {
    super(store);
  }

  ngAfterViewInit() {
    this.autoUploadSubscription = this.flow.events$.subscribe(event => {
      if (event.type === 'filesAdded') {
        const readers = [];
        (event.event[0] as flowjs.FlowFile[]).forEach(file => {
          if (this.filterFile(file)) {
            readers.push(this.readerAsFile(file));
          }
        });
        if (readers.length) {
          Promise.all(readers).then((files) => {
            files = files.filter(file => file.fileContent != null || file.files != null);
            if (files.length === 1) {
              this.fileContent = files[0].fileContent;
              this.fileName = files[0].fileName;
              this.files = files[0].files;
              this.updateModel();
            } else if (files.length > 1) {
              this.fileContent = files.map(content => content.fileContent);
              this.fileName = files.map(content => content.fileName);
              this.files = files.map(content => content.files);
              this.updateModel();
            }
          });
        }
      }
    });
    if (!this.multipleFile) {
      this.updateMultipleFileMode(this.multipleFile);
    }
  }

  private readerAsFile(file: flowjs.FlowFile): Promise<any> {
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = () => {
        let fileName = null;
        let fileContent = null;
        let files = null;
        if (reader.readyState === reader.DONE) {
          if (!this.workFromFileObj) {
            fileContent = reader.result;
            if (fileContent && fileContent.length > 0) {
              if (this.contentConvertFunction) {
                fileContent = this.contentConvertFunction(fileContent);
              }
              fileName = fileContent ? file.name : null;
            }
          } else if (file.name || file.file){
            files = file.file;
            fileName = file.name;
          }
        }
        resolve({fileContent, fileName, files});
      };
      reader.onerror = () => {
        resolve({fileContent: null, fileName: null, files: null});
      };
      if (this.readAsBinary) {
        reader.readAsBinaryString(file.file);
      } else {
        reader.readAsText(file.file);
      }
    });
  }

  private filterFile(file: flowjs.FlowFile): boolean {
    if (this.allowedExtensions) {
      return this.allowedExtensions.split(',').indexOf(file.getExtension()) > -1;
    } else {
      return true;
    }
  }

  ngOnDestroy() {
    if (this.autoUploadSubscription) {
      this.autoUploadSubscription.unsubscribe();
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

  writeValue(value: any): void {
    let fileName = null;
    if (this.workFromFileObj && value instanceof File) {
      fileName = Array.isArray(value) ? value.map(file => file.name) : value.name;
    }
    this.fileName = this.existingFileName || fileName;
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue) {
        if (propName === 'existingFileName') {
          this.fileName = this.existingFileName || null;
        }
      }
    }
  }

  private updateModel() {
    if (this.workFromFileObj) {
      this.propagateChange(this.files);
    } else {
      this.propagateChange(this.fileContent);
      this.fileNameChanged.emit(this.fileName);
    }
  }

  clearFile() {
    this.fileName = null;
    this.fileContent = null;
    this.files = null;
    this.updateModel();
  }

  private updateMultipleFileMode(multiple: boolean) {
    this.flow.flowJs.opts.singleFile = !multiple;
    if (!multiple) {
      this.flowInput.nativeElement.removeAttribute('multiple');
    }
  }
}
