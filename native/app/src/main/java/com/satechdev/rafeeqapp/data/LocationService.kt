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
import com.satechdev.rafeeqapp.data.prayer.UserLocation
import kotlinx.coroutines.Dispatchers
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

    /**
     * Saves the phone's current position as the automatic location, named by
     * the geocoder. Needs location permission. Returns false when no fix comes.
     */
    suspend fun updateFromGps(context: Context): Boolean {
        val position = currentPosition(context) ?: return false
        val address = reverseGeocode(context, position.latitude, position.longitude)
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
        return true
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

    @SuppressLint("MissingPermission")
    private suspend fun currentPosition(context: Context): Location? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            ?: return null
        val fresh = withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine { continuation ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val signal = CancellationSignal()
                    continuation.invokeOnCancellation { signal.cancel() }
                    manager.getCurrentLocation(provider, signal, context.mainExecutor) { continuation.resume(it) }
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
        return fresh ?: runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
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
