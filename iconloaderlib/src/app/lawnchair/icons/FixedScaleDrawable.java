package app.lawnchair.icons;

import static com.android.launcher3.icons.BaseIconFactory.LEGACY_ICON_SCALE;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

/**
 * Extension of {@link DrawableWrapper} which scales the child drawables by a fixed amount.
 */
public class FixedScaleDrawable extends DrawableWrapper {

    private float mScaleX, mScaleY;
    private float mOffsetX, mOffsetY;

    public FixedScaleDrawable() {
        super(new ColorDrawable());
        mScaleX = LEGACY_ICON_SCALE;
        mScaleY = LEGACY_ICON_SCALE;
    }

    @Override
    public void draw(Canvas canvas) {
        int saveCount = canvas.save();
        canvas.scale(mScaleX, mScaleY,
                getBounds().exactCenterX(), getBounds().exactCenterY());
        canvas.translate(mOffsetX * getBounds().width(), mOffsetY * getBounds().height());
        super.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) { }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme) { }

    public void setScale(float scale) {
        mOffsetX = mOffsetY = 0;
        float h = getIntrinsicHeight();
        float w = getIntrinsicWidth();
        mScaleX = scale * LEGACY_ICON_SCALE;
        mScaleY = scale * LEGACY_ICON_SCALE;
        if (h > w && w > 0) {
            mScaleX *= w / h;
        } else if (w > h && h > 0) {
            mScaleY *= h / w;
        }
    }

    /** Cover the visible mask while preserving the source ratio, compensating adaptive overscan. */
    public void setScaleToFill() {
        mOffsetX = mOffsetY = 0;
        float h = getIntrinsicHeight();
        float w = getIntrinsicWidth();
        mScaleX = mScaleY = 1f / (1 + 2 * AdaptiveIconDrawable.getExtraInsetFraction());
        if (h > w && w > 0) {
            mScaleY *= h / w;
        } else if (w > h && h > 0) {
            mScaleX *= w / h;
        }
    }

    public void setScaleToFill(float zoom, float centerX, float centerY) {
        setScaleToFill();
        mScaleX *= zoom;
        mScaleY *= zoom;
        mOffsetX = .5f - centerX;
        mOffsetY = .5f - centerY;
    }
}
