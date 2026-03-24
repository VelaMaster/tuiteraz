package com.example.Tuiteraz.data.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {
    private const val SUPABASE_URL = "https://hjfifedzjoxjxpcsxvls.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhqZmlmZWR6am94anhwY3N4dmxzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQzNzA0MTMsImV4cCI6MjA4OTk0NjQxM30.aWUygrkwX1JMIPY0-Gr-TtQDf_wxmxW-UEBOqhDfo9w"
    private const val WEB_CLIENT_ID = "575964157463-vvu9va2cca31guisb564p2hpt6rqo42q.apps.googleusercontent.com"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Auth)
        install(ComposeAuth) {
            googleNativeLogin(serverClientId = WEB_CLIENT_ID)
        }
    }
}