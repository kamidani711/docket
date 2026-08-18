package com.docket.ui.screens.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.common.Result
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data class InProgress(val message: String) : BackupUiState
    data object BackupComplete : BackupUiState
    data object RestoreStaged : BackupUiState
    data class Failed(val message: String) : BackupUiState
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun createBackup(destinationUri: Uri, passphrase: String) {
        viewModelScope.launch {
            _state.value = BackupUiState.InProgress("Starting…")
            val result = backupManager.createBackup(destinationUri, passphrase.toCharArray()) { message ->
                _state.value = BackupUiState.InProgress(message)
            }
            if (result is Result.Success) analyticsRepository.logEvent(AnalyticsEventType.FEATURE_USED, "backup_created")
            _state.value = when (result) {
                is Result.Success -> BackupUiState.BackupComplete
                is Result.Error -> BackupUiState.Failed(result.message ?: "Backup failed.")
                Result.Loading -> BackupUiState.Idle
            }
        }
    }

    fun restoreBackup(sourceUri: Uri, passphrase: String) {
        viewModelScope.launch {
            _state.value = BackupUiState.InProgress("Starting…")
            val result = backupManager.restoreBackup(sourceUri, passphrase.toCharArray()) { message ->
                _state.value = BackupUiState.InProgress(message)
            }
            _state.value = when (result) {
                is Result.Success -> BackupUiState.RestoreStaged
                is Result.Error -> BackupUiState.Failed(result.message ?: "Restore failed.")
                Result.Loading -> BackupUiState.Idle
            }
        }
    }

    fun reset() {
        _state.value = BackupUiState.Idle
    }
}
