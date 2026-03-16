package com.heartbeatmusic.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApiService {
    @GET("tracks/")
    suspend fun getTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 50,
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("speed") speed: String,
        @Query("fuzzytags") fuzzytags: String,
        @Query("vocalinstrumental") vocalInstrumental: String? = null
    ): JamendoResponse

    @GET("tracks/")
    suspend fun getTracksByIds(
        @Query("client_id") clientId: String,
        @Query("id[]") ids: List<String>,
        @Query("format") format: String = "json"
    ): JamendoResponse
}

data class JamendoResponse(val results: List<JamendoTrack>)

data class JamendoTrack(
    val id: String,
    val name: String,
    @SerializedName("artist_name") val artistName: String,
    val audio: String,
    val image: String
)
