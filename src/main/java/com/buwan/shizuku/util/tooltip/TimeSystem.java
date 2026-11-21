package com.buwan.shizuku.util.tooltip;

/**
 * 给 shader / 动画用的时间源：同时维护游戏时间、shader 时间与动画时间三套输出
 * （TimeOutput 含 ticks / seconds / normalizedTime），并支持切换真实时间。
 */
public class TimeSystem {
    private static TimeSystem instance;
    private long totalTicks = 0L;
    private float totalSeconds = 0.0F;
    private float smoothTime = 0.0F;
    private long startTimeMillis = System.currentTimeMillis();
    private float realTimeSeconds = 0.0F;
    private float tickRate = 20.0F;
    private float timeScale = 1.0F;
    private boolean isPaused = false;
    private boolean useRealTime = true;
    private static final float MAX_FLOAT_CYCLE = 3600.0F;
    private static final float MAX_SHADER_TIME = 86400.0F;
    private static final long MAX_TICK_CYCLE = 72000L;
    private final TimeSystem.TimeOutput gameTime = new TimeSystem.TimeOutput();
    private final TimeSystem.TimeOutput shaderTime = new TimeSystem.TimeOutput();
    private final TimeSystem.TimeOutput animationTime = new TimeSystem.TimeOutput();
    private final TimeSystem.TimeOutput realTime = new TimeSystem.TimeOutput();

    private TimeSystem() {
    }

    public static TimeSystem getInstance() {
        if (instance == null) {
            instance = new TimeSystem();
        }

        return instance;
    }

    public void tick() {
        this.updateRealTime();
        if (!this.isPaused) {
            this.totalTicks++;
            this.totalSeconds = this.totalSeconds + 1.0F / this.tickRate * this.timeScale;
            this.smoothTime = this.smoothTime + 0.05F * this.timeScale;
            this.preventOverflow();
        }

        this.updateOutputs();
    }

    public void onRenderTick() {
        this.updateRealTime();
        this.updateOutputs();
    }

    private void updateRealTime() {
        if (this.useRealTime) {
            long currentTimeMillis = System.currentTimeMillis();
            this.realTimeSeconds = (float)(currentTimeMillis - this.startTimeMillis) / 1000.0F * this.timeScale;
            if (this.realTimeSeconds >= 86400.0F) {
                this.startTimeMillis = currentTimeMillis;
                this.realTimeSeconds = 0.0F;
            }
        }
    }

    private void preventOverflow() {
        if (this.totalTicks >= 72000L) {
            this.totalTicks %= 72000L;
        }

        if (this.totalSeconds >= 86400.0F) {
            this.totalSeconds %= 86400.0F;
        }

        if (this.smoothTime >= 3600.0F) {
            this.smoothTime %= 3600.0F;
        }
    }

    private void updateOutputs() {
        this.gameTime.update(this.totalTicks, this.totalSeconds, this.totalSeconds);
        float shaderSeconds = this.totalSeconds % 3600.0F;
        this.shaderTime.update(this.totalTicks, shaderSeconds, shaderSeconds * 0.1F);
        float animSeconds = this.smoothTime % 3600.0F;
        this.animationTime.update(this.totalTicks, animSeconds, animSeconds * 0.01F);
        float realSeconds = this.realTimeSeconds % 3600.0F;
        this.realTime.update(this.totalTicks, realSeconds, realSeconds * 0.1F);
    }

    public TimeSystem.TimeOutput getGameTime() {
        return this.gameTime;
    }

    public TimeSystem.TimeOutput getShaderTime() {
        return this.shaderTime;
    }

    public TimeSystem.TimeOutput getAnimationTime() {
        return this.animationTime;
    }

    public TimeSystem.TimeOutput getRealTime() {
        return this.realTime;
    }

    public float getShaderTimeValue() {
        TimeSystem.TimeOutput timeSource = this.useRealTime ? this.realTime : this.shaderTime;
        return timeSource.getSeconds() / 20.0F;
    }

