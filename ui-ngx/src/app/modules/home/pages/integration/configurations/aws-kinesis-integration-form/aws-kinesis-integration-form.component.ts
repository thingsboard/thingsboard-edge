import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { IntegrationType } from '@app/shared/models/integration.models';
import { initialPositionInStream } from '../../integartionFormTemapltes';

@Component({
  selector: 'tb-aws-kinesis-integration-form',
  templateUrl: './aws-kinesis-integration-form.component.html',
  styleUrls: ['./aws-kinesis-integration-form.component.scss']
})
export class AwsKinesisIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;

  initialPositionInStream  = initialPositionInStream;

  constructor() { }

  ngOnInit(): void {
  }

 
}
