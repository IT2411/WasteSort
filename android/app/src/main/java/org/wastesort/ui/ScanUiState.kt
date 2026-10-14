package org.wastesort.ui

import org.wastesort.model.DisposalRecommendation

sealed interface ScanUiState {
    data object PermissionRequired : ScanUiState
    data object Scanning : ScanUiState
    data object Analyzing : ScanUiState
    data class ResultReady(val recommendation: DisposalRecommendation) : ScanUiState
    data class Error(val message: String) : ScanUiState
}
