package com.yannickpulver.gridline.di

import com.yannickpulver.gridline.BuildKonfig
import com.yannickpulver.gridline.data.api.InstaApi
import com.yannickpulver.gridline.data.api.SupabaseApi
import com.yannickpulver.gridline.data.prefs.AppPrefs
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val commonModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(get())
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        co.touchlab.kermit.Logger.i(message, null, "HTTClient")
                    }
                }
                level = LogLevel.BODY
                // These requests carry the Instagram access token
                filter { it.url.host != "graph.instagram.com" && "instagram-auth" !in it.url.pathSegments }
            }
        }
    }

    single { AppPrefs(get()) }

    single { InstaApi(get(), get()) }
    single { SupabaseApi(get(), get(), get()) }

    single {
        createSupabaseClient(
            supabaseUrl = BuildKonfig.SUPABASE_URL,
            supabaseKey = BuildKonfig.SUPABASE_KEY
        ) {
            install(Storage)
            install(Postgrest)
            install(Realtime)
        }
    }
}
