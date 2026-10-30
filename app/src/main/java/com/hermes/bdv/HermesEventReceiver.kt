package com.hermes.bdv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File

/**
 * Recibe los eventos que emite [HermesAccessibilityService] durante la
 * ejecución y los refleja en Telegram + el registro en pantalla.
 *
 * Acción: com.hermes.bdv.EVENTO
 * Extras:
 *   - "tipo":    "pantalla" | "error" | "exito"
 *   - "mensaje": texto descriptivo
 *   - "captura":      ByteArray PNG opcional (solo "exito")
 *   - "captura_path": ruta a un PNG en caché opcional (solo "exito",
 *                     preferido para imágenes grandes y evitar
 *                     TransactionTooLargeException)
 *
 * Reglas de notificación (petición del usuario):
 *   - "pantalla": solo texto por Telegram, sin captura.
 *   - "error":    solo texto por Telegram ("⚠ ..."), sin captura.
 *   - "exito":    foto por Telegram con el mensaje
 *                 "🎉 ¡Felicidades, lograste comprar divisas!".
 *                 Si no hay imagen, se envía solo el texto.
 *
 * Se registra de forma dinámica en MainActivity (no va en el Manifest)
 * porque solo tiene sentido mientras la app está en ejecución.
 */
class HermesEventReceiver(
    private val onLog: ((String) -> Unit)? = null
) : BroadcastReceiver() {

    companion object {
        const val ACCION_EVENTO = "com.hermes.bdv.EVENTO"
        const val EXTRA_TIPO = "tipo"
        const val EXTRA_MENSAJE = "mensaje"
        const val EXTRA_CAPTURA = "captura"
        const val EXTRA_CAPTURA_PATH = "captura_path"

        const val TIPO_PANTALLA = "pantalla"
        const val TIPO_ERROR = "error"
        const val TIPO_EXITO = "exito"
        const val TIPO_HUELLA = "huella"
        const val TIPO_DIAG = "diag"

        const val CAPTION_EXITO = "🎉 ¡Felicidades, lograste comprar divisas!"

        /** Límite prudente para una imagen enviada por extras (≈4 MB). */
        private const val MAX_PNG_BYTES = 4_000_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACCION_EVENTO) return
        intent.getStringExtra(EXTRA_TIPO) ?: return
        val mensaje = intent.getStringExtra(EXTRA_MENSAJE).orEmpty()
        // El envío a Telegram ahora lo hace el servicio directamente (porque la
        // pantalla pasa a segundo plano al abrir el BDV y dejaba de enviar).
        // Aquí solo se refleja en el registro en pantalla cuando la app Hermes
        // está visible; cuando está en segundo plano, simplemente no hay registro
        // visible, pero Telegram sigue llegando desde el servicio.
        if (mensaje.isNotBlank()) onLog?.invoke(mensaje)
    }

    private fun leerCaptura(intent: Intent): ByteArray? {
        intent.getByteArrayExtra(EXTRA_CAPTURA)
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        val path = intent.getStringExtra(EXTRA_CAPTURA_PATH) ?: return null
        return try {
            val f = File(path)
            if (f.exists() && f.length() in 1..MAX_PNG_BYTES) f.readBytes() else null
        } catch (_: Exception) {
            null
        }
    }
}
