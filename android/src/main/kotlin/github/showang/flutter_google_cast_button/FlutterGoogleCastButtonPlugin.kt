package github.showang.flutter_google_cast_button

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StyleRes
import androidx.mediarouter.app.MediaRouteChooserDialog
import androidx.mediarouter.app.MediaRouteControllerDialog
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterGoogleCastButtonPlugin : MethodCallHandler {
    companion object {
        private const val TAG = "CastButtonPlugin"

        @SuppressLint("StaticFieldLeak")
        val instance = FlutterGoogleCastButtonPlugin()
        @StyleRes
        var customStyleResId: Int? = null
        private val castStreamHandler = CastStreamHandler()
        private val themeResId get() = customStyleResId ?: R.style.DefaultCastDialogTheme

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            CastContext.getSharedInstance(registrar.context().applicationContext)
            MethodChannel(registrar.messenger(), "flutter_google_cast_button").apply {
                setMethodCallHandler(instance)
            }
            EventChannel(registrar.messenger(), "cast_state_event").apply {
                setStreamHandler(castStreamHandler)
            }
        }
    }

    private var currentContext: Context? = null

    fun initContext(context: Context) {
        currentContext = context
        castStreamHandler.updateState()
    }

    fun disposeContext() {
        currentContext = null
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "showCastDialog" -> showCastDialog()
            else -> result.notImplemented()
        }
    }

    private fun showCastDialog() {
        val context = currentContext ?: run { return }
        val castContext = CastContext.getSharedInstance(context)
        castContext.sessionManager.currentCastSession?.let {
            MediaRouteControllerDialog(currentContext, themeResId)
                    .show()
        } ?: run {
            MediaRouteChooserDialog(currentContext, themeResId).apply {
                routeSelector = castContext.mergedSelector
                show()
            }
        }
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