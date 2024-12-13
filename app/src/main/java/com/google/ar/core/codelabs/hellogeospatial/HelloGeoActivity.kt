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
package com.google.ar.core.codelabs.hellogeospatial

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.codelabs.hellogeospatial.helpers.ARCoreSessionLifecycleHelper
import com.google.ar.core.codelabs.hellogeospatial.helpers.GeoPermissionsHelper
import com.google.ar.core.codelabs.hellogeospatial.helpers.HelloGeoView
import com.google.ar.core.codelabs.hellogeospatial.helpers.MenuView
import com.google.ar.core.codelabs.hellogeospatial.helpers.SettingsView
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

lateinit var PosDataList: MutableList<PosData>

var VN = 8.0
var MaxDist = 80.0
var ModelFlag = true
var AlwaysDo = false

class HelloGeoActivity : AppCompatActivity() {
  companion object {
    const val TAG = "HelloGeoActivity"
  }

  lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper
  lateinit var view: HelloGeoView
  lateinit var renderer: HelloGeoRenderer

  lateinit var menuView: MenuView
  lateinit var settingsView: SettingsView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // getting the data which is stored in shared preferences.
    sharedpreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

    VN = sharedpreferences.getInt(radDel, 8).toDouble()
    MaxDist = sharedpreferences.getInt(radAnc, 80).toDouble()
    ModelFlag = sharedpreferences.getBoolean(isTank, false)
    AlwaysDo = sharedpreferences.getBoolean(alwdo, false)

    val json = sharedpreferences.getString(list, null)
    if (json != null){
      // below line is to get the type of our array list.
      val type: Type = object : TypeToken<ArrayList<PosData?>?>() {}.type

      var datalist = gson.fromJson<Any>(json, type) as ArrayList<PosData>
      // checking below if the array list is empty or not
      if (datalist == null) {
        // if the array list is empty
        // creating a new array list.
        datalist = ArrayList()
      }
      PosDataList = datalist
    }
    else
      PosDataList = mutableListOf<PosData>()


    menuView = MenuView(this)
    lifecycle.addObserver(menuView)
    setContentView(menuView.root)

  }

  fun SR(v:GLSurfaceView, r: HelloGeoRenderer){
    SampleRender(view.surfaceView, renderer, assets)
  }

  // Configure the session, setting the desired options according to your usecase.
  fun configureSession(session: Session) {
    session.configure(
      session.config.apply {
        // Enable Geospatial Mode.
        geospatialMode = Config.GeospatialMode.ENABLED
      }
    )
  }

  fun initAnchors(){

  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    results: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, results)
    if (!GeoPermissionsHelper.hasGeoPermissions(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera and location permissions are needed to run this application", Toast.LENGTH_LONG)
        .show()
      if (!GeoPermissionsHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        GeoPermissionsHelper.launchPermissionSettings(this)
      }
      finish()
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
  }
}
