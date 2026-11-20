package com.snj.travelvista.data.remote.provider

import java.net.URLEncoder

/**
 * Modular service contracts for TravelVista's global travel & booking architecture.
 *
 * SECURITY & SAFETY POLICY:
 * - No fabricated hotel availability, flight schedules, or simulated booking confirmations.
 * - Private partner API keys (Amadeus, Booking.com Rapid, Stripe/Adyen) must NEVER be embedded in the Android APK;
 *   they must be mediated through a secure backend proxy configured via BuildConfig.PROVIDER_PROXY_BASE_URL.
 * - Until partner credentials & backend proxy are provisioned, booking search actions route users transparently
 *   to official external provider search & checkout pages.
 */

enum class IntegrationMode(val label: String) {
    LIVE_PUBLIC_API("Live Public API"),
    EXTERNAL_PROVIDER_REDIRECT("External Provider Redirect"),
    REQUIRES_PARTNER_CREDENTIALS("Requires Partner Credentials & Backend Proxy")
}

data class ProviderCapability(
    val serviceName: String,
    val providerName: String,
    val mode: IntegrationMode,
    val description: String,
    val requiredCredentials: String = "None (Public Open API)"
)

data class HotelSearchQuery(
    val destination: String,
    val checkInDate: String,      // yyyy-MM-dd
    val checkOutDate: String,     // yyyy-MM-dd
    val adults: Int = 2,
    val rooms: Int = 1,
    val minStars: Int = 0
)

enum class FlightTripType(val displayName: String) {
    ROUND_TRIP("Round-trip"),
    ONE_WAY("One-way")
}

enum class CabinClass(val displayName: String, val skyscannerCode: String) {
    ECONOMY("Economy", "economy"),
    PREMIUM_ECONOMY("Premium Economy", "premiumeconomy"),
    BUSINESS("Business", "business"),
    FIRST("First Class", "first")
}

data class FlightSearchQuery(
    val origin: String,
    val destination: String,
    val departureDate: String,    // yyyy-MM-dd
    val returnDate: String? = null,
    val passengers: Int = 1,
    val tripType: FlightTripType = FlightTripType.ROUND_TRIP,
    val cabinClass: CabinClass = CabinClass.ECONOMY
)

data class ActivitySearchQuery(
    val destination: String,
    val category: String = "All"
)

data class ExternalProviderLink(
    val providerName: String,
    val url: String,
    val disclaimer: String
)

// --- MODULAR SERVICE INTERFACES ---

interface DestinationService {
    suspend fun isLiveDiscoveryAvailable(): Boolean = true
}

interface HotelService {
    fun buildProviderSearchLinks(query: HotelSearchQuery): List<ExternalProviderLink>
    fun isDirectBookingEnabled(): Boolean
}

interface FlightService {
    fun buildProviderSearchLinks(query: FlightSearchQuery): List<ExternalProviderLink>
    fun isDirectBookingEnabled(): Boolean
}

interface ActivityService {
    fun buildProviderSearchLinks(query: ActivitySearchQuery): List<ExternalProviderLink>
}

interface MapService {
    fun buildDirectionsUrl(latitude: Double, longitude: Double, label: String): String
    fun buildOpenStreetMapUrl(latitude: Double, longitude: Double, zoom: Int = 12): String
}

/**
 * Booking & Payment service contracts.
 * Enforces safety: never stores payment cards, never marks a booking confirmed without a verified provider reference.
 */
sealed class BookingVerificationResult {
    data class RedirectToOfficialProvider(val link: ExternalProviderLink) : BookingVerificationResult()
    data class PartnerProxyRequired(val reason: String, val missingCredentials: List<String>) : BookingVerificationResult()
}

interface BookingService {
    fun verifyBookingCapability(serviceType: String): BookingVerificationResult
}

interface PaymentService {
    /**
     * TravelVista never collects or stores raw payment card numbers inside the Android client.
     * Payments must execute inside an official provider checkout or PCI-DSS compliant SDK session.
     */
    fun isDirectInAppPaymentSupported(): Boolean = false
    fun getPaymentSafetyNotice(): String =
        "For your security, TravelVista never stores payment card details. All payments, taxes, and cancellation policies are confirmed directly on the authorized booking provider's checkout."
}

