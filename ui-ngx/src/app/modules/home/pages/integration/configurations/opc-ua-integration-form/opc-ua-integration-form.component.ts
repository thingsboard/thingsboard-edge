import { Component, OnInit, Input } from '@angular/core';
import { FormGroup, FormArray, FormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs';

import { opcUaMappingType, extensionKeystoreType, opcSecurityTypes, identityType } from '../../integartion-forms-templates';

@Component({
  selector: 'tb-opc-ua-integration-form',
  templateUrl: './opc-ua-integration-form.component.html',
  styleUrls: ['./opc-ua-integration-form.component.scss']
})
export class OpcUaIntegrationFormComponent implements OnInit {

  @Input() isLoading$: Observable<boolean>;
  @Input() isEdit: boolean;
  @Input() form: FormGroup;

  identityType = identityType;
  opcUaMappingType = opcUaMappingType;
  extensionKeystoreType = extensionKeystoreType;
  opcSecurityTypes = opcSecurityTypes;

  constructor(private fb: FormBuilder) { }

  ngOnInit(): void {
  }

  opcUaSecurityTypeChanged() { }

  log(a) {
    console.log(a);

  }

  addMap() {
    (this.form.get('mapping') as FormArray).push(
      this.fb.group({
        deviceNodePattern: ["Channel1\\.Device\\d+$"],
        mappingType: ['FQN', Validators.required],
        subscriptionTags: this.fb.array([]),
        namespace: [Validators.min(0)]
      }      )
    );
  }



}
