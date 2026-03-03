package org.example.Services.user;

import javafx.application.Platform;
import javafx.scene.image.*;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.*;

/**
 * CameraService — OpenCV 4.7.0 webcam service.
 *
 * Loading strategy (tried in order):
 *  1. Extract opencv_java470.dll from the openpnp jar to a temp file → System.load()
 *  2. System.loadLibrary("opencv_java470")  (if dll is on java.library.path)
 *  3. Abort with a clear error message
 *
 * The static block runs exactly once when the class is first referenced,
 * before any constructor or field initializer — this is why the previous
 * UnsatisfiedLinkError occurred (Mat was constructed before any load call).
 */
public class CameraService {

    // ── Native library name for OpenCV 4.7.0 ─────────────────────────────────
    private static final String OPENCV_LIB_NAME    = "opencv_java470";
    // Path inside the openpnp jar (works on Windows x64)
    private static final String[] NATIVE_JAR_PATHS = {
            "/nu/pattern/opencv/windows/x86_64/" + OPENCV_LIB_NAME + ".dll",  // openpnp 4.x layout
            "/org/opencv/windows/x86_64/"         + OPENCV_LIB_NAME + ".dll",
            "/"                                   + OPENCV_LIB_NAME + ".dll",
    };

    // ── Load native library ONCE before any Mat is constructed ────────────────
    static {
        if (!tryLoadFromJar()) {
            if (!tryLoadLibrary()) {
                System.err.println(
                        "[CameraService] FATAL: opencv_java470 native library could not be loaded.\n" +
                                "  Ensure org.openpnp:opencv:4.7.0-0 is in your pom.xml and\n" +
                                "  run: mvn dependency:resolve  then restart the application."
                );
            }
        }
    }

    /**
     * Extracts the .dll bundled inside the openpnp opencv jar to a temp file
     * and loads it via System.load(absolutePath).
     */
    private static boolean tryLoadFromJar() {
        for (String path : NATIVE_JAR_PATHS) {
            try (InputStream in = CameraService.class.getResourceAsStream(path)) {
                if (in == null) continue;

                // Write to a temp file (JVM requires an absolute path for System.load)
                File tmp = File.createTempFile(OPENCV_LIB_NAME + "_", ".dll");
                tmp.deleteOnExit();

                try (OutputStream out = Files.newOutputStream(tmp.toPath())) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                }

                System.load(tmp.getAbsolutePath());
                System.out.println("[CameraService] OpenCV 4.7.0 loaded from jar resource: " + path);
                return true;

            } catch (Throwable ignored) {}
        }
        return false;
    }

    /**
     * Falls back to System.loadLibrary — works if the dll is on java.library.path.
     */
    private static boolean tryLoadLibrary() {
        try {
            System.loadLibrary(OPENCV_LIB_NAME);
            System.out.println("[CameraService] OpenCV loaded via System.loadLibrary.");
            return true;
        } catch (Throwable t) {
            System.err.println("[CameraService] loadLibrary failed: " + t.getMessage());
            return false;
        }
    }

    // ── Instance state ────────────────────────────────────────────────────────
    private VideoCapture             capture;
    private ScheduledExecutorService executor;
    private volatile Mat             lastFrame = new Mat();
    private volatile boolean         running   = false;

    // ─── Start Camera ─────────────────────────────────────────────────────────

    public boolean start(ImageView imageView) {
        // Try index 0 then 1 with DirectShow (fixes ASUS / modern USB webcam timeout on Windows)
        for (int index = 0; index <= 1; index++) {
            capture = new VideoCapture();
            capture.open(index, Videoio.CAP_DSHOW);

            if (capture.isOpened()) {
                capture.set(Videoio.CAP_PROP_FRAME_WIDTH,  640);
                capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
                capture.set(Videoio.CAP_PROP_FPS,          30);
                running = true;
                startFrameLoop(imageView);
                return true;
            }
        }

        // Fallback: no explicit backend (Linux / macOS)
        capture = new VideoCapture(0);
        if (capture.isOpened()) {
            running = true;
            startFrameLoop(imageView);
            return true;
        }

        return false;
    }

    // ─── Frame Loop ───────────────────────────────────────────────────────────

    private void startFrameLoop(ImageView imageView) {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "camera-frame-loop");
            t.setDaemon(true);
            return t;
        });

        executor.scheduleAtFixedRate(() -> {
            if (!running || capture == null || !capture.isOpened()) return;
            Mat frame = new Mat();
            if (capture.read(frame) && !frame.empty()) {
                Core.flip(frame, frame, 1); // mirror / selfie view
                lastFrame = frame.clone();
                Image fxImage = matToFxImage(frame);
                if (fxImage != null) {
                    Platform.runLater(() -> imageView.setImage(fxImage));
                }
            }
        }, 0, 33, TimeUnit.MILLISECONDS);
    }

    // ─── Capture Frame as JPEG bytes ─────────────────────────────────────────

    public byte[] captureFrame() {
        if (lastFrame == null || lastFrame.empty()) return null;
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", lastFrame, buffer);
        byte[] bytes = buffer.toArray();
        return bytes.length == 0 ? null : bytes;
    }

    // ─── Stop Camera ─────────────────────────────────────────────────────────

    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            try { executor.awaitTermination(500, TimeUnit.MILLISECONDS); }
            catch (InterruptedException ignored) {}
            executor = null;
        }
        if (capture != null) {
            if (capture.isOpened()) capture.release();
            capture = null;
        }
        if (lastFrame != null) {
            lastFrame.release();
            lastFrame = new Mat();
        }
    }

    public boolean isRunning() { return running; }

    // ─── Mat → JavaFX Image ───────────────────────────────────────────────────

    private Image matToFxImage(Mat frame) {
        try {
            Mat rgb = new Mat();
            Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_BGR2RGB);
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".jpg", rgb, buffer);
            byte[] bytes = buffer.toArray();
            if (bytes.length == 0) return null;
            return new Image(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }
}