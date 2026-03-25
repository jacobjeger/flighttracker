package com.megalife.flighttracker.data.model

import com.google.gson.annotations.SerializedName

// FlightAware API Response Models

data class FlightSearchResponse(
    @SerializedName("flights") val flights: List<FlightData>?
)

data class FlightIdentResponse(
    @SerializedName("flights") val flights: List<FlightData>?
)

data class FlightResponse(
    @SerializedName("flights") val flights: List<FlightData>?
)

data class FlightData(
    @SerializedName("fa_flight_id") val faFlightId: String?,
    @SerializedName("ident") val ident: String?,
    @SerializedName("ident_iata") val identIata: String?,
    @SerializedName("ident_icao") val identIcao: String?,
    @SerializedName("operator") val operator: String?,
    @SerializedName("operator_iata") val operatorIata: String?,
    @SerializedName("operator_icao") val operatorIcao: String?,
    @SerializedName("flight_number") val flightNumber: String?,
    @SerializedName("registration") val registration: String?,
    @SerializedName("atc_ident") val atcIdent: String?,
    @SerializedName("inbound_fa_flight_id") val inboundFaFlightId: String?,
    @SerializedName("codeshares") val codeshares: List<String>?,
    @SerializedName("codeshares_iata") val codesharesIata: List<String>?,
    @SerializedName("blocked") val blocked: Boolean?,
    @SerializedName("diverted") val diverted: Boolean?,
    @SerializedName("cancelled") val cancelled: Boolean?,
    @SerializedName("position_only") val positionOnly: Boolean?,
    @SerializedName("origin") val origin: AirportInfo?,
    @SerializedName("destination") val destination: AirportInfo?,
    @SerializedName("departure_delay") val departureDelay: Int?,
    @SerializedName("arrival_delay") val arrivalDelay: Int?,
    @SerializedName("filed_ete") val filedEte: Int?,
    @SerializedName("progress_percent") val progressPercent: Int?,
    @SerializedName("status") val status: String?,
    @SerializedName("aircraft_type") val aircraftType: String?,
    @SerializedName("route_distance") val routeDistance: Int?,
    @SerializedName("filed_airspeed") val filedAirspeed: Int?,
    @SerializedName("filed_altitude") val filedAltitude: Int?,
    @SerializedName("scheduled_out") val scheduledOut: String?,
    @SerializedName("estimated_out") val estimatedOut: String?,
    @SerializedName("actual_out") val actualOut: String?,
    @SerializedName("scheduled_off") val scheduledOff: String?,
    @SerializedName("estimated_off") val estimatedOff: String?,
    @SerializedName("actual_off") val actualOff: String?,
    @SerializedName("scheduled_on") val scheduledOn: String?,
    @SerializedName("estimated_on") val estimatedOn: String?,
    @SerializedName("actual_on") val actualOn: String?,
    @SerializedName("scheduled_in") val scheduledIn: String?,
    @SerializedName("estimated_in") val estimatedIn: String?,
    @SerializedName("actual_in") val actualIn: String?,
    @SerializedName("gate_origin") val gateOrigin: String?,
    @SerializedName("gate_destination") val gateDestination: String?,
    @SerializedName("terminal_origin") val terminalOrigin: String?,
    @SerializedName("terminal_destination") val terminalDestination: String?,
    @SerializedName("baggage_claim") val baggageClaim: String?,
    @SerializedName("type") val type: String?
)

data class AirportInfo(
    @SerializedName("code") val code: String?,
    @SerializedName("code_iata") val codeIata: String?,
    @SerializedName("code_icao") val codeIcao: String?,
    @SerializedName("code_lid") val codeLid: String?,
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("airport_info_url") val airportInfoUrl: String?
)

data class AirportResponse(
    @SerializedName("airport_code") val airportCode: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("country_code") val countryCode: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("wiki_url") val wikiUrl: String?,
    @SerializedName("alternatives") val alternatives: List<AirportAlternative>?
)

data class AirportAlternative(
    @SerializedName("code") val code: String?,
    @SerializedName("name") val name: String?
)

data class AirportFlightsResponse(
    @SerializedName("departures") val departures: List<FlightData>?,
    @SerializedName("arrivals") val arrivals: List<FlightData>?,
    @SerializedName("links") val links: PaginationLinks?,
    @SerializedName("num_pages") val numPages: Int?
)

data class PaginationLinks(
    @SerializedName("next") val next: String?
)

// Weather API Models
data class WeatherResponse(
    @SerializedName("main") val main: WeatherMain?,
    @SerializedName("weather") val weather: List<WeatherCondition>?,
    @SerializedName("name") val name: String?
)

data class WeatherMain(
    @SerializedName("temp") val temp: Double?,
    @SerializedName("feels_like") val feelsLike: Double?,
    @SerializedName("humidity") val humidity: Int?
)

data class WeatherCondition(
    @SerializedName("main") val main: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("icon") val icon: String?
)

// Railway Backend Models
data class DeviceRegistration(
    val fcmToken: String,
    val trackedFlights: List<String>
)

data class AddFlightRequest(
    val fcmToken: String,
    val flightId: String
)

data class RemoveFlightRequest(
    val fcmToken: String,
    val flightId: String
)

data class BackendResponse(
    val success: Boolean,
    val message: String?
)
