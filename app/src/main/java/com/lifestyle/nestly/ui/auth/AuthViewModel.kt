package com.lifestyle.nestly.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.model.ProfileEntity
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AuthState { LOADING, SIGNED_OUT, SIGNED_IN }

class AuthViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<AuthState> = container.auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> AuthState.SIGNED_IN
                is SessionStatus.NotAuthenticated -> AuthState.SIGNED_OUT
                else -> AuthState.LOADING
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.LOADING)

    val email: StateFlow<String?> = container.auth.userId
        .map { container.auth.currentEmail }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Profiles visible to this account; null until a fetch has completed
     * *as the signed-in user*. Resetting to null whenever the session is not
     * authenticated (cold-start restore, token refresh, sign-out) ensures a
     * result produced before/without a session can never reach the
     * onboarding gate in NestlyApp — that stale empty result was the
     * onboarding flash seen before the home screen appeared.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val profiles: StateFlow<List<ProfileEntity>?> = container.auth.sessionStatus
        .flatMapLatest { status ->
            if (status is SessionStatus.Authenticated) container.family.profiles else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Null until the families fetch has completed as the signed-in user. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val hasFamily: StateFlow<Boolean?> = container.auth.sessionStatus
        .flatMapLatest { status ->
            if (status is SessionStatus.Authenticated) {
                container.family.families.map { it.isNotEmpty() }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val _naraStatus = MutableStateFlow<String?>(null)
    val naraStatus: StateFlow<String?> = _naraStatus.asStateFlow()

    /** One-time Nara load during onboarding; imported profiles end onboarding via the profiles flow. */
    fun importNara(uri: android.net.Uri) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        _naraStatus.value = "Importing your Nara history…"
        viewModelScope.launch {
            try {
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val stream = container.appContext.contentResolver.openInputStream(uri)
                        ?: error("Could not open the selected file")
                    stream.use { container.importer.import(it) }
                }
                _naraStatus.value = "Imported ${result.totalActivities} activities for ${result.profiles} child profile(s) " +
                    "and ${result.caregivers} caregiver(s). Skipped ${result.skippedRows} unsupported row(s)."
            } catch (e: Exception) {
                _naraStatus.value = null
                _error.value = e.message ?: "Import failed. Check your connection and try again."
            } finally {
                _busy.value = false
            }
        }
    }

    fun signIn(email: String, password: String) = launchAuth {
        container.auth.signIn(email, password)
    }

    fun signUp(email: String, password: String, caregiverName: String) = launchAuth {
        container.auth.signUp(email, password)
        if (container.auth.currentUserId != null) {
            // Session is live immediately when email confirmation is disabled;
            // otherwise the family is created on first sign-in via onboarding.
            runCatching { container.family.createFamily(caregiverName) }
        } else {
            _notice.value = "Check your email to confirm your account, then sign in."
        }
    }

    fun createFamily(caregiverName: String) = launchAuth {
        container.family.createFamily(caregiverName)
    }

    fun joinFamily(code: String, caregiverName: String) = launchAuth {
        container.family.joinWithInvite(code, caregiverName)
    }

    fun addFirstChild(name: String, sex: com.lifestyle.nestly.data.model.Sex?, birthDate: java.time.LocalDate?, caregiverName: String) =
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                container.family.addChild(name, sex, birthDate, caregiverName)
            } catch (e: Exception) {
                _error.value = e.message ?: "Couldn't save. Check your connection and try again."
            } finally {
                _busy.value = false
            }
        }

    fun signOut() = viewModelScope.launch {
        runCatching { container.auth.signOut() }
        container.realtime.unsubscribe()
    }

    private fun launchAuth(block: suspend () -> Unit) = viewModelScope.launch {
        _busy.value = true
        _error.value = null
        try {
            block()
        } catch (e: Exception) {
            _error.value = friendly(e)
        } finally {
            _busy.value = false
        }
    }

    private fun friendly(e: Exception): String = when {
        e.message?.contains("rate limit", ignoreCase = true) == true ->
            "Supabase's built-in email limit was hit, so no confirmation email was sent and the account wasn't created. " +
                "For testing, turn off \"Confirm email\" in Supabase Dashboard → Authentication → Providers → Email, then try again."
        e.message?.contains("Invalid login", ignoreCase = true) == true -> "Wrong email or password."
        e.message?.contains("already registered", ignoreCase = true) == true -> "That email already has an account. Sign in instead."
        e.message?.contains("invite", ignoreCase = true) == true -> "That invite code is invalid or expired."
        else -> e.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Check your connection and try again."
    }
}
