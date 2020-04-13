import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { generateId } from '@app/core/utils';


@Component({
  selector: 'tb-kafka-integration-form',
  templateUrl: './kafka-integration-form.component.html',
  styleUrls: ['./kafka-integration-form.component.scss']
})
export class KafkaIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;

  constructor() { }

  ngOnInit(): void {
    if (!this.form.get('groupId').value)
      this.form.get('groupId').patchValue('group_id_' + generateId(10));
    if (!this.form.get('clientId').value)
      this.form.get('clientId').patchValue('client_id_' + generateId(10));
  }

}
