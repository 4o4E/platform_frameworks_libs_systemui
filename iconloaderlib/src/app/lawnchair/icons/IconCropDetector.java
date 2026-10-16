package app.lawnchair.icons;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

/** Automatic cropping removes only a solid background, including its transparent rounded corners. */
public final class IconCropDetector {
    // Tolerate one 5-bit RGB quantization step of rasterized matte variation.
    private static final int BACKGROUND_CHANNEL_TOLERANCE = 7;

    private final int mSize;
    private final Bitmap mSource;
    private final Bitmap mMask;
    private final Canvas mSourceCanvas;
    private final Canvas mMaskCanvas;
    private final Paint mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mMaskPath = new Path();
    private final Rect mSourceBounds = new Rect();
    private final Rect mOldBounds = new Rect();
    private final int[] mPixels;
    private final int[] mMaskPixels;
    private int mMaskWidth;
    private int mMaskHeight;

    public IconCropDetector(int size) {
        mSize = size;
        mSource = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        mMask = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        mSourceCanvas = new Canvas(mSource);
        mMaskCanvas = new Canvas(mMask);
        mPixels = new int[size * size];
        mMaskPixels = new int[size * size];
        mMaskPaint.setColor(Color.WHITE);
    }

    public boolean canCrop(Drawable source, AdaptiveIconDrawable shape) {
        int width = mSize;
        int height = mSize;
        int intrinsicWidth = source.getIntrinsicWidth();
        int intrinsicHeight = source.getIntrinsicHeight();
        if (intrinsicWidth > intrinsicHeight && intrinsicHeight > 0) {
            height = Math.max(1, Math.round(mSize * (float) intrinsicHeight / intrinsicWidth));
        } else if (intrinsicHeight > intrinsicWidth && intrinsicWidth > 0) {
            width = Math.max(1, Math.round(mSize * (float) intrinsicWidth / intrinsicHeight));
        }
        mSourceBounds.set((mSize - width) / 2, (mSize - height) / 2,
                (mSize + width) / 2, (mSize + height) / 2);
        mSource.eraseColor(Color.TRANSPARENT);
        mOldBounds.set(source.getBounds());
        source.setBounds(mSourceBounds);
        source.draw(mSourceCanvas);
        source.setBounds(mOldBounds);
        mSource.getPixels(mPixels, 0, mSize, 0, 0, mSize, mSize);

        // Factories already own a fixed icon shape; reuse its mask for matching source ratios.
        if (mMaskWidth != width || mMaskHeight != height) {
            int side = Math.min(width, height);
            shape.setBounds(0, 0, side, side);
            mMaskPath.set(shape.getIconMask());
            mMaskPath.offset((mSize - side) / 2f, (mSize - side) / 2f);
            mMask.eraseColor(Color.WHITE);
            int saved = mMaskCanvas.save();
            mMaskCanvas.clipRect(mSourceBounds);
            mMaskCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            mMaskCanvas.drawPath(mMaskPath, mMaskPaint);
            mMaskCanvas.restoreToCount(saved);
            mMask.getPixels(mMaskPixels, 0, mSize, 0, 0, mSize, mSize);
            mMaskWidth = width;
            mMaskHeight = height;
        }
        return canCropPixels(mPixels, mMaskPixels, mSize, mSourceBounds.left,
                mSourceBounds.top, mSourceBounds.right, mSourceBounds.bottom);
    }

    static boolean canCropPixels(int[] pixels, int[] mask, int size,
            int left, int top, int right, int bottom) {
        int middleX = (left + right) / 2;
        int middleY = (top + bottom) / 2;
        int background = pixels[top * size + middleX];
        boolean solidBorder = (background >>> 24) == 255
                && (pixels[(bottom - 1) * size + middleX] >>> 24) == 255
                && matchesBackground(pixels[(bottom - 1) * size + middleX], background)
                && (pixels[middleY * size + left] >>> 24) == 255
                && matchesBackground(pixels[middleY * size + left], background)
                && (pixels[middleY * size + right - 1] >>> 24) == 255
                && matchesBackground(pixels[middleY * size + right - 1], background);
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                int index = y * size + x;
                // Include antialiasing and one inward pixel so low-resolution sampling is conservative.
                boolean clipped = (mask[index] >>> 24) != 255
                        || (x > 0 && (mask[index - 1] >>> 24) != 255)
                        || (x + 1 < size && (mask[index + 1] >>> 24) != 255)
                        || (y > 0 && (mask[index - size] >>> 24) != 255)
                        || (y + 1 < size && (mask[index + size] >>> 24) != 255);
                if (!clipped) continue;
                if (!solidBorder || !matchesBackground(pixels[index], background)) return false;
            }
        }
        return true;
    }

    private static boolean matchesBackground(int color, int background) {
        int alpha = color >>> 24;
        if (alpha == 0 || color == background) return true;
        for (int shift = 0; shift < 24; shift += 8) {
            int difference = ((color >>> shift) & 255) - ((background >>> shift) & 255);
            if (Math.abs(difference) <= BACKGROUND_CHANNEL_TOLERANCE) continue;
            if (alpha == 255) return false;
            // Unpremultiplication can round an antialiased background by one premultiplied byte.
            int actual = ((((color >>> shift) & 255) * alpha) + 127) / 255;
            int expected = ((((background >>> shift) & 255) * alpha) + 127) / 255;
            if (Math.abs(actual - expected) > 1) return false;
        }
        return true;
    }
}
