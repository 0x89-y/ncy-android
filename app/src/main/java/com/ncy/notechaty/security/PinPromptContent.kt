package com.ncy.notechaty.security

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.ncy.notechaty.R
import com.ncy.notechaty.ui.theme.NcyButtonShape
import com.ncy.notechaty.ui.theme.NcyInkButton
import com.ncy.notechaty.ui.theme.NcyInkTile
import com.ncy.notechaty.ui.theme.NcyTheme
import com.ncy.notechaty.ui.theme.ncyPaper

/**
 * Full-screen PIN entry: the app lock, protected chats and protected profiles.
 *
 * A number pad by default. PINs were always set with a full keyboard, so some hold
 * letters; "Use keyboard" switches to a text field for those. Nothing is checked
 * until Unlock, so a half-typed PIN never counts as a failed attempt.
 */
@Composable
fun PinPromptContent(
    title: String,
    subtitle: String,
    biometricTitle: String,
    biometricSubtitle: String,
    useBiometric: Boolean,
    onVerify: (String) -> Boolean,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    /** Shows a back arrow when the prompt can be left without unlocking. */
    onCancel: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? FragmentActivity
    val canUseBio = useBiometric && activity != null && BiometricHelper.canUseBiometric(context)
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var useKeyboard by remember { mutableStateOf(false) }
    val lockoutSeconds = rememberPinLockoutSeconds(failedAttempts)
    val locked = lockoutSeconds > 0

    val askBiometric = {
        if (canUseBio) {
            BiometricHelper.authenticate(
                activity = activity!!,
                title = biometricTitle,
                subtitle = biometricSubtitle,
                onSuccess = onUnlocked
            )
        }
    }
    LaunchedEffect(canUseBio) { askBiometric() }

    val submit = {
        if (pin.isNotEmpty() && !locked) {
            if (onVerify(pin)) {
                onUnlocked()
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                error = true
                pin = ""
                failedAttempts++
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .ncyPaper()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            if (onCancel != null) {
                IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        }
        Spacer(Modifier.weight(0.4f))
        NcyInkTile(size = 56.dp) { Icon(Icons.Outlined.Lock, null, Modifier.size(26.dp)) }
        Spacer(Modifier.height(14.dp))
        Text(title, style = NcyTheme.type.pageTitle.copy(fontSize = 30.sp, lineHeight = 34.sp), modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                locked -> stringResource(R.string.pin_try_again_in, formatWait(lockoutSeconds))
                error -> stringResource(R.string.pin_wrong)
                else -> subtitle
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (error || locked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        if (useKeyboard) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.take(64); error = false },
                label = { Text(stringResource(R.string.pin)) },
                singleLine = true,
                isError = error,
                enabled = !locked,
                shape = NcyButtonShape,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp).focusRequester(focus),
            )
            Spacer(Modifier.weight(1f))
        } else {
            // One dot per digit typed, with at least four places shown.
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 26.dp).height(16.dp),
            ) {
                repeat(maxOf(4, pin.length)) { i ->
                    val filled = i < pin.length
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .then(
                                if (filled) Modifier.background(MaterialTheme.colorScheme.onSurface)
                                else Modifier.border(1.5.dp, if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, CircleShape)
                            )
                    )
                }
            }
            Spacer(Modifier.weight(0.6f))
            val letters = listOf("", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ")
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                (0 until 3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        (1..3).forEach { col ->
                            val n = row * 3 + col
                            PadKey(enabled = !locked, label = n.toString(), sub = letters[n - 1]) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (pin.length < 64) pin += n.toString()
                                error = false
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    val fingerprint = stringResource(R.string.use_fingerprint)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .then(if (canUseBio) Modifier.clickable(role = Role.Button) { askBiometric() }.semantics { contentDescription = fingerprint } else Modifier),
                    ) {
                        if (canUseBio) Icon(Icons.Default.Fingerprint, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    PadKey(enabled = !locked, label = "0", sub = "") {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (pin.length < 64) pin += "0"
                        error = false
                    }
                    val delete = stringResource(R.string.pin_delete_digit)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable(enabled = pin.isNotEmpty(), role = Role.Button) { pin = pin.dropLast(1) }
                            .semantics { contentDescription = delete },
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Backspace, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        NcyInkButton(
            text = stringResource(R.string.unlock),
            onClick = submit,
            enabled = pin.isNotEmpty() && !locked,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        )
        Text(
            stringResource(if (useKeyboard) R.string.pin_use_keypad else R.string.pin_use_keyboard),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 4.dp)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { useKeyboard = !useKeyboard; pin = "" }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

/** One round key of the number pad: a serif digit with its letters underneath. */
@Composable
private fun PadKey(enabled: Boolean, label: String, sub: String, onClick: () -> Unit) {
    val c = NcyTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, c.line, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Text(label, style = NcyTheme.type.name.copy(fontSize = 28.sp, lineHeight = 30.sp))
        if (sub.isNotEmpty()) Text(sub, style = NcyTheme.type.label.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
