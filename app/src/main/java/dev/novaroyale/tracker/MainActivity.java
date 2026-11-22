package dev.novaroyale.tracker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int NOTIFICATIONS_REQUEST = 12;
    private TextView permissionState;
    private Button startButton;

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int stroke, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) bg.setStroke(dp(1), stroke);
        return bg;
    }

    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private Button button(String text, int fill) {
        Button view = new Button(this);
        view.setText(text);
        view.setAllCaps(false);
        view.setTextColor(Color.WHITE);
        view.setTextSize(15);
        view.setBackground(rounded(fill, 0xFF7854B8, 14));
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF090B15);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(32), dp(25), dp(32), dp(20));
        scroll.addView(root);

        TextView title = label("NOVA  ROYALE", 34, 0xFFEDE6FF);
        title.setLetterSpacing(0.07f);
        root.addView(title);

        TextView subtitle = label("BATTLE TRACKER  /  v0.1  /  ANDROID", 12, 0xFFB69AEF);
        root.addView(subtitle);
        addSpace(root, 18);

        permissionState = label("", 16, Color.WHITE);
        permissionState.setPadding(dp(16), dp(14), dp(16), dp(14));
        permissionState.setBackground(rounded(0xFF1A1930, 0xFF574475, 12));
        root.addView(permissionState);
        addSpace(root, 15);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button grant = button("1. Разрешить поверх окон", 0xFF353052);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(56), 1);
        left.setMargins(0, 0, dp(10), 0);
        buttons.addView(grant, left);
        grant.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Разреши показ поверх окон в настройках", Toast.LENGTH_LONG).show();
            }
        });

        startButton = button("2. Запустить панель", 0xFF6D3CBE);
        buttons.addView(startButton, new LinearLayout.LayoutParams(0, dp(56), 1));
        startButton.setOnClickListener(v -> requestAndStart());
        root.addView(buttons);
        addSpace(root, 10);

        Button stop = button("Остановить панель", 0xFF242739);
        root.addView(stop, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "Панель отключена", Toast.LENGTH_SHORT).show();
        });

        addSpace(root, 22);
        root.addView(label("КАК ПОЛЬЗОВАТЬСЯ", 15, 0xFFBE9AF7));
        addSpace(root, 6);
        root.addView(label(
                "Открой Null's Royale. Нажми круглую кнопку NR у края экрана. " +
                "Когда противник сыграет карту, нажми «Записать карту» и выбери её. " +
                "Трекер покажет найденные карты и примерный эликсир. " +
                "Начинается новый бой — нажми «Новый бой».",
                15, 0xFFD1D0E0));
        addSpace(root, 14);
        root.addView(label(
                "Ничего не внедряется в игру: нет чтения памяти, автокликов и доступа к сети. " +
                "Расчёт эликсира приблизительный. Правила сервера всё равно нужно учитывать.",
                12, 0xFF9393AC));

        setContentView(scroll);
        updateStatus();
    }

    private void addSpace(LinearLayout root, int heightDp) {
        View space = new View(this);
        root.addView(space, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private void updateStatus() {
        if (permissionState == null) return;
        boolean allowed = Settings.canDrawOverlays(this);
        permissionState.setText(allowed
                ? "● Разрешение получено. Панель готова к запуску."
                : "○ Нужно разрешение «Показывать поверх других приложений».");
        permissionState.setTextColor(allowed ? 0xFFBAF4D3 : 0xFFFFC8C8);
        startButton.setEnabled(allowed);
        startButton.setAlpha(allowed ? 1f : 0.45f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void requestAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Сначала выдай разрешение на окна", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATIONS_REQUEST);
            return;
        }
        startPanel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATIONS_REQUEST) startPanel();
    }

    private void startPanel() {
        try {
            Intent intent = new Intent(this, OverlayService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
            else startService(intent);
            Toast.makeText(this, "Панель включена! Открывай игру", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось запустить: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
