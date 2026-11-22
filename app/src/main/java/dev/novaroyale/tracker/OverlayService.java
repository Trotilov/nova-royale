package dev.novaroyale.tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class OverlayService extends Service {
    public static final String ACTION_STOP = "dev.novaroyale.tracker.STOP";
    private static final String CHANNEL = "nova_royale_overlay";
    private static final int NOTIFICATION_ID = 21;
    private static final int BG = 0xFF151528;
    private static final int PURPLE = 0xFF9764F3;
    private static final int SOFT = 0xFF28273E;
    private static final int WHITE = 0xFFF4EFFF;
    private static final int MUTED = 0xFFB4A9D0;

    private WindowManager windowManager;
    private TrackerState state;
    private View bubble;
    private ScrollView panel;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;
    private LinearLayout panelContent;
    private boolean showingCatalog;
    private TextView elixirLabel;
    private ProgressBar elixirBar;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTask = new Runnable() {
        @Override public void run() {
            if (state != null) {
                state.tick();
                if (!showingCatalog) refreshElixir();
            }
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        state = new TrackerState(this);
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL, "NOVA ROYALE tracker", NotificationManager.IMPORTANCE_LOW));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Разреши показ поверх других окон", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (bubble != null || panel != null) return START_STICKY;
        try {
            startInForeground();
            showBubble();
            handler.post(refreshTask);
        } catch (Exception e) {
            Toast.makeText(this, "Не получилось показать панель: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void startInForeground() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent launch = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, OverlayService.class).setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 1, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("NOVA ROYALE")
                .setContentText("Трекер работает поверх игры")
                .setContentIntent(launch)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Остановить", stopAction).build())
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable bg(int fill, int stroke, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private TextView text(String value, int size, int color) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setGravity(Gravity.CENTER_VERTICAL);
        return label;
    }

    private Button button(String title, int fill) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(WHITE);
        b.setBackground(bg(fill, 0xFF5F477D, 10));
        return b;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void space(LinearLayout parent, int px) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(px)));
    }

    private WindowManager.LayoutParams params(int width, int height, int x, int y) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.RIGHT;
        p.x = x;
        p.y = y;
        return p;
    }

    private void showBubble() {
        if (bubble != null) return;
        TextView icon = text("NR", 17, WHITE);
        icon.setGravity(Gravity.CENTER);
        icon.setTypeface(null, android.graphics.Typeface.BOLD);
        icon.setBackground(bg(0xFF6631BD, PURPLE, 19));
        icon.setElevation(dp(8));
        icon.setContentDescription("Открыть трекер NOVA ROYALE");
        bubble = icon;
        bubbleParams = params(dp(58), dp(58), dp(18), dp(150));
        installDrag(icon, bubbleParams, true);
        windowManager.addView(bubble, bubbleParams);
    }

    private void installDrag(View handle, WindowManager.LayoutParams lp, boolean isBubble) {
        handle.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private int initialX, initialY;
            private boolean moved;

            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        initialX = lp.x;
                        initialY = lp.y;
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = Math.round(event.getRawX() - downX);
                        int dy = Math.round(event.getRawY() - downY);
                        if (Math.abs(dx) + Math.abs(dy) > dp(9)) moved = true;
                        if (moved) {
                            lp.x = Math.max(0, initialX - dx);
                            lp.y = Math.max(0, initialY + dy);
                            try {
                                windowManager.updateViewLayout(
                                        isBubble ? bubble : panel, lp);
                            } catch (Exception ignored) {}
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved && isBubble) showPanel();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void removeBubble() {
        if (bubble == null) return;
        try { windowManager.removeView(bubble); } catch (Exception ignored) {}
        bubble = null;
    }

    private void removePanel() {
        if (panel == null) return;
        try { windowManager.removeView(panel); } catch (Exception ignored) {}
        panel = null;
        panelContent = null;
        elixirLabel = null;
        elixirBar = null;
    }

    private void showPanel() {
        removeBubble();
        showingCatalog = false;
        panel = new ScrollView(this);
        panel.setFillViewport(false);
        panel.setClipToOutline(true);
        panel.setBackground(bg(BG, PURPLE, 18));
        panelContent = new LinearLayout(this);
        panelContent.setOrientation(LinearLayout.VERTICAL);
        panelContent.setPadding(dp(15), dp(12), dp(15), dp(16));
        panel.addView(panelContent);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        panelParams = params(dp(394), Math.min(dp(530),
                Math.max(dp(310), screenHeight - dp(24))), dp(12), dp(12));
        windowManager.addView(panel, panelParams);
        showPanelContent();
    }

    private void hidePanel() {
        hideKeyboard();
        removePanel();
        showingCatalog = false;
        showBubble();
    }

    private void hideKeyboard() {
        if (panel == null) return;
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(panel.getWindowToken(), 0);
        panel.clearFocus();
    }

    private void showPanelContent() {
        if (panelContent == null) return;
        panelContent.removeAllViews();
        elixirLabel = null;
        elixirBar = null;

        LinearLayout header = row();
        TextView title = text(showingCatalog ? "NOVA / КАРТЫ" : "NOVA ROYALE", 19, WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1));
        installDrag(title, panelParams, false);
        Button minimize = button("—", SOFT);
        header.addView(minimize, new LinearLayout.LayoutParams(dp(47), dp(36)));
        minimize.setOnClickListener(v -> hidePanel());
        panelContent.addView(header);
        space(panelContent, 4);

        if (showingCatalog) buildCatalog();
        else buildTracker();
    }

    private void buildTracker() {
        List<String> known = state.known();
        panelContent.addView(text(
                "КОЛОДА СОПЕРНИКА  •  " + known.size() + "/8", 12, MUTED));
        space(panelContent, 8);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        for (int i = 0; i < 8; i++) {
            final String name = i < known.size() ? known.get(i) : null;
            String label = name == null ? "?" : name;
            TextView slot = text(label, name == null ? 21 : 10,
                    name == null ? 0xFF7D719C : WHITE);
            slot.setGravity(Gravity.CENTER);
            slot.setPadding(dp(4), dp(2), dp(4), dp(2));
            slot.setMaxLines(3);
            slot.setBackground(bg(name == null ? 0xFF252339 : 0xFF413066,
                    0xFF55476D, 9));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(81);
            lp.height = dp(58);
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            grid.addView(slot, lp);
            if (name != null) {
                slot.setOnLongClickListener(v -> {
                    state.removeKnown(name);
                    Toast.makeText(this, "Карта удалена: " + name, Toast.LENGTH_SHORT).show();
                    showPanelContent();
                    return true;
                });
                slot.setContentDescription("Удалить " + name + " долгим нажатием");
            }
        }
        panelContent.addView(grid);
        space(panelContent, 12);

        elixirLabel = text("", 15, WHITE);
        elixirLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        panelContent.addView(elixirLabel);
        space(panelContent, 5);
        elixirBar = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        elixirBar.setMax(100);
        elixirBar.setProgressTintList(ColorStateList.valueOf(PURPLE));
        elixirBar.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF36334C));
        panelContent.addView(elixirBar, new LinearLayout.LayoutParams(-1, dp(12)));
        refreshElixir();
        space(panelContent, 7);

        LinearLayout controls = row();
        Button minus = button("−1", SOFT);
        controls.addView(minus, new LinearLayout.LayoutParams(0, dp(39), 1));
        minus.setOnClickListener(v -> { state.changeElixir(-1); refreshElixir(); });
        Button phase = button("×" + state.multiplier() + " СКОРОСТЬ", 0xFF49316A);
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0, dp(39), 2);
        middle.setMargins(dp(5), 0, dp(5), 0);
        controls.addView(phase, middle);
        phase.setOnClickListener(v -> { state.changeMultiplier(); showPanelContent(); });
        Button plus = button("+1", SOFT);
        controls.addView(plus, new LinearLayout.LayoutParams(0, dp(39), 1));
        plus.setOnClickListener(v -> { state.changeElixir(1); refreshElixir(); });
        panelContent.addView(controls);
        space(panelContent, 10);

        List<String> recent = state.recent();
        panelContent.addView(text("ПОСЛЕДНИЕ 4: " +
                (recent.isEmpty() ? "пока неизвестны" : String.join("  ·  ", recent)),
                11, MUTED));
        space(panelContent, 5);
        panelContent.addView(text(
                "Эликсир приблизительный. Долгое нажатие на карту удаляет её из колоды.",
                11, 0xFF9693B2));
        space(panelContent, 15);

        Button record = button("＋  ЗАПИСАТЬ СЫГРАННУЮ КАРТУ", 0xFF6335B5);
        panelContent.addView(record, new LinearLayout.LayoutParams(-1, dp(53)));
        record.setOnClickListener(v -> {
            showingCatalog = true;
            showPanelContent();
        });
        space(panelContent, 9);

        Button reset = button("НОВЫЙ БОЙ  /  СБРОСИТЬ", SOFT);
        panelContent.addView(reset, new LinearLayout.LayoutParams(-1, dp(43)));
        reset.setOnClickListener(v -> {
            state.reset();
            showPanelContent();
            Toast.makeText(this, "Новый бой: колода сброшена", Toast.LENGTH_SHORT).show();
        });
    }

    private void refreshElixir() {
        if (elixirLabel == null || elixirBar == null || state == null) return;
        double current = state.elixir();
        elixirLabel.setText("ЭЛИКСИР ~ " +
                String.format(Locale.US, "%.1f", current) + " / 10");
        elixirBar.setProgress((int) Math.round(current * 10));
    }

    private void buildCatalog() {
        Button back = button("← НАЗАД К ТРЕКЕРУ", SOFT);
        panelContent.addView(back, new LinearLayout.LayoutParams(-1, dp(39)));
        back.setOnClickListener(v -> {
            hideKeyboard();
            showingCatalog = false;
            showPanelContent();
        });
        space(panelContent, 8);

        TextView explain = text("Нажми на карту ПОСЛЕ того, как соперник её сыграл.", 12, MUTED);
        panelContent.addView(explain);
        space(panelContent, 9);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Поиск карты…");
        search.setTextColor(WHITE);
        search.setHintTextColor(0xFF9590AC);
        search.setTextSize(14);
        search.setPadding(dp(11), dp(5), dp(11), dp(5));
        search.setBackground(bg(0xFF24243C, 0xFF655283, 9));
        panelContent.addView(search, new LinearLayout.LayoutParams(-1, dp(44)));
        space(panelContent, 10);

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        panelContent.addView(choices);
        renderCatalog(choices, "");
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderCatalog(choices, s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        space(panelContent, 14);
        panelContent.addView(text("НЕТ КАРТЫ? ДОБАВЬ СВОЮ", 12, MUTED));
        space(panelContent, 7);

        LinearLayout customRow = row();
        EditText customName = new EditText(this);
        customName.setSingleLine(true);
        customName.setTextSize(13);
        customName.setTextColor(WHITE);
        customName.setHintTextColor(0xFF9590AC);
        customName.setHint("Название");
        customName.setPadding(dp(8), 0, dp(8), 0);
        customName.setBackground(bg(0xFF24243C, 0xFF655283, 9));
        customRow.addView(customName, new LinearLayout.LayoutParams(0, dp(43), 3));

        EditText cost = new EditText(this);
        cost.setSingleLine(true);
        cost.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        cost.setText("3");
        cost.setTextSize(14);
        cost.setTextColor(WHITE);
        cost.setGravity(Gravity.CENTER);
        cost.setBackground(bg(0xFF24243C, 0xFF655283, 9));
        LinearLayout.LayoutParams costLp = new LinearLayout.LayoutParams(0, dp(43), 1);
        costLp.setMargins(dp(8), 0, 0, 0);
        customRow.addView(cost, costLp);
        panelContent.addView(customRow);
        space(panelContent, 7);

        Button add = button("СОХРАНИТЬ СВОЮ КАРТУ", SOFT);
        panelContent.addView(add, new LinearLayout.LayoutParams(-1, dp(42)));
        add.setOnClickListener(v -> {
            int amount;
            try { amount = Integer.parseInt(cost.getText().toString()); }
            catch (Exception e) { amount = 0; }
            String name = customName.getText().toString();
            if (!state.addCustom(name, amount)) {
                Toast.makeText(this, "Название должно быть новым, цена — от 1 до 10",
                        Toast.LENGTH_LONG).show();
                return;
            }
            hideKeyboard();
            showingCatalog = true;
            showPanelContent();
            Toast.makeText(this, "Карта добавлена. Теперь выбери её в каталоге.",
                    Toast.LENGTH_LONG).show();
        });
    }

    private void renderCatalog(LinearLayout choices, String query) {
        choices.removeAllViews();
        List<CardCatalog.Card> cards = new ArrayList<>(CardCatalog.baseCards());
        cards.addAll(state.customCards());
        Collections.sort(cards, (a, b) -> a.name.compareToIgnoreCase(b.name));
        String filter = query.toLowerCase(Locale.ROOT).trim();
        ArrayList<CardCatalog.Card> selected = new ArrayList<>();
        for (CardCatalog.Card c : cards) {
            if (c.name.toLowerCase(Locale.ROOT).contains(filter)) selected.add(c);
        }
        if (selected.isEmpty()) {
            choices.addView(text("Не нашлось. Можно создать свою карту ниже.", 12, MUTED));
            return;
        }
        for (int i = 0; i < selected.size(); i += 2) {
            LinearLayout line = row();
            for (int j = i; j < Math.min(i + 2, selected.size()); j++) {
                CardCatalog.Card card = selected.get(j);
                Button choice = button(card.name + "  [" + card.cost + "]", SOFT);
                choice.setTextSize(11);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1);
                lp.setMargins(dp(2), dp(2), dp(2), dp(2));
                line.addView(choice, lp);
                choice.setOnClickListener(v -> {
                    if (!state.record(card)) {
                        Toast.makeText(this,
                                "Уже найдено 8 разных карт. Удали ошибочную долгим нажатием.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    hideKeyboard();
                    showingCatalog = false;
                    showPanelContent();
                });
            }
            if (i + 1 >= selected.size()) {
                View filler = new View(this);
                line.addView(filler, new LinearLayout.LayoutParams(0, dp(46), 1));
            }
            choices.addView(line);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(refreshTask);
        removePanel();
        removeBubble();
        super.onDestroy();
    }
}
