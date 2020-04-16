import { Component, OnInit, Input, ChangeDetectionStrategy } from '@angular/core';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'tb-cert-upload',
  templateUrl: './cert-upload.component.html',
  styleUrls: ['./cert-upload.component.scss'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class CertUploadComponent implements OnInit {

  @Input() form: FormGroup;

  constructor() { }

  ngOnInit(): void {
  }

}
