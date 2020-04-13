import { Component, Input, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';

import {
  extensionKeystoreType,
  identityType,
  opcSecurityTypes,
  opcUaMappingType
} from '../../integration-forms-templates';

@Component({
  selector: 'tb-opc-ua-integration-form',
  templateUrl: './opc-ua-integration-form.component.html',
  styleUrls: ['./opc-ua-integration-form.component.scss']
})
export class OpcUaIntegrationFormComponent implements OnInit {


  @Input() form: FormGroup;

  identityType = identityType;
  opcUaMappingType = opcUaMappingType;
  extensionKeystoreType = extensionKeystoreType;
  opcSecurityTypes = opcSecurityTypes;

  constructor(private fb: FormBuilder) { }

  ngOnInit(): void {
  }

  opcUaSecurityTypeChanged() { }

  addMap() {
    (this.form.get('mapping') as FormArray).push(
      this.fb.group({
        deviceNodePattern: ['Channel1\\.Device\\d+$'],
        mappingType: ['FQN', Validators.required],
        subscriptionTags: this.fb.array([]),
        namespace: [Validators.min(0)]
      })
    );
  }



}
