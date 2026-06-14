package com.bibleread.bread.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class BibleVerse(
    val book_name: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

data class BibleResponse(
    val reference: String,
    val verses: List<BibleVerse>,
    val text: String,
    val translation_name: String
)

interface BibleApiService {
    @GET("{reference}?translation=kjv")
    suspend fun getChapter(@Path("reference", encoded = true) reference: String): BibleResponse
}

object BibleApi {
    val service: BibleApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://bible-api.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BibleApiService::class.java)
    }
}
