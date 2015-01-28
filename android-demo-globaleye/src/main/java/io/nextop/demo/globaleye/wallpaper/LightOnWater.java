package io.nextop.demo.globaleye.wallpaper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import net.rbgrn.android.glwallpaperservice.GLWallpaperService;

import javax.microedition.khronos.opengles.GL10;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.TimeUnit;

public class LightOnWater extends GLWallpaperService {
    static final String TAG = "LightOnWater";


    final Handler handler = new Handler();
    @Nullable
    Movement movement = null;


    @Override
    public void onCreate() {
        super.onCreate();

        new LoadMovement().execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new LightOnWaterEngine();
    }


    static final class Movement {
        final float ccx;
        final float ccy;
        final P[][] allPoints;

        Movement(float ccx, float ccy, P[][] allPoints) {
            this.ccx = ccx;
            this.ccy = ccy;
            this.allPoints = allPoints;
        }
    }

    static final class P {
        final float x;
        final float y;
        final float s;
        final float q;
        final float t;
        final float r;
        final float vx;
        final float vy;


        float rx;
        float ry;
        float rs;
        final float[] sc = new float[4];
        final float[] fc = new float[4];
        boolean stroke;
        boolean fill;

        P(float x, float y, float s, float q, float t, float r) {
            this.x = x;
            this.y = y;
            this.s = s;
            this.q = q;
            this.t = t;
            this.r = r;
            vx = (float) Math.cos(t);
            vy = (float) Math.sin(t);
        }
    }


    private final class LoadMovement extends AsyncTask<Void, Void, Movement> {
        @Override
        @Nullable
        protected Movement doInBackground(Void... args) {
            try {
                BitInputStream bis = new BitInputStream(new BufferedInputStream(getAssets().open("points.bin")));
                try {
                    // [ccx, 16 bits][ccy, 16 bits][# frames, 16 bits][frame]...
                    // [frame] = [# points 16 bits][point]...
                    // [point] = [x, 9 bits][y, 9 bits][s * 15, 7 bits][q * 15, 5 bits]

                    int ccx = bis.readBits(16);
                    int ccy = bis.readBits(16);

                    float hccx = ccx * 0.5f;
                    float hccy = ccy * 0.5f;

                    int c = bis.readBits(16);
                    P[][] allPoints = new P[c][];
                    for (int i = 0; i < c; ++i) {
                        int n = bis.readBits(16);
                        P[] points = new P[n];
                        allPoints[i] = points;

                        for (int j = 0; j < n; ++j) {
                            int x = bis.readBits(9);
                            int y = bis.readBits(9);
                            float s = bis.readBits(7) / 15.f;
                            float q = bis.readBits(5) / 15.f;


                            // convert to radial
                            float dx = x - hccx;
                            float dy = y - hccy;
                            float t = (float) Math.atan2(dy, dx);
                            float r = (float) Math.sqrt(dx * dx + dy * dy);

                            points[j] = new P(x, y, s, q, t, r);
                        }
                    }

                    return new Movement(ccx, ccy, allPoints);
                } finally {
                    bis.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "LoadPoints#doInBackground", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Movement movement) {
            LightOnWater.this.movement = movement;
        }
    }

    private static final class BitInputStream {
        private final InputStream is;
        private int b = -1;
        private int bi = 8;

        BitInputStream(BufferedInputStream is) {
            this.is = is;
        }

        private static final int[] masks = {
                0xFF,
                0x7F,
                0x3F,
                0x1F,
                0x0F,
                0x07,
                0x03,
                0x01
        };

        public int readBits(int n) throws IOException {
            if (32 < n) {
                throw new IllegalArgumentException();
            }

            int out = 0;

            while (0 < n) {
                if (8 <= bi) {
                    b = is.read();
                    if (b < 0) {
                        break;
                    }
                    bi = 0;
                }

                int a = 8 - bi;
                int u = a < n ? a : n;

                out <<= u;
                out |= (b & masks[bi]) >>> (a - u);

                bi += u;
                n -= u;
            }

            return out;
        }

        public void close() throws Exception {
            is.close();
        }
    }


    /**
     * image masks to be used as tinted textures to draw shapes (circles)
     */
    private static final class Masks {
        static Masks create(int s, float ts, float tStrokeWidth) {
            Paint paint = new Paint();
            paint.setColor(Color.argb(255, 255, 255, 255));
            paint.setAntiAlias(true);

            Canvas c;

            float p = 4;

            Bitmap fill = Bitmap.createBitmap(2 * s, 2 * s, Bitmap.Config.ARGB_8888);
            c = new Canvas(fill);
            paint.setStyle(Paint.Style.FILL);
            c.drawCircle(s, s, s - p, paint);

            Bitmap stroke = Bitmap.createBitmap(2 * s, 2 * s, Bitmap.Config.ARGB_8888);
            c = new Canvas(stroke);
            paint.setStyle(Paint.Style.STROKE);
            // when sized at ts the stroke width should be tStrokeWidth
            // strokeWidth / s = tStrokeWidth / ts
            paint.setStrokeWidth(tStrokeWidth * s / ts);
            c.drawCircle(s, s, s - p, paint);

            return new Masks(fill, stroke);
        }


        final Bitmap circleFill;
        final Bitmap circleStroke;

        Masks(Bitmap circleFill, Bitmap circleStroke) {
            this.circleFill = circleFill;
            this.circleStroke = circleStroke;
        }
    }


    final class LightOnWaterEngine extends GLEngine {
        volatile boolean touch = false;
        volatile float touchX = 0.f;
        volatile float touchY = 0.f;
        volatile float offsetX = 0.f;
        volatile float offsetY = 0.f;

        final Renderer renderer;


        LightOnWaterEngine() {
            renderer = new Renderer(Masks.create(128, 8.f, 1.f));
            setRenderer(renderer);
            setRenderMode(RENDERMODE_CONTINUOUSLY);
        }


        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            setTouchEventsEnabled(true);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                                     float xStep, float yStep, int xPixels, int yPixels) {
            super.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);

            offsetX = xPixels;
            offsetY = yPixels;
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    touch = true;
                    touchX = event.getX();
                    touchY = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    touch = false;
                    break;
                default:
                    // ignore
                    break;
            }
        }


