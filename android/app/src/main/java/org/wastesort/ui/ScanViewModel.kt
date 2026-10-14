package org.wastesort.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wastesort.model.ClassificationOutput
import org.wastesort.model.DecisionStatus
import org.wastesort.rules.RulesEngine
import org.wastesort.vision.WasteClassifierClient

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Scanning)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var classifier: WasteClassifierClient? = null
    private val rulesEngine: RulesEngine

    init {
        try {
            classifier = WasteClassifierClient(application)
        } catch (_: Exception) {
            _uiState.value = ScanUiState.Error("Failed to initialize ML model")
        }
        rulesEngine = RulesEngine.fromAssets(application)
    }

    fun onFrameCaptured(bitmap: Bitmap) {
        if (_uiState.value is ScanUiState.Analyzing) return

        _uiState.value = ScanUiState.Analyzing(bitmap)

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val classification = classifier?.classify(bitmap, 0)
                    ?: ClassificationOutput("unknown", 0f, DecisionStatus.NOT_SURE)
                val recommendation = rulesEngine.resolve(classification)
                _uiState.value = ScanUiState.ResultReady(recommendation, bitmap)
            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error(e.localizedMessage ?: "Inference error")
            }
        }
    }

    fun onManualSelect(classId: String) {
        val currentBitmap = when (val state = _uiState.value) {
            is ScanUiState.ResultReady -> state.frozenBitmap
            is ScanUiState.Analyzing -> state.frozenBitmap
            else -> Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        val classification = ClassificationOutput(classId, 1.0f, DecisionStatus.CONFIDENT)
        val recommendation = rulesEngine.resolve(classification)
        _uiState.value = ScanUiState.ResultReady(recommendation, currentBitmap)
    }

    fun resetScan() {
        _uiState.value = ScanUiState.Scanning
    }

    override fun onCleared() {
        super.onCleared()
        classifier?.close()
    }
}
