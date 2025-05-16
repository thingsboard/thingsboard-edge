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

import { Component, ElementRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { Converter, ConverterType } from '@shared/models/converter.models';
import { ConverterService } from '@core/http/converter.service';
import { ConverterId } from '@shared/models/id/converter-id';
import { PageLink } from '@shared/models/page/page-link';
import { getEntityDetailsPageURL } from '@core/utils';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatDialog } from '@angular/material/dialog';
import {
  AddConverterDialogComponent,
  AddConverterDialogData
} from '@home/components/converter/add-converter-dialog.component';
import { Operation, Resource } from '@shared/models/security.models';
import { IntegrationType } from '@shared/models/integration.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-converter-autocomplete',
  templateUrl: './converter-autocomplete.component.html',
  styleUrls: ['./converter-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ConverterAutocompleteComponent),
    multi: true
  }]
})
export class ConverterAutocompleteComponent implements ControlValueAccessor, OnInit, OnChanges {

  selectConverterFormGroup: FormGroup;

  private modelValue: ConverterId | string | null;

  @Input()
  useFullEntityId = false;

  @Input()
  isEdgeTemplate = false;

  @Input()
  addNewConverter = false;

  @Input()
  converterType: ConverterType;

  @Input()
  excludeEntityIds: Array<string>;

  @Input()
  labelText = 'converter.converter';

  @Input()
  requiredText = 'converter.converter-required';

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  showDetailsPageLink = false;

  @Input()
  integrationType: IntegrationType

  @ViewChild('converterInput', {static: true}) converterInput: ElementRef<HTMLElement>;
  @ViewChild('converterInput', {read: MatAutocompleteTrigger}) converterAutocomplete: MatAutocompleteTrigger;

  filteredEntities: Observable<Array<Converter>>;

  searchText = '';
  converterURL: string;

  resource = Resource;
  operation = Operation;

  private dirty = false;

  private propagateChange: (value: any) => void = () => {};

  constructor(private converterService: ConverterService,
              private dialog: MatDialog,
              private fb: FormBuilder) {
    this.selectConverterFormGroup = this.fb.group({
      entity: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.filteredEntities = this.selectConverterFormGroup.get('entity').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = this.useFullEntityId ? value.id : value.id.id;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value.trim() : value.name) : ''),
        switchMap(name => this.fetchEntities(name)),
        share()
      );
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'converterType' || propName === 'integrationType') {
          this.reset();
          this.dirty = true;
        }
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectConverterFormGroup.disable({emitEvent: false});
    } else {
      this.selectConverterFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | ConverterId | null): void {
    this.searchText = '';
    if (value != null) {
      const converterId = typeof value === 'string' ? value : value.id;
      this.converterService.getConverter(converterId, {ignoreLoading: true}).subscribe(
        (entity) => {
          this.modelValue = this.useFullEntityId ? entity.id : entity.id.id;
          this.converterURL = getEntityDetailsPageURL(entity.id.id, entity.id.entityType);
          if (entity.edgeTemplate) {
            this.converterURL = `/edgeManagement${this.converterURL.replace(/^\/integrationsCenter/, '')}`;
          }
          this.selectConverterFormGroup.get('entity').patchValue(entity, {emitEvent: false});
        }
      );
    } else {
      this.modelValue = null;
      this.selectConverterFormGroup.get('entity').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectConverterFormGroup.get('entity').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  private reset() {
    this.selectConverterFormGroup.get('entity').patchValue('', {emitEvent: false});
  }

  private updateView(value: string | ConverterId | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayEntityFn(converter?: Converter): string | undefined {
    return converter ? converter.name : undefined;
  }

  private fetchEntities(searchText?: string): Observable<Array<Converter>> {
    this.searchText = searchText;
    let limit = 50;
    if (this.excludeEntityIds && this.excludeEntityIds.length) {
      limit += this.excludeEntityIds.length;
    }
    const pageLink = new PageLink(limit, 0, this.searchText);
    return this.converterService.getConvertersByEdgeTemplate(pageLink, this.isEdgeTemplate, this.integrationType, {ignoreLoading: true}).pipe(
      catchError(() => of(null)),
      map((data) => {
          if (data) {
            let entities: Array<Converter>;
            if (this.excludeEntityIds && this.excludeEntityIds.length) {
              entities = data.data.filter((entity) => this.excludeEntityIds.indexOf(entity.id.id) === -1);
            } else {
              entities = data.data;
            }
            if (this.converterType) {
              entities = entities.filter((converter) => converter.type === this.converterType);
            }
            return entities;
          } else {
            return [];
          }
        }
      ));
  }

  clear() {
    this.selectConverterFormGroup.get('entity').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.converterInput.nativeElement.blur();
      this.converterInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return text?.trim().length > 0;
  }

  createConverter($event: Event, converterName: string) {
    $event.preventDefault();
    if (this.addNewConverter) {
      this.converterAutocomplete.closePanel();
      this.dialog.open<AddConverterDialogComponent, AddConverterDialogData,
        Converter>(AddConverterDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          name: converterName.trim(),
          edgeTemplate: this.isEdgeTemplate,
          type: this.converterType,
          integrationType: this.integrationType
        }
      }).afterClosed().subscribe(
        (entity) => {
          if (entity) {
            this.selectConverterFormGroup.get('entity').patchValue(entity);
          }
        }
      );
    }
  }
}
