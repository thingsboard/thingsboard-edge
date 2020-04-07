import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';

import { opcUaMappingType, extensionKeystoreType, opcSecurityTypes, identityType } from '../../integartion-forms-temapltes';

@Component({
  selector: 'tb-opc-ua-integration-form',
  templateUrl: './opc-ua-integration-form.component.html',
  styleUrls: ['./opc-ua-integration-form.component.scss']
})
export class OpcUaIntegrationFormComponent implements OnInit {

  @Input() isLoading$: Observable<boolean>;
  @Input() isEdit: boolean;
  @Input() form: FormGroup;

  identityType =identityType;
  opcUaMappingType = opcUaMappingType;
  extensionKeystoreType = extensionKeystoreType;
  opcSecurityTypes = opcSecurityTypes;

  constructor() { }

  ngOnInit(): void {
  }

  opcUaSecurityTypeChanged(){}



}
