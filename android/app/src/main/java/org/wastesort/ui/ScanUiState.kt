package org.wastesort.ui

import android.graphics.Bitmap
import org.wastesort.model.DisposalRecommendation

sealed interface ScanUiState {
    data object PermissionRequired : ScanUiState
    data object Scanning : ScanUiState
    data class Analyzing(val frozenBitmap: Bitmap) : ScanUiState
    data class ResultReady(
        val recommendation: DisposalRecommendation,
        val frozenBitmap: Bitmap,
    ) : ScanUiState
    data class Error(val message: String) : ScanUiState
}
