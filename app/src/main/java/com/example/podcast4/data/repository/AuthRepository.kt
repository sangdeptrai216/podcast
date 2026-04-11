package com.example.podcast4.data.repository

import android.content.Context
import com.example.podcast4.data.local.PodcastDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val podcastDao: PodcastDao
) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val userPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
    
    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _userName = MutableStateFlow(prefs.getString("user_name", ""))
    val userName: StateFlow<String?> = _userName

    fun register(username: String, password: String): Boolean {
        if (userPrefs.contains(username)) {
            return false // User already exists
        }
        userPrefs.edit().putString(username, password).apply()
        return true
    }

    fun login(username: String, password: String): Boolean {
        val savedPassword = userPrefs.getString(username, null)
        if (savedPassword == password) {
            prefs.edit().apply {
                putBoolean("is_logged_in", true)
                putString("user_name", username)
                apply()
            }
            _isLoggedIn.value = true
            _userName.value = username
            return true
        }
        return false
    }

    fun logout() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
        _userName.value = ""
        
        // Không xóa podcast khi đăng xuất để giữ lại các bản tải xuống trên thiết bị
        // CoroutineScope(Dispatchers.IO).launch {
        //     podcastDao.clearAllDownloads()
        // }
    }
}
