package com.example.voicetotext.voiceaction.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicetotext.action.data.PromptModelStatus

@Composable
fun VoiceActionRoute(
    isDebug: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: VoiceActionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val modelStatus by viewModel.promptModelStatus.collectAsStateWithLifecycle(
        initialValue = PromptModelStatus.Checking
    )
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onMicrophonePermissionUpdated(isGranted)
    }

    LaunchedEffect(context) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onMicrophonePermissionUpdated(isGranted)
    }

    LaunchedEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        kotlinx.coroutines.awaitCancellation()
        lifecycleOwner.lifecycle.removeObserver(observer)
    }

    VoiceActionScreen(
        uiState = uiState,
        modelStatus = modelStatus,
        isDebug = isDebug,
        onMicTapped = viewModel::onMicTapped,
        onRequestMicrophonePermission = {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onActionResolved = viewModel::onActionResolved,
        onRunActionClicked = viewModel::onRunActionClicked,
        onResetClicked = viewModel::onResetClicked,
        modifier = modifier
    )
}
