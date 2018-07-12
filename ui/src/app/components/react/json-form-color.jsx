/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
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
import './json-form-color.scss';

import $ from 'jquery';
import React from 'react';
import ReactDOM from 'react-dom';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import reactCSS from 'reactcss';
import tinycolor from 'tinycolor2';
import TextField from 'material-ui/TextField';
import IconButton from 'material-ui/IconButton';

class ThingsboardColor extends React.Component {

    constructor(props) {
        super(props);
        this.onValueChanged = this.onValueChanged.bind(this);
        this.onSwatchClick = this.onSwatchClick.bind(this);
        this.onClear = this.onClear.bind(this);
        var value = props.value ? props.value + '' : null;
        var color = value != null ? tinycolor(value).toRgb() : null;
        this.state = {
            color: color
        };
    }

    componentDidMount() {
        var node = ReactDOM.findDOMNode(this);
        var colContainer = $(node).children('#color-container');
        colContainer.click(this, function(event) {
            if (!event.data.props.form.readonly) {
                event.data.onSwatchClick(event);
            }
        });
    }

    componentWillUnmount () {
        var node = ReactDOM.findDOMNode(this);
        var colContainer = $(node).children('#color-container');
        colContainer.off( "click" );
    }

    onValueChanged(value) {
        var color = null;
        if (value != null) {
            color = tinycolor(value);
        }
        this.setState({
            color: value
        })
        var colorValue = '';
        if (color != null && color.getAlpha() != 1) {
            colorValue = color.toRgbString();
        } else if (color != null) {
            colorValue = color.toHexString();
        }
        this.props.onChangeValidate({
            target: {
                value: colorValue
            }
        });
    }

    onSwatchClick(event) {
        this.props.onColorClick(event, this.props.form.key, this.state.color);
    }

    onClear(event) {
        if (event) {
            event.stopPropagation();
        }
        this.onValueChanged(null);
    }

    render() {

        var background = 'rgba(0,0,0,0)';
        if (this.state.color != null) {
            background = `rgba(${ this.state.color.r }, ${ this.state.color.g }, ${ this.state.color.b }, ${ this.state.color.a })`;
        }

        const styles = reactCSS({
            'default': {
                color: {
                    background: `${ background }`
                },
                swatch: {
                    display: 'inline-block',
                    marginRight: '10px',
                    marginTop: 'auto',
                    marginBottom: 'auto',
                    cursor: 'pointer',
                    opacity: `${ this.props.form.readonly ? '0.6' : '1' }`
                },
                swatchText: {
                    display: 'inline-block',
                    width: '100%'
                },
                container: {
                    display: 'flex'
                },
                colorContainer: {
                    display: 'flex',
                    width: '100%'
                }
            },
        });

        var fieldClass = "tb-field";
        if (this.props.form.required) {
            fieldClass += " tb-required";
        }
        if (this.props.form.readonly) {
            fieldClass += " tb-readonly";
        }
        if (this.state.focused) {
            fieldClass += " tb-focused";
        }

        var stringColor = '';
        if (this.state.color != null) {
            var color = tinycolor(this.state.color);
            stringColor = color.toRgbString();
        }

        return (
            <div style={ styles.container }>
                 <div id="color-container" style={ styles.colorContainer }>
                    <div className="tb-color-preview" style={ styles.swatch }>
                        <div className="tb-color-result" style={ styles.color }/>
                    </div>
                    <TextField
                        className={fieldClass}
                        floatingLabelText={this.props.form.title}
                        hintText={this.props.form.placeholder}
                        errorText={this.props.error}
                        value={stringColor}
                        disabled={this.props.form.readonly}
                        style={ styles.swatchText } />
                 </div>
                <IconButton iconClassName="material-icons" tooltip="Clear" onTouchTap={this.onClear}>clear</IconButton>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardColor);
