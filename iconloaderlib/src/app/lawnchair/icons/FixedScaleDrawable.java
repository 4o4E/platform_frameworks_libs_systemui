package app.lawnchair.icons;

import static com.android.launcher3.icons.BaseIconFactory.LEGACY_ICON_SCALE;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

/**
 * Extension of {@link DrawableWrapper} which scales the child drawables by a fixed amount.
 */
public class FixedScaleDrawable extends DrawableWrapper {

    private float mScaleX, mScaleY;
    private float mOffsetX, mOffsetY;
    private FixedScaleState mConstantState;

    public FixedScaleDrawable() {
        super(new ColorDrawable());
        mScaleX = LEGACY_ICON_SCALE;
        mScaleY = LEGACY_ICON_SCALE;
    }

    private FixedScaleDrawable(FixedScaleState state, Drawable child) {
        super(child);
        mScaleX = state.mScaleX;
        mScaleY = state.mScaleY;
        mOffsetX = state.mOffsetX;
        mOffsetY = state.mOffsetY;
        setChangingConfigurations(state.mChangingConfigurations);
        mConstantState = state;
    }

    @Override
    public void setDrawable(Drawable drawable) {
        super.setDrawable(drawable);
        mConstantState = null;
    }

    @Override
    public ConstantState getConstantState() {
        Drawable child = getDrawable();
        ConstantState childState = child == null ? null : child.getConstantState();
        if (childState == null) {
            return null;
        }
        int configurations = getChangingConfigurations();
        if (mConstantState == null || mConstantState.mChildState != childState
                || mConstantState.mChangingConfigurations != configurations) {
            mConstantState = new FixedScaleState(this, childState, configurations);
        }
        return mConstantState;
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
        mConstantState = null;
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
        mConstantState = null;
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

    /**
     * Animation drawables need independent children with the same normalization geometry.
     * Snapshot only when the source or transform changes; the icon factory reuses this wrapper.
     */
    private static final class FixedScaleState extends ConstantState {
        final ConstantState mChildState;
        final int mChangingConfigurations;
        final float mScaleX, mScaleY;
        final float mOffsetX, mOffsetY;

        FixedScaleState(FixedScaleDrawable drawable, ConstantState childState,
                int configurations) {
            mChildState = childState;
            mChangingConfigurations = configurations;
            mScaleX = drawable.mScaleX;
            mScaleY = drawable.mScaleY;
            mOffsetX = drawable.mOffsetX;
            mOffsetY = drawable.mOffsetY;
        }

        @Override
        public Drawable newDrawable() {
            return new FixedScaleDrawable(this, mChildState.newDrawable());
        }

        @Override
        public Drawable newDrawable(Resources resources) {
            return new FixedScaleDrawable(this, mChildState.newDrawable(resources));
        }

        @Override
        public Drawable newDrawable(Resources resources, Theme theme) {
            return new FixedScaleDrawable(this, mChildState.newDrawable(resources, theme));
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }

        @Override
        public boolean canApplyTheme() {
            return mChildState.canApplyTheme();
        }
    }
}
