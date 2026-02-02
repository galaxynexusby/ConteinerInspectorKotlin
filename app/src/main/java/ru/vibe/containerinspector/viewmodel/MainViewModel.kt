package ru.vibe.containerinspector.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.vibe.containerinspector.data.AppDatabase
import ru.vibe.containerinspector.data.Operator

import ru.vibe.containerinspector.data.*

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.Calendar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONArray

data class SessionState(
    val operator: String = "",
    val shift: Int = 1,
    val containerNumber: String = "",
    val isContainerValid: Boolean = false,
    val currentStep: Int = 0,
    val photos: List<String> = emptyList(),
    val isAdmin: Boolean = false
)

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Loading : ConnectionState()
    object Success : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

sealed class RemoteCheckState {
    object Idle : RemoteCheckState()
    object Checking : RemoteCheckState()
    object Success : RemoteCheckState()
    object AlreadyExists : RemoteCheckState()
    data class NetworkError(val message: String) : RemoteCheckState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val operatorDao = database.operatorDao()
    private val prefs = application.getSharedPreferences("inspector_prefs", Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow(
        SessionState(
            operator = prefs.getString("last_operator", "") ?: "",
            shift = prefs.getInt("last_shift", 1)
        )
    )
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _ncConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val ncConnectionState: StateFlow<ConnectionState> = _ncConnectionState.asStateFlow()

    private val _remoteCheckState = MutableStateFlow<RemoteCheckState>(RemoteCheckState.Idle)
    val remoteCheckState: StateFlow<RemoteCheckState> = _remoteCheckState.asStateFlow()

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
        prefs.edit().putString("last_operator", name).apply()
        _sessionState.value = _sessionState.value.copy(
            operator = name,
            isAdmin = name == "admin"
        )
    }

    fun setShift(shift: Int) {
        prefs.edit().putInt("last_shift", shift).apply()
        _sessionState.value = _sessionState.value.copy(shift = shift)
    }

    // Nextcloud Connection Test
    fun testConnection() {
        val config = appConfig.value ?: return
        viewModelScope.launch {
            _ncConnectionState.value = ConnectionState.Loading
            val url = config.nextcloudUrl ?: ""
            val user = config.nextcloudUser ?: ""
            val pass = config.nextcloudPass ?: ""

            if (url.isEmpty()) {
                _ncConnectionState.value = ConnectionState.Error("URL не указан")
                return@launch
            }
            
            val result = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val auth = android.util.Base64.encodeToString("$user:$pass".toByteArray(), android.util.Base64.NO_WRAP)
                    
                    val request = Request.Builder()
                        .url(url)
                        .head()
                        .addHeader("Authorization", "Basic $auth")
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        ConnectionState.Success
                    } else {
                        ConnectionState.Error("Ошибка ${response.code}: ${response.message}")
                    }
                } catch (e: Exception) {
                    ConnectionState.Error("Сбой сети: ${e.localizedMessage}")
                }
            }
            _ncConnectionState.value = result
        }
    }

    // Export Helpers
    fun getExportJson(): String {
        val ops = allOperators.value
        val config = appConfig.value
        
        val opsArray = JSONArray()
        ops.forEach { op ->
            val obj = JSONObject().apply {
                put("name", op.name)
                put("shift", op.shift)
                put("password", op.password)
            }
            opsArray.put(obj)
        }
        
        val root = JSONObject().apply {
            put("operators", opsArray)
            config?.let {
                val c = JSONObject().apply {
                    put("url", it.nextcloudUrl)
                    put("user", it.nextcloudUser)
                    put("pass", it.nextcloudPass)
                }
                put("config", c)
            }
        }
        return root.toString(4)
    }

    fun importData(jsonString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonString)
                
                // Import Operators
                if (root.has("operators")) {
                    val opsArray = root.getJSONArray("operators")
                    for (i in 0 until opsArray.length()) {
                        val obj = opsArray.getJSONObject(i)
                        val name = obj.getString("name")
                        val shift = obj.getInt("shift")
                        val password = obj.optString("password", "123")
                        
                        // Check if exists or just insert (DAO handles insert/update if needed)
                        operatorDao.insertOperator(Operator(name = name, shift = shift, password = password))
                    }
                }
                
                // Import Config
                if (root.has("config")) {
                    val c = root.getJSONObject("config")
                    updateConfig(
                        c.optString("url", ""),
                        c.optString("user", ""),
                        c.optString("pass", "")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    fun checkAndLockRemoteContainer(number: String) {
        val config = appConfig.value ?: return
        val session = _sessionState.value
        
        viewModelScope.launch {
            _remoteCheckState.value = RemoteCheckState.Checking
            
            val result = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val user = config.nextcloudUser ?: ""
                    val pass = config.nextcloudPass ?: ""
                    val auth = android.util.Base64.encodeToString("$user:$pass".toByteArray(), android.util.Base64.NO_WRAP)
                    val authHeader = "Basic $auth"
                    
                    val baseUrl = if (config.nextcloudUrl?.endsWith("/") == true) config.nextcloudUrl else "${config.nextcloudUrl}/"
                    val fullDavUrl = "${baseUrl}remote.php/dav/files/$user/"
                    
                    val calendar = Calendar.getInstance()
                    val year = calendar.get(Calendar.YEAR).toString()
                    val month = getRussianMonthName(calendar.get(Calendar.MONTH))
                    val shift = "Смена${session.shift}"
                    
                    val path = "$year/$month/$shift/$number/"
                    val checkUrl = "$fullDavUrl$path"
                    
                    // 1. Check if folder exists using PROPFIND
                    val checkRequest = Request.Builder()
                        .url(checkUrl)
                        .method("PROPFIND", null)
                        .addHeader("Authorization", authHeader)
                        .addHeader("Depth", "0")
                        .build()
                        
                    client.newCall(checkRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            return@withContext RemoteCheckState.AlreadyExists
                        } else if (response.code != 404) {
                            return@withContext RemoteCheckState.NetworkError("Ошибка проверки: ${response.code}")
                        }
                    }
                    
                    // 2. Folder does not exist, create it to "lock" the container
                    // We need to create parent folders first (logic similar to SyncWorker)
                    createRemoteFolders(client, fullDavUrl, path, authHeader)
                    RemoteCheckState.Success
                    
                } catch (e: Exception) {
                    RemoteCheckState.NetworkError("Нет связи с облаком: ${e.localizedMessage}")
                }
            }
            _remoteCheckState.value = result
        }
    }

    private fun createRemoteFolders(client: OkHttpClient, baseUrl: String, path: String, auth: String) {
        val parts = path.split("/").filter { it.isNotEmpty() }
        var currentPath = ""
        for (part in parts) {
            currentPath += "$part/"
            val request = Request.Builder()
                .url("$baseUrl$currentPath")
                .method("MKCOL", null)
                .addHeader("Authorization", auth)
                .build()
            client.newCall(request).execute().close()
        }
    }

    private fun getRussianMonthName(monthIndex: Int): String {
        return when (monthIndex) {
            0 -> "Январь"; 1 -> "Февраль"; 2 -> "Март"; 3 -> "Апрель"
            4 -> "Май"; 5 -> "Июнь"; 6 -> "Июль"; 7 -> "Август"
            8 -> "Сентябрь"; 9 -> "Октябрь"; 10 -> "Ноябрь"; 11 -> "Декабрь"
            else -> "Неизвестно"
        }
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
        prefs.edit().remove("last_operator").remove("last_shift").apply()
        _sessionState.value = SessionState()
        _activeReport.value = null
    }
}
