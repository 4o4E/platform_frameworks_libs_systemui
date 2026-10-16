package app.lawnchair.icons

import android.content.Context
import android.graphics.drawable.Drawable
import com.android.launcher3.util.ComponentKey

/** Stored by launch component and Android user, matching existing per-app customization identity. */
enum class IconRenderingMode {
    FOLLOW_GLOBAL,
    ADAPTIVE,
    CENTERED,
}

object IconRenderingSettings {
    private const val MODE_PREFIX = "app_icon_rendering_mode:"
    private const val STATE_PREFIX = "app_icon_rendering_state:"

    @JvmStatic
    fun getMode(context: Context, component: ComponentKey): IconRenderingMode =
        IconRenderingMode.valueOf(
            context.prefs.getString(MODE_PREFIX + component, IconRenderingMode.FOLLOW_GLOBAL.name)!!,
        )

    /** Default entries are absent; a package revision invalidates only that package's disk icons. */
    @JvmStatic
    fun setMode(context: Context, component: ComponentKey, mode: IconRenderingMode) {
        if (getMode(context, component) == mode) return
        val prefs = context.prefs
        val stateKey = STATE_PREFIX + component.componentName.packageName
        val editor = prefs.edit()
        if (mode == IconRenderingMode.FOLLOW_GLOBAL) {
            editor.remove(MODE_PREFIX + component)
        } else {
            editor.putString(MODE_PREFIX + component, mode.name)
        }
        editor.putLong(stateKey, prefs.getLong(stateKey, 0L) + 1L).apply()
    }

    @JvmStatic
    fun getPackageState(context: Context, packageName: String): Long =
        context.prefs.getLong(STATE_PREFIX + packageName, 0L)

    /** Keep the selected artwork intact until the shared bitmap factory applies its policy. */
    @JvmStatic
    fun prepare(source: Drawable, mode: IconRenderingMode): Drawable {
        return if (mode == IconRenderingMode.FOLLOW_GLOBAL) source else IconRenderingDrawable(source, mode)
    }
}
