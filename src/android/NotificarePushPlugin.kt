package re.notifica.push.cordova

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Observer
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaArgs
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.PluginResult
import org.json.JSONArray
import org.json.JSONObject
import re.notifica.Notificare
import re.notifica.push.ktx.push

class NotificarePushPlugin : CordovaPlugin() {

    private val allowedUIObserver = Observer<Boolean> { allowedUI ->
        if (allowedUI == null) return@Observer

        NotificarePushPluginEventBroker.dispatchEvent("notification_settings_changed", allowedUI)
    }

    override fun pluginInitialize() {
        Notificare.push().intentReceiver = NotificarePushPluginReceiver::class.java

        onMainThread {
            Notificare.push().observableAllowedUI.observeForever(allowedUIObserver)
        }

        val intent = cordova.activity.intent
        if (intent != null) onNewIntent(intent)
    }

    override fun onDestroy() {
        onMainThread {
            Notificare.push().observableAllowedUI.removeObserver(allowedUIObserver)
        }
    }

    override fun onNewIntent(intent: Intent) {
        Notificare.push().handleTrampolineIntent(intent)
    }

    override fun execute(action: String, args: CordovaArgs, callback: CallbackContext): Boolean {
        when (action) {
            "setAuthorizationOptions" -> setAuthorizationOptions(args, callback)
            "setCategoryOptions" -> setCategoryOptions(args, callback)
            "setPresentationOptions" -> setPresentationOptions(args, callback)
            "hasRemoteNotificationsEnabled" -> hasRemoteNotificationsEnabled(args, callback)
            "allowedUI" -> allowedUI(args, callback)
            "enableRemoteNotifications" -> enableRemoteNotifications(args, callback)
            "disableRemoteNotifications" -> disableRemoteNotifications(args, callback)

            // Event broker
            "registerListener" -> registerListener(args, callback)

            else -> {
                callback.error("No implementation for action '$action'.")
                return false
            }
        }

        return true
    }

    // region Notificare Push

    private fun setAuthorizationOptions(@Suppress("UNUSED_PARAMETER") args: CordovaArgs, callback: CallbackContext) {
        // no-op: iOS-only method
        callback.void()
    }

    private fun setCategoryOptions(@Suppress("UNUSED_PARAMETER") args: CordovaArgs, callback: CallbackContext) {
        // no-op: iOS-only method
        callback.void()
    }

    private fun setPresentationOptions(@Suppress("UNUSED_PARAMETER") args: CordovaArgs, callback: CallbackContext) {
        // no-op: iOS-only method
        callback.void()
    }

    private fun hasRemoteNotificationsEnabled(
        @Suppress("UNUSED_PARAMETER") args: CordovaArgs,
        callback: CallbackContext
    ) {
        callback.success(Notificare.push().hasRemoteNotificationsEnabled)
    }

    private fun allowedUI(@Suppress("UNUSED_PARAMETER") args: CordovaArgs, callback: CallbackContext) {
        callback.success(Notificare.push().allowedUI)
    }

    private fun enableRemoteNotifications(@Suppress("UNUSED_PARAMETER") args: CordovaArgs, callback: CallbackContext) {
        Notificare.push().enableRemoteNotifications()
        callback.void()
    }

    private fun disableRemoteNotifications(@Suppress("UNUSED_PARAMETER") args: CordovaArgs, callback: CallbackContext) {
        Notificare.push().disableRemoteNotifications()
        callback.void()
    }

    // endregion

    private fun registerListener(@Suppress("UNUSED_PARAMETER") args: CordovaArgs, callback: CallbackContext) {
        NotificarePushPluginEventBroker.setup(object : NotificarePushPluginEventBroker.Consumer {
            override fun onEvent(event: NotificarePushPluginEventBroker.Event) {
                val payload = JSONObject()
                payload.put("name", event.name)
                when (event.payload) {
                    null -> {} // Skip encoding null payloads.
                    is Boolean -> payload.put("data", event.payload)
                    is Int -> payload.put("data", event.payload)
                    is Float -> payload.put("data", event.payload)
                    is Double -> payload.put("data", event.payload)
                    is String -> payload.put("data", event.payload)
                    is JSONObject -> payload.put("data", event.payload)
                    is JSONArray -> payload.put("data", event.payload)
                    else -> throw IllegalArgumentException("Unsupported event payload of type '${event.payload::class.java.simpleName}'.")
                }

                val result = PluginResult(PluginResult.Status.OK, payload)
                result.keepCallback = true

                callback.sendPluginResult(result)
            }
        })
    }
}

private fun onMainThread(action: () -> Unit) = Handler(Looper.getMainLooper()).post(action)

private fun CallbackContext.void() {
    sendPluginResult(PluginResult(PluginResult.Status.OK, null as String?))
}

private fun CallbackContext.success(b: Boolean) {
    sendPluginResult(PluginResult(PluginResult.Status.OK, b))
}
