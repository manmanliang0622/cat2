package catcatch;

import java.io.File;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

final class SynthAudio {
    private static final float SAMPLE_RATE = 22_050f;
    private volatile boolean musicEnabled = true;
    private volatile boolean sfxEnabled   = true;
    private volatile float   volume       = 0.7f;
    private final Random rng = new Random();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "catcatch-audio");
        t.setDaemon(true);
        return t;
    });

    // MP3 MediaPlayers
    private final MediaPlayer[] meowPlayers;
    private final MediaPlayer   barkPlayer;
    private final MediaPlayer   bgmPlayer;

    SynthAudio() {
        String base = new File("assets").getAbsolutePath();
        meowPlayers = new MediaPlayer[3];
        for (int i = 0; i < 3; i++) {
            meowPlayers[i] = loadMedia(base + "/meow" + (i + 1) + ".mp3");
        }
        barkPlayer = loadMedia(base + "/bark.mp3");
        MediaPlayer bgm = loadMedia(base + "/bgm.mp3");
        if (bgm != null) {
            bgm.setCycleCount(MediaPlayer.INDEFINITE);
            bgm.setVolume(volume * 0.5);
        }
        bgmPlayer = bgm;
    }

    private MediaPlayer loadMedia(String path) {
        File f = new File(path);
        if (!f.exists()) return null;
        try {
            return new MediaPlayer(new Media(f.toURI().toString()));
        } catch (Exception e) {
            return null;
        }
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    void setMusicEnabled(boolean on) {
        musicEnabled = on;
        if (bgmPlayer == null) return;
        if (on) {
            bgmPlayer.play();
        } else {
            bgmPlayer.pause();
        }
    }

    void setSfxEnabled(boolean on) { sfxEnabled = on; }

    /** Legacy single-switch used by old code paths — keeps both in sync. */
    void setEnabled(boolean on) { setMusicEnabled(on); setSfxEnabled(on); }

    void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        if (bgmPlayer != null) bgmPlayer.setVolume(volume * 0.5);
    }

    boolean isMusicEnabled() { return musicEnabled; }
    boolean isSfxEnabled()   { return sfxEnabled; }
    boolean isEnabled()      { return sfxEnabled; }
    float   getVolume()      { return volume; }

    // ── BGM ───────────────────────────────────────────────────────────────────

    /** Start BGM from the beginning (or resume if already playing). */
    void startBgm() {
        if (!musicEnabled || bgmPlayer == null) return;
        MediaPlayer.Status status = bgmPlayer.getStatus();
        if (status == MediaPlayer.Status.PLAYING) return;
        if (status == MediaPlayer.Status.STOPPED || status == MediaPlayer.Status.READY) {
            bgmPlayer.seek(Duration.ZERO);
        }
        bgmPlayer.play();
    }

    void stopBgm() {
        if (bgmPlayer != null) bgmPlayer.stop();
    }

    // ── SFX ───────────────────────────────────────────────────────────────────

    void playMeow() {
        if (!sfxEnabled) return;
        MediaPlayer p = meowPlayers[rng.nextInt(meowPlayers.length)];
        if (p != null) {
            p.stop(); p.seek(Duration.ZERO); p.setVolume(volume); p.play();
        } else {
            play(this::buildMeow);
        }
    }

    void playBark() {
        if (!sfxEnabled) return;
        if (barkPlayer != null) {
            barkPlayer.stop(); barkPlayer.seek(Duration.ZERO); barkPlayer.setVolume(volume); barkPlayer.play();
        } else {
            play(this::buildBark);
        }
    }

    void playDog()     { playBark(); }
    void playStart()   { if (sfxEnabled) play(() -> sequence(note(660,0.10,0.24),silence(0.03),note(880,0.12,0.26),silence(0.03),note(1050,0.18,0.28))); }
    void playSuccess() { if (sfxEnabled) play(() -> sequence(note(720,0.06,0.20),note(920,0.08,0.26))); }
    void playWrong()   { if (sfxEnabled) play(() -> sequence(note(300,0.09,0.18),note(220,0.12,0.18))); }
    void playFinish()  { if (sfxEnabled) play(() -> sequence(note(523,0.09,0.22),silence(0.02),note(659,0.10,0.24),silence(0.02),note(784,0.16,0.27))); }

    void shutdown() { stopBgm(); executor.shutdownNow(); }

    // ── Synth helpers ─────────────────────────────────────────────────────────

    private void play(SampleProducer producer) {
        executor.submit(() -> writeSamples(producer.produce()));
    }

    private byte[] buildMeow() {
        int n = (int)(SAMPLE_RATE * 0.42);
        byte[] data = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double t = i / SAMPLE_RATE;
            double glide = 1.0 - (t / 0.42);
            double freq = 860 - 360 * glide + 40 * Math.sin(2 * Math.PI * 3 * t);
            double env = Math.sin(Math.PI * Math.min(1.0, t / 0.09)) * Math.exp(-2.2 * t);
            double s = (Math.sin(2*Math.PI*freq*t) + 0.45*Math.sin(2*Math.PI*freq*2.02*t)) * env;
            short pcm = (short)Math.max(Math.min(s * 12000 * volume, Short.MAX_VALUE), Short.MIN_VALUE);
            data[i*2] = (byte)(pcm & 0xff); data[i*2+1] = (byte)((pcm>>8) & 0xff);
        }
        return data;
    }

    private byte[] buildBark() {
        int n = (int)(SAMPLE_RATE * 0.24);
        byte[] data = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double t = i / SAMPLE_RATE;
            double env = Math.exp(-12 * t);
            double s = (Math.random()*2-1)*env*0.7 + Math.sin(2*Math.PI*180*t)*0.45*env;
            short pcm = (short)Math.max(Math.min(s * 15000 * volume, Short.MAX_VALUE), Short.MIN_VALUE);
            data[i*2] = (byte)(pcm & 0xff); data[i*2+1] = (byte)((pcm>>8) & 0xff);
        }
        return data;
    }

    private byte[] note(double freq, double sec, double amp) {
        int n = (int)(SAMPLE_RATE * sec);
        byte[] data = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double t = i / SAMPLE_RATE;
            double env = Math.sin(Math.PI * Math.min(1.0, t/sec)) * Math.exp(-4*t);
            double s = Math.sin(2*Math.PI*freq*t) * env * amp;
            short pcm = (short)Math.max(Math.min(s * 32000 * volume, Short.MAX_VALUE), Short.MIN_VALUE);
            data[i*2] = (byte)(pcm & 0xff); data[i*2+1] = (byte)((pcm>>8) & 0xff);
        }
        return data;
    }

    private byte[] silence(double sec) { return new byte[(int)(SAMPLE_RATE*sec)*2]; }

    private byte[] sequence(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) total += p.length;
        byte[] data = new byte[total];
        int off = 0;
        for (byte[] p : parts) { System.arraycopy(p, 0, data, off, p.length); off += p.length; }
        return data;
    }

    private void writeSamples(byte[] data) {
        AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
            line.open(fmt, data.length);
            line.start();
            line.write(data, 0, data.length);
            line.drain();
        } catch (LineUnavailableException ignored) {}
    }

    @FunctionalInterface
    private interface SampleProducer { byte[] produce(); }
}
