
package com.google.ar.core.codelabs.hellogeospatial.helpers

import android.opengl.GLSurfaceView
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Config
import com.google.ar.core.Earth
import com.google.ar.core.GeospatialPose
import com.google.ar.core.Session
import com.google.ar.core.codelabs.hellogeospatial.HelloGeoActivity
import com.google.ar.core.codelabs.hellogeospatial.HelloGeoRenderer
import com.google.ar.core.codelabs.hellogeospatial.R
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
var fi2 = true
var fi3 = true
/** Contains UI elements for Menu. */
class MenuView(val activity: HelloGeoActivity) : DefaultLifecycleObserver {
    val root = View.inflate(activity, R.layout.menu, null)
    val toSettings = root.findViewById<Button>(R.id.buttonSettings)
    val toActivity = root.findViewById<Button>(R.id.buttonToActivity)
    val snackbarHelper = SnackbarHelper()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        toSettings.setOnClickListener {
            activity.settingsView = SettingsView(activity)
            activity.lifecycle.addObserver(activity.settingsView)
            activity.lifecycle.removeObserver(activity.menuView)
            activity.setContentView(activity.settingsView.root)
        }

        toActivity.setOnClickListener {
            activity.lifecycle.removeObserver(activity.menuView)
            if (fi3){
                fi3 = false
                // Setup ARCore session lifecycle helper and configuration.
                activity.arCoreSessionHelper = ARCoreSessionLifecycleHelper(activity)
                // If Session creation or Session.resume() fails, display a message and log detailed
                // information.
                activity.arCoreSessionHelper.exceptionCallback =
                    { exception ->
                        val message =
                            when (exception) {
                                is UnavailableUserDeclinedInstallationException ->
                                    "Please install Google Play Services for AR"
                                is UnavailableApkTooOldException -> "Please update ARCore"
                                is UnavailableSdkTooOldException -> "Please update this app"
                                is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                                is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                                else -> "Failed to create AR session: $exception"
                            }
                        Log.e(HelloGeoActivity.TAG, "ARCore threw an exception", exception)
                        activity.view.snackbarHelper.showError(activity, message)
                    }

                // Configure session features.
                activity.arCoreSessionHelper.beforeSessionResume = ::configureSession
                activity.lifecycle.addObserver(activity.arCoreSessionHelper)

                // Set up the Hello AR renderer.
                activity.renderer = HelloGeoRenderer(activity)
                activity.lifecycle.addObserver(activity.renderer)

                // Set up Hello AR UI.
                activity.view = HelloGeoView(activity)
                activity.lifecycle.addObserver(activity.view)
                activity.lifecycle.removeObserver(activity.view)
                activity.setContentView(activity.view.root)

                // Sets up an example renderer using our HelloGeoRenderer.
                if (activity.view.surfaceView != null)
                //SampleRender(activity.view.surfaceView, activity.renderer, assets)
                //if (fi2)
                //{
                    activity.SR(activity.view.surfaceView, activity.renderer)
                //fi2 = false
                //}

                else
                    Log.e("Fuck", "surfaceView is null")
                activity.view.buttonAct.visibility = View.INVISIBLE
                //view.statusvw.visibility = View.INVISIBLE
                //view.buttonToAR.visibility = View.VISIBLE
                activity.view.ScrStatus.visibility = View.VISIBLE
            }
            else{
                activity.arCoreSessionHelper.onResume(activity)
                activity.lifecycle.addObserver(activity.arCoreSessionHelper)

                activity.lifecycle.addObserver(activity.renderer)
                activity.lifecycle.addObserver(activity.view)
                activity.setContentView(activity.view.root)
                //activity.finish()
                //activity.recreate()
            }
        }

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
}
