package com.lrchao.cropimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.lrchao.cropimageview.utils.BitmapUtils;
import com.lrchao.cropimageview.utils.ImageViewUtils;

/**
 * Description: 裁剪图片的view
 *
 * @author liuranchao
 */
public class CropImageView extends FrameLayout implements CropOverlayView.OnDrawCropOverlayViewFinishListener {

    private static final String TAG = "CropImageView";

    // Private Constants ///////////////////////////////////////////////////////

    private static final Rect EMPTY_RECT = new Rect();

    // Member Variables ////////////////////////////////////////////////////////

    // Sets the default image guidelines to show when resizing
    public static final boolean DEFAULT_FIXED_ASPECT_RATIO = true;
    public static final int DEFAULT_ASPECT_RATIO_X = 1;
    public static final int DEFAULT_ASPECT_RATIO_Y = 1;

    private static final int DEFAULT_IMAGE_RESOURCE = 0;

    private static final String DEGREES_ROTATED = "DEGREES_ROTATED";

    /**
     * 背景图片的View
     */
    private CropZoomImageView mImageView;
    /**
     * 蒙层view
     */
    private CropOverlayView mCropOverlayView;

    /**
     * 头像的Bitmap
     */
    private Bitmap mBitmapIcon;

    private int mDegreesRotated = 0;

    private int mLayoutWidth;
    private int mLayoutHeight;

    private boolean mFixAspectRatio = DEFAULT_FIXED_ASPECT_RATIO;
    private int mAspectRatioX = DEFAULT_ASPECT_RATIO_X;
    private int mAspectRatioY = DEFAULT_ASPECT_RATIO_Y;
    private int mImageResource = DEFAULT_IMAGE_RESOURCE;

    // Constructors ////////////////////////////////////////////////////////////

    public CropImageView(Context context) {
        super(context);
        init(context);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CropImageView, 0, 0);

        try {
            mFixAspectRatio = ta.getBoolean(R.styleable.CropImageView_fixAspectRatio,
                    DEFAULT_FIXED_ASPECT_RATIO);
            mAspectRatioX = ta.getInteger(R.styleable.CropImageView_aspectRatioX,
                    DEFAULT_ASPECT_RATIO_X);
            mAspectRatioY = ta.getInteger(R.styleable.CropImageView_aspectRatioY,
                    DEFAULT_ASPECT_RATIO_Y);
            mImageResource = ta.getResourceId(R.styleable.CropImageView_imageResource,
                    DEFAULT_IMAGE_RESOURCE);
        } finally {
            ta.recycle();
        }

        init(context);
    }

    //=======================================
    //View Methods
    //=======================================

    @Override
    public Parcelable onSaveInstanceState() {

        final Bundle bundle = new Bundle();

        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putInt(DEGREES_ROTATED, mDegreesRotated);

        return bundle;

    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {

        if (state instanceof Bundle) {

            final Bundle bundle = (Bundle) state;

            // Fixes the rotation of the image when orientation changes.
            mDegreesRotated = bundle.getInt(DEGREES_ROTATED);
            int tempDegrees = mDegreesRotated;
            rotateImage(mDegreesRotated);
            mDegreesRotated = tempDegrees;

            super.onRestoreInstanceState(bundle.getParcelable("instanceState"));

        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mBitmapIcon != null) {
            final Rect bitmapRect = ImageViewUtils.getBitmapRectCenterInside(mBitmapIcon, this);
            mCropOverlayView.setBitmapRect(bitmapRect);
        } else {
            mCropOverlayView.setBitmapRect(EMPTY_RECT);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (mBitmapIcon != null) {

            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            // Bypasses a baffling bug when used within a ScrollView, where
            // heightSize is set to 0.
            if (heightSize == 0) {
                heightSize = mBitmapIcon.getHeight();
            }

            int desiredWidth;
            int desiredHeight;

            double viewToBitmapWidthRatio = Double.POSITIVE_INFINITY;
            double viewToBitmapHeightRatio = Double.POSITIVE_INFINITY;

            // Checks if either width or height needs to be fixed
            if (widthSize < mBitmapIcon.getWidth()) {
                viewToBitmapWidthRatio = (double) widthSize / (double) mBitmapIcon.getWidth();
            }
            if (heightSize < mBitmapIcon.getHeight()) {
                viewToBitmapHeightRatio = (double) heightSize / (double) mBitmapIcon.getHeight();
            }

            // If either needs to be fixed, choose smallest ratio and calculate
            // from there
            if (viewToBitmapWidthRatio != Double.POSITIVE_INFINITY || viewToBitmapHeightRatio != Double.POSITIVE_INFINITY) {
                if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
                    desiredWidth = widthSize;
                    desiredHeight = (int) (mBitmapIcon.getHeight() * viewToBitmapWidthRatio);
                } else {
                    desiredHeight = heightSize;
                    desiredWidth = (int) (mBitmapIcon.getWidth() * viewToBitmapHeightRatio);
                }
            }

            // Otherwise, the picture is within frame layout bounds. Desired
            // width is
            // simply picture size
            else {
                desiredWidth = mBitmapIcon.getWidth();
                desiredHeight = mBitmapIcon.getHeight();
            }

            int width = getOnMeasureSpec(widthMode, widthSize, desiredWidth);
            int height = getOnMeasureSpec(heightMode, heightSize, desiredHeight);

            mLayoutWidth = width;
            mLayoutHeight = height;

            // KevinLau 画画布
            final Rect bitmapRect = ImageViewUtils.getBitmapRectCenterInside(mBitmapIcon.getWidth(),
                    mBitmapIcon.getHeight(),
                    mLayoutWidth,
                    mLayoutHeight);
            mCropOverlayView.setBitmapRect(bitmapRect);

            // MUST CALL THIS
            setMeasuredDimension(mLayoutWidth, mLayoutHeight);

        } else {

            mCropOverlayView.setBitmapRect(EMPTY_RECT);
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        super.onLayout(changed, l, t, r, b);

        if (mLayoutWidth > 0 && mLayoutHeight > 0) {
            // Gets original parameters, and creates the new parameters
            final ViewGroup.LayoutParams origparams = this.getLayoutParams();
            origparams.width = mLayoutWidth;
            origparams.height = mLayoutHeight;
            setLayoutParams(origparams);
        }
    }

    //================================================================
    // public
    //================================================================

    /**
     * 设置成截取圆形，默认
     */
    public void setCropOverViewTypeCircle() {
        if (mCropOverlayView != null) {
            mCropOverlayView.setOverlayViewType(CropOverlayView.CLIP_IMAGE_TYPE_CIRCLE);
        }
    }

    /**
     * 设置裁剪成矩形
     */
    public void setCropOverViewTypeRectangl() {
        if (mCropOverlayView != null) {
            mCropOverlayView.setOverlayViewType(CropOverlayView.CLIP_IMAGE_TYPE_RECTANGLE);
        }
    }

    /**
     * 设置Content Bitmap
     *
     * @param bitmap the Bitmap to set
     */
    public void setImageBitmap(Bitmap bitmap) {
        mBitmapIcon = bitmap;
        mImageView.setBitmap(mBitmapIcon);

        if (mCropOverlayView != null) {
            mCropOverlayView.resetCropOverlayView();
        }
        requestLayout();
    }

    /**
     * Sets a Drawable as the content of the CropImageView.
     *
     * @param resId the drawable resource ID to set
     */
    public void setImageResource(int resId) {
        if (resId != 0) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
            setImageBitmap(bitmap);
        }
    }

    /**
     * 获取裁剪后的Bitmap
     * Gets the cropped image based on the current crop window.
     *
     * @return a new Bitmap representing the cropped image
     */
    public Bitmap getCroppedImage() {
        return mImageView.getBitmap(mCropOverlayView.getCropRect());
    }

    /**
     * Sets a Bitmap and initializes the image rotation according to the EXIT data.
     * <p>
     * The EXIF can be retrieved by doing the following:
     * <code>ExifInterface exif = new ExifInterface(path);</code>
     *
     * @param bitmap the original bitmap to set; if null, this
     * @param exif   the EXIF information about this bitmap; may be null
     */
   /* public void setImageBitmap(Bitmap bitmap, ExifInterface exif)
    {

        if (bitmap == null)
        {
            return;
        }

        if (exif == null)
        {
            setImageBitmap(bitmap);
            return;
        }

        final Matrix matrix = new Matrix();
        final int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        int rotate = -1;

        switch (orientation)
        {
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
        }

        if (rotate == -1)
        {
            setImageBitmap(bitmap);
        }
        else
        {
            matrix.postRotate(rotate);
            final Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                                             bitmap.getHeight(), matrix, true);
            setImageBitmap(rotatedBitmap);
            bitmap.recycle();
        }
    }*/

    /**
     * Gets the cropped circle image based on the current crop selection.
     *
     * @return a new Circular Bitmap representing the cropped image
     */
    /*public Bitmap getCroppedCircleImage()
    {
        Bitmap bitmap = getCroppedImage();
        if (!BitmapUtils.isAvailableBitmap(bitmap))
        {
            return null;
        }
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                                            Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2,
                          paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output;
    }*/

    /**
     * Sets whether the aspect ratio is fixed or not; true fixes the aspect ratio, while
     * false allows it to be changed.
     *
     * @param fixAspectRatio Boolean that signals whether the aspect ratio should be
     *                       maintained.
     */
/*    public void setFixedAspectRatio(boolean fixAspectRatio)
    {
        mCropOverlayView.setFixedAspectRatio(fixAspectRatio);
    }*/

    /**
     * Sets the both the X and Y values of the aspectRatio.
     *
     * @param aspectRatioX int that specifies the new X value of the aspect ratio
     * @param aspectRatioX int that specifies the new Y value of the aspect ratio
     */
    /*public void setAspectRatio(int aspectRatioX, int aspectRatioY)
    {
        mAspectRatioX = aspectRatioX;
        mCropOverlayView.setAspectRatioX(mAspectRatioX);

        mAspectRatioY = aspectRatioY;
        mCropOverlayView.setAspectRatioY(mAspectRatioY);
    }*/

    /**
     * Rotates image by the specified number of degrees clockwise. Cycles from 0 to 360
     * degrees.
     *
     * @param degrees Integer specifying the number of degrees to rotate.
     */
    public void rotateImage(int degrees) {

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        mBitmapIcon = Bitmap.createBitmap(mBitmapIcon, 0, 0, mBitmapIcon.getWidth(), mBitmapIcon.getHeight(),
                matrix, true);
        setImageBitmap(mBitmapIcon);

        mDegreesRotated += degrees;
        mDegreesRotated = mDegreesRotated % 360;
    }

    // Private Methods /////////////////////////////////////////////////////////

    private void init(Context context) {

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View v = inflater.inflate(R.layout.view_crop_image, this, true);

        mImageView = (CropZoomImageView) v.findViewById(R.id.ImageView_image);
        setImageResource(mImageResource);
        mCropOverlayView = (CropOverlayView) v.findViewById(R.id.CropOverlayView);
        mCropOverlayView.setInitialAttributeValues(mFixAspectRatio, mAspectRatioX, mAspectRatioY);
        mCropOverlayView.setOnDrawCropOverlayViewFinishListener(this);
    }

    /**
     * Determines the specs for the onMeasure function. Calculates the width or height
     * depending on the mode.
     *
     * @param measureSpecMode The mode of the measured width or height.
     * @param measureSpecSize The size of the measured width or height.
     * @param desiredSize     The desired size of the measured width or height.
     * @return The final size of the width or height.
     */
    private static int getOnMeasureSpec(int measureSpecMode, int measureSpecSize, int desiredSize) {

        // Measure Width
        int spec;
        if (measureSpecMode == MeasureSpec.EXACTLY) {
            // Must be this size
            spec = measureSpecSize;
        } else if (measureSpecMode == MeasureSpec.AT_MOST) {
            // Can't be bigger than...; match_parent value
            spec = Math.min(desiredSize, measureSpecSize);
        } else {
            // Be whatever you want; wrap_content
            spec = desiredSize;
        }

        return spec;
    }


    @Override
    public void onDrawCropOverlayViewFinish(float radius) {

    }

    /**
     * 释放内存
     */
    public void clear() {
        if (BitmapUtils.isAvailableBitmap(mBitmapIcon)) {
            BitmapUtils.recycleBitmap(mBitmapIcon);
        }
    }
}
