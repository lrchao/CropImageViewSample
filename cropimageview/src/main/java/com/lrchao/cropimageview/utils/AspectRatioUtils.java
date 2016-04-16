package com.lrchao.cropimageview.utils;

import android.graphics.Rect;

/**
 * Description: Utility class for handling calculations involving a fixed aspect ratio.
 *
 * @author liuranchao
 * @date 16/4/16 下午4:01
 */
public class AspectRatioUtils {


    /**
     * Calculates the aspect ratio given a rectangle.
     */
    public static float calculateAspectRatio(Rect rect) {

        final float aspectRatio = (float) rect.width() / (float) rect.height();

        return aspectRatio;
    }

    /**
     * Calculates the width of a rectangle given the top and bottom edges and an
     * aspect ratio.
     */
    public static float calculateWidth(float top, float bottom, float targetAspectRatio) {

        final float height = bottom - top;
        final float width = targetAspectRatio * height;

        return width;
    }

    /**
     * Calculates the height of a rectangle given the left and right edges and
     * an aspect ratio.
     */
    public static float calculateHeight(float left, float right, float targetAspectRatio) {

        final float width = right - left;
        final float height = width / targetAspectRatio;

        return height;
    }
}
