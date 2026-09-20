package org.kazinduzi.rinjora.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lightweight celebratory overlay mirroring the prototype's confetti burst on a
 * correct answer / end screen. A one-shot animation of ~140 falling chip particles in
 * the Rinjora palette; auto-hides when done. Non-interactive (transparent touch).
 */
public class ConfettiView extends View {

    private static final long DURATION_MS = 2600L;
    private static final int COUNT = 140;

    private static final int[] COLORS = {
            Color.rgb(23, 145, 90),   // proto_green
            Color.rgb(246, 178, 26),  // proto_gold
            Color.rgb(180, 68, 31),   // proto_terra
            Color.rgb(107, 66, 38),   // proto_choco
            Color.rgb(216, 155, 74),  // proto_ochre
            Color.rgb(225, 75, 75),   // proto_red
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private ValueAnimator animator;
    private long startMs;

    public ConfettiView(@NonNull Context context) {
        this(context, null);
    }

    public ConfettiView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setVisibility(GONE);
    }

    /** Fire the burst (idempotent — restarts if already running). */
    public void play() {
        stop();
        int w = getWidth();
        int h = getHeight();
        if (w <= 0) w = 1080;
        if (h <= 0) h = 1920;
        particles.clear();
        for (int i = 0; i < COUNT; i++) {
            particles.add(new Particle(w, h));
        }
        startMs = System.currentTimeMillis();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.addUpdateListener(a -> postInvalidateOnAnimation());
        animator.start();
        setVisibility(VISIBLE);
        postInvalidateOnAnimation();
    }

    public void stop() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        particles.clear();
        setVisibility(GONE);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (particles.isEmpty()) {
            return;
        }
        float t = (System.currentTimeMillis() - startMs) / (float) DURATION_MS;
        for (Particle p : particles) {
            p.draw(canvas, paint);
        }
        if (t < 1f) {
            postInvalidateOnAnimation();
        } else {
            stop();
        }
    }

    private class Particle {
        private final float x0;
        private final float y0;
        private final float vx;
        private final float vy;
        private final float size;
        private final float rotation0;
        private final float vr;
        private final int color;

        Particle(int w, int h) {
            x0 = random.nextFloat() * w;
            y0 = -40f - random.nextFloat() * h * 0.5f;
            vx = (random.nextFloat() - 0.5f) * 260f;
            vy = 240f + random.nextFloat() * 460f;
            size = 6f + random.nextFloat() * 9f;
            rotation0 = random.nextFloat() * 360f;
            vr = (random.nextFloat() - 0.5f) * 360f;
            color = COLORS[random.nextInt(COLORS.length)];
        }

        void draw(Canvas canvas, Paint paint) {
            float dt = (System.currentTimeMillis() - startMs) / 1000f;
            float x = x0 + vx * dt;
            float y = y0 + vy * dt;
            if (y > getHeight() + size) {
                return;
            }
            paint.setColor(color);
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(rotation0 + vr * dt);
            canvas.drawRect(-size / 2f, -size / 2f, size / 2f, size * 0.6f, paint);
            canvas.restore();
        }
    }
}