/*
 * Copyright (C) - Emanuel Moecklin
 *
 * Licensed under the Apache License, Version . (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onegravity.rteditor.spans;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.onegravity.rteditor.api.RTApi;
import com.onegravity.rteditor.api.format.RTFormat;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.media.MediaUtils;

import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * An ImageSpan representing an embedded image.
 */
public class ImageSpan extends MediaSpan {

    private Context mContext;
    private Uri mContentUri;
    private Drawable mDrawable;

    public ImageSpan(RTImage image, boolean isSaved) {
        super(image, isSaved);
        mContentUri = MediaUtils.createFileUri(image.getFilePath(RTFormat.SPANNED));
        mContext = RTApi.getApplicationContext();
    }

    public RTImage getImage() {
        return (RTImage) mMedia;
    }

    @Override
    public Drawable getDrawable() {
        Drawable drawable = null;
        if (mDrawable != null) {
            drawable = mDrawable;
        } else if (mContentUri != null) {
            Bitmap bitmap = null;
            try {
                InputStream is = mContext.getContentResolver().openInputStream(
                        mContentUri);
                //根据屏幕缩放读取读片
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                bitmap = BitmapFactory.decodeStream(is, null, options);
                is.close();
                is = mContext.getContentResolver().openInputStream(
                        mContentUri);
                DisplayMetrics metrics = new DisplayMetrics();
                WindowManager mWindowManager = (WindowManager) mContext.getApplicationContext().getSystemService(Application.WINDOW_SERVICE);
                mWindowManager.getDefaultDisplay().getMetrics(metrics);
                int screenWidth = metrics.widthPixels;
                int screenHeight = metrics.heightPixels;
                int imageHeight = options.outHeight;
                int imageWidth = options.outWidth;
                options.inJustDecodeBounds = false;
                // recreate the stream
                // make some calculation to define inSampleSize
                options.inSampleSize = (int) Math.ceil(imageWidth * 1.0f / screenWidth);
                bitmap = BitmapFactory.decodeStream(is, null, options);
                drawable = new BitmapDrawable(mContext.getResources(), bitmap);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight());
                mDrawable = drawable;
                is.close();
            } catch (Exception e) {
                Log.e("sms", "Failed to loaded content " + mContentUri, e);
            }
        }
        return drawable;
    }

    private Drawable getCachedDrawable() {
        WeakReference<Drawable> wr = mDrawableRef;
        Drawable d = null;

        if (wr != null)
            d = wr.get();

        if (d == null) {
            d = getDrawable();
            mDrawableRef = new WeakReference<Drawable>(d);
        }

        return d;
    }

    private WeakReference<Drawable> mDrawableRef;

    @Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, Paint paint) {
        Drawable b = getCachedDrawable();
        canvas.save();

        int transY = bottom - b.getBounds().bottom;
        if (mVerticalAlignment == ALIGN_BASELINE) {
            transY -= paint.getFontMetricsInt().descent;
        }

        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();
    }
}