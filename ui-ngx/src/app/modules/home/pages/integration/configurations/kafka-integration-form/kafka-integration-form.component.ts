import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';


@Component({
  selector: 'tb-kafka-integration-form',
  templateUrl: './kafka-integration-form.component.html',
  styleUrls: ['./kafka-integration-form.component.scss']
})
export class KafkaIntegrationFormComponent implements OnInit {

  @Input() isLoading$: Observable<boolean>;
  @Input() isEdit: boolean;
  @Input() form: FormGroup;


  constructor() { }

  ngOnInit(): void {
  }

}