    public float getFastShaderTime() {
        TimeSystem.TimeOutput timeSource = this.useRealTime ? this.realTime : this.shaderTime;
        return timeSource.getSeconds() * 2.0F;
    }

    public float getSlowShaderTime() {
        TimeSystem.TimeOutput timeSource = this.useRealTime ? this.realTime : this.shaderTime;
        return timeSource.getSeconds() * 0.5F;
    }

    public float getBreathingTime(float intensity) {
        TimeSystem.TimeOutput timeSource = this.useRealTime ? this.realTime : this.shaderTime;
        return timeSource.getPulseTime(4.0F, 1.0F - intensity, 1.0F + intensity);
    }

    public void setTimeScale(float scale) {
        this.timeScale = Math.max(0.0F, scale);
    }

    public float getTimeScale() {
        return this.timeScale;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public boolean isPaused() {
        return this.isPaused;
    }

    public void setUseRealTime(boolean useRealTime) {
        this.useRealTime = useRealTime;
    }

    public boolean isUsingRealTime() {
        return this.useRealTime;
    }

    public void reset() {
        this.totalTicks = 0L;
        this.totalSeconds = 0.0F;
        this.smoothTime = 0.0F;
        this.startTimeMillis = System.currentTimeMillis();
        this.realTimeSeconds = 0.0F;
        this.updateOutputs();
    }

    public void setTickRate(float rate) {
        this.tickRate = Math.max(1.0F, rate);
    }

    public static class TimeOutput {
        private long ticks;
        private float seconds;
        private float normalizedTime;
        private float smoothDelta;

        private void update(long ticks, float seconds, float smoothDelta) {
            this.ticks = ticks;
            this.seconds = seconds;
            this.normalizedTime = seconds % 60.0F / 60.0F;
            this.smoothDelta = smoothDelta;
        }

        public long getTicks() {
            return this.ticks;
        }

        public float getSeconds() {
            return this.seconds;
        }

        public float getMilliseconds() {
            return this.seconds * 1000.0F;
        }

        public float getNormalizedTime() {
            return this.normalizedTime;
        }

        public float getInterpolatedSeconds(float partialTicks) {
            return this.seconds + partialTicks / 20.0F;
        }

        public float getInterpolatedCyclicTime(float period, float partialTicks) {
            float interpolatedSeconds = this.getInterpolatedSeconds(partialTicks);
            return interpolatedSeconds % period / period;
        }

        public float getInterpolatedSineTime(float period, float partialTicks) {
            float cyclicTime = this.getInterpolatedCyclicTime(period, partialTicks);
            return (float)Math.sin(cyclicTime * Math.PI * 2.0);
        }

        public float getInterpolatedCosineTime(float period, float partialTicks) {
            float cyclicTime = this.getInterpolatedCyclicTime(period, partialTicks);
            return (float)Math.cos(cyclicTime * Math.PI * 2.0);
        }

        public float getInterpolatedPulseTime(float period, float minValue, float maxValue, float partialTicks) {
            float cyclicTime = this.getInterpolatedCyclicTime(period, partialTicks);
            float sine = (float)Math.sin(cyclicTime * Math.PI * 2.0) * 0.5F + 0.5F;
            return minValue + sine * (maxValue - minValue);
        }

        public float getCyclicTime(float period) {
            return this.seconds % period / period;
        }

        public float getSineTime(float period) {
            return (float)Math.sin(this.seconds % period / period * Math.PI * 2.0);
        }

        public float getCosineTime(float period) {
            return (float)Math.cos(this.seconds % period / period * Math.PI * 2.0);
        }

        public float getSmoohDelta() {
            return this.smoothDelta;
        }

        public float getPulseTime(float period, float minValue, float maxValue) {
            float normalized = this.getCyclicTime(period);
            float sine = (float)Math.sin(normalized * Math.PI * 2.0) * 0.5F + 0.5F;
            return minValue + sine * (maxValue - minValue);
        }
    }
}
