package github.showang.flutter_google_cast_button

import android.content.Context
import androidx.annotation.StyleRes
import androidx.mediarouter.app.MediaRouteChooserDialog
import androidx.mediarouter.app.MediaRouteControllerDialog
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.lang.Exception

class FlutterGoogleCastButtonPlugin(private var castStreamHandler: CastStreamHandler? = null,
                                    private var context: Context? = null,
                                    private var methodChannel: MethodChannel? = null,
                                    private var eventChannel: EventChannel? = null) : MethodCallHandler,
    FlutterPlugin {
    companion object {
        @StyleRes
        var customStyleResId: Int? = null
        var instance: FlutterGoogleCastButtonPlugin? = null

        private val themeResId get() = customStyleResId ?: R.style.DefaultCastDialogTheme

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            instance = FlutterGoogleCastButtonPlugin()
            instance?.initInstance(registrar.messenger(), registrar.context())
        }
    }

    fun initInstance(messenger: BinaryMessenger, context: Context) {
        this.context = context

        // Note: it raises exceptions when the current device does not have Google Play service.
        try {
            CastContext.getSharedInstance(context)
        } catch (error: Exception) {
        }

        instance = FlutterGoogleCastButtonPlugin()
        castStreamHandler = CastStreamHandler()
        methodChannel = MethodChannel(messenger, "flutter_google_cast_button").apply {
            setMethodCallHandler(instance)
        }
        eventChannel = EventChannel(messenger, "cast_state_event").apply {
            setStreamHandler(castStreamHandler)
        }
    }

    private val castContext: CastContext?
        // Note: it raises exceptions when the current device does not have Google Play service.
        get() = try {
            context?.let { CastContext.getSharedInstance(it) }
        } catch (error: Exception) {
            null
        }

    fun onResume() {
        castStreamHandler?.updateState()
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "showCastDialog" -> showCastDialog()
            else -> result.notImplemented()
        }
    }

    // Shows the Chromecast dialog.
    private fun showCastDialog() {
        castContext?.let {
            it.sessionManager?.currentCastSession?.let {
                MediaRouteControllerDialog(context, themeResId)
                    .show()
            } ?: run {
                MediaRouteChooserDialog(context, themeResId).apply {
                    routeSelector = it.mergedSelector
                    show()
                }
            }
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        initInstance(binding.binaryMessenger, binding.applicationContext)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = null
        castStreamHandler = null
        methodChannel = null
        eventChannel = null
    }
}

class CastStreamHandler : EventChannel.StreamHandler {

    private var lastState = CastState.NO_DEVICES_AVAILABLE
    private var eventSink: EventChannel.EventSink? = null
    private val castStateListener = CastStateListener { state ->
        lastState = state
        eventSink?.success(state)
    }

    override fun onListen(p0: Any?, sink: EventChannel.EventSink?) {
        val castContext = CastContext.getSharedInstance() ?: return
        eventSink = sink
        castContext.addCastStateListener(castStateListener)
        lastState = castContext.castState
        eventSink?.success(lastState)
    }

    override fun onCancel(p0: Any?) {
        val castContext = CastContext.getSharedInstance() ?: return
        castContext.removeCastStateListener(castStateListener)
        eventSink = null
    }

    fun updateState() {
        eventSink?.success(lastState)
    }

}
