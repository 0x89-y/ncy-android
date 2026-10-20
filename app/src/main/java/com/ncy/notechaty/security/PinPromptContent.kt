package com.ncy.notechaty.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.ncy.notechaty.ui.desktoptheme.NcyAvatarShape
import com.ncy.notechaty.ui.desktoptheme.NcyButtonShape
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid

@Composable
fun PinPromptContent(
    title: String,
    subtitle: String,
    biometricTitle: String,
    biometricSubtitle: String,
    useBiometric: Boolean,
    onVerify: (String) -> Boolean,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val canUseBio = useBiometric && activity != null && BiometricHelper.canUseBiometric(context)
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    val lockoutSeconds = rememberPinLockoutSeconds(failedAttempts)

    LaunchedEffect(canUseBio) {
        // `canUseBio` already includes `activity != null` at construction — the
        // Kotlin compiler now tracks that flow and complains about a redundant
        // null check. `!!` is safe here for the same reason.
        if (canUseBio) {
            BiometricHelper.authenticate(
                activity = activity!!,
                title = biometricTitle,
                subtitle = biometricSubtitle,
                onSuccess = onUnlocked
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .ncyDottedGrid()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Accent avatar-style lock badge, matching the app's round icons.
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(NcyAvatarShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = {
                pin = it.take(64)
                error = false
            },
            label = { Text("PIN") },
            singleLine = true,
            isError = error,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = pinSupportingText(error, lockoutSeconds),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (onVerify(pin)) {
                    onUnlocked()
                } else {
                    error = true
                    pin = ""
                    failedAttempts++
                }
            },
            enabled = pin.isNotEmpty() && lockoutSeconds == 0,
            shape = NcyButtonShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock")
        }
        if (canUseBio) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    BiometricHelper.authenticate(
                        activity = activity!!,
                        title = biometricTitle,
                        subtitle = biometricSubtitle,
                        onSuccess = onUnlocked
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use fingerprint")
            }
        }
    }
}
