package com.nutriscan.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Gerencia a preferência do usuário para ativar o modo de alerta diabético.
 *
 * Armazena em [SharedPreferences] se o usuário é diabético e quer
 * ver alertas de risco glicêmico nos produtos.
 */
class DiabeticPreference(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /** Retorna true se o modo de alerta diabético está ativado. */
    fun isDiabeticModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DIABETIC_MODE, false)
    }

    /** Ativa ou desativa o modo de alerta diabético. */
    fun setDiabeticModeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DIABETIC_MODE, enabled) }
    }

    companion object {
        private const val PREFS_NAME = "nutriscan_diabetic_prefs"
        private const val KEY_DIABETIC_MODE = "diabetic_mode_enabled"
    }
}