        private class Renderer implements GLWallpaperService.Renderer {
            final Masks masks;


            int width = 0;
            int height = 0;

            long startNanos = 0L;

            final float fps = 60.f;
            final int tt = 2;

            float frameCount;

            float mmm = 0.f;


            int fillTextureId;
            int strokeTextureId;
            FloatBuffer circleVertexes2d;
            FloatBuffer circleTexCoords2d;
            ShortBuffer circleIndexes;


            Renderer(Masks masks) {
                this.masks = masks;
            }

            void release() {
                masks.circleFill.recycle();
                masks.circleStroke.recycle();
            }


            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                this.width = width;
                this.height = height;

                gl.glViewport(0, 0, width, height);

                gl.glMatrixMode(GL10.GL_PROJECTION);
                gl.glLoadIdentity();
                GLU.gluOrtho2D(gl, 0, width, 0, height);

                gl.glMatrixMode(GL10.GL_MODELVIEW);
                gl.glLoadIdentity();
            }

            @Override
            public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig eglConfig) {
                startNanos = System.nanoTime();

                gl.glDisable(GL10.GL_DEPTH_TEST);
                gl.glEnable(GL10.GL_TEXTURE_2D);

                gl.glEnable(GL10.GL_BLEND);
                gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

                fillTextureId = loadTexture(gl, masks.circleFill);

                strokeTextureId = loadTexture(gl, masks.circleStroke);


                circleVertexes2d = toBuffer(new float[]{
                        -1.f, 1.f,
                        -1.f, -1.f,
                        1.f, -1.f,
                        1.f, 1.f,
                });
                circleTexCoords2d = toBuffer(new float[]{
                        0.f, 0.f,
                        0.f, 1.f,
                        1.f, 1.f,
                        1.f, 0.f
                });
                circleIndexes = toBuffer(new short[]{
                        0, 1, 2,
                        0, 2, 3
                });
            }


            @Override
            public void onDrawFrame(GL10 gl) {
                long nanos = System.nanoTime();
                frameCount = TimeUnit.NANOSECONDS.toMillis(nanos - startNanos) * fps / TimeUnit.SECONDS.toMillis(1);


                gl.glClearColor(0.f, 0.f, 0.f, 0.f);
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

                if (touch) {
                    mmm = lerp(mmm, 1.f, 0.5f);
                } else {
                    mmm = lerp(mmm, 0.f, 0.25f);
                }

                if (null != movement) {
                    drawMovement(gl, movement);
                }

            }


            void drawMovement(GL10 gl, Movement m) {
                float ku = (frameCount % tt) / tt;
                int k = (int) (frameCount / tt) % m.allPoints.length;

                P[] points = m.allPoints[k];
                P[] ppoints = m.allPoints[(k + m.allPoints.length - 1) % m.allPoints.length];


                gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
                gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

                gl.glVertexPointer(2, GL10.GL_FLOAT, 0, circleVertexes2d);
                gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, circleTexCoords2d);

                gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
                gl.glColorMask(true, true, true, true);

                layout(m.ccx, m.ccy, ppoints, 1.f, 0.f);
                fill(gl, ppoints);
                stroke(gl, ppoints);

                layout(m.ccx, m.ccy, points, lerp(0.f, 1.f, ku), 0.8f);
                fill(gl, points);
                stroke(gl, points);
            }

            /**
             * stores layout state into layout fields of points
             */
            void layout(float ccx, float ccy, P[] points, float sf, float ff) {
                float rCr = (float) Math.sqrt(ccx * ccx + ccy * ccy) / 2;
                float rCx = width * 0.5f;
                float rCy = height * 0.5f;
                float rTr = Math.max(width, height) * 0.5f;
                float rTfs = height * 0.3f;
                float rTor = rTfs * 0.5f;

                float xOff = offsetX * 0.8f;
                float yOff = offsetY * 0.8f;

                float[] a = new float[4];
                float[] b = new float[4];

                float ss = width / 800.f;
                for (int i = points.length - 1; 0 <= i; --i) {
                    P p = points[i];

                    float rf = p.r / rCr;
                    // semi-spherical (or inverted) transform of r
                    float r = rTr * (float) Math.pow(rf, 0.95);


                    float af = p.q * (float) Math.pow(1.f - r / rTr, lerp(0.5f, 2.f, mmm));
                    float h = (float) Math.sqrt(rTr * rTr - r * r);


                    p.rs = p.s * lerp(0.5f * ss * lerp(1.f, 4.f, mmm), 4.f * ss * lerp(1.f, 1.7f, mmm), (float) Math.pow(h / rTr, 4.0));
                    p.rx = xOff + rCx + r * p.vx;
                    p.ry = yOff + rCy + r * p.vy;


                    p.stroke = 0 < sf;
                    if (p.stroke) {
                        float[] out = p.sc;
                        if (r < rTor) {
                            alphaScale(COLOR_PUPIL, sf, a);
                            alphaScale(COLOR_RETINA, sf * af, b);
                            lerpColor(a, b,
                                    (float) Math.pow(map(r, 0, rTor, 0.f, 1.f), 2.f), out);
                        } else {
                            alphaScale(COLOR_RETINA, sf * af, a);
                            alphaScale(COLOR_OUTER, sf * af, b);
                            lerpColor(a, b,
                                    (float) Math.pow(map(r, rTor, rTr, 0.f, 1.f), 0.5f), out);
                        }
                    }

                    p.fill = 0 < ff;
                    if (p.fill) {
                        float[] out = p.fc;
                        if (r < rTor) {
                            alphaScale(COLOR_PUPIL, ff, a);
                            alphaScale(COLOR_RETINA, ff * af, b);
                            lerpColor(a, b,
                                    (float) Math.pow(map(r, 0, rTor, 0.f, 1.f), 2.f), out);
                        } else {
                            alphaScale(COLOR_RETINA, ff * af, a);
                            alphaScale(COLOR_OUTER, ff * af, b);
                            lerpColor(a, b,
                                    (float) Math.pow(map(r, rTor, rTr, 0.f, 1.f), 0.5f), out);
                        }
                    }
                }
            }

            private void stroke(GL10 gl, P[] points) {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, strokeTextureId);

                for (P p : points) {
                    if (p.stroke) {
                        p(gl, p.rx, p.ry, p.rs, p.fc);
                    }
                }
            }

            private void fill(GL10 gl, P[] points) {
                gl.glBindTexture(GL10.GL_TEXTURE_2D, fillTextureId);

                for (P p : points) {
                    if (p.fill) {
                        p(gl, p.rx, p.ry, p.rs, p.fc);
                    }
                }
            }

            private void p(GL10 gl, float x, float y, float s, float[] c) {
                gl.glPushMatrix();
                {
                    gl.glTranslatef(x, y, 0.f);
                    gl.glScalef(s, s, 1.f);

                    gl.glColor4f(c[0], c[1], c[2], c[3]);

                    gl.glDrawElements(GL10.GL_TRIANGLES, circleIndexes.remaining(),
                            GL10.GL_UNSIGNED_SHORT, circleIndexes);
                }
                gl.glPopMatrix();
            }
        }
    }


    /////// GL UTILS ///////

    static FloatBuffer toBuffer(float... values) {
        return (FloatBuffer) ByteBuffer.allocateDirect(values.length * Float.SIZE / 8)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(values)
                .position(0);
    }

    static ShortBuffer toBuffer(short... values) {
        return (ShortBuffer) ByteBuffer.allocateDirect(values.length * Short.SIZE / 8)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(values)
                .position(0);
    }

    static int loadTexture(GL10 gl, Bitmap b) {
        int[] id = new int[1];
        gl.glGenTextures(1, id, 0);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, id[0]);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, b, 0);

        return id[0];
    }


    //////// PROCESSING + GRAPHICS UTILS ///////

    static final float PI = (float) (Math.PI);
    static final float TWO_PI = (float) (2 * Math.PI);

    static float dist(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    static float lerp(float start, float stop, float amt) {
        return start + (stop - start) * amt;
    }

    static void lerpColor(float[] c1, float[] c2, float amt, float[] out) {
        out[0] = lerp(c1[0], c2[0], amt);
        out[1] = lerp(c1[1], c2[1], amt);
        out[2] = lerp(c1[2], c2[2], amt);
        out[3] = lerp(c1[3], c2[3], amt);
    }

    static float map(float value, float start1, float stop1, float start2, float stop2) {
        return lerp(start2, stop2, (value - start1) / (stop1 - start1));
    }

    static void alphaScale(float[] c, float af, float[] out) {
        out[0] = c[0];
        out[1] = c[1];
        out[2] = c[2];
        out[3] = c[3] * af;
    }


    /////// COLORS ///////

    /**
     * convert [0, 255] components to [0, 1]
     */
    static float[] rgbaNormalize(float[] rgba) {
        return new float[]{
                rgba[0] / 255.f,
                rgba[1] / 255.f,
                rgba[2] / 255.f,
                rgba[3] / 255.f
        };
    }

    static final float[] COLOR_PUPIL = rgbaNormalize(new float[]{240, 40, 120, 255});
    static final float[] COLOR_RETINA = rgbaNormalize(new float[]{40, 170, 240, 255});
    static final float[] COLOR_OUTER = rgbaNormalize(new float[]{220, 220, 220, 255});
}
