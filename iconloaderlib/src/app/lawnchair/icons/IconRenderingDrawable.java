package app.lawnchair.icons;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;

/** Per-app policy travels with its original artwork through the existing icon cache pipeline. */
public final class IconRenderingDrawable extends DrawableWrapper {
    private final IconRenderingMode mMode;

    public IconRenderingDrawable(Drawable source, IconRenderingMode mode) {
        super(source);
        mMode = mode;
    }

    public IconRenderingMode getMode() {
        return mMode;
    }
}
