package com.satechdev.rafeeqapp.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.data.prayer.UserLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

/** A country in the manual location picker (lib/data/services/country_city_service.dart). */
data class Country(val name: String, val code: String)

/**
 * Finding and naming the user's location, as the Flutter app's
 * user_location_notifier.dart and country_city_service.dart: GPS and the
 * platform geocoder, and countries and cities from the same open dataset.
 */
object LocationService {
    private const val DATA = "https://cdn.jsdelivr.net/gh/srestre/world-countries-cities-db@main"

    private var countries: List<Country>? = null
    private val cities = mutableMapOf<String, List<String>>()

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** How a request for the phone's position ended, so the message can say why. */
    enum class Located(@androidx.annotation.StringRes val message: Int) {
        Updated(R.string.locationUpdated),
        NoPermission(R.string.locationPermissionDenied),
        Off(R.string.locationServicesOff),
        NoFix(R.string.locationUnavailable),
    }

    private val background = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var running: Deferred<Located>? = null

    /**
     * Saves the phone's current position as the automatic location, named by
     * the geocoder. It runs in the app's own scope, so it finishes and saves
     * even after the screen that asked is gone; a second ask joins the first.
     */
    fun locate(context: Context): Deferred<Located> {
        running?.takeIf { it.isActive }?.let { return it }
        val app = context.applicationContext
        return background.async {
            when {
                !hasPermission(app) -> Located.NoPermission
                !locationOn(app) -> Located.Off
                else -> {
                    val position = runCatching { currentPosition(app) }.getOrNull()
                    if (position == null) {
                        Located.NoFix
                    } else {
                        val address = reverseGeocode(app, position.latitude, position.longitude)
                        Settings.setLocation(
                            UserLocation(
                                latitude = position.latitude,
                                longitude = position.longitude,
                                city = address?.let(::cityOf),
                                country = address?.countryName,
                                countryCode = address?.countryCode,
                                source = "automatic",
                            ),
                        )
                        Located.Updated
                    }
                }
            }
        }.also { running = it }
    }

    /** [locate], waiting for it. */
    suspend fun updateFromGps(context: Context): Located = locate(context).await()

    private fun locationOn(context: Context): Boolean {
        val manager = context.getSystemService(LocationManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) manager.isLocationEnabled
        else listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).any { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    }

    /** Saves "[city], [country]" as the manual location. False when the geocoder cannot find it. */
    suspend fun setManual(context: Context, country: Country, city: String): Boolean {
        val match = forwardGeocode(context, "$city, ${country.name}") ?: return false
        Settings.setLocation(
            UserLocation(match.latitude, match.longitude, city, country.name, country.code, source = "manual"),
        )
        return true
    }

    /** All countries, by name. Throws when offline. */
    suspend fun countries(): List<Country> = countries ?: withContext(Dispatchers.IO) {
        val array = JSONArray(get("$DATA/metadata/countries.json"))
        List(array.length()) { array.optJSONObject(it) }
            .filterNotNull()
            .map { Country(it.optString("name"), it.optString("iso2").uppercase()) }
            .filter { it.name.isNotEmpty() && it.code.isNotEmpty() }
            .sortedBy { it.name }
            .also { countries = it }
    }

    /** The cities of a country (ISO-2 [code]), sorted and without repeats. Throws when offline. */
    suspend fun cities(code: String): List<String> = cities[code] ?: withContext(Dispatchers.IO) {
        require(Regex("^[A-Z]{2}$").matches(code)) { "Country code must be ISO-2." }
        val array = JSONArray(get("$DATA/flat-cities/$code.json"))
        List(array.length()) { array.opt(it) as? String }
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .toSortedSet()
            .toList()
            .also { cities[code] = it }
    }

    // ---------- Internals ----------

    /** The first of these the geocoder gives, as the Flutter app picks the city. */
    private fun cityOf(a: Address): String? =
        listOf(a.locality, a.subAdminArea, a.adminArea, a.subLocality).firstOrNull { !it.isNullOrEmpty() }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                error("Failed to load $url (HTTP ${connection.responseCode}).")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun fine(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /**
     * The phone's position: from Play services' fused location, as Flutter's
     * geolocator, which is quick indoors and works with approximate
     * permission; else from the platform's providers.
     */
    private suspend fun currentPosition(context: Context): Location? =
        (if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) fusedPosition(context) else null)
            ?: platformPosition(context)

    @SuppressLint("MissingPermission")
    private suspend fun fusedPosition(context: Context): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = CurrentLocationRequest.Builder()
            .setPriority(if (fine(context)) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            // A fix from the last few minutes is as good for prayer times and the Qibla.
            .setMaxUpdateAgeMillis(5 * 60_000L)
            .setDurationMillis(20_000L)
            .build()
        val cancel = CancellationTokenSource()
        val fresh = runCatching {
            withTimeoutOrNull(21_000) { client.getCurrentLocation(request, cancel.token).awaitOrNull() }
        }.getOrNull()
        cancel.cancel()
        return fresh ?: runCatching { client.lastLocation.awaitOrNull() }.getOrNull()
    }

    private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { if (continuation.isActive) continuation.resume(if (it.isSuccessful) it.result else null) }
    }

    /** Without Play services: the most recent provider fix, else a new one from network or GPS, whichever is allowed and on. */
    @SuppressLint("MissingPermission")
    private suspend fun platformPosition(context: Context): Location? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        // GPS needs precise permission; approximate allows only the network.
        val allowed = buildList {
            add(LocationManager.NETWORK_PROVIDER)
            if (fine(context)) add(LocationManager.GPS_PROVIDER)
        }.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (allowed.isEmpty()) return null
        val recent = allowed.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < 5 * 60_000L }
            .maxByOrNull { it.time }
        if (recent != null) return recent
        val provider = allowed.first()
        val fresh = withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine { continuation ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val signal = CancellationSignal()
                    continuation.invokeOnCancellation { signal.cancel() }
                    manager.getCurrentLocation(provider, signal, context.mainExecutor) { if (continuation.isActive) continuation.resume(it) }
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            manager.removeUpdates(this)
                            if (continuation.isActive) continuation.resume(location)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
                        }
                    }
                    continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                }
            }
        }
        return fresh ?: allowed.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
    }

    private suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): Address? =
        geocode(context) { geocoder, done ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1, listener(done))
            } else {
                @Suppress("DEPRECATION")
                done(geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull())
            }
        }

    private suspend fun forwardGeocode(context: Context, query: String): Address? =
        geocode(context) { geocoder, done ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(query, 1, listener(done))
            } else {
                @Suppress("DEPRECATION")
                done(geocoder.getFromLocationName(query, 1)?.firstOrNull())
            }
        }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun listener(done: (Address?) -> Unit) = object : Geocoder.GeocodeListener {
        override fun onGeocode(addresses: MutableList<Address>) = done(addresses.firstOrNull())
        override fun onError(errorMessage: String?) = done(null)
    }

    /** Runs a geocoder call off the main thread, in the phone's language as Flutter's geocoding does; null on any failure or after 15 seconds. */
    private suspend fun geocode(context: Context, call: (Geocoder, (Address?) -> Unit) -> Unit): Address? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine { continuation ->
                    try {
                        call(Geocoder(context, Locale.getDefault())) { if (continuation.isActive) continuation.resume(it) }
                    } catch (_: Exception) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
        }
}
