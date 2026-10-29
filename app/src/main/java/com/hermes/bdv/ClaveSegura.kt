package com.hermes.bdv

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Almacena la clave bancaria de forma CIFRADA en el dispositivo
 * (AndroidX Security: EncryptedSharedPreferences con MasterKey AES256).
 *
 * La clave se configura UNA VEZ desde la app (petición del usuario 29/09/2026)
 * y el servicio la lee al iniciar cada ejecución. Nunca se envía a Telegram,
 * nunca aparece en logs y nunca sale del teléfono.
 */
object ClaveSegura {
    private const val TAG = "HermesClave"
    private const val PREFS = "hermes_clave_cifrada"
    private const val K_CLAVE = "clave"
    private const val K_PIN = "pin_operaciones"   // clave/PIN para confirmar operaciones

    @Volatile
    private var cache: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences? {
        cache?.let { return it }
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { cache = it }
        } catch (e: Exception) {
            Log.e(TAG, "no se pudo abrir el almacén cifrado", e)
            null
        }
    }

    /** Guarda la clave cifrada. Retorna true si se guardó. */
    fun guardar(context: Context, clave: String): Boolean {
        val c = clave.trim()
        if (c.isEmpty()) return false
        return try {
            prefs(context)?.edit()?.putString(K_CLAVE, c)?.commit() == true
        } catch (e: Exception) {
            Log.e(TAG, "no se pudo guardar la clave", e)
            false
        }
    }

    /** Lee la clave (solo el servicio la usa al ejecutar). */
    fun leer(context: Context): String {
        return try {
            prefs(context)?.getString(K_CLAVE, "").orEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "no se pudo leer la clave", e)
            ""
        }
    }

    /** true si hay una clave configurada. */
    fun configurada(context: Context): Boolean = leer(context).isNotEmpty()

    /** Borra la clave del almacén cifrado. */
    fun borrar(context: Context) {
        try {
            prefs(context)?.edit()?.remove(K_CLAVE)?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "no se pudo borrar la clave", e)
        }
    }

    // ------------------------------------------------------------------
    // Clave/PIN de operaciones (opcional): solo se usa si el BDV confirma la
    // compra con una clave ESCRITA en su propia app. Se guarda cifrada, nunca
    // se envía a Telegram ni aparece en logs. Si el banco usa huella, se deja
    // vacío y no se usa.
    // ------------------------------------------------------------------

    /** Guarda el PIN/clave de operaciones (cifrado). Retorna true si se guardó. */
    fun guardarPin(context: Context, pin: String): Boolean {
        val p = pin.trim()
        if (p.isEmpty()) return false
        return try {
            prefs(context)?.edit()?.putString(K_PIN, p)?.commit() == true
        } catch (e: Exception) {
            Log.e(TAG, "no se pudo guardar el PIN de operaciones", e)
            false
        }
    }

    /** Lee el PIN/clave de operaciones (solo el servicio lo usa al confirmar). */
    fun leerPin(context: Context): String {
        return try {
            prefs(context)?.getString(K_PIN, "").orEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "no se pudo leer el PIN de operaciones", e)
            ""
        }
    }

    /** true si hay un PIN/clave de operaciones configurado. */
    fun pinConfigurado(context: Context): Boolean = leerPin(context).isNotEmpty()

    /** Borra el PIN/clave de operaciones del almacén cifrado. */
    fun borrarPin(context: Context) {
        try {
            prefs(context)?.edit()?.remove(K_PIN)?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "no se pudo borrar el PIN de operaciones", e)
        }
    }
}
