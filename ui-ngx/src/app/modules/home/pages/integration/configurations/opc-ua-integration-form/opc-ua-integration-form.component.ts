import { Component, OnInit, Input, ChangeDetectionStrategy, ChangeDetectorRef, AfterViewInit } from '@angular/core';
import { FormGroup, FormArray, FormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs';

import { opcUaMappingType, extensionKeystoreType, opcSecurityTypes, identityType } from '../../integartion-forms-templates';

@Component({
  selector: 'tb-opc-ua-integration-form',
  templateUrl: './opc-ua-integration-form.component.html',
  styleUrls: ['./opc-ua-integration-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class OpcUaIntegrationFormComponent implements OnInit {


  @Input() form: FormGroup;

  identityType = identityType;
  opcUaMappingType = opcUaMappingType;
  extensionKeystoreType = extensionKeystoreType;
  opcSecurityTypes = opcSecurityTypes;
  showIdentityForm: boolean;

  constructor(private fb: FormBuilder, private cd: ChangeDetectorRef) { }

  ngOnInit(): void {
    this.form.get('mapping').setValidators(Validators.required)
    this.form.get('keystore').get('location').setValidators(Validators.required);
    this.form.get('keystore').get('fileContent').setValidators(Validators.required);
  }

  identityTypeChanged($event?) {
    this.showIdentityForm = $event?.value === 'username';
  }

  addMap() {
    (this.form.get('mapping') as FormArray).push(
      this.fb.group({
        deviceNodePattern: ['Channel1\\.Device\\d+$'],
        mappingType: ['FQN', Validators.required],
        subscriptionTags: this.fb.array([], [Validators.required]),
        namespace: [Validators.min(0)]
      })
    );
  }
}
