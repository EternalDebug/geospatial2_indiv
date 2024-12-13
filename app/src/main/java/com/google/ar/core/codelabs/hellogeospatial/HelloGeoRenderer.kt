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
import android.content.SharedPreferences
import android.graphics.Color
import android.opengl.Matrix
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Anchor
import com.google.ar.core.Earth
import com.google.ar.core.TrackingState
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper
import com.google.ar.core.examples.java.common.samplerender.Framebuffer
import com.google.ar.core.examples.java.common.samplerender.Mesh
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.Shader
import com.google.ar.core.examples.java.common.samplerender.Texture
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.sceneform.math.Vector3
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.lang.reflect.Type
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

const val SHARED_PREFS = "shared_prefs"
const val list = "shared_list"
const val isTank = "shared_tank"
const val radAnc = "radius_anchor"
const val radDel = "radius_deletion"

data class PosData(var lat:Double, var long:Double, var alt:Double)

var AncList = mutableListOf<Anchor>()

// shared preferences.
lateinit var sharedpreferences: SharedPreferences

// creating a variable for gson.
val gson = Gson()
var ancInited = false
var needArrow = true
lateinit var  nearestAnc: Anchor
var smallestDist = Double.MAX_VALUE
var act = 1

class HelloGeoRenderer(val activity: HelloGeoActivity) :
  SampleRender.Renderer, DefaultLifecycleObserver {
  //<editor-fold desc="ARCore initialization" defaultstate="collapsed">
  companion object {
    val TAG = "HelloGeoRenderer"

    private val Z_NEAR = 0.1f
    private val Z_FAR = 1000f
  }

  lateinit var backgroundRenderer: BackgroundRenderer
  lateinit var virtualSceneFramebuffer: Framebuffer
  var hasSetTextureNames = false

  // Virtual object (ARCore pawn)
  lateinit var virtualObjectMesh: Mesh
  lateinit var virtualObjectShader: Shader
  lateinit var virtualObjectTexture: Texture

  lateinit var virtualObjectMesh2: Mesh
  lateinit var virtualObjectTexture2: Texture
  lateinit var virtualObjectShader2: Shader

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  val modelMatrix = FloatArray(16)
  val viewMatrix = FloatArray(16)
  val projectionMatrix = FloatArray(16)
  val modelViewMatrix = FloatArray(16) // view x model

  val modelViewProjectionMatrix = FloatArray(16) // projection x view x model

  val session
    get() = activity.arCoreSessionHelper.session

  val displayRotationHelper = DisplayRotationHelper(activity)
  val trackingStateHelper = TrackingStateHelper(activity)

  override fun onResume(owner: LifecycleOwner) {
    displayRotationHelper.onResume()
    hasSetTextureNames = false
  }

  override fun onPause(owner: LifecycleOwner) {
    displayRotationHelper.onPause()
  }

  override fun onSurfaceCreated(render: SampleRender) {
    // Prepare the rendering objects.
    // This involves reading shaders and 3D model files, so may throw an IOException.
    try {
      backgroundRenderer = BackgroundRenderer(render)
      virtualSceneFramebuffer = Framebuffer(render, /*width=*/ 1, /*height=*/ 1)

      // Virtual object to render (Geospatial Marker)
      virtualObjectTexture =
        Texture.createFromAsset(
          render,
          //"models/spatial_marker_baked.png",
          "models/tank.png",
          Texture.WrapMode.CLAMP_TO_EDGE,
          Texture.ColorFormat.SRGB
        )

      virtualObjectTexture2 =
        Texture.createFromAsset(
          render,
          //"models/spatial_marker_baked.png",
          "models/spatial_marker_baked.png",
          Texture.WrapMode.CLAMP_TO_EDGE,
          Texture.ColorFormat.SRGB
        )

      virtualObjectMesh = Mesh.createFromAsset(render, "models/tank.obj");

      virtualObjectMesh2 = Mesh.createFromAsset(render, "models/geospatial_marker.obj");

      virtualObjectShader =
        Shader.createFromAssets(
          render,
          "shaders/ar_unlit_object.vert",
          "shaders/ar_unlit_object.frag",
          /*defines=*/ null)
          .setTexture("u_Texture", virtualObjectTexture)

      virtualObjectShader2 =
        Shader.createFromAssets(
          render,
          "shaders/ar_unlit_object.vert",
          "shaders/ar_unlit_object.frag",
          /*defines=*/ null)
          .setTexture("u_Texture", virtualObjectTexture2)

      backgroundRenderer.setUseDepthVisualization(render, false)
      backgroundRenderer.setUseOcclusion(render, false)
    } catch (e: IOException) {
      Log.e(TAG, "Failed to read a required asset file", e)
      showError("Failed to read a required asset file: $e")
    }
  }

  override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
    displayRotationHelper.onSurfaceChanged(width, height)
    virtualSceneFramebuffer.resize(width, height)
  }
  //</editor-fold>

  fun initAnc(earth: Earth?){

    val qx = 0f
    val qy = 0f
    val qz = 0f
    val qw = 1f

    AncList = mutableListOf<Anchor>()

    for (elem in PosDataList){
      if (earth?.trackingState == TrackingState.TRACKING) {
        if (distance(LatLng(elem.lat, elem.long), earth) <= MaxDist){
          earthAnchor = earth.createAnchor(elem.lat, elem.long, elem.alt, qx, qy, qz, qw)
          var dst = distance(LatLng(elem.lat, elem.long), earth)
          earthAnchor?.let {
            AncList.add(it)}
          if (dst < smallestDist){
            smallestDist = dst
            AncList.last().let {
              nearestAnc = it
            }
            //nearestAnc = AncList.last()
          }
        }
      }
    }

    drawOnMap()
    activity.runOnUiThread {
      activity.view.ScrStatus.text = "Инициализация данных завершена"


      activity.lifecycleScope.launch{
        kotlinx.coroutines.delay(3000)
        activity.view.ScrStatus.visibility = View.INVISIBLE
        activity.view.surfaceView.visibility = View.VISIBLE
      }
    }
  }

  fun drawOnMap(){
    activity.runOnUiThread {
      for (pos in PosDataList){
      activity.view.mapView?.addMarker(Color.argb(255, 150,150,150))
      activity.view.mapView?.earthMarkers?.last()?.apply {
        position = LatLng(pos.lat, pos.long)
        isVisible = true
      }
    }}

  }

  override fun onStart(owner: LifecycleOwner) {
    super.onStart(owner)

  }

  fun ClearAll(){
    AncList = mutableListOf<Anchor>()
    PosDataList = mutableListOf<PosData>()
    activity.view.mapView?.clearMarkers()

    val json = gson.toJson(PosDataList.toList())
    val editor = sharedpreferences.edit()

    editor.putString(list, json)
    editor.apply()

    smallestDist = Double.MAX_VALUE
  }

  fun GetNearestAnc(earth: Earth?): Double{
    var res = Double.MAX_VALUE
    AncList.let {
      for (anc in it){
        if (earth != null)
        {
          val ind = AncList.indexOf(anc)
          val dst = distance(LatLng(PosDataList[ind].lat, PosDataList[ind].long), earth)
          if (res > dst){
            res = dst
            //anc.let {
            //  nearestAnc = it
            //}
            nearestAnc = anc
          }
        }
      }
    }

    return res
  }

  override fun onDrawFrame(render: SampleRender) {
    val session = session ?: return

    //<editor-fold desc="ARCore frame boilerplate" defaultstate="collapsed">
    // Texture names should only be set once on a GL thread unless they change. This is done during
    // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
    // initialized during the execution of onSurfaceCreated.
    if (!hasSetTextureNames) {
      session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
      hasSetTextureNames = true
    }

    // -- Update per-frame state

    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    displayRotationHelper.updateSessionIfNeeded(session)

    // Obtain the current frame from ARSession. When the configuration is set to
    // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
    // camera framerate.
    val frame =
      try {
        try {
          session.update()
        }
        catch (e: Exception){
          Log.e("SHIT", "Something went wrong... again", e)
          return
        }
      } catch (e: CameraNotAvailableException) {
        Log.e(TAG, "Camera not available during onDrawFrame", e)
        showError("Camera not available. Try restarting the app.")
        return
      }

    val camera = frame.camera

    // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
    // used to draw the background camera image.
    backgroundRenderer.updateDisplayGeometry(frame)

    // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
    trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

    // -- Draw background
    if (frame.timestamp != 0L) {
      // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
      // drawing possible leftover data from previous sessions if the texture is reused.
      backgroundRenderer.drawBackground(render)
    }

    // If not tracking, don't draw 3D objects.
    if (camera.trackingState == TrackingState.PAUSED) {
      return
    }

    // Get projection matrix.
    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)

    // Get camera matrix and draw.
    camera.getViewMatrix(viewMatrix, 0)

    render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)
    //</editor-fold>

    // TODO: Obtain Geospatial information and display it on the map.
    val earth = session.earth

    if (!ancInited){

      initAnc(earth)
      ancInited = true
    }

    if (earth?.trackingState == TrackingState.TRACKING) {
      // TODO: the Earth object may be used here.
      val cameraGeospatialPose = earth.cameraGeospatialPose
      activity.view.mapView?.updateMapPosition(
        latitude = cameraGeospatialPose.latitude,
        longitude = cameraGeospatialPose.longitude,
        heading = cameraGeospatialPose.heading
      )

      activity.view.updateStatusText(earth, earth.cameraGeospatialPose)


      activity.view.button5_clicker.setOnClickListener{
        onMapClick(LatLng(earth.cameraGeospatialPose.latitude, earth.cameraGeospatialPose.longitude))
      }
    }

    activity.view.button4_clicker.setOnClickListener{
      ClearAll()
    }
    AncList.let {
      if (it.size > 0){
        smallestDist = GetNearestAnc(earth)
      }
    }

    for (elem in AncList){
      earthAnchor = elem
      earthAnchor?.let{
        if (it == nearestAnc)
        {
          if (!isAnchorVisible(it, earth)){
            activity.runOnUiThread{
              activity.view.txt.visibility = View.VISIBLE
              activity.view.buttonAct.visibility = View.INVISIBLE
            }
          }
          else{
            activity.runOnUiThread{
              activity.view.txt.visibility = View.INVISIBLE
              activity.view.buttonAct.visibility = View.VISIBLE
            }
          }
          render.renderCompassAtAnchor(it, act)
        }
        else
          render.renderCompassAtAnchor(it)
      }
    }

    if (PosDataList.size == 0){
      activity.runOnUiThread{activity.view.txt.visibility = View.INVISIBLE}
    }

    //if (needArrow && AncList.size > 0){
    //  activity.runOnUiThread{
    //    nearestAnc.let {
          ////val ts = it.trackingState

    //      if (isAnchorVisible(nearestAnc, earth)){
    //        activity.view.txt.visibility = View.VISIBLE
            ////Log.e("NoTrack",ts.toString())
    //      } else{
    //        activity.view.txt.visibility = View.INVISIBLE
            ////Log.e("Track",ts.toString())
    //      }
    //    }

    //  }
    //}

    // Compose the virtual scene with the background.
    backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)
  }

  var earthAnchor: Anchor? = null

  fun onMapClick(latLng: LatLng) {
    // TODO: place an anchor at the given position.
    val earth = session?.earth ?: return
    if (earth.trackingState != TrackingState.TRACKING) {
      return
    }

    //earthAnchor?.detach()
    // Place the earth anchor at the same altitude as that of the camera to make it easier to view.
    val altitude = earth.cameraGeospatialPose.altitude - 1
// The rotation quaternion of the anchor in the East-Up-South (EUS) coordinate system.
    val qx = 0f
    val qy = 0f
    val qz = 0f
    val qw = 1f

    val newAnchor = PosData(latLng.latitude, latLng.longitude, altitude)
    val (flag, el) = CheckIsVeryNear(latLng)
    if (flag){
      PosDataList.remove(el)
      activity.view.mapView?.removeMarker(LatLng(el.lat, el.long))
      if (PosDataList.size == 0){
        ClearAll()
      }
      AncList = mutableListOf()
      initAnc(earth)
      return
    }

    PosDataList.add(newAnchor)
    val json = gson.toJson(PosDataList.toList())
    val editor = sharedpreferences.edit()

    editor.putString(list, json)
    editor.apply()

    earthAnchor =
      earth.createAnchor(latLng.latitude, latLng.longitude, altitude, qx, qy, qz, qw)

    //initAnc(earth)
    var dst = distance(latLng,earth)
    if (dst <= MaxDist){
      earthAnchor?.let {
        AncList.add(it)}
      if (dst < smallestDist){
        smallestDist = dst
        AncList.last().let {
          nearestAnc = it
        }
        //nearestAnc = AncList.last()
      }
    }



    activity.view.mapView?.addMarker(Color.argb(255, 150,150,150))
    activity.view.mapView?.earthMarkers?.last()?.apply {
      position = latLng
      isVisible = true
    }

  }

  private fun CheckIsVeryNear(latLng: LatLng, veryNear:Double = VN): Pair<Boolean, PosData> {
    val res = Pair(false, PosData(0.0,0.0,0.0))
    for (elem in PosDataList){
      //if (Math.abs(elem.lat - latLng.latitude) < veryNear && Math.abs(elem.long - latLng.longitude) < veryNear)
      //  return Pair(true, elem)
      if (distance2markers(LatLng(elem.lat, elem.long), latLng) < veryNear)
        return Pair(true, elem)
    }
    return res
  }

  fun distance2markers(latlng: LatLng, latlng2: LatLng): Double{
    val lat2 = latlng2.latitude
    val lon2 = latlng2.longitude
    val R = 6371000 // радиус Земли в метрах
    val dLat = Math.toRadians(lat2 - latlng.latitude)
    val dLon = Math.toRadians(lon2 - latlng.longitude)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latlng.latitude)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c // расстояние в метрах
  }

  fun distance(latlng: LatLng, earth: Earth): Double {
    val lat2 = earth.cameraGeospatialPose.latitude
    val lon2 = earth.cameraGeospatialPose.longitude
    val R = 6371000 // радиус Земли в метрах
    val dLat = Math.toRadians(lat2 - latlng.latitude)
    val dLon = Math.toRadians(lon2 - latlng.longitude)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latlng.latitude)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c // расстояние в метрах
  }

  private fun SampleRender.renderCompassAtAnchor(anchor: Anchor, action: Int = 0) {
    // Get the current pose of the Anchor in world space. The Anchor pose is updated
    // during calls to session.update() as ARCore refines its estimate of the world.
    anchor.pose.toMatrix(modelMatrix, 0)

    // Calculate model/view/projection matrices
    Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
    Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

    // Update shader properties and draw
    virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)

    virtualObjectShader.setInt("action", action)

    // Update shader properties and draw
    virtualObjectShader2.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)

    virtualObjectShader2.setInt("action", action)

    if (ModelFlag)
    {
      draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer)
    }
    else{
      draw(virtualObjectMesh2, virtualObjectShader2, virtualSceneFramebuffer)
    }

  }

  private fun isAnchorVisible(anchor: Anchor, earth: Earth?): Boolean {
    // Получаем позу якоря
    val anchorPose = anchor.pose
    // Получаем кватернион якоря
    val anchorQuaternion = anchorPose.rotationQuaternion // [x, y, z, w]
    // Извлекаем компоненты кватерниона
    val (x, y, z, w) = anchorQuaternion
    // Вычисляем матрицу вращения из кватерниона
    val heading = atan2(2.0 * (y * w + x * z), (w * w + x * x - y * y - z * z).toDouble())
    // Переводим радианы в градусы
    val headingDegrees = Math.toDegrees(heading)
    // Получаем позу камеры
    val cameraPose = earth?.cameraGeospatialPose ?: return false
    // Получаем направление heading камеры (угол в градусах)
    val cameraHeading = cameraPose.heading // Угол ориентации камеры
    return abs((headingDegrees + 360 - 120) % 360 - cameraHeading) <= 60
  }

  private fun showError(errorMessage: String) =
    activity.view.snackbarHelper.showError(activity, errorMessage)

}
