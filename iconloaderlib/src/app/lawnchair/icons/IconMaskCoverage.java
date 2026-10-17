package app.lawnchair.icons;

import java.util.Arrays;

/**
 * Explicit cropping covers the destination mask with the source's visible convex contour, rather
 * than its transparent bitmap canvas. Internal transparent details remain part of the artwork.
 * Arrays are owned by the bitmap factory and reused while loading icons.
 */
public final class IconMaskCoverage {
    private final int mSize;
    private final float[] mLeft;
    private final float[] mRight;
    private final int[] mHullRows;
    private final int[] mMaskLeft;
    private final int[] mMaskRight;
    private int mTop;
    private int mBottom;
    private float mCenterX;
    private float mCenterY;
    private float mNormalizedCenterX = .5f;
    private float mNormalizedCenterY = .5f;

    public IconMaskCoverage(int size) {
        mSize = size;
        mLeft = new float[size];
        mRight = new float[size];
        mHullRows = new int[size];
        mMaskLeft = new int[size];
        mMaskRight = new int[size];
    }

    public float getCenterX() {
        return mNormalizedCenterX;
    }

    public float getCenterY() {
        return mNormalizedCenterY;
    }

    public float update(int[] source, int[] mask, int left, int top, int right, int bottom) {
        Arrays.fill(mLeft, -1);
        Arrays.fill(mRight, -1);
        Arrays.fill(mMaskLeft, -1);
        Arrays.fill(mMaskRight, -1);
        mTop = -1;
        mBottom = -1;
        long weight = 0;
        long weightedX = 0;
        long weightedY = 0;
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                int index = y * mSize + x;
                int alpha = source[index] >>> 24;
                if (alpha >= 128) {
                    if (mLeft[y] < 0) mLeft[y] = x;
                    mRight[y] = x;
                    if (mTop < 0) mTop = y;
                    mBottom = y;
                    weight += alpha;
                    weightedX += (long) x * alpha;
                    weightedY += (long) y * alpha;
                }
                if ((mask[index] >>> 24) >= 128) {
                    if (mMaskLeft[y] < 0) mMaskLeft[y] = x;
                    mMaskRight[y] = x;
                }
            }
        }
        // A wholly transparent drawable has no visible contour to crop.
        if (weight == 0) {
            mNormalizedCenterX = mNormalizedCenterY = .5f;
            return 1;
        }
        mCenterX = (float) weightedX / weight;
        mCenterY = (float) weightedY / weight;
        mNormalizedCenterX = (mCenterX + .5f - left) / (right - left);
        mNormalizedCenterY = (mCenterY + .5f - top) / (bottom - top);
        encloseContour(mLeft, 1);
        encloseContour(mRight, -1);
        float low = 1;
        float high = 1;
        while (!covers(high)) high *= 2;
        if (high == 1) return 1;
        // Subpixel precision at the final icon size; no per-candidate bitmap rendering.
        for (int i = 0; i < 12; i++) {
            float middle = (low + high) / 2;
            if (covers(middle)) high = middle;
            else low = middle;
        }
        return high;
    }

    /** Build the outward monotone hull before interpolating, retaining every visible pixel. */
    private void encloseContour(float[] edge, int direction) {
        int count = 0;
        for (int y = mTop; y <= mBottom; y++) {
            if (edge[y] < 0) continue;
            while (count >= 2) {
                int a = mHullRows[count - 2];
                int b = mHullRows[count - 1];
                double turn = (double) (edge[y] - edge[b]) * (b - a)
                        - (double) (edge[b] - edge[a]) * (y - b);
                if (turn * direction > 0) break;
                count--;
            }
            mHullRows[count++] = y;
        }
        for (int i = 1; i < count; i++) {
            int a = mHullRows[i - 1];
            int b = mHullRows[i];
            float start = edge[a];
            float slope = (edge[b] - start) / (b - a);
            for (int y = a + 1; y < b; y++) edge[y] = start + slope * (y - a);
        }
    }

    private boolean covers(float zoom) {
        float center = (mSize - 1) / 2f;
        for (int y = 0; y < mSize; y++) {
            if (mMaskLeft[y] < 0) continue;
            float sourceY = mCenterY + (y - center) / zoom;
            if (sourceY < mTop - .5f || sourceY > mBottom + .5f) return false;
            sourceY = Math.max(mTop, Math.min(mBottom, sourceY));
            int row = (int) sourceY;
            int next = Math.min(mBottom, row + 1);
            float fraction = sourceY - row;
            float sourceLeft = mLeft[row] + (mLeft[next] - mLeft[row]) * fraction - .5f;
            float sourceRight = mRight[row] + (mRight[next] - mRight[row]) * fraction + .5f;
            float targetLeft = mCenterX + (mMaskLeft[y] - center - .5f) / zoom;
            float targetRight = mCenterX + (mMaskRight[y] - center + .5f) / zoom;
            if (targetLeft < sourceLeft || targetRight > sourceRight) return false;
        }
        return true;
    }
}