// --- LEGITIMATE EXTERNAL PROVIDER LINK BUILDERS (NO FAKE DATA) ---

object ProviderBookingLinkBuilder : HotelService, FlightService, ActivityService, MapService, BookingService, PaymentService {

    private fun encode(value: String): String =
        URLEncoder.encode(value.trim(), "UTF-8")

    override fun buildProviderSearchLinks(query: HotelSearchQuery): List<ExternalProviderLink> {
        val city = encode(query.destination)
        val adults = query.adults.coerceIn(1, 12)
        val rooms = query.rooms.coerceIn(1, 8)
        val checkIn = query.checkInDate.trim()
        val checkOut = query.checkOutDate.trim()

        val bookingUrl = buildString {
            append("https://www.booking.com/searchresults.html?ss=$city")
            if (checkIn.isNotBlank()) append("&checkin=$checkIn")
            if (checkOut.isNotBlank()) append("&checkout=$checkOut")
            append("&group_adults=$adults&no_rooms=$rooms")
            if (query.minStars in 1..5) append("&nflt=class%3D${query.minStars}")
        }

        val googleHotelsUrl = buildString {
            append("https://www.google.com/travel/hotels?q=${encode("Hotels in ${query.destination}")}")
            if (checkIn.isNotBlank()) append("&dates=${checkIn}_${checkOut}")
        }

        val agodaUrl = "https://www.agoda.com/search?text=$city"
        val expediaUrl = "https://www.expedia.com/Hotel-Search?destination=$city"

        val disclaimer =
            "Live room availability, nightly rates, taxes, fees, and cancellation policies are provided and confirmed directly on the official partner website."

        return listOf(
            ExternalProviderLink("Booking.com", bookingUrl, disclaimer),
            ExternalProviderLink("Google Hotels", googleHotelsUrl, disclaimer),
            ExternalProviderLink("Agoda", agodaUrl, disclaimer),
            ExternalProviderLink("Expedia", expediaUrl, disclaimer)
        )
    }

    override fun isDirectBookingEnabled(): Boolean = false

    override fun buildProviderSearchLinks(query: FlightSearchQuery): List<ExternalProviderLink> {
        val originClean = query.origin.trim()
        val destClean = query.destination.trim()
        val tripPhrase = buildString {
            append("Flights from $originClean to $destClean")
            if (query.departureDate.isNotBlank()) append(" on ${query.departureDate}")
            if (query.tripType == FlightTripType.ROUND_TRIP && !query.returnDate.isNullOrBlank()) {
                append(" returning ${query.returnDate}")
            }
            if (query.cabinClass != CabinClass.ECONOMY) {
                append(" ${query.cabinClass.displayName}")
            }
        }

        val googleFlightsUrl = "https://www.google.com/travel/flights?q=${encode(tripPhrase)}"

        val skyscannerQuery = encode("$originClean to $destClean")
        val skyscannerUrl = "https://www.skyscanner.net/transport/flights/?q=$skyscannerQuery&adults=${query.passengers.coerceIn(1, 9)}&cabinclass=${query.cabinClass.skyscannerCode}"

        val kayakUrl = "https://www.kayak.com/flights/${encode(originClean)}-${encode(destClean)}"

        val disclaimer =
            "Live flight schedules, airline fares, baggage allowances, taxes, and ticketing are handled directly by the official flight search provider."

        return listOf(
            ExternalProviderLink("Google Flights", googleFlightsUrl, disclaimer),
            ExternalProviderLink("Skyscanner", skyscannerUrl, disclaimer),
            ExternalProviderLink("Kayak", kayakUrl, disclaimer)
        )
    }

    override fun buildProviderSearchLinks(query: ActivitySearchQuery): List<ExternalProviderLink> {
        val term = if (query.category.equals("All", ignoreCase = true) || query.category.isBlank()) {
            query.destination
        } else {
            "${query.destination} ${query.category}"
        }
        val encoded = encode(term)
        val airbnbUrl = "https://www.airbnb.com/s/${encoded}/experiences"
        val disclaimer =
            "Ticket prices, time slots, and instant reservations are verified directly on the official tour provider website."

        return listOf(
            ExternalProviderLink("GetYourGuide", "https://www.getyourguide.com/s/?q=$encoded", disclaimer),
            ExternalProviderLink("Viator", "https://www.viator.com/searchResults/all?text=$encoded", disclaimer),
            ExternalProviderLink("Airbnb Experiences", airbnbUrl, disclaimer)
        )
    }

