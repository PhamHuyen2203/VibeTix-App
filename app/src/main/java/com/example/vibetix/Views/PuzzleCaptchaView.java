package com.example.vibetix.Views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.Random;

/**
 * Displays a puzzle CAPTCHA: a colorful background with a piece cut out,
 * and the piece draggable horizontally by a SeekBar.
 */
public class PuzzleCaptchaView extends View {

    private Bitmap backgroundBitmap; // full background with gap darkened
    private Bitmap pieceBitmap;      // the extracted piece

    private float pieceX = 0f;
    private float targetX;
    private float pieceY;
    private float pieceW;
    private float pieceH;
    private float TOLERANCE_DP = 18f;

    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean generated = false;

    public PuzzleCaptchaView(Context ctx) { super(ctx); init(); }
    public PuzzleCaptchaView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public PuzzleCaptchaView(Context ctx, AttributeSet a, int s) { super(ctx, a, s); init(); }

    private void init() {
        shadowPaint.setColor(Color.parseColor("#66000000"));
        shadowPaint.setStyle(Paint.Style.FILL);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setAlpha(180);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            generatePuzzle(w, h);
        }
    }

    public void regenerate() {
        generated = false;
        int w = getWidth();
        int h = getHeight();
        if (w > 0 && h > 0) generatePuzzle(w, h);
    }

    private void generatePuzzle(int w, int h) {
        // Thử load ảnh thật từ Picsum, fallback grid màu nếu offline
        int seed = new Random().nextInt(1000);
        String url = "https://picsum.photos/seed/" + seed + "/" + w + "/" + h;

        Glide.with(getContext())
                .asBitmap()
                .load(url)
                .override(w, h)
                .centerCrop()
                .into(new CustomTarget<Bitmap>(w, h) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bmp, @Nullable Transition<? super Bitmap> t) {
                        buildPuzzleFromBitmap(bmp, w, h);
                    }
                    @Override
                    public void onLoadFailed(@Nullable Drawable d) {
                        // Offline fallback: dùng grid màu
                        Bitmap fallback = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                        drawColorGrid(new Canvas(fallback), w, h, new Random());
                        buildPuzzleFromBitmap(fallback, w, h);
                    }
                    @Override public void onLoadCleared(@Nullable Drawable d) {}
                });
    }

    private void buildPuzzleFromBitmap(Bitmap source, int w, int h) {
        pieceW = w * 0.18f;
        pieceH = h * 0.55f;

        Random rnd = new Random();
        float margin = pieceW * 1.5f;
        targetX = margin + rnd.nextFloat() * (w - pieceW - margin * 2f);
        pieceY = (h - pieceH) / 2f;

        // Scale source nếu kích thước khác
        Bitmap full = (source.getWidth() == w && source.getHeight() == h)
                ? source.copy(Bitmap.Config.ARGB_8888, true)
                : Bitmap.createScaledBitmap(source, w, h, true);
        Canvas c = new Canvas(full);

        // Extract the piece
        pieceBitmap = Bitmap.createBitmap(full, (int) targetX, (int) pieceY,
                (int) pieceW, (int) pieceH);

        // Darken the gap area
        c.drawRect(targetX, pieceY, targetX + pieceW, pieceY + pieceH, shadowPaint);
        c.drawRect(targetX, pieceY, targetX + pieceW, pieceY + pieceH, borderPaint);

        backgroundBitmap = full;
        pieceX = 0f;
        generated = true;
        invalidate();
    }

    private static final int[] PALETTE = {
            0xFFE57373, 0xFFEF9A9A, 0xFF80CBC4, 0xFF80DEEA,
            0xFFA5D6A7, 0xFFF48FB1, 0xFFFFCC80, 0xFFCE93D8,
            0xFF90CAF9, 0xFFFFEB3B, 0xFF69F0AE, 0xFFFF8A65
    };

    private void drawColorGrid(Canvas c, int w, int h, Random rnd) {
        int cols = 10;
        int rows = 7;
        float cellW = (float) w / cols;
        float cellH = (float) h / rows;
        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                p.setColor(PALETTE[rnd.nextInt(PALETTE.length)]);
                c.drawRect(col * cellW, row * cellH,
                        (col + 1) * cellW, (row + 1) * cellH, p);
            }
        }
        // Overlay subtle translucent gradient for depth
        Paint overlay = new Paint();
        overlay.setColor(Color.parseColor("#22000000"));
        for (int row = 0; row < rows; row++) {
            overlay.setAlpha(row * 8);
            c.drawRect(0, row * cellH, w, (row + 1) * cellH, overlay);
        }
    }

    /** Move piece to a normalized position 0..1 across the draggable range. */
    public void setProgress(float progress) {
        if (!generated) return;
        float maxX = getWidth() - pieceW;
        pieceX = progress * maxX;
        invalidate();
    }

    /** True if piece is aligned within tolerance of target. */
    public boolean isAligned() {
        return Math.abs(pieceX - targetX) < dpToPx(TOLERANCE_DP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!generated || backgroundBitmap == null) return;
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        if (pieceBitmap != null) {
            canvas.drawBitmap(pieceBitmap, pieceX, pieceY, null);
            // Draw white border around moving piece
            canvas.drawRect(pieceX, pieceY, pieceX + pieceW, pieceY + pieceH, borderPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }
}
