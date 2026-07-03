package com.example.vibetix.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Utility for image compression (→ Base64 for Firestore) and loading.
 * Handles both HTTP URLs and Base64-encoded strings transparently.
 */
public class ImageUtils {

    /**
     * Load a circle-cropped image into an ImageView from a Fragment.
     * Accepts HTTP URLs or Base64 strings.
     */
    public static void loadCircle(@NonNull Fragment fragment, @Nullable String value,
                                  @NonNull ImageView target, @DrawableRes int placeholder) {
        if (!fragment.isAdded()) return;
        applyLoad(Glide.with(fragment), value, placeholder, target);
    }

    /**
     * Load a circle-cropped image into an ImageView from a Context (Activity).
     * Accepts HTTP URLs or Base64 strings.
     */
    public static void loadCircle(@NonNull Context context, @Nullable String value,
                                  @NonNull ImageView target, @DrawableRes int placeholder) {
        applyLoad(Glide.with(context), value, placeholder, target);
    }

    private static void applyLoad(com.bumptech.glide.RequestManager glide,
                                  @Nullable String value,
                                  @DrawableRes int placeholder,
                                  @NonNull ImageView target) {
        RequestOptions opts = new RequestOptions()
                .circleCrop()
                .placeholder(placeholder)
                .error(placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        if (value == null || value.isEmpty()) {
            glide.load(placeholder).apply(opts).into(target);
        } else if (value.startsWith("http")) {
            glide.load(value).apply(opts).into(target);
        } else {
            try {
                byte[] bytes = Base64.decode(value, Base64.DEFAULT);
                glide.load(bytes).apply(opts).into(target);
            } catch (Exception e) {
                glide.load(placeholder).apply(opts).into(target);
            }
        }
    }

    /**
     * Read image from URI, resize to maxDim × maxDim, compress JPEG to under maxKb.
     * Returns null if the image cannot be read/decoded.
     */
    @Nullable
    public static byte[] compressToJpeg(@NonNull Context context, @NonNull Uri uri,
                                        int maxDim, int maxKb) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            if (bitmap == null) return null;

            int w = bitmap.getWidth(), h = bitmap.getHeight();
            if (w > maxDim || h > maxDim) {
                float scale = Math.min((float) maxDim / w, (float) maxDim / h);
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                        (int) (w * scale), (int) (h * scale), true);
                bitmap.recycle();
                bitmap = scaled;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int quality = 85;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            while (baos.toByteArray().length > maxKb * 1024L && quality > 30) {
                baos.reset();
                quality -= 10;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            }
            bitmap.recycle();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /** Encode bytes to Base64 string suitable for Firestore storage (no line wraps). */
    @NonNull
    public static String toBase64(@NonNull byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}
