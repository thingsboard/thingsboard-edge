/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
import './json-form-image.scss';

import React from 'react';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import Dropzone from 'react-dropzone';
import IconButton from 'material-ui/IconButton';

class ThingsboardImage extends React.Component {

    constructor(props) {
        super(props);
        this.onValueChanged = this.onValueChanged.bind(this);
        this.onDrop = this.onDrop.bind(this);
        this.onClear = this.onClear.bind(this);
        var value = props.value ? props.value + '' : null;
        this.state = {
            imageUrl: value
        };
    }

    onValueChanged(value) {
        this.setState({
            imageUrl: value
        });
        this.props.onChangeValidate({
            target: {
                value: value
            }
        });
    }

    onDrop(files) {
        var reader = new FileReader();
        reader.onload = (function(tImg) {
            return function(event) {
                tImg.onValueChanged(event.target.result);
            };
        })(this);
        reader.readAsDataURL(files[0]);
    }

    onClear(event) {
        if (event) {
            event.stopPropagation();
        }
        this.onValueChanged("");
    }

    render() {

        var labelClass = "tb-label";
        if (this.props.form.required) {
            labelClass += " tb-required";
        }
        if (this.props.form.readonly) {
            labelClass += " tb-readonly";
        }
        if (this.state.focused) {
            labelClass += " tb-focused";
        }

        var previewComponent;
        if (this.state.imageUrl) {
            previewComponent = <img className="tb-image-preview" src={this.state.imageUrl} />;
        } else {
            previewComponent = <div>No image selected</div>;
        }

        return (
            <div className="tb-container">
                <label className={labelClass}>{this.props.form.title}</label>
                <div className="tb-image-select-container">
                    <div className="tb-image-preview-container">{previewComponent}</div>
                    <div className="tb-image-clear-container">
                        <IconButton className="tb-image-clear-btn" iconClassName="material-icons" tooltip="Clear" onTouchTap={this.onClear}>clear</IconButton>
                    </div>
                    <Dropzone className="tb-dropzone"
                              onDrop={this.onDrop}
                              multiple={false}
                              accept="image/*">
                        <div>Drop an image or click to select a file to upload.</div>
                    </Dropzone>
                </div>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardImage);