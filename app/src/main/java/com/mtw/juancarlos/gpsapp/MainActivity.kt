package com.mtw.juancarlos.gpsapp

import android.Manifest
import android.animation.AnimatorInflater
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        private val REQUEST_CHECK_SETTINGS = 1
        private val REQUEST_LOCATION_PERMISSION = 1
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var requestingLocationUpdates = false
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        animRotate.setTarget(imageview_android)
        progressBar.visibility = View.INVISIBLE

        button_location.setOnClickListener {
            if (checkPermission()) {
                Log.e("GPSANDROIDAPP", "Acceso Concedido")
                getLastLocation()
            } else {
                Log.e("GPSANDROIDAPP", "Acceso denegado")
            }
        }

        button_startTrack.setOnClickListener {
            if (checkPermission()) {
                if (!requestingLocationUpdates) {
                    startLocationUpdates()
                } else {
                    stopLocationUpdates()
                }
            } else {
                Log.e("GPSANDROIDAPP", "Acceso denegado")
            }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                onLocationChanged(locationResult.lastLocation)
            }
        }
    }

    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION)
            return false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // related task you need to do.
                    //getLastLocation()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this,
                            "Acceso al GPS denegado",
                            Toast.LENGTH_SHORT).show()
                }
                return
            }
        // Add other 'when' lines to check for other
        // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun getLastLocation() {
        try {
            fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        Log.e("GPSANDROIDAPP", "OnSucessListener lastLocation")
                        // Got last known location. In some rare situations this can be null.
                        onLocationChanged(location)
                    }
                    .addOnFailureListener {
                        Log.e("GPSANDROIDAPP", "lastLoc: " + it.message)
                        Toast.makeText(this@MainActivity, "lastLoc:" + it.message, Toast.LENGTH_LONG)
                    }
        } catch (e: SecurityException) {
            Log.e("GPSANDROIDAPP", "SecEx " + e.message)
            Toast.makeText(this@MainActivity, "secex:" + e.message, Toast.LENGTH_LONG)
        } finally {
            progressBar.visibility = View.INVISIBLE
        }
    }

    private fun onLocationChanged(location: Location?) {
        if (location != null) {
            textview_location.text = getString(R.string.location_text,
                    location?.latitude,
                    location?.longitude,
                    location?.time
            )
        } else {
            textview_location.text = "No se recupero la ubicacion"
        }
    }

    private val animRotate by lazy {
        AnimatorInflater.loadAnimator(this, R.animator.rotate)
    }

    private fun startLocationUpdates() {
        progressBar.visibility = View.VISIBLE
        animRotate.start()
        requestingLocationUpdates = true
        button_startTrack.text = "Detener"
        textview_location.text = "Localizando..."
        val locationRequest = LocationRequest().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            Log.e("GPSANDROIDAPP", "OnSucessListener Task")
            try {
                //fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null)

            } catch (e: SecurityException) {
                Log.e("GPSANDROIDAPP", "SecEx " + e.message)
                Toast.makeText(this@MainActivity, "secex:" + e.message, Toast.LENGTH_LONG)
                progressBar.visibility = View.INVISIBLE
            }
        }
        task.addOnFailureListener { exception ->
            progressBar.visibility = View.INVISIBLE
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    Log.e("GPSANDROIDAPP", "OnFailureListener")

                    try {
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

                    } catch (e: SecurityException) {
                        Log.e("GPSANDROIDAPP", "SecEx " + e.message)
                        Toast.makeText(this@MainActivity, "secex:" + e.message, Toast.LENGTH_LONG)
                        progressBar.visibility = View.INVISIBLE
                    }

                    exception.startResolutionForResult(this@MainActivity,
                            REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        if (requestingLocationUpdates) {
            requestingLocationUpdates = false
            progressBar.visibility = View.INVISIBLE
            button_startTrack.text = "Rastrear"
            textview_location.text = "Presiona el boton para obtener la ultima ubicacion"
            animRotate.end()
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.e("GPSANDROIDAPP", "ACCESS GRANTED")
                    startLocationUpdates()
                }
                return
            }
        }
    }

    override fun onResume() {
        if (requestingLocationUpdates) startLocationUpdates()
        super.onResume()
    }

    override fun onPause() {
        if (requestingLocationUpdates) {
            stopLocationUpdates()
            requestingLocationUpdates = false
        }
        super.onPause()
    }
}
