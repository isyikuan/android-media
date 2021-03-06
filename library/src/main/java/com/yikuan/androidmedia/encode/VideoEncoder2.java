package com.yikuan.androidmedia.encode;

import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.yikuan.androidmedia.codec.AsyncCodec;

/**
 * @author yikuan
 * @date 2020/09/21
 */
public class VideoEncoder2 extends AsyncCodec<VideoEncodeParam> {

    @Override
    protected boolean isEncoder() {
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected MediaFormat configureMediaFormat() {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mParam.type, mParam.width, mParam.height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mParam.bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mParam.colorFormat);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mParam.frameRate);
        mediaFormat.setInteger(MediaFormat.KEY_CAPTURE_RATE, mParam.frameRate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mParam.iFrameInterval);
        return mediaFormat;
    }
}
