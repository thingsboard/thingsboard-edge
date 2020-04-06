import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { IntegrationType } from '@app/shared/models/integration.models';
import { opcUaMappingType, extensionKeystoreType } from '../../integartionFormTemapltes';

@Component({
  selector: 'tb-opc-ua-integration-form',
  templateUrl: './opc-ua-integration-form.component.html',
  styleUrls: ['./opc-ua-integration-form.component.scss']
})
export class OpcUaIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;

  opcUaMappingType = opcUaMappingType;
  extensionKeystoreType = extensionKeystoreType;
  constructor() { }

  ngOnInit(): void {
  }

  opcUaSecurityTypeChanged(){}



}
