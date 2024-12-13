
package com.google.ar.core.codelabs.hellogeospatial.helpers

import android.opengl.GLSurfaceView
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Earth
import com.google.ar.core.GeospatialPose
import com.google.ar.core.codelabs.hellogeospatial.AlwaysDo
import com.google.ar.core.codelabs.hellogeospatial.HelloGeoActivity
import com.google.ar.core.codelabs.hellogeospatial.MaxDist
import com.google.ar.core.codelabs.hellogeospatial.ModelFlag
import com.google.ar.core.codelabs.hellogeospatial.R
import com.google.ar.core.codelabs.hellogeospatial.VN
import com.google.ar.core.codelabs.hellogeospatial.alwdo
import com.google.ar.core.codelabs.hellogeospatial.isTank
import com.google.ar.core.codelabs.hellogeospatial.radAnc
import com.google.ar.core.codelabs.hellogeospatial.radDel
import com.google.ar.core.codelabs.hellogeospatial.sharedpreferences
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper

/** Contains UI elements for Menu. */
class SettingsView(val activity: HelloGeoActivity) : DefaultLifecycleObserver {
    val root = View.inflate(activity, R.layout.settings, null)

    val maxDistET = root.findViewById<EditText>(R.id.ETRadiusAnc)
    val delRadET = root.findViewById<EditText>(R.id.ETRadiusDel)
    val CBIsTank = root.findViewById<CheckBox>(R.id.checkBoxIsTank)
    val CBADO = root.findViewById<CheckBox>(R.id.checkBoxCanAct)

    val toMenu = root.findViewById<Button>(R.id.setToMenu)
    val applyBtn = root.findViewById<Button>(R.id.buttonApply)

    val snackbarHelper = SnackbarHelper()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        activity.runOnUiThread {
            MaxDist = sharedpreferences.getInt(radAnc, 80).toDouble()
            VN = sharedpreferences.getInt(radDel, 8).toDouble()
            ModelFlag = sharedpreferences.getBoolean(isTank, false)
            AlwaysDo = sharedpreferences.getBoolean(alwdo, false)

            maxDistET.setText(MaxDist.toString())
            delRadET.setText(VN.toString())

            CBIsTank.isChecked = ModelFlag
            CBADO.isChecked = AlwaysDo
        }

        toMenu.setOnClickListener {
            activity.menuView = MenuView(activity)
            activity.lifecycle.addObserver(activity.menuView)
            activity.lifecycle.removeObserver(activity.settingsView)
            activity.setContentView(activity.menuView.root)
        }

        applyBtn.setOnClickListener {
            ApplyVals()
        }
    }

    fun ApplyVals(){
        val editor = sharedpreferences.edit()
        MaxDist = maxDistET.text.toString().toDouble()
        VN = delRadET.text.toString().toDouble()
        ModelFlag = CBIsTank.isChecked
        AlwaysDo = CBADO.isChecked

        editor.putInt(radAnc, MaxDist.toInt())
        editor.putInt(radDel, VN.toInt())
        editor.putBoolean(isTank,ModelFlag)
        editor.putBoolean(alwdo, AlwaysDo)
        editor.apply()
    }
}
