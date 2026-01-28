package ru.vibe.containerinspector.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionState(
    val operator: String = "",
    val shift: Int = 1,
    val containerNumber: String = "",
    val isContainerValid: Boolean = false,
    val currentStep: Int = 1,
    val photos: List<String> = emptyList()
)

class MainViewModel : ViewModel() {
    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun setOperator(name: String) {
        _sessionState.value = _sessionState.value.copy(operator = name)
    }

    fun setShift(shift: Int) {
        _sessionState.value = _sessionState.value.copy(shift = shift)
    }

    fun setContainerNumber(number: String) {
        val isValid = ru.vibe.containerinspector.logic.ContainerValidator.isValid(number)
        _sessionState.value = _sessionState.value.copy(
            containerNumber = number.uppercase(),
            isContainerValid = isValid
        )
    fun addPhoto(path: String) {
        val currentPhotos = _sessionState.value.photos.toMutableList()
        currentPhotos.add(path)
        
        val nextStep = _sessionState.value.currentStep + 1
        _sessionState.value = _sessionState.value.copy(
            photos = currentPhotos,
            currentStep = nextStep
        )
    }

    fun resetSession() {
        _sessionState.value = SessionState()
    }
}
