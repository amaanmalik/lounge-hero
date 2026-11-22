package com.amaanmalik.herolounge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class MainActivity extends Activity {
    private static final String ORIGIN = "https://hero-lounge.invalid/";
    private static final int BACKGROUND = Color.rgb(27, 25, 48);
    private static final int PANEL = Color.rgb(39, 36, 67);
    private static final int PINK = Color.rgb(255, 170, 205);
    private static final int LAVENDER = Color.rgb(236, 226, 255);
    private LinearLayout root;
    private LinearLayout dock;
    private LinearLayout actions;
    private WebView game;
    private Stick stick;
    private Space spacer;
    private Button interact;
    private Button switchHero;
    private boolean playing;
    private boolean chatOpen;
    private boolean movementQueued;
    private float moveX;
    private float moveY;

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                var safe = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            });
        }

        game = new WebView(this);
        game.setBackgroundColor(BACKGROUND);
        game.getSettings().setJavaScriptEnabled(true);
        game.getSettings().setDomStorageEnabled(true);
        game.getSettings().setAllowFileAccess(false);
        game.getSettings().setAllowContentAccess(false);
        game.getSettings().setMediaPlaybackRequiresUserGesture(false);
        game.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }
        });
        game.addJavascriptInterface(new NativeControls(), "HeroLoungeAndroid");
        root.addView(game, new LinearLayout.LayoutParams(-1, 0, 1));

        dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setBackgroundColor(PANEL);
        dock.setVisibility(View.GONE);
        stick = new Stick();
        spacer = new Space(this);
        actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        interact = button(actions, "Grab / Serve", "interact", true);
        LinearLayout secondary = new LinearLayout(this);
        actions.addView(secondary);
        button(secondary, "Chat", "chat", false);
        switchHero = button(secondary, "Switch", "switch", false);
        button(secondary, "Break", "break", false);
        dock.addView(stick);
        dock.addView(spacer);
        dock.addView(actions);
        root.addView(dock);
        adjustControls();
        setContentView(root);
        game.loadDataWithBaseURL(ORIGIN, readGame(), "text/html", "UTF-8", null);
    }


    private Button button(LinearLayout parent, String label, String action, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(primary ? 16 : 13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(BACKGROUND);
        button.setBackgroundTintList(new android.content.res.ColorStateList(
                new int[][] { {-android.R.attr.state_enabled}, {android.R.attr.state_pressed}, {} },
                new int[] { 0xff9e96b2, 0xfff2c3fb, primary ? PINK : LAVENDER }));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.WHITE);
        shape.setCornerRadius(dp(15));
        button.setBackground(shape);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        var layout = new LinearLayout.LayoutParams(primary ? -1 : 0, dp(primary ? 52 : 48), primary ? 0 : 1);
        if (!primary) layout.setMargins(dp(2), dp(4), dp(2), 0);
        parent.addView(button, layout);
        button.setOnClickListener(view -> {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            invoke("action('" + action + "')");
        });
        return button;
    }

    private void adjustControls() {
        Configuration config = getResources().getConfiguration();
        boolean wide = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        dock.setPadding(dp(12), dp(3), dp(12), dp(3));
        if (wide) {
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.removeAllViews();
            dock.removeAllViews();
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.addView(stick, new LinearLayout.LayoutParams(dp(112), dp(120)));
            root.addView(game, new LinearLayout.LayoutParams(0, -1, 1));
            root.addView(actions, new LinearLayout.LayoutParams(dp(165), -2));
        } else {
            root.setOrientation(LinearLayout.VERTICAL);
            root.removeAllViews();
            dock.removeAllViews();
            dock.addView(stick, new LinearLayout.LayoutParams(dp(104), dp(108)));
            dock.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
            dock.addView(actions, new LinearLayout.LayoutParams(dp(Math.min(196, Math.max(144, config.screenWidthDp - 132))), -2));
            root.addView(game, new LinearLayout.LayoutParams(-1, 0, 1));
            root.addView(dock, new LinearLayout.LayoutParams(-1, -2));
        }
        updateVisibility();
    }

    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        stick.release();
        adjustControls();
    }

    private void updateVisibility() {
        int visible = playing && !chatOpen ? View.VISIBLE : View.GONE;
        if (visible == View.GONE) stick.release();
        dock.setVisibility(visible);
        stick.setVisibility(visible);
        actions.setVisibility(visible);
    }

    @Override protected void onPause() {
        stick.release();
        invoke("pause(true)");
        game.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        game.onResume();
        invoke("pause(false)");
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (!focused && stick != null) stick.release();
    }

    @Override public void onBackPressed() {
        if (playing) invoke("back()");
        else super.onBackPressed();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (playing && !chatOpen && event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
            if (event.getAction() == KeyEvent.ACTION_UP) invoke("back()");
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void invoke(String call) {
        if (game != null) game.evaluateJavascript("window.loungeControls && window.loungeControls." + call, null);
    }

    private void move(float x, float y) {
        moveX = x;
        moveY = y;
        if (movementQueued) return;
        movementQueued = true;
        root.postOnAnimation(() -> {
            movementQueued = false;
            invoke("move(" + moveX + "," + moveY + ")");
        });
    }

    private String readGame() {
        try (InputStream source = getAssets().open("index.html");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = source.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString("UTF-8");
        } catch (IOException error) {
            return "<h1>The Hero Lounge</h1><p>The game file could not be opened.</p>";
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class NativeControls {
        @JavascriptInterface public void state(boolean inGame, boolean chatting, boolean canSwitch) {
            runOnUiThread(() -> {
                playing = inGame;
                chatOpen = chatting;
                switchHero.setEnabled(canSwitch);
                updateVisibility();
            });
        }

        @JavascriptInterface public void interaction(String action) {
            runOnUiThread(() -> {
                switch (action) {
                    case "grab": interact.setText("Grab"); break;
                    case "serve": interact.setText("Serve"); break;
                    case "return": interact.setText("Return"); break;
                    default: interact.setText("Grab / Serve");
                }
            });
        }
    }

    private final class Stick extends View {
        private static final float DEAD_ZONE = 0.14f;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int pointer = -1;
        private float knobX;
        private float knobY;

        Stick() {
            super(MainActivity.this);
            setContentDescription("Movement joystick");
            setClickable(true);
        }

        @Override protected void onDraw(Canvas canvas) {
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            paint.setColor(0xff453c64);
            canvas.drawCircle(centerX, centerY, dp(45), paint);
            paint.setColor(0xff6e608c);
            canvas.drawCircle(centerX, centerY, dp(38), paint);
            paint.setColor(pointer == -1 ? LAVENDER : PINK);
            canvas.drawCircle(centerX + knobX, centerY + knobY, dp(18), paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            int index = event.getActionIndex();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    pointer = event.getPointerId(index);
                    getParent().requestDisallowInterceptTouchEvent(true);
                    track(event.getX(index), event.getY(index));
                    return true;
                case MotionEvent.ACTION_MOVE:
                    index = event.findPointerIndex(pointer);
                    if (index >= 0) track(event.getX(index), event.getY(index));
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    if (event.getPointerId(index) == pointer) release();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    release();
                    return true;
                default:
                    return true;
            }
        }

        private void track(float x, float y) {
            float dx = (x - getWidth() / 2f) / dp(32);
            float dy = (y - getHeight() / 2f) / dp(32);
            float distance = (float) Math.hypot(dx, dy);
            float scale = Math.max(1, distance);
            knobX = dx / scale * dp(26);
            knobY = dy / scale * dp(26);
            float speed = Math.max(0, (Math.min(1, distance) - DEAD_ZONE) / (1 - DEAD_ZONE));
            move(distance == 0 ? 0 : dx / distance * speed,
                 distance == 0 ? 0 : dy / distance * speed);
            invalidate();
        }

        void release() {
            if (pointer == -1) return;
            pointer = -1;
            knobX = knobY = 0;
            move(0, 0);
            invalidate();
        }
    }
}
