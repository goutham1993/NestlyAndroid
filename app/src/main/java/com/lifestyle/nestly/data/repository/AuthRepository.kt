package com.lifestyle.nestly.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(private val client: SupabaseClient) {

    val sessionStatus: Flow<SessionStatus> = client.auth.sessionStatus

    /** Signed-in user id, or null while loading / signed out. */
    val userId: Flow<String?> = client.auth.sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user?.id
    }

    val currentUserId: String? get() = client.auth.currentSessionOrNull()?.user?.id
    val currentEmail: String? get() = client.auth.currentSessionOrNull()?.user?.email

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String) {
        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signOut() = client.auth.signOut()
}
