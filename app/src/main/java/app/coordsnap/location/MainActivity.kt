package app.coordsnap.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            MaterialTheme(colorScheme = ExactLocationDarkColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationClipboardScreen(fusedLocationClient)
                }
            }
        }
    }
}

private val Amber = Color(0xFFF4B84A)
private val DeepNavy = Color(0xFF0B1620)
private val Charcoal = Color(0xFF111820)
private val Panel = Color(0xFF182330)
private val PanelHigh = Color(0xFF223040)
private val TextPrimary = Color(0xFFF3F0E8)
private val TextSecondary = Color(0xFFB9C0C8)

private val ExactLocationDarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF231806),
    primaryContainer = Color(0xFF5A3D0D),
    onPrimaryContainer = Color(0xFFFFE4A8),
    secondary = Color(0xFF9FC4D2),
    onSecondary = Color(0xFF0B2028),
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = Charcoal,
    onSurface = TextPrimary,
    surfaceVariant = Panel,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF68727D),
    outlineVariant = Color(0xFF334252),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private enum class CoordinateFormat(val label: String) {
    WGS84("WGS84 latitude / longitude"),
    NZTM2000("NZTM2000 / EPSG:2193"),
    BOTH("Both formats")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationClipboardScreen(fusedLocationClient: FusedLocationProviderClient) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(CoordinateFormat.BOTH) }
    var expanded by remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready") }
    var isLoading by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(hasAnyLocationPermission(context)) }
    var latestLocation by remember { mutableStateOf<Location?>(null) }

    fun hasLocationPermission(): Boolean = hasAnyLocationPermission(context)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            permissionGranted = true
            status = "Waiting for location accuracy"
        } else {
            permissionGranted = false
            status = "Location permission denied"
        }
    }

    DisposableEffect(permissionGranted) {
        if (!permissionGranted) {
            onDispose { }
        } else {
            val callback = startLiveLocationUpdates(
                fusedLocationClient = fusedLocationClient,
                onLocation = { location ->
                    latestLocation = location
                    status = "Live location ready"
                },
                onError = { error ->
                    status = error.localizedMessage ?: "Could not update location"
                }
            )

            onDispose {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "CoordSnap",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AccuracyIndicator(
            location = latestLocation,
            permissionGranted = permissionGranted
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = locationName,
            onValueChange = { locationName = it },
            singleLine = true,
            label = { Text("Location name / ID") },
            placeholder = { Text("BH01-PZ") }
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                value = selectedFormat.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Coordinate format") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                CoordinateFormat.entries.forEach { format ->
                    DropdownMenuItem(
                        text = { Text(format.label) },
                        onClick = {
                            selectedFormat = format
                            expanded = false
                        }
                    )
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            onClick = {
                if (hasLocationPermission()) {
                    permissionGranted = true
                    captureLocation(
                        context = context,
                        fusedLocationClient = fusedLocationClient,
                        selectedFormat = selectedFormat,
                        locationName = locationName,
                        latestLocation = latestLocation,
                        setLoading = { isLoading = it },
                        setStatus = { status = it },
                        setMessage = { message = it }
                    )
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        ) {
            Text(if (isLoading) "Getting location..." else "Capture")
        }

        if (message.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PanelHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = {
                            copyToClipboard(context, message)
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Copy")
                        }
                        OutlinedButton(onClick = { shareText(context, message) }) {
                            Text("Share")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Uses the device's current GPS/network fix. NZTM2000 is calculated offline from WGS84 coordinates.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun hasAnyLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

@Composable
private fun AccuracyIndicator(
    location: Location?,
    permissionGranted: Boolean
) {
    val accuracy = location?.takeIf { it.hasAccuracy() }?.accuracy
    val accuracyText = when {
        !permissionGranted -> "Accuracy: enable location"
        accuracy == null -> "Accuracy: waiting for fix"
        accuracy < 1f -> "Accuracy: +/- %.2f m".format(Locale.US, accuracy)
        else -> "Accuracy: +/- %.1f m".format(Locale.US, accuracy)
    }
    val indicatorColor = accuracyColor(accuracy)
    val fillFraction = accuracyFillFraction(accuracy)
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(shape)
            .background(Color(0xFF2C3742))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fillFraction)
                .background(indicatorColor)
        )
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = accuracyText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF101820)
        )
    }
}

@SuppressLint("MissingPermission")
private fun startLiveLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    onLocation: (Location) -> Unit,
    onError: (Exception) -> Unit
): LocationCallback {
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000)
        .setMinUpdateIntervalMillis(500)
        .setMaxUpdateDelayMillis(1_000)
        .setWaitForAccurateLocation(true)
        .build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(onLocation)
        }
    }

    fusedLocationClient.requestLocationUpdates(
        request,
        callback,
        Looper.getMainLooper()
    ).addOnFailureListener(onError)

    return callback
}

private fun accuracyFillFraction(accuracy: Float?): Float {
    if (accuracy == null) return 0.35f
    return ((10f - accuracy.coerceIn(1f, 10f)) / 9f)
        .coerceIn(0f, 1f)
        .coerceAtLeast(0.12f)
}

private fun accuracyColor(accuracy: Float?): Color {
    if (accuracy == null) return Amber

    val red = Color(0xFFE5534B)
    val yellow = Color(0xFFF4B84A)
    val green = Color(0xFF45C46A)
    val clamped = accuracy.coerceIn(1f, 10f)

    return if (clamped <= 5.5f) {
        val fraction = (5.5f - clamped) / 4.5f
        interpolateColor(yellow, green, fraction)
    } else {
        val fraction = (10f - clamped) / 4.5f
        interpolateColor(red, yellow, fraction)
    }
}

private fun interpolateColor(start: Color, end: Color, fraction: Float): Color {
    val amount = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * amount,
        green = start.green + (end.green - start.green) * amount,
        blue = start.blue + (end.blue - start.blue) * amount,
        alpha = start.alpha + (end.alpha - start.alpha) * amount
    )
}

@SuppressLint("MissingPermission")
private fun captureLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    selectedFormat: CoordinateFormat,
    locationName: String,
    latestLocation: Location?,
    setLoading: (Boolean) -> Unit,
    setStatus: (String) -> Unit,
    setMessage: (String) -> Unit
) {
    if (!hasAnyLocationPermission(context)) {
        setStatus("Location permission required")
        return
    }

    if (latestLocation != null) {
        val text = formatLocation(latestLocation, selectedFormat, locationName)
        copyToClipboard(context, text)
        setMessage(text)
        setStatus("Copied current location")
        Toast.makeText(context, "Location copied", Toast.LENGTH_SHORT).show()
        return
    }

    setLoading(true)
    setStatus("Requesting current location")

    val request = CurrentLocationRequest.Builder()
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMaxUpdateAgeMillis(5_000)
        .setGranularity(com.google.android.gms.location.Granularity.GRANULARITY_FINE)
        .build()

    val cancellationTokenSource = CancellationTokenSource()
    fusedLocationClient.getCurrentLocation(request, cancellationTokenSource.token)
        .addOnSuccessListener { location ->
            setLoading(false)
            if (location == null) {
                setStatus("No location fix available")
            } else {
                val text = formatLocation(location, selectedFormat, locationName)
                copyToClipboard(context, text)
                setMessage(text)
                setStatus("Copied current location")
                Toast.makeText(context, "Location copied", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { error ->
            setLoading(false)
            setStatus(error.localizedMessage ?: "Could not get location")
        }
}

private fun formatLocation(
    location: Location,
    selectedFormat: CoordinateFormat,
    locationName: String
): String {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date())
    val title = locationName.trim().ifBlank { "Location" }
    val wgs84 = "WGS84: %.8f, %.8f".format(Locale.US, location.latitude, location.longitude)
    val mapsUrl = "https://www.google.com/maps?q=%.8f,%.8f".format(
        Locale.US,
        location.latitude,
        location.longitude
    )
    val nztm = latLonToNztm(location.latitude, location.longitude)
    val nztmText = "NZTM2000: E %.3f, N %.3f".format(Locale.US, nztm.easting, nztm.northing)
    val accuracy = if (location.hasAccuracy()) {
        "Accuracy: +/- %.1f m".format(Locale.US, location.accuracy)
    } else {
        "Accuracy: unknown"
    }

    val coordinates = when (selectedFormat) {
        CoordinateFormat.WGS84 -> wgs84
        CoordinateFormat.NZTM2000 -> nztmText
        CoordinateFormat.BOTH -> "$wgs84\n$nztmText"
    }

    return "Location: $title\n$coordinates\nMap: $mapsUrl\n$accuracy\nCaptured: $timestamp"
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("CoordSnap location", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, "Share location"))
}

private data class NztmCoordinate(val easting: Double, val northing: Double)

private fun latLonToNztm(latitude: Double, longitude: Double): NztmCoordinate {
    val semiMajorAxis = 6378137.0
    val inverseFlattening = 298.257222101
    val flattening = 1.0 / inverseFlattening
    val eccentricitySquared = 2 * flattening - flattening * flattening
    val secondEccentricitySquared = eccentricitySquared / (1 - eccentricitySquared)
    val centralMeridian = 173.0 * PI / 180.0
    val scaleFactor = 0.9996
    val falseEasting = 1_600_000.0
    val falseNorthing = 10_000_000.0

    val lat = latitude * PI / 180.0
    val lon = longitude * PI / 180.0
    val sinLat = sin(lat)
    val cosLat = cos(lat)
    val tanLat = tan(lat)

    val e4 = eccentricitySquared.pow(2)
    val e6 = eccentricitySquared.pow(3)
    val meridionalArc = semiMajorAxis * (
        (1 - eccentricitySquared / 4 - 3 * e4 / 64 - 5 * e6 / 256) * lat -
            (3 * eccentricitySquared / 8 + 3 * e4 / 32 + 45 * e6 / 1024) * sin(2 * lat) +
            (15 * e4 / 256 + 45 * e6 / 1024) * sin(4 * lat) -
            (35 * e6 / 3072) * sin(6 * lat)
        )

    val radiusPrimeVertical = semiMajorAxis / sqrt(1 - eccentricitySquared * sinLat.pow(2))
    val tangentSquared = tanLat.pow(2)
    val etaSquared = secondEccentricitySquared * cosLat.pow(2)
    val a = cosLat * (lon - centralMeridian)

    val easting = falseEasting + scaleFactor * radiusPrimeVertical * (
        a +
            (1 - tangentSquared + etaSquared) * a.pow(3) / 6 +
            (
                5 - 18 * tangentSquared + tangentSquared.pow(2) +
                    72 * etaSquared - 58 * secondEccentricitySquared
                ) * a.pow(5) / 120
        )

    val northing = falseNorthing + scaleFactor * (
        meridionalArc + radiusPrimeVertical * tanLat * (
            a.pow(2) / 2 +
                (5 - tangentSquared + 9 * etaSquared + 4 * etaSquared.pow(2)) * a.pow(4) / 24 +
                (
                    61 - 58 * tangentSquared + tangentSquared.pow(2) +
                        600 * etaSquared - 330 * secondEccentricitySquared
                    ) * a.pow(6) / 720
            )
        )

    return NztmCoordinate(easting = easting, northing = northing)
}
