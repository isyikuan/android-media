package com.yikuan.androidmedia.record;

import android.media.AudioFormat;
import android.media.AudioRecord;

import com.yikuan.androidmedia.base.State;
import com.yikuan.androidmedia.base.Worker1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yikuan
 * @date 2020/09/17
 */
public class AudioRecorder extends Worker1<AudioRecorder.Param> {
    private AudioRecord mAudioRecord;
    private byte[] mAudioData;
    private ExecutorService mExecutorService;
    private Runnable mRecordRunnable;
    private Callback mCallback;
    private Param mParam;

    @Override
    public void configure(Param param) {
        checkCurrentStateInStates(State.UNINITIALIZED);
        mAudioRecord = new AudioRecord(param.audioSource, param.sampleRateInHz, param.channelConfig, param.audioFormat, param.bufferSizeInBytes);
        mAudioData = new byte[param.bufferSizeInBytes];
        mParam = param;
        mState = State.CONFIGURED;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void start() {
        if (mState == State.RUNNING) {
            return;
        }
        checkCurrentStateInStates(State.CONFIGURED, State.STOPPED);
        mAudioRecord.startRecording();
        mState = State.RUNNING;
        if (mCallback == null) {
            return;
        }
        if (mExecutorService == null) {
            mExecutorService = Executors.newSingleThreadExecutor();
        }
        if (mRecordRunnable == null) {
            mRecordRunnable = new RecordRunnable();
        }
        mExecutorService.execute(mRecordRunnable);
    }

    public int read(byte[] audioData) {
        checkCurrentStateInStates(State.RUNNING);
        return mAudioRecord.read(audioData, 0, audioData.length);
    }

    @Override
    public void stop() {
        if (mState == State.STOPPED) {
            return;
        }
        checkCurrentStateInStates(State.RUNNING);
        mAudioRecord.stop();
        mState = State.STOPPED;
    }

    @Override
    public void release() {
        if (mState == State.UNINITIALIZED || mState == State.RELEASED) {
            return;
        }
        mAudioRecord.release();
        mAudioRecord = null;
        mState = State.RELEASED;
    }

    public long getPtsPerSample() {
        return mParam.getPtsPerSample();
    }

    public long computePtsByCount(long count) {
        return mParam.computePtsByCount(count);
    }

    private class RecordRunnable implements Runnable {
        @Override
        public void run() {
            while (isRunning()) {
                int read = mAudioRecord.read(mAudioData, 0, mAudioData.length);
                if (read >= 0) {
                    mCallback.onDataAvailable(mAudioData);
                } else {
                    mCallback.onDataError(read);
                }
            }
        }
    }

    public static class Param {
        /**
         * 音频源
         *
         * 麦克风：{@link android.media.MediaRecorder.AudioSource#MIC}
         */
        private int audioSource;
        /**
         * 采样率
         *
         * 音频CD：44100
         * miniDV数码视频camcorder：32000
         * FM调频广播：24000, 22050
         * AM调幅广播：11025
         * 电话：8000
         */
        private int sampleRateInHz;
        /**
         * 声道设置
         *
         * 单声道：{@link AudioFormat#CHANNEL_IN_MONO}
         * 立体声：{@link AudioFormat#CHANNEL_IN_STEREO}
         */
        private int channelConfig;
        /**
         * 编码制式
         *
         * 主流：{@link AudioFormat#ENCODING_PCM_16BIT}
         * 低质量：{@link AudioFormat#ENCODING_PCM_8BIT}
         */
        private int audioFormat;
        /**
         * buffer大小
         */
        private int bufferSizeInBytes;

        public Param(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) {
            this.audioSource = audioSource;
            this.sampleRateInHz = sampleRateInHz;
            this.channelConfig = channelConfig;
            this.audioFormat = audioFormat;
            this.bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        }

        public int getChannel() {
            return channelConfig == AudioFormat.CHANNEL_IN_STEREO ? 2 : 1;
        }

        public int getBit() {
            return audioFormat == AudioFormat.ENCODING_PCM_16BIT ? 16 : 8;
        }

        public int getBufferSizeInBytes() {
            return bufferSizeInBytes;
        }

        public long getPtsPerSample() {
            return computePtsByCount(1);
        }

        public long computePtsByCount(long count) {
            return computePtsBySize(bufferSizeInBytes * count);
        }

        public long computePtsBySize(long size) {
            int bit = getBit();
            int channel = getChannel();
            return size * 1000_000 / (sampleRateInHz * bit * channel / 8);
        }
    }

    public interface Callback {
        void onDataAvailable(byte[] data);

        void onDataError(int error);
    }
}
