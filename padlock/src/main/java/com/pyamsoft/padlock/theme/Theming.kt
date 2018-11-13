package com.pyamsoft.padlock.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.CheckResult
import androidx.core.content.edit
import androidx.preference.PreferenceManager

object Theming {

  private const val DARK_THEME_DEFAULT = false
  private const val DARK_THEME_KEY = "dark_theme"

  @CheckResult
  private fun preferences(context: Context): SharedPreferences {
    return PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
  }

  @CheckResult
  fun isDarkTheme(context: Context): Boolean {
    return preferences(context).getBoolean(DARK_THEME_KEY, DARK_THEME_DEFAULT)
  }

  fun setDarkTheme(
    context: Context,
    dark: Boolean
  ) {
    preferences(context).edit {
      putBoolean(DARK_THEME_KEY, dark)
    }
  }
}
