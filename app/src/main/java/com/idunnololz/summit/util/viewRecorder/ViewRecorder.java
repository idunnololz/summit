package com.idunnololz.summit.util.viewRecorder;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.os.Looper;
import androidx.annotation.NonNull;

import android.util.Log;
import android.util.Size;
import android.view.View;

/**
 * Used to record a video with view that can be captured. It also supports to switch views during recording.
 * This class extends {@link SurfaceMediaRecorder} and provides an extra API {@link #setRecordedView(View)}.
 *
 * <p>By default, capture is drawn in the center of canvas in scale if necessary.
 * It is easy to change drawing behavior with {@link #setVideoFrameDrawer(VideoFrameDrawer)}.
 *
 * <p>Main thread is set for drawing as capture is only available in this thread,
 * it's OK to move composing to a background thread with {@link #setWorkerLooper(Looper)},
 * in this case, a capture buffer for multi-thread may be required.
 *
 * Created by z4hyoung on 2017/11/8.
 */
public class ViewRecorder extends SurfaceMediaRecorder {

    private View mRecordedView;

    private Size mVideoSize;

    private final VideoFrameDrawer mVideoFrameDrawer = new VideoFrameDrawer() {
        private Matrix getMatrix(int bw, int bh, int vw, int vh) {
            Matrix matrix = new Matrix();
            float scale, scaleX = 1, scaleY = 1, transX, transY;

            if (bw > vw) {
                scaleX = ((float) vw) / bw;
            }
            if (bh > vh) {
                scaleY = ((float) vh) / bh;
            }
            scale = (scaleX < scaleY ? scaleX : scaleY);
            transX = (vw - bw * scale) / 2;
            transY = (vh - bh * scale) / 2;

            matrix.postScale(scale, scale);
            matrix.postTranslate(transX, transY);

            return matrix;
        }

        @Override
        public void onDraw(Canvas canvas) {
            Log.d("HAHA", "onDraw()");
//            mRecordedView.setDrawingCacheEnabled(true);
//            Bitmap bitmap = mRecordedView.getDrawingCache();

//            int bitmapWidth = bitmap.getWidth();
//            int bitmapHeight = bitmap.getHeight();
            int videoWidth = mVideoSize.getWidth();
            int videoHeight = mVideoSize.getHeight();
            Matrix matrix = getMatrix(mRecordedView.getMeasuredWidth(), mRecordedView.getMeasuredHeight(), videoWidth, videoHeight);
            canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
            canvas.setMatrix(matrix);
//            canvas.drawBitmap(bitmap, matrix, null);
            mRecordedView.draw(canvas);

//            mRecordedView.setDrawingCacheEnabled(false);
        }
    };

    @Override
    public void setVideoSize(int width, int height) throws IllegalStateException {
        super.setVideoSize(width, height);
        mVideoSize = new Size(width, height);
    }

    @Override
    public void start() throws IllegalStateException {
        if (isSurfaceAvailable()) {
            if (mVideoSize == null) {
                throw new IllegalStateException("video size is not initialized yet");
            }
            if (mRecordedView == null) {
                throw new IllegalStateException("recorded view is not initialized yet");
            }
            setWorkerLooper(Looper.getMainLooper());
            setVideoFrameDrawer(mVideoFrameDrawer);
        }

        super.start();
    }

    /**
     * Sets recorded view to be captured for video frame composition. Call this method before start().
     * You may change the recorded view with this method during recording.
     *
     * @param view the view to be captured
     */
    public void setRecordedView(@NonNull View view) throws IllegalStateException {
        mRecordedView = view;
    }
}