package com.lifestyle.nestly.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifestyle.nestly.data.supabase.DataErrors
import com.lifestyle.nestly.ui.activity.AddChildDialog
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType

@Composable
fun LoadingScreen(message: String = "Loading…") {
    Box(
        Modifier
            .fillMaxSize()
            .background(NestlyColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = NestlyColors.AccentText)
            Text(message, style = NestlyType.Small, color = NestlyColors.TextSecondary)
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(NestlyColors.Background)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Couldn't reach Nestly", style = NestlyType.RowLabel)
            Text(message, style = NestlyType.Small, color = NestlyColors.TextSecondary)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = NestlyColors.AccentText),
            ) { Text("Retry") }
        }
    }
}

@Composable
fun DataErrorBanner() {
    val message by DataErrors.message.collectAsStateWithLifecycle()
    val text = message ?: return
    Column(
        Modifier
            .fillMaxWidth()
            .background(NestlyColors.Danger)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text, style = NestlyType.Small, color = NestlyColors.Background)
        TextButton(onClick = { DataErrors.retry() }) {
            Text("Retry", color = NestlyColors.Background)
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NestlyColors.AccentText,
    unfocusedBorderColor = NestlyColors.Divider,
    cursorColor = NestlyColors.AccentText,
    focusedLabelColor = NestlyColors.AccentText,
)

@Composable
fun AuthScreen(authViewModel: AuthViewModel) {
    var signingUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var caregiverName by remember { mutableStateOf("") }
    val busy by authViewModel.busy.collectAsStateWithLifecycle()
    val error by authViewModel.error.collectAsStateWithLifecycle()
    val notice by authViewModel.notice.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(NestlyColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Nestly", style = NestlyType.Display)
        Text(
            if (signingUp) "Create your account to share tracking with your family."
            else "Sign in to see your family's tracking.",
            style = NestlyType.Small,
            color = NestlyColors.TextSecondary,
        )
        OutlinedTextField(
            email, { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            password, { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (signingUp) {
            OutlinedTextField(
                caregiverName, { caregiverName = it },
                label = { Text("Your name (as caregiver)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        error?.let { Text(it, style = NestlyType.Small, color = NestlyColors.Danger) }
        notice?.let { Text(it, style = NestlyType.Small, color = NestlyColors.TextSecondary) }
        Button(
            onClick = {
                if (signingUp) authViewModel.signUp(email, password, caregiverName)
                else authViewModel.signIn(email, password)
            },
            enabled = !busy && email.contains("@") && password.length >= 6 && (!signingUp || caregiverName.isNotBlank()),
            colors = ButtonDefaults.buttonColors(containerColor = NestlyColors.AccentText),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Please wait…" else if (signingUp) "Create account" else "Sign in")
        }
        TextButton(onClick = { signingUp = !signingUp }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
                if (signingUp) "Already have an account? Sign in" else "New to Nestly? Create an account",
                color = NestlyColors.AccentText,
            )
        }
    }
}

private val CSV_MIME_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "application/csv",
    "text/plain",
    "text/tab-separated-values",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
    "application/vnd.ms-excel", // .xls
    "application/zip",
    "application/octet-stream",
)

/** Signed in but no child yet: create a family, join one by invite, then add the first child. */
@Composable
fun OnboardingScreen(authViewModel: AuthViewModel, hasFamily: Boolean) {
    var mode by remember { mutableStateOf(if (hasFamily) "child" else "choose") }
    var caregiverName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var addingChild by remember { mutableStateOf(false) }
    val busy by authViewModel.busy.collectAsStateWithLifecycle()
    val error by authViewModel.error.collectAsStateWithLifecycle()
    val naraStatus by authViewModel.naraStatus.collectAsStateWithLifecycle()
    val naraPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(authViewModel::importNara) }

    Column(
        Modifier
            .fillMaxSize()
            .background(NestlyColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Welcome to Nestly", style = NestlyType.Display)
        error?.let { Text(it, style = NestlyType.Small, color = NestlyColors.Danger) }
        when (mode) {
            "choose" -> {
                Text(
                    "Start a new family, or join one with an invite code from another caregiver.",
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                )
                OutlinedTextField(
                    caregiverName, { caregiverName = it },
                    label = { Text("Your name (as caregiver)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { authViewModel.createFamily(caregiverName); mode = "child" },
                    enabled = !busy && caregiverName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NestlyColors.AccentText),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Create my family") }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    inviteCode, { inviteCode = it.uppercase() },
                    label = { Text("Invite code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = { authViewModel.joinFamily(inviteCode, caregiverName) },
                    enabled = !busy && caregiverName.isNotBlank() && inviteCode.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Join with invite code", color = NestlyColors.AccentText) }
            }
            else -> {
                Text(
                    "Add your first child to start tracking, or do a one-time import of your Nara Baby history.",
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                )
                Button(
                    onClick = { addingChild = true },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = NestlyColors.AccentText),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add child") }
                OutlinedButton(
                    onClick = { naraPicker.launch(CSV_MIME_TYPES) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (busy) "Importing…" else "Import from Nara instead", color = NestlyColors.AccentText) }
                naraStatus?.let {
                    Text(it, style = NestlyType.Small, color = NestlyColors.TextSecondary)
                }
                if (!hasFamily) {
                    TextButton(onClick = { mode = "choose" }) {
                        Text("Back", color = NestlyColors.AccentText)
                    }
                }
            }
        }
        TextButton(onClick = { authViewModel.signOut() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Sign out", color = NestlyColors.TextSecondary)
        }
    }

    if (addingChild) {
        AddChildDialog(
            askCaregiverName = false,
            onDismiss = { addingChild = false },
            onConfirm = { name, sex, birthDate, _ ->
                authViewModel.addFirstChild(name, sex, birthDate, caregiverName)
                addingChild = false
            },
        )
    }
}