    override fun buildDirectionsUrl(latitude: Double, longitude: Double, label: String): String {
        return "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&destination_place_id=&travelmode=driving"
    }

    override fun buildOpenStreetMapUrl(latitude: Double, longitude: Double, zoom: Int): String {
        return "https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=$zoom/$latitude/$longitude"
    }

    override fun verifyBookingCapability(serviceType: String): BookingVerificationResult {
        return BookingVerificationResult.PartnerProxyRequired(
            reason = "Direct in-app $serviceType booking requires authorized commercial API credentials mediated via a secure backend proxy.",
            missingCredentials = listOf(
                "AMADEUS_CLIENT_ID & AMADEUS_CLIENT_SECRET (Flights & Hotels GDS)",
                "BOOKING_COM_AFFILIATE_ID or EXPEDIA_RAPID_API_KEY (Direct Hotel Checkout)",
                "PROVIDER_PROXY_BASE_URL (Backend server holding private keys & PCI payment tokens)"
            )
        )
    }

    /**
     * Transparent registry of all integrated APIs and their current runtime status.
     */
    val capabilities: List<ProviderCapability> = listOf(
        ProviderCapability(
            serviceName = "Destination Guides & Photos",
            providerName = "Wikimedia Wikivoyage & Commons",
            mode = IntegrationMode.LIVE_PUBLIC_API,
            description = "Live travel guides, descriptions, geosearch nearby attractions, and licensed photography."
        ),
        ProviderCapability(
            serviceName = "Worldwide Geocoding & Search",
            providerName = "OpenStreetMap Nominatim",
            mode = IntegrationMode.LIVE_PUBLIC_API,
            description = "Search any country, city, region, island, mountain, beach, or landmark worldwide."
        ),
        ProviderCapability(
            serviceName = "Live Weather & Forecasts",
            providerName = "Open-Meteo",
            mode = IntegrationMode.LIVE_PUBLIC_API,
            description = "Real-time temperature, WMO weather conditions, humidity, wind, and daily min/max."
        ),
        ProviderCapability(
            serviceName = "Country Metadata",
            providerName = "REST Countries v3.1",
            mode = IntegrationMode.LIVE_PUBLIC_API,
            description = "Official local currency, languages, time zones, regions, and international dialing codes."
        ),
        ProviderCapability(
            serviceName = "Live Currency Conversion",
            providerName = "Frankfurter (European Central Bank)",
            mode = IntegrationMode.LIVE_PUBLIC_API,
            description = "Verified daily ECB exchange rates for multi-currency budget display."
        ),
        ProviderCapability(
            serviceName = "Interactive Worldwide Map",
            providerName = "OpenStreetMap (osmdroid)",
            mode = IntegrationMode.LIVE_PUBLIC_API,
            description = "Interactive pan/zoom global map with destination pins, geocoded search, and Haversine distances."
        ),
        ProviderCapability(
            serviceName = "Hotels, Flights & Activities Search",
            providerName = "Booking.com, Google Hotels/Flights, Skyscanner, GetYourGuide, Viator",
            mode = IntegrationMode.EXTERNAL_PROVIDER_REDIRECT,
            description = "Pre-filled search forms redirecting transparently to official partner websites for real live rates & checkout.",
            requiredCredentials = "For in-app booking: Amadeus API / Booking.com Affiliate / Viator Partner API + Backend Proxy"
        ),
        ProviderCapability(
            serviceName = "Cloud Accounts & Direct Checkout",
            providerName = "Backend Auth & PCI Payment Gateway",
            mode = IntegrationMode.REQUIRES_PARTNER_CREDENTIALS,
            description = "Local device profile & offline Trip Planner are active; cloud sync & in-app card checkout require backend auth & Stripe/Adyen server keys.",
            requiredCredentials = "OAuth/Firebase Auth Project + PCI Payment Gateway Backend"
        )
    )
}
