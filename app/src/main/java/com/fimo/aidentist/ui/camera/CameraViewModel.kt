package com.fimo.aidentist.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fimo.aidentist.data.model.RecognitionResult
import com.fimo.aidentist.data.model.Resource
import com.fimo.aidentist.data.repository.AuthRepository
import com.fimo.aidentist.data.repository.UserRepository
import com.fimo.aidentist.ml.Classifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CameraViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _classificationState = MutableStateFlow<Resource<RecognitionResult>?>(null)
    val classificationState: StateFlow<Resource<RecognitionResult>?> = _classificationState.asStateFlow()

    /**
     * Classifies the given bitmap using TFLite Classifier and saves
     * the result (disease + confidence) to Firestore.
     */
    fun classifyAndSave(bitmap: Bitmap, classifier: Classifier) {
        viewModelScope.launch {
            _classificationState.value = Resource.Loading()

            // 1. Classify image via TFLite
            val result = classifier.recognizeImage(bitmap)
            if (result.isEmpty()) {
                _classificationState.value = Resource.Error("No results from classification")
                return@launch
            }

            val topResult = result[0]

            // 2. Save to Firestore
            val uid = authRepository.currentUser?.uid
            if (uid != null) {
                val saveResult = userRepository.saveDiseaseData(
                    uid, topResult.title, topResult.confidence
                )
                if (saveResult is Resource.Error) {
                    _classificationState.value = Resource.Error(
                        saveResult.message ?: "Failed to save disease data"
                    )
                    return@launch
                }
            }

            // 3. Emit success with recognition result
            _classificationState.value = Resource.Success(
                RecognitionResult(topResult.title, topResult.confidence)
            )
        }
    }

    fun resetClassificationState() {
        _classificationState.value = null
    }
}
