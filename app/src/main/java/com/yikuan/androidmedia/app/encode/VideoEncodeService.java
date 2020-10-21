package com.yikuan.androidmedia.app.encode;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.yikuan.androidcommon.util.ScreenUtils;
import com.yikuan.androidcommon.util.ThreadPoolManager;
import com.yikuan.androidmedia.app.base.MediaProjectionService;
import com.yikuan.androidmedia.base.State;
import com.yikuan.androidmedia.codec.SyncCodec;
import com.yikuan.androidmedia.encode.VideoEncoder;
import com.yikuan.androidmedia.encode.VideoEncoder2;
import com.yikuan.androidmedia.encode.VideoParam;

import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VideoEncodeService extends MediaProjectionService {
    private static final String TAG = "VideoEncodeService";
    private static final boolean ASYNC_MODE = true;
    private VideoEncoder mVideoEncoder;
    private VideoEncoder2 mVideoEncoder2;
    private VirtualDisplay mVirtualDisplay;
    private VideoParam mParam = new VideoParam(MediaFormat.MIMETYPE_VIDEO_AVC, ScreenUtils.getScreenWidth(),
            ScreenUtils.getScreenHeight(), 8 * 1024 * 1024, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface, 30, 1);

    public VideoEncodeService() {
    }

    @Override
    protected void start() {
        if (ASYNC_MODE) {
            initEncoder();
        } else {
            initEncoder2();
        }
        initVirtualDisplay();
        startEncode();
    }

    private void initEncoder() {
        mVideoEncoder = new VideoEncoder();
        mVideoEncoder.configure(mParam);
        mVideoEncoder.setCallback(new SyncCodec.Callback() {
            @Override
            public void onOutputFormatChanged(MediaFormat format) {
                Log.d(TAG, "onOutputFormatChanged: ");
            }

            @Override
            public void onOutputAvailable(byte[] output) {
                Log.d(TAG, "onOutputAvailable: ");
            }
        });
    }

    private void initEncoder2() {
        mVideoEncoder2 = new VideoEncoder2();
        mVideoEncoder2.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                Log.d(TAG, "onInputBufferAvailable: " + index);
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                Log.d(TAG, "onOutputBufferAvailable: " + index);
                ByteBuffer byteBuffer = codec.getOutputBuffer(index);
                Log.d(TAG, "onOutputBufferAvailable: " + byteBuffer.toString());
                codec.releaseOutputBuffer(index, false);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                Log.d(TAG, "onError: ");
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                Log.d(TAG, "onOutputFormatChanged: ");
            }
        });
        mVideoEncoder2.configure(mParam);
    }

    private void initVirtualDisplay() {
        Surface surface = mVideoEncoder != null ? mVideoEncoder.getInputSurface() : mVideoEncoder2.getInputSurface();
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG, ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight(),
                ScreenUtils.getScreenDpi(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
    }

    private void startEncode() {
        if (mVideoEncoder != null) {
            mVideoEncoder.start();
            ThreadPoolManager.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    while (mVideoEncoder.getState() == State.RUNNING) {
                        mVideoEncoder.read();
                    }
                }
            });
        } else {
            mVideoEncoder2.start();
        }
    }

    @Override
    protected void stop() {
        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
        } else {
            mVideoEncoder2.stop();
            mVideoEncoder2.release();
        }
        mVirtualDisplay.release();
    }
}
