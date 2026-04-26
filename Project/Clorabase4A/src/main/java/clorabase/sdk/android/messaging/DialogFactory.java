package clorabase.sdk.android.messaging;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Utility class to generate custom programmatically-built dialogs.
 */
public class DialogFactory {

    /**
     * Style 1: Coupon/Success Dialog
     */
    public static Dialog createCouponDialog(Context context, DialogConfig config) {
        Dialog dialog = createBaseDialog(context, config);

        LinearLayout root = createVerticalLayout(context, 24);
        root.setBackground(createRoundedBackground(Color.WHITE, 16));

        // Top Banner Image
        if (config.imageBitmap != null) {
            ImageView imageView = new ImageView(context);
            imageView.setImageBitmap(config.imageBitmap);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 120));
            imgParams.setMargins(0, 0, 0, dpToPx(context, 16));
            root.addView(imageView, imgParams);
        }

        // Title
        TextView titleView = createTextView(context, config.title, 20, Color.BLACK, true);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView);

        // Description
        if (!TextUtils.isEmpty(config.message)) {
            TextView messageView = createTextView(context, config.message, 14, Color.DKGRAY, false);
            messageView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            msgParams.setMargins(0, dpToPx(context, 8), 0, dpToPx(context, 24));
            root.addView(messageView, msgParams);
        }

        // Primary Button
        Button primaryBtn = createButton(context, config.primaryButtonText, Color.parseColor("#4CAF50"), Color.WHITE);
        primaryBtn.setOnClickListener(v -> handlePrimaryAction(dialog, config));
        root.addView(primaryBtn);

        // Secondary Button
        if (!TextUtils.isEmpty(config.secondaryButtonText)) {
            Button secondaryBtn = createButton(context, config.secondaryButtonText, Color.TRANSPARENT, Color.GRAY);
            secondaryBtn.setElevation(0);
            secondaryBtn.setOnClickListener(v -> handleSecondaryAction(dialog, config));
            root.addView(secondaryBtn);
        }

        dialog.setContentView(root);
        finalizeDialogBounds(context, dialog, root);
        return dialog;
    }

    /**
     * Style 2: Simple Center Dialog
     */
    public static Dialog createSimpleDialog(Context context, DialogConfig config) {
        Dialog dialog = createBaseDialog(context, config);

        LinearLayout root = createVerticalLayout(context, 24);
        root.setBackground(createRoundedBackground(Color.WHITE, 24));
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        // Top Illustration/Icon
        if (config.imageBitmap != null) {
            ImageView iconView = new ImageView(context);
            iconView.setImageBitmap(config.imageBitmap);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iconView.setAdjustViewBounds(true);

            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(context, 80));
            iconParams.gravity = Gravity.CENTER_HORIZONTAL;
            iconParams.setMargins(0, 0, 0, dpToPx(context, 16));
            root.addView(iconView, iconParams);
        }

        // Title
        TextView titleView = createTextView(context, config.title, 18, Color.BLACK, true);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView);

        // Subtitle/Message
        if (!TextUtils.isEmpty(config.message)) {
            TextView messageView = createTextView(context, config.message, 14, Color.GRAY, false);
            messageView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            msgParams.setMargins(0, dpToPx(context, 8), 0, dpToPx(context, 24));
            root.addView(messageView, msgParams);
        }

        // Single Action Button
        Button primaryBtn = createButton(context, config.primaryButtonText, Color.parseColor("#2196F3"), Color.WHITE);
        primaryBtn.setOnClickListener(v -> handlePrimaryAction(dialog, config));
        root.addView(primaryBtn);

        dialog.setContentView(root);
        finalizeDialogBounds(context, dialog, root);
        return dialog;
    }

    /**
     * Style 3: Promotional A/B Testing Dialog
     */
    public static Dialog createPromoDialog(Context context, DialogConfig config) {
        Dialog dialog = createBaseDialog(context, config);

        FrameLayout root = new FrameLayout(context);
        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootParams.setMargins(dpToPx(context, 16), 0, dpToPx(context, 16), 0);
        root.setLayoutParams(rootParams);

        // Inner Card Container
        LinearLayout cardLayout = createVerticalLayout(context, 0);
        cardLayout.setBackground(createRoundedBackground(Color.WHITE, 12));
        cardLayout.setClipToOutline(true);

        // Colored Header
        LinearLayout header = createVerticalLayout(context, 16);
        header.setBackgroundColor(Color.parseColor("#FF5722"));
        header.setGravity(Gravity.CENTER);

        TextView titleView = createTextView(context, config.title, 22, Color.WHITE, true);
        titleView.setGravity(Gravity.CENTER);
        header.addView(titleView);

        if (!TextUtils.isEmpty(config.message)) {
            TextView subTitleView = createTextView(context, config.message, 14, Color.WHITE, false);
            subTitleView.setGravity(Gravity.CENTER);
            subTitleView.setAlpha(0.9f);
            header.addView(subTitleView);
        }
        cardLayout.addView(header);

        // Product Image
        if (config.imageBitmap != null) {
            ImageView productImg = new ImageView(context);
            productImg.setImageBitmap(config.imageBitmap);
            productImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 200));
            imgParams.setMargins(0, dpToPx(context, 16), 0, dpToPx(context, 16));
            cardLayout.addView(productImg, imgParams);
        }

        // Bottom Container for CTA
        LinearLayout bottomLayout = createVerticalLayout(context, 16);
        Button ctaBtn = createButton(context, config.primaryButtonText, Color.parseColor("#FF5722"), Color.WHITE);
        ctaBtn.setOnClickListener(v -> handlePrimaryAction(dialog, config));
        bottomLayout.addView(ctaBtn);
        cardLayout.addView(bottomLayout);

        root.addView(cardLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Floating Close (X) Button
        TextView closeBtn = new TextView(context);
        closeBtn.setText("✕");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setBackground(createRoundedBackground(Color.parseColor("#80000000"), 20));

        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                dpToPx(context, 32), dpToPx(context, 32));
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(0, dpToPx(context, 8), dpToPx(context, 8), 0);
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        root.addView(closeBtn);

        dialog.setContentView(root);
        finalizeDialogBounds(context, dialog, root);
        return dialog;
    }

    // --- Action Handlers ---

    private static void handlePrimaryAction(Dialog dialog, DialogConfig config) {
        if (config.listener != null) {
            config.listener.onPrimaryClick(dialog, config.deepLink);
        }
        if (config.autoDismiss) {
            dialog.dismiss();
        }
    }

    private static void handleSecondaryAction(Dialog dialog, DialogConfig config) {
        if (config.listener != null) {
            config.listener.onSecondaryClick(dialog);
        }
        if (config.autoDismiss) {
            dialog.dismiss();
        }
    }

    // --- UI Helper Methods ---

    private static Dialog createBaseDialog(Context context, DialogConfig config) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(config.isCancelable);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    /**
     * Enforces dialog width by setting root minimum width and applying Window layout
     * MUST be called AFTER dialog.setContentView()
     */
    private static void finalizeDialogBounds(Context context, Dialog dialog, View root) {
        if (dialog.getWindow() != null) {
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = width;
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
        }
    }

    private static LinearLayout createVerticalLayout(Context context, int paddingDp) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(context, paddingDp);
        layout.setPadding(padding, padding, padding, padding);
        layout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return layout;
    }

    private static TextView createTextView(Context context, String text, float sizeSp, int color, boolean bold) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        textView.setTextColor(color);
        if (bold) {
            textView.setTypeface(null, Typeface.BOLD);
        }
        return textView;
    }

    private static Button createButton(Context context, String text, int bgColor, int textColor) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setBackground(createRoundedBackground(bgColor, 8));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 48));
        button.setLayoutParams(params);
        return button;
    }

    private static GradientDrawable createRoundedBackground(int color, float cornerRadiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(cornerRadiusDp * 3);
        return drawable;
    }

    private static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    // --- Configuration Classes & Interfaces ---

    public interface DialogActionListener {
        void onPrimaryClick(Dialog dialog, String deepLink);
        void onSecondaryClick(Dialog dialog);
    }

    public static class DialogConfig {
        public String title;
        public String message;
        public Bitmap imageBitmap;
        public String primaryButtonText;
        public String secondaryButtonText;
        public String deepLink;
        public boolean isCancelable;
        public boolean autoDismiss;
        public DialogActionListener listener;

        private DialogConfig(Builder builder) {
            this.title = builder.title;
            this.message = builder.message;
            this.imageBitmap = builder.imageBitmap;
            this.primaryButtonText = builder.primaryButtonText;
            this.secondaryButtonText = builder.secondaryButtonText;
            this.deepLink = builder.deepLink;
            this.isCancelable = builder.isCancelable;
            this.autoDismiss = builder.autoDismiss;
            this.listener = builder.listener;
        }

        public static class Builder {
            private String title;
            private String message;
            private Bitmap imageBitmap = null;
            private String primaryButtonText = "OK";
            private String secondaryButtonText;
            private String deepLink;
            private boolean isCancelable = true;
            private boolean autoDismiss = true;
            private DialogActionListener listener;

            public Builder setTitle(String title) {
                this.title = title;
                return this;
            }

            public Builder setMessage(String message) {
                this.message = message;
                return this;
            }

            public Builder setImageBitmap(Bitmap imageBitmap) {
                this.imageBitmap = imageBitmap;
                return this;
            }

            public Builder setPrimaryButtonText(String text) {
                this.primaryButtonText = text;
                return this;
            }

            public Builder setSecondaryButtonText(String text) {
                this.secondaryButtonText = text;
                return this;
            }

            public Builder setDeepLink(String deepLink) {
                this.deepLink = deepLink;
                return this;
            }

            public Builder setCancelable(boolean cancelable) {
                this.isCancelable = cancelable;
                return this;
            }

            public Builder setAutoDismiss(boolean autoDismiss) {
                this.autoDismiss = autoDismiss;
                return this;
            }

            public Builder setListener(DialogActionListener listener) {
                this.listener = listener;
                return this;
            }

            public DialogConfig build() {
                return new DialogConfig(this);
            }
        }
    }
}