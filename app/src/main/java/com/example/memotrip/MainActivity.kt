package com.example.memotrip

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.memotrip.ui.theme.MemotripTheme
import com.google.android.gms.location.*
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MemoTripActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val locationList = mutableListOf<Location>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    locationList.add(location)
                }
            }
        }

        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            initializeContent()
            requestLocationUpdates()
        }
    }

    private fun initializeContent() {
        setContent {
            MemotripTheme {
                MemoTripApp(locationList)
                AddPhotoScreen()
            }
        }
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setMinUpdateIntervalMillis(5000)
                    .build()
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
            } catch (e: SecurityException) {
                // Gérer l'exception de sécurité ici, par exemple en affichant un message ou en journalisant l'erreur
                e.printStackTrace()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            initializeContent()
            requestLocationUpdates()
        } else {

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MemoTripApp(locationList: List<Location>) {
    var note by remember { mutableStateOf("") }

    val mapProperties = MapProperties(
        isMyLocationEnabled = true
    )
    val cameraPositionState = rememberCameraPositionState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("MemoTrip - Journal de Voyage Immersif") })
        }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Ajouter une note sur votre voyage :")
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(8.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {

            }) {
                Text("Ajouter Note")
            }

            Spacer(modifier = Modifier.height(16.dp))

            GoogleMap(
                modifier = Modifier.fillMaxHeight(),
                properties = mapProperties,
                cameraPositionState = cameraPositionState,
                onMapClick = { _ ->

                }
            ) {
                locationList.forEach { location ->
                    Marker(
                        state = com.google.maps.android.compose.MarkerState(
                            position = com.google.android.gms.maps.model.LatLng(
                                location.latitude,
                                location.longitude
                            )
                        ),
                        title = "Souvenir",
                        snippet = "Note ou photo"
                    )
                }
            }
        }
    }
}

@Composable
fun AddPhotoScreen() {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var capturedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val imageCaptureConfig = ImageCapture.Builder().build()
        imageCapture = imageCaptureConfig

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            context as AppCompatActivity,
            cameraSelector,
            imageCaptureConfig
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = {
            val photoFile = File(context.getExternalFilesDir(null), "captured_image.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            imageCapture?.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                capturedImage = bitmap.asImageBitmap()
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {

                    }
                }
            )
        }) {
            Text("Capturer une photo")
        }

        Spacer(modifier = Modifier.height(16.dp))
        capturedImage?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Image capturée",
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )
        }
    }
}
