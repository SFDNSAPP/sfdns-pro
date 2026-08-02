package com.sfdnsapp.pro

import android.content.SharedPreferences

fun SharedPreferences.getSafeString(key: String, defaultValue: String): String {
    return try {
        this.getString(key, defaultValue) ?: defaultValue
    } catch (e: ClassCastException) {
        try {
            val boolVal = this.getBoolean(key, false)
            if (boolVal) "true" else defaultValue
        } catch (e2: Exception) {
            defaultValue
        }
    } catch (e: Exception) {
        defaultValue
    }
}

fun SharedPreferences.getSafeBoolean(key: String, defaultValue: Boolean): Boolean {
    return try {
        this.getBoolean(key, defaultValue)
    } catch (e: ClassCastException) {
        try {
            val str = this.getString(key, null)
            if (str != null) {
                str.equals("true", ignoreCase = true)
            } else {
                defaultValue
            }
        } catch (e2: Exception) {
            defaultValue
        }
    } catch (e: Exception) {
        defaultValue
    }
}
