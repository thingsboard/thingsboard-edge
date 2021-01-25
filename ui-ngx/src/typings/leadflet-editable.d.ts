///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import * as leaflet from 'leaflet';

declare module 'leaflet' {

  /**
   * Make geometries editable in Leaflet.
   *
   * This is not a plug and play UI, and will not. This is a minimal, lightweight, and fully extendable API to
   * control editing of geometries. So you can easily build your own UI with your own needs and choices.
   */
  interface EditableStatic {
    new (map: Map, options: EditOptions): Editable;
  }

  /**
   * Options to pass to L.Editable when instanciating.
   */
  interface EditOptions extends  leaflet.MapOptions{
    /**
     * Whether to create a L.Editable instance at map init or not.
     */
    editable: boolean;
    /**
     * Class to be used when creating a new Polyline.
     */
    polylineClass?: object;

    /**
     * Class to be used when creating a new Polygon.
     */
    polygonClass?: object;

    /**
     * Class to be used when creating a new Marker.
     */
    markerClass?: object;

    /**
     * CSS class to be added to the map container while drawing.
     */
    drawingCSSClass?: string;

    /**
     * Layer used to store edit tools (vertex, line guide…).
     */
    editLayer?: LayerGroup<leaflet.Layer>;

    /**
     * Default layer used to store drawn features (marker, polyline…).
     */
    featuresLayer?: LayerGroup<Polyline|Polygon|Marker>;

    /**
     * Class to be used as vertex, for path editing.
     */
    vertexMarkerClass?: object;

    /**
     * Class to be used as middle vertex, pulled by the user to create a new point in the middle of a path.
     */
    middleMarkerClass?: object;

    /**
     * Class to be used as Polyline editor.
     */
    polylineEditorClass?: object;

    /**
     * Class to be used as Polygon editor.
     */
    polygonEditorClass?: object;

    /**
     * Class to be used as Marker editor.
     */
    markerEditorClass?: object;

    /**
     * Options to be passed to the line guides.
     */
    lineGuideOptions?: object;

    /**
     * Set this to true if you don't want middle markers.
     */
    skipMiddleMarkers?: boolean;
  }

  /**
   * Make geometries editable in Leaflet.
   *
   * This is not a plug and play UI, and will not. This is a minimal, lightweight, and fully extendable API to
   * control editing of geometries. So you can easily build your own UI with your own needs and choices.
   */
  interface Editable extends leaflet.Evented {
    /**
     * Options to pass to L.Editable when instanciating.
     */
    options: EditOptions;

    currentPolygon: Polyline|Polygon|Marker;

    /**
     * Start drawing a polyline. If latlng is given, a first point will be added. In any case, continuing on user
     * click. If options is given, it will be passed to the polyline class constructor.
     */
    startPolyline(latLng?: LatLng, options?: PolylineOptions): Polyline;

    /**
     * Start drawing a polygon. If latlng is given, a first point will be added. In any case, continuing on user
     * click. If options is given, it will be passed to the polygon class constructor.
     */
    startPolygon(latLng?: LatLng, options?: PolylineOptions): Polygon;

    /**
     * Start adding a marker. If latlng is given, the marker will be shown first at this point. In any case, it
     * will follow the user mouse, and will have a final latlng on next click (or touch). If options is given,
     * it will be passed to the marker class constructor.
     */
    startMarker(latLng?: LatLng, options?: MarkerOptions): Marker;

    /**
     * When you need to stop any ongoing drawing, without needing to know which editor is active.
     */
    stopDrawing(): void;

    /**
     * When you need to commit any ongoing drawing, without needing to know which editor is active.
     */
    commitDrawing(): void;
  }

  let Editable: EditableStatic;

  /**
   * EditableMixin is included to L.Polyline, L.Polygon and L.Marker. It adds the following methods to them.
   *
   * When editing is enabled, the editor is accessible on the instance with the editor property.
   */
  interface EditableMixin {
    /**
     * Enable editing, by creating an editor if not existing, and then calling enable on it.
     */
    enableEdit(map: L.Map): any;

    /**
     * Disable editing, also remove the editor property reference.
     */
    disableEdit(): void;

    /**
     * Enable or disable editing, according to current status.
     */
    toggleEdit(): void;

    /**
     * Return true if current instance has an editor attached, and this editor is enabled.
     */
    editEnabled(): boolean;
  }

  interface Map {


    /**
     * Options to pass to L.Editable when instanciating.
     */
    editOptions: MapOptions;

    /**
     * L.Editable plugin instance.
     */
    editTools: Editable;
  }

  // tslint:disable-next-line:no-empty-interface
  interface Polyline extends EditableMixin {}

  namespace Map {
    interface MapOptions {
      /**
       * Whether to create a L.Editable instance at map init or not.
       */
      editable?: boolean;

      /**
       * Options to pass to L.Editable when instanciating.
       */
      editOptions?: MapOptions;
    }
  }

  /**
   * When editing a feature (marker, polyline…), an editor is attached to it. This editor basically knows
   * how to handle the edition.
   */
  interface BaseEditor {
    /**
     * Set up the drawing tools for the feature to be editable.
     */
    enable(): MarkerEditor|PolylineEditor|PolygonEditor;

    /**
     * Remove editing tools.
     */
    disable(): MarkerEditor|PolylineEditor|PolygonEditor;
  }

  /**
   * Inherit from L.Editable.BaseEditor.
   * Inherited by L.Editable.PolylineEditor and L.Editable.PolygonEditor.
   */
  interface PathEditor extends BaseEditor {
    /**
     * Rebuild edit elements (vertex, middlemarker, etc.).
     */
    reset(): void;
  }

  /**
   * Inherit from L.Editable.PathEditor.
   */
  interface PolylineEditor extends PathEditor {
    /**
     * Set up drawing tools to continue the line forward.
     */
    continueForward(): void;

    /**
     * Set up drawing tools to continue the line backward.
     */
    continueBackward(): void;
  }

  /**
   * Inherit from L.Editable.PathEditor.
   */
  interface PolygonEditor extends PathEditor {
    /**
     * Set up drawing tools for creating a new hole on the polygon. If the latlng param is given, a first
     * point is created.
     */
    newHole(latlng: LatLng): void;
  }

  /**
   * Inherit from L.Editable.BaseEditor.
   */
    // tslint:disable-next-line:no-empty-interface
  interface MarkerEditor extends BaseEditor {}

  interface Marker extends EditableMixin, MarkerEditor {}

  interface Polyline extends EditableMixin, PolylineEditor {}

  interface Polygon extends EditableMixin, PolygonEditor {}

  function map(element: string | HTMLElement, options?: EditOptions): Map;
}
