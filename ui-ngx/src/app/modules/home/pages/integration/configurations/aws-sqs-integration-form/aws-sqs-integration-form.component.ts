import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { IntegrationType } from '@app/shared/models/integration.models';

@Component({
  selector: 'tb-aws-sqs-integration-form',
  templateUrl: './aws-sqs-integration-form.component.html',
  styleUrls: ['./aws-sqs-integration-form.component.scss']
})
export class AwsSqsIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;

  constructor() { }

  ngOnInit(): void {
  }

}
