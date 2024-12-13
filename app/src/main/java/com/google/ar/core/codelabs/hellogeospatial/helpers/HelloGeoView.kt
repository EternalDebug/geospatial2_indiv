/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.codelabs.hellogeospatial.helpers

import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Earth
import com.google.ar.core.GeospatialPose
import com.google.ar.core.codelabs.hellogeospatial.HelloGeoActivity
import com.google.ar.core.codelabs.hellogeospatial.ModelFlag
import com.google.ar.core.codelabs.hellogeospatial.R
import com.google.ar.core.codelabs.hellogeospatial.act
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper

/** Contains UI elements for Hello Geo. */

var fi = true

class HelloGeoView(val activity: HelloGeoActivity) : DefaultLifecycleObserver {
    //lateinit var root:
  val root = View.inflate(activity, R.layout.activity_main, null)//GetRoot()


  val surfaceView = root.findViewById<GLSurfaceView>(R.id.surfaceview)

  val button4_clicker = root.findViewById<Button>(R.id.button4)
  val button5_clicker = root.findViewById<Button>(R.id.button5)
    val statusvw = root.findViewById<TextView>(R.id.statusText)
    val buttonAct = root.findViewById<Button>(R.id.buttonDo)
    val buttonMenu = root.findViewById<Button>(R.id.buttonmenu)
    val buttonToAR = root.findViewById<Button>(R.id.buttonToAR)
    val txt = root.findViewById<TextView>(R.id.textView)
    val ScrStatus = root.findViewById<TextView>(R.id.statustext)

  val session
    get() = activity.arCoreSessionHelper.session

  val snackbarHelper = SnackbarHelper()

  var mapView: MapView? = null
  val mapTouchWrapper = root.findViewById<MapTouchWrapper>(R.id.map_wrapper).apply {
    setup { screenLocation ->
      val latLng: LatLng =
        mapView?.googleMap?.projection?.fromScreenLocation(screenLocation) ?: return@setup
      activity.renderer.onMapClick(latLng)
    }
  }
  val mapFragment =
    (activity.supportFragmentManager.findFragmentById(R.id.map)!! as SupportMapFragment).also {
      it.getMapAsync { googleMap -> mapView = MapView(activity, googleMap) }
    }
  val statusText = root.findViewById<TextView>(R.id.statusText)
  fun updateStatusText(earth: Earth, cameraGeospatialPose: GeospatialPose?) {
    activity.runOnUiThread {
      val poseText = if (cameraGeospatialPose == null) "" else
        activity.getString(R.string.geospatial_pose,
                           cameraGeospatialPose.latitude,
                           cameraGeospatialPose.longitude,
                           cameraGeospatialPose.horizontalAccuracy,
                           cameraGeospatialPose.altitude,
                           cameraGeospatialPose.verticalAccuracy,
                           cameraGeospatialPose.heading,
                           cameraGeospatialPose.headingAccuracy)
      statusText.text = activity.resources.getString(R.string.earth_state,
                                                     earth.earthState.toString(),
                                                     earth.trackingState.toString(),
                                                     poseText)
    }
  }

    fun GetRoot(): View{
        if (fi)
        {
            fi = false
            return View.inflate(activity, R.layout.activity_main, null)
        }
        else
            return activity.view.root
    }

  override fun onResume(owner: LifecycleOwner) {
      try {
          surfaceView.onResume()
      }
      catch (e: Exception){
          Log.e("FCK!", "OnResume")
      }
    //surfaceView.onResume()
  }

  override fun onPause(owner: LifecycleOwner) {
    surfaceView.onPause()
  }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        ScrStatus.gravity = Gravity.CENTER
        buttonMenu.setOnClickListener {
            activity.lifecycle.addObserver(activity.menuView)
            activity.lifecycle.removeObserver(activity.view)
            activity.lifecycle.removeObserver(activity.renderer)
            activity.lifecycle.removeObserver(activity.arCoreSessionHelper)
            activity.setContentView(activity.menuView.root)
        }

        buttonToAR.setOnClickListener { ModelFlag = !ModelFlag }
        buttonAct.setOnClickListener {
            if (act == 1)
            {
                act = 2
            }
            else{
                act = 1
            }
        }
    }

    fun setStatus(string: String){
        statusvw.text = string
    }

}
