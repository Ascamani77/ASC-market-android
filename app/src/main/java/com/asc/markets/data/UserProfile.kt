package com.asc.markets.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val firstName: String = "",
    val surname: String = "",
    val fullName: String = "User",
    val email: String = "",
    val phone: String = "",
    val firm: String = "",
    val region: String = "",
    val role: String = "Trader"
) {
    val initials: String
        get() {
            val parts = fullName.trim().split("\\s+".toRegex())
            return if (parts.size >= 2) {
                "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
            } else {
                fullName.take(2).uppercase()
            }
        }
}

object UserProfileStore {
    private const val PREFS_NAME = "asc_user_profile"
    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_SURNAME = "surname"
    private const val KEY_EMAIL = "email"
    private const val KEY_PHONE = "phone"
    private const val KEY_FIRM = "firm"
    private const val KEY_REGION = "region"
    private const val KEY_ROLE = "role"

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val p = prefs!!
        val firstName = p.getString(KEY_FIRST_NAME, "") ?: ""
        val surname = p.getString(KEY_SURNAME, "") ?: ""
        val fullName = if (firstName.isNotBlank() || surname.isNotBlank()) "$firstName $surname".trim() else "User"
        _profile.value = UserProfile(
            firstName = firstName,
            surname = surname,
            fullName = fullName,
            email = p.getString(KEY_EMAIL, "") ?: "",
            phone = p.getString(KEY_PHONE, "") ?: "",
            firm = p.getString(KEY_FIRM, "") ?: "",
            region = p.getString(KEY_REGION, "") ?: "",
            role = p.getString(KEY_ROLE, "Trader") ?: "Trader"
        )
    }

    fun updateProfile(newProfile: UserProfile) {
        _profile.value = newProfile
        save()
    }

    fun updateFirstName(name: String) {
        val current = _profile.value
        val fullName = "$name ${current.surname}".trim().ifBlank { "User" }
        _profile.value = current.copy(firstName = name, fullName = fullName)
        save()
    }

    fun updateSurname(surname: String) {
        val current = _profile.value
        val fullName = "${current.firstName} $surname".trim().ifBlank { "User" }
        _profile.value = current.copy(surname = surname, fullName = fullName)
        save()
    }

    fun updateFullName(name: String) {
        _profile.value = _profile.value.copy(fullName = name)
        save()
    }

    fun updateEmail(email: String) {
        _profile.value = _profile.value.copy(email = email)
        save()
    }

    fun updatePhone(phone: String) {
        _profile.value = _profile.value.copy(phone = phone)
        save()
    }

    fun updateFirm(firm: String) {
        _profile.value = _profile.value.copy(firm = firm)
        save()
    }

    fun updateRegion(region: String) {
        _profile.value = _profile.value.copy(region = region)
        save()
    }

    private fun save() {
        val p = prefs ?: return
        val prof = _profile.value
        p.edit().apply {
            putString(KEY_FIRST_NAME, prof.firstName)
            putString(KEY_SURNAME, prof.surname)
            putString(KEY_EMAIL, prof.email)
            putString(KEY_PHONE, prof.phone)
            putString(KEY_FIRM, prof.firm)
            putString(KEY_REGION, prof.region)
            putString(KEY_ROLE, prof.role)
            apply()
        }
    }
}
