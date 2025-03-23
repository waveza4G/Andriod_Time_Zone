package com.example.timezone

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.app.TimePickerDialog
import android.location.Address
import android.location.Geocoder
import android.media.MediaPlayer
import android.widget.Button
import java.text.SimpleDateFormat
import java.util.*

class TimeActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var editTextTime: EditText
    private lateinit var listViewFrom: Spinner
    private lateinit var listViewTo: Spinner
    private lateinit var countries: ArrayList<String>
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var textViewGPS: TextView  // Added TextView for GPS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time)

        // Initialize views
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        editTextTime = findViewById(R.id.editTextTime)
        listViewFrom = findViewById(R.id.listViewFrom)
        listViewTo = findViewById(R.id.listViewTo)
        textViewGPS = findViewById(R.id.textView7)  // Initialize the TextView

        mediaPlayer = MediaPlayer.create(this, R.raw.beep)  // "beep.mp3" in res/raw

        // Initialize countries list
        countries = arrayListOf("USA", "Canada", "Thailand", "Australia", "UK", "Japan")

        // Set up adapter for spinners
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        listViewFrom.adapter = adapter
        listViewTo.adapter = adapter

        // Set onClickListener for EditText (time picker)
        editTextTime.setOnClickListener {
            showTimePickerDialog()
        }

        // Set onClickListener for the "Change" button to show AlertDialog
        findViewById<Button>(R.id.button).setOnClickListener {
            playSound()
            val timeFrom = editTextTime.text.toString()
            val fromZone = listViewFrom.selectedItem.toString()
            val toZone = listViewTo.selectedItem.toString()

            // Convert the time based on selected time zones
            convertTime(timeFrom, fromZone, toZone)
        }

        // Request permissions if not granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLastLocation() // If permissions granted, get location
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun playSound() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.seekTo(0)  // Reset the position if it's already playing
        }
        mediaPlayer.start()  // Start the sound
    }

    // Convert time from one timezone to another
    private fun convertTime(time: String, fromZone: String, toZone: String) {
        try {
            // Parse the time from the EditText
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = sdf.parse(time)

            // Get the TimeZone for the "from" and "to" countries
            val fromTimeZone = TimeZone.getTimeZone(getTimeZoneId(fromZone))
            val toTimeZone = TimeZone.getTimeZone(getTimeZoneId(toZone))

            // Get the offset of both time zones in milliseconds
            val fromOffset = fromTimeZone.getOffset(date.time)
            val toOffset = toTimeZone.getOffset(date.time)

            // Calculate the time difference in milliseconds
            val timeDifference = toOffset - fromOffset

            // Use Calendar to apply the time difference
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.MILLISECOND, timeDifference)

            // Format the final time
            val convertedTime = sdf.format(calendar.time)

            // Show the converted time as a toast
            Toast.makeText(this, "Converted Time: $convertedTime", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error converting time", Toast.LENGTH_SHORT).show()
        }
    }

    // Get TimeZone ID from country name
    private fun getTimeZoneId(country: String): String {
        return when (country) {
            "USA" -> "America/New_York"
            "Canada" -> "America/Toronto"
            "Thailand" -> "Asia/Bangkok"
            "Australia" -> "Australia/Sydney"
            "UK" -> "Europe/London"
            "Japan" -> "Asia/Tokyo"
            else -> "GMT" // Default timezone
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        // Get the last known location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val country = getCountryFromLocation(latitude, longitude)

                    // Display location in TextView
                    textViewGPS.text = "GPS: Latitude: $latitude, Longitude: $longitude, Country: $country"
                    Log.d("GPS", "Latitude: $latitude, Longitude: $longitude, Country: $country")
                }
            }
    }

    // Dummy method to get country based on location (replace with actual geocoding method)
    private fun getCountryFromLocation(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

        return if (!addresses.isNullOrEmpty()) {
            addresses[0].countryName // Return the country name
        } else {
            "Unknown Country"
        }
    }

    // Show time picker dialog
    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val time = "$selectedHour:$selectedMinute"
            editTextTime.setText(time) // Set the selected time in EditText
        }, hour, minute, true)

        timePickerDialog.show()
    }

    // Handle permissions result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getLastLocation() // If granted, get location
        }
    }
}
