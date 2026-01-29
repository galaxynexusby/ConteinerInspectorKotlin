package ru.vibe.containerinspector.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.vibe.containerinspector.data.AppDatabase
import ru.vibe.containerinspector.data.Operator

import ru.vibe.containerinspector.data.*

data class SessionState(
    val operator: String = "",
    val shift: Int = 1,
    val containerNumber: String = "",
    val isContainerValid: Boolean = false,
    val currentStep: Int = 0,
    val photos: List<String> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val operatorDao = database.operatorDao()

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    // Flows from DB
    // List of operators with "admin" always present
    val allOperators: StateFlow<List<Operator>> = operatorDao.getAllOperators()
        .map { list ->
            if (list.none { it.name == "admin" }) {
                listOf(Operator(name = "admin", shift = 1, password = "fasovka")) + list
            } else {
                list
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(Operator(name = "admin", shift = 1, password = "fasovka")))

    val allReports: StateFlow<List<InspectionReport>> = operatorDao.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appConfig: StateFlow<AppConfig?> = operatorDao.getConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current active inspection in memory
    private val _activeReport = MutableStateFlow<InspectionReport?>(null)
    val activeReport: StateFlow<InspectionReport?> = _activeReport.asStateFlow()

    suspend fun getOperatorCount(): Int = operatorDao.getCount()

    fun addOperator(name: String, shift: Int, password: String) {
        viewModelScope.launch {
            operatorDao.insertOperator(Operator(name = name, shift = shift, password = password))
        }
    }

    fun deleteOperator(operator: Operator) {
        viewModelScope.launch {
            operatorDao.deleteOperator(operator)
        }
    }

    fun setOperator(name: String) {
        _sessionState.value = _sessionState.value.copy(operator = name)
    }

    fun setShift(shift: Int) {
        _sessionState.value = _sessionState.value.copy(shift = shift)
    }

    // OCR Logic
    fun setContainerNumber(number: String) {
        val uppercaseNumber = number.uppercase()
        val isValid = ru.vibe.containerinspector.logic.ContainerValidator.isValid(uppercaseNumber)
        _sessionState.value = _sessionState.value.copy(
            containerNumber = uppercaseNumber,
            isContainerValid = isValid
        )
    }

    suspend fun checkContainerExists(number: String): Boolean {
        // We check in the database directly for robustness
        return allReports.value.any { it.containerNumber == number.uppercase() }
    }

    fun confirmContainerScan() {
        val state = _sessionState.value
        val newReport = InspectionReport(
            containerNumber = state.containerNumber,
            operatorName = state.operator,
            shift = state.shift,
            status = ReportStatus.READY_TO_INSPECT
        )
        viewModelScope.launch {
            operatorDao.insertReport(newReport)
            _sessionState.value = state.copy(containerNumber = "", isContainerValid = false)
        }
    }

    // Inspection Flow
    fun startInspection(report: InspectionReport) {
        viewModelScope.launch {
            val updatedReport = report.copy(
                status = ReportStatus.IN_PROGRESS
            )
            operatorDao.updateReport(updatedReport)
            _activeReport.value = updatedReport
        }
    }

    fun addPhoto(path: String) {
        val report = _activeReport.value ?: return
        
        val updatedReport = when (report.currentStep) {
            0 -> report.copy(photo1 = path)
            1 -> report.copy(photo2 = path)
            2 -> report.copy(photo3 = path)
            3 -> report.copy(photo4 = path)
            4 -> report.copy(photo5 = path)
            5 -> report.copy(photo6 = path)
            6 -> report.copy(photo7 = path)
            else -> report
        }
        
        viewModelScope.launch {
            operatorDao.updateReport(updatedReport)
            _activeReport.value = updatedReport
        }
    }

    fun nextStep() {
        val report = _activeReport.value ?: return
        val nextStep = report.currentStep + 1
        
        val updatedReport = if (nextStep >= 7) {
            report.copy(status = ReportStatus.COMPLETED)
        } else {
            report.copy(currentStep = nextStep)
        }
        
        viewModelScope.launch {
            operatorDao.updateReport(updatedReport)
            _activeReport.value = updatedReport
        }
    }

    fun postponeInspection() {
        val report = _activeReport.value ?: return
        viewModelScope.launch {
            val updatedReport = report.copy(status = ReportStatus.UNFINISHED)
            operatorDao.updateReport(updatedReport)
            _activeReport.value = null
        }
    }

    fun deleteReport(report: InspectionReport) {
        viewModelScope.launch {
            operatorDao.deleteReport(report)
        }
    }

    // Settings
    fun updateConfig(url: String, user: String, pass: String) {
        viewModelScope.launch {
            operatorDao.insertConfig(AppConfig(nextcloudUrl = url, nextcloudUser = user, nextcloudPass = pass))
        }
    }

    fun resetSession() {
        _sessionState.value = SessionState()
        _activeReport.value = null
    }
}
