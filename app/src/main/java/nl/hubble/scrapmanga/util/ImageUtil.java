package nl.hubble.scrapmanga.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;

import nl.hubble.scrapmanga.R;

public class ImageUtil {
    public static void saveImageFromView(Context context, final ImageView image) {
        final String imageTitle = "image_" + image.getId();

        Drawable drawable = image.getDrawable();
        if (!(drawable instanceof BitmapDrawable)) return;
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        String imagePath = MediaStore.Images.Media.insertImage(
                context.getContentResolver(),
                bitmap,
                imageTitle,
                imageTitle
        );
        Uri.parse(imagePath);
        Toast.makeText(context, "Image Saved", Toast.LENGTH_SHORT).show();
    }

    private static boolean noReferer(String url) {
        String[] noReferer = new String[]{
                "isekaiscan.com",
                "1stkissmanga.com",
                "zeroscans.com",
                "the-nonames.com",
                "manhuaus.com",
                "manhwatop.com",
                "mangahz.com",
                "mangarockteam.com",
                "mangafunny.com",
                "mangatx.com"
        };
        for (String host : noReferer) {
            if (url.contains(host)) {
                return true;
            }
        }
        return false;
    }

    public static void loadImage(ImageView image, @NonNull String urlString, @Nullable ErrorListener errorListener, String referer, boolean local) {
        if (urlString.isEmpty()) return;
        GlideUrl url = null;

        RequestBuilder<Drawable> rb;
        if (local) {
            rb = Glide.with(image).load(new File(urlString));
        } else {
            if (noReferer(urlString)) {
                url = new GlideUrl(urlString);
            } else {
                url = new GlideUrl(urlString, new LazyHeaders.Builder()
                        .addHeader("referer", referer)
                        .build());
            }
            rb = Glide.with(image).load(url);
        }

        if (errorListener != null) {
            rb = rb.listener(createRequestListener(errorListener, local ? urlString : url.toStringUrl(), image));
        }

        boolean caching = isCachingOn(image.getContext());
        rb
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.error)
                .dontTransform()
                .encodeQuality(100)
                .skipMemoryCache(!caching)
                .diskCacheStrategy(caching ? DiskCacheStrategy.AUTOMATIC : DiskCacheStrategy.NONE)
                .into(image);
    }

    private static boolean isCachingOn(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean(context.getString(R.string.image_caching), false);
    }

    private static RequestListener<Drawable> createRequestListener(ErrorListener errorListener, String url, ImageView image) {
        return new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                errorListener.error(e, url, image);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                return false;
            }
        };
    }

    public interface ErrorListener {
        void error(GlideException e, String url, ImageView imageView);
    }
}