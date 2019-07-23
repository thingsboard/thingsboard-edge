/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
/* eslint-disable */

const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const CompressionPlugin = require('compression-webpack-plugin');

const webpack = require('webpack');
const path = require('path');
const dirTree = require('directory-tree');
const jsonminify = require("jsonminify");

const PUBLIC_RESOURCE_PATH = '/static/';

var langs = [];
dirTree('./src/app/locale/', {extensions:/\.json$/}, (item) => {
    /* It is expected what the name of a locale file has the following format: */
    /* 'locale.constant-LANG_CODE[_REGION_CODE].json', e.g. locale.constant-es.json or locale.constant-zh_CN.json*/
    langs.push(item.name.slice(item.name.lastIndexOf('-') + 1, -5));
});

module.exports = {
    mode: 'production',
    entry: [
        './src/app/app.js',
        'webpack-material-design-icons'
    ],
    output: {
        path: path.resolve(__dirname, 'target/generated-resources/public/static'),
        publicPath: PUBLIC_RESOURCE_PATH,
        filename: 'bundle.[hash].js',
        pathinfo: false
    },
    plugins: [
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery",
            "window.jQuery": "jquery",
            tinycolor: "tinycolor2",
            tv4: "tv4",
            moment: "moment"
        }),
        new CopyWebpackPlugin([
            {
                from: './src/thingsboard.ico',
                to: 'thingsboard.ico'
            },
            {
                from: './src/app/locale',
                to: 'locale',
                ignore: [ '*.js' ],
                transform: function(content, path) {
                    return Buffer.from(jsonminify(content.toString()));
                }
            }
        ]),
        new HtmlWebpackPlugin({
            template: './src/index.html',
            filename: '../index.html',
            title: 'ThingsBoard',
            inject: 'body',
        }),
        new MiniCssExtractPlugin({
            filename: 'style.[contentHash].css'
        }),
        new webpack.DefinePlugin({
            THINGSBOARD_VERSION: JSON.stringify(require('./package.json').version),
            '__DEVTOOLS__': false,
            PUBLIC_PATH: PUBLIC_RESOURCE_PATH,
            SUPPORTED_LANGS: JSON.stringify(langs)
        }),
        new CompressionPlugin({
            filename: "[path].gz[query]",
            algorithm: "gzip",
            test: /\.js$|\.css$|\.svg$|\.ttf$|\.woff$|\.woff2$|\.eot$|\.json$/,
            threshold: 10240,
            minRatio: 0.8
        })
    ],
    node: {
        tls: "empty",
        fs: "empty"
    },
    module: {
        rules: [
            {
                test: /\.jsx$/,
                use: [
                    {
                        loader: 'babel-loader',
                        options: {
                            cacheDirectory: true
                        }
                    }
                ],
                exclude: /node_modules/,
                include: __dirname,
            },
            {
                test: /\.js$/,
                use: [
                    {
                        loader: 'ng-annotate-loader',
                        options: {
                            ngAnnotate: 'ng-annotate-patched',
                            es6: true,
                            explicitOnly: false
                        }
                    },
                    {
                        loader: 'babel-loader',
                        options: {
                            cacheDirectory: true
                        }
                    }
                ],
                exclude: /node_modules/,
                include: __dirname,
            },
            {
                test: /\.js$/,
                use: [
                    {
                        loader: 'eslint-loader',
                        options: {
                            parser: 'babel-eslint'
                        }
                    }
                ],
                exclude: /node_modules|vendor/,
                include: __dirname,
            },
            {
                test: /\.css$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    'css-loader'
                ]
            },
            {
                test: /\.scss$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    'css-loader',
                    'postcss-loader',
                    'sass-loader'
                ]
            },
            {
                test: /\.less$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    'css-loader',
                    'postcss-loader',
                    'less-loader'
                ]
            },
            {
                test: /\.tpl\.html$/,
                use: [
                    {
                        loader: 'ngtemplate-loader',
                        options: {
                            relativeTo: path.resolve(__dirname, './src/app')
                        }
                    },
                    {
                        loader: 'html-loader'
                    },
                    {
                        loader: 'html-minifier-loader',
                        options: {
                            caseSensitive: true,
                            removeComments: true,
                            collapseWhitespace: false,
                            preventAttributesEscaping: true,
                            removeEmptyAttributes: false
                        }
                    }
                ]
            },
            {
                test: /\.tpl\.txt$/,
                loader: 'raw-loader'
            },
            {
                test: /\.(svg)(\?v=[0-9]+\.[0-9]+\.[0-9]+)?$/,
                use: [
                    {
                        loader: 'url-loader',
                        options: {
                            limit: 8192
                        }
                    }
                ]
            },
            {
                test: /\.(png|jpe?g|gif|woff|woff2|ttf|otf|eot|ico)(\?v=[0-9]+\.[0-9]+\.[0-9]+)?$/,
                use: [
                    {
                        loader: 'url-loader',
                        options: {
                            limit: 8192
                        }
                    },
                    {
                        loader: 'img-loader',
                        options: {
                            minimize: true
                        }
                    }
                ]
            }
        ],
    }
};
