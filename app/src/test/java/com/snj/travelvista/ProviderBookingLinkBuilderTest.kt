package com.snj.travelvista

import com.snj.travelvista.data.remote.provider.ActivitySearchQuery
import com.snj.travelvista.data.remote.provider.BookingVerificationResult
import com.snj.travelvista.data.remote.provider.CabinClass
import com.snj.travelvista.data.remote.provider.FlightSearchQuery
import com.snj.travelvista.data.remote.provider.FlightTripType
import com.snj.travelvista.data.remote.provider.HotelSearchQuery
import com.snj.travelvista.data.remote.provider.ProviderBookingLinkBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderBookingLinkBuilderTest {

    @Test
    fun testHotelProviderUrlsContainValidParameters() {
        val links = ProviderBookingLinkBuilder.buildProviderSearchLinks(
            HotelSearchQuery(
                destination = "Tokyo",
                checkInDate = "2026-12-01",
                checkOutDate = "2026-12-05",
                adults = 2,
                rooms = 1
            )
        )
        assertEquals(4, links.size)
        val bookingLink = links[0]
        assertEquals("Booking.com", bookingLink.providerName)
        assertTrue(bookingLink.url.startsWith("https://www.booking.com/searchresults.html?"))
        assertTrue(bookingLink.url.contains("ss=Tokyo"))
        assertTrue(bookingLink.url.contains("checkin=2026-12-01"))
        assertTrue(bookingLink.url.contains("checkout=2026-12-05"))
        assertTrue(bookingLink.url.contains("group_adults=2"))

        val googleHotelsLink = links[1]
        assertEquals("Google Hotels", googleHotelsLink.providerName)

        val agodaLink = links[2]
        assertEquals("Agoda", agodaLink.providerName)
        assertTrue(agodaLink.url.contains("agoda.com"))

        val expediaLink = links[3]
        assertEquals("Expedia", expediaLink.providerName)
        assertTrue(expediaLink.url.contains("expedia.com"))
    }

    @Test
    fun testFlightProviderUrlsContainOriginDestinationAndCabin() {
        val links = ProviderBookingLinkBuilder.buildProviderSearchLinks(
            FlightSearchQuery(
                origin = "LHR",
                destination = "NRT",
                departureDate = "2026-12-10",
                returnDate = "2026-12-20",
                passengers = 2,
                tripType = FlightTripType.ROUND_TRIP,
                cabinClass = CabinClass.BUSINESS
            )
        )
        assertEquals(3, links.size)
        assertTrue(links[0].url.startsWith("https://www.google.com/travel/flights?q="))
        assertTrue(links[0].url.contains("LHR"))
        assertTrue(links[0].url.contains("NRT"))
        assertTrue(links[1].url.contains("cabinclass=business"))
        assertTrue(links[1].url.contains("adults=2"))
        assertEquals("Kayak", links[2].providerName)
        assertTrue(links[2].url.contains("kayak.com"))
    }

    @Test
    fun testActivityProviderUrls() {
        val links = ProviderBookingLinkBuilder.buildProviderSearchLinks(
            ActivitySearchQuery(destination = "Rome", category = "Museums")
        )
        assertEquals(3, links.size)
        assertTrue(links[0].url.contains("getyourguide.com"))
        assertTrue(links[0].url.contains("Rome+Museums"))
        assertTrue(links[1].url.contains("viator.com"))
        assertEquals("Airbnb Experiences", links[2].providerName)
        assertTrue(links[2].url.contains("airbnb.com"))
    }

    @Test
    fun testBookingAndPaymentSafetyContracts() {
        // Must never claim direct in-app checkout or card storage without verified partner proxy
        assertFalse(ProviderBookingLinkBuilder.isDirectBookingEnabled())
        assertFalse(ProviderBookingLinkBuilder.isDirectInAppPaymentSupported())
        val check = ProviderBookingLinkBuilder.verifyBookingCapability("Hotel")
        assertTrue(check is BookingVerificationResult.PartnerProxyRequired)
        assertTrue(ProviderBookingLinkBuilder.getPaymentSafetyNotice().contains("never stores payment card"))
    }
}
