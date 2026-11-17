package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.media.ExifInterface
import com.example.data.model.CompartmentPhoto
import com.example.data.model.NoFareRecord
import com.example.data.model.PassengerBooking
import com.example.data.model.StaffAttendance
import com.example.data.model.Trip
import com.example.data.model.TripExpense
import com.example.data.model.computeTripMetrics
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

data class LocationCapture(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val label: String = "Location unavailable"
)

object MediaAndLocationHelper {

    private val inrLocale: Locale = Locale.Builder().setLanguage("en").setRegion("IN").build()

    fun formatInr(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(inrLocale)
        formatter.maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        return formatter.format(amount)
    }

    fun formatTimestamp(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: Date()
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
    }

    fun createCameraImageUri(context: Context): Uri {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun compressUriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

            val maxDim = 720
            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > maxDim * 2 ||
                boundsOptions.outHeight / sampleSize > maxDim * 2
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: return@withContext null

            val rotated = rotateIfNeeded(rawBytes, decoded)
            val scaled = scaleDownBitmap(rotated, maxDim)
            if (rotated !== decoded) decoded.recycle()
            if (scaled !== rotated) rotated.recycle()

            var quality = 70
            var encoded = ""
            while (quality >= 30) {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                if (encoded.length <= 220_000) break
                quality -= 15
            }
            scaled.recycle()
            encoded.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun rotateIfNeeded(rawBytes: ByteArray, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(rawBytes.inputStream())
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val angle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (angle == 0f) bitmap else {
                val matrix = Matrix().apply { postRotate(angle) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (newW, newH) = if (width >= height) {
            maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
        }
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun captureCurrentLocation(context: Context): LocationCapture = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) {
            return@withContext LocationCapture(label = "Location permission denied")
        }

        val location = withTimeoutOrNull(4500L) {
            fetchFusedLocation(context)
        } ?: getLastKnownFrameworkLocation(context)

        if (location == null) {
            return@withContext LocationCapture(label = "GPS signal unavailable")
        }

        val lat = location.latitude
        val lng = location.longitude
        val addressText = reverseGeocode(context, lat, lng)
        val coordText = String.format(Locale.US, "%.4f, %.4f", lat, lng)
        val label = if (!addressText.isNullOrBlank()) {
            "$addressText ($coordText)"
        } else {
            "GPS: $coordText"
        }.take(190)

        LocationCapture(latitude = lat, longitude = lng, label = label)
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchFusedLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            if (cont.isActive) cont.resume(loc)
                        } else {
                            client.lastLocation
                                .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                        }
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private fun getLastKnownFrameworkLocation(context: Context): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(2000L) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lng, 1) { addresses ->
                            val addr = addresses.firstOrNull()
                            val parts = listOfNotNull(
                                addr?.subLocality ?: addr?.thoroughfare,
                                addr?.locality,
                                addr?.adminArea
                            ).distinct()
                            if (cont.isActive) cont.resume(parts.joinToString(", ").ifBlank { null })
                        }
                    }
                }
            } else {
                val addr = geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
                listOfNotNull(
                    addr?.subLocality ?: addr?.thoroughfare,
                    addr?.locality,
                    addr?.adminArea
                ).distinct().joinToString(", ").ifBlank { null }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun openLocationInMaps(context: Context, lat: Double, lng: Double, label: String) {
        val encodedLabel = Uri.encode(label)
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    fun shareTripReport(
        context: Context,
        trip: Trip,
        attendance: List<StaffAttendance>,
        expenses: List<TripExpense>,
        bookings: List<PassengerBooking>,
        photos: List<CompartmentPhoto>,
        noFareRecords: List<NoFareRecord> = emptyList()
    ) {
        val m = computeTripMetrics(attendance, expenses, bookings, photos, noFareRecords)
        val sb = StringBuilder()
        sb.appendLine("KRISHNA TRAVELS — PER-TRIP REPORT")
        sb.appendLine("================================")
        sb.appendLine("Bus: ${trip.busNumber}")
        sb.appendLine("Route: ${trip.origin} -> ${trip.destination}")
        sb.appendLine("Date & Time: ${trip.tripDate} | ${trip.departureTime}")
        sb.appendLine("Driver: ${trip.driverName} | Conductor: ${trip.conductorName.ifBlank { "N/A" }}")
        sb.appendLine("Trip Status: ${trip.status.uppercase(Locale.getDefault())}")
        sb.appendLine()
        sb.appendLine("BOOKINGS & BILLING (${m.totalBookings} Bills | ${m.totalBookedCabins}/30 Cabins)")
        sb.appendLine("• Total Fare: ${formatInr(m.totalFare)}")
        sb.appendLine("• Collected: ${formatInr(m.totalCollected)}")
        sb.appendLine("• Pending Due: ${formatInr(m.totalPending)}")
        bookings.forEach { b ->
            sb.appendLine("  - [${b.invoiceNumber}] ${b.passengerName} | Cabin ${b.berthNumbers} | ${formatInr(b.paidAmount)}/${formatInr(b.fareAmount)} (${b.paymentStatus}) | Manager: ${b.createdByName}")
        }
        sb.appendLine()
        sb.appendLine("DIESEL, TOLL, TAX & EXPENSES (${expenses.size} Entries)")
        sb.appendLine("• Diesel: ${formatInr(m.dieselExpense)} (${String.format(Locale.US, "%.1f", m.dieselLiters)} L)")
        sb.appendLine("• Toll: ${formatInr(m.tollExpense)}")
        sb.appendLine("• Tax/Permit: ${formatInr(m.taxExpense)}")
        sb.appendLine("• Other: ${formatInr(m.otherExpense)}")
        sb.appendLine("• Total Expenses: ${formatInr(m.totalExpenses)}")
        expenses.forEach { e ->
            val loc = if (e.capturedLocationLabel.isNotBlank()) " @ ${e.capturedLocationLabel}" else ""
            val timeStr = formatTimestamp(e.createdAt)
            sb.appendLine("  - ${e.category}: ${formatInr(e.amount)} (${e.paymentMode}) ${e.vendorOrPlaza} | $timeStr$loc | By: ${e.createdByName}")
        }
        sb.appendLine()
        sb.appendLine("NET TRIP BALANCE: ${formatInr(m.netProfit)}")
        sb.appendLine()
        sb.appendLine("STAFF ATTENDANCE WITH PHOTO & GPS (${attendance.size})")
        attendance.forEach { a ->
            sb.appendLine("• ${a.staffName} (${a.staffRole}) - ${a.status} | ${formatTimestamp(a.createdAt)} | ${a.capturedLocationLabel} | By: ${a.createdByName}")
        }
        sb.appendLine()
        sb.appendLine("SLEEPER 30-CABIN CONFIRMATION PHOTOS (${m.cabinPhotosCount}/30 Cabins Uploaded)")
        photos.sortedBy { it.cabinNumber }.forEach { p ->
            val verifiedTag = if (p.verifiedByOwner) " [Owner Verified]" else ""
            sb.appendLine("• Cabin C${p.cabinNumber.toString().padStart(2, '0')}: ${p.passengerName.ifBlank { "Passenger" }}$verifiedTag | ${formatTimestamp(p.createdAt)} | ${p.capturedLocationLabel} | By: ${p.createdByName}")
        }
        if (noFareRecords.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("UNPAID / NO-FARE HISTORY (${noFareRecords.size})")
            noFareRecords.forEach { n ->
                val cabinText = if (n.cabinNumber > 0) "Cabin C${n.cabinNumber.toString().padStart(2, '0')}" else "No Cabin"
                sb.appendLine("• ${n.personName} (${n.mobileNumber}) | $cabinText | Due: ${formatInr(n.expectedFare)} | ${n.reasonOrNote} | ${formatTimestamp(n.createdAt)} | By: ${n.createdByName}")
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Krishna Travels Trip Report: ${trip.busNumber} (${trip.tripDate})")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Trip Report"))
    }

    fun shareBookingInvoice(context: Context, trip: Trip, booking: PassengerBooking) {
        val text = buildString {
            appendLine("KRISHNA TRAVELS — TICKET & BILL")
            appendLine("Invoice: ${booking.invoiceNumber}")
            appendLine("Bus: ${trip.busNumber}")
            appendLine("Route: ${booking.boardingPoint} -> ${booking.droppingPoint}")
            appendLine("Date: ${trip.tripDate} | Dep: ${trip.departureTime}")
            appendLine("Passenger: ${booking.passengerName} ${if (booking.passengerPhone.isNotBlank()) "(${booking.passengerPhone})" else ""}")
            appendLine("Sleeper Cabin(s): ${booking.berthNumbers} (${booking.seatCount} Berth)")
            appendLine("Total Fare: ${formatInr(booking.fareAmount)}")
            appendLine("Paid: ${formatInr(booking.paidAmount)} (${booking.paymentMode} - ${booking.paymentStatus})")
            appendLine("Bill Manager: ${booking.createdByName}")
            appendLine("Bill Time: ${formatTimestamp(booking.createdAt)}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Ticket ${booking.invoiceNumber} - Krishna Travels")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Passenger Bill"))
    }

    fun sharePhotoWithCaption(
        context: Context,
        photoBase64: String,
        fileNamePrefix: String,
        subject: String,
        caption: String
    ) {
        try {
            val bytes = Base64.decode(photoBase64, Base64.DEFAULT)
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(dir, "${fileNamePrefix}_${System.currentTimeMillis()}.jpg")
            file.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, subject))
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            context.startActivity(Intent.createChooser(intent, subject))
        }
    }
}
