package dev.novaroyale.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TrackerState {
    private final SharedPreferences prefs;
    private final LinkedHashSet<String> known = new LinkedHashSet<>();
    private final ArrayList<String> recent = new ArrayList<>();
    private final ArrayList<CardCatalog.Card> custom = new ArrayList<>();

    private double elixir;
    private int multiplier;
    private long updatedAt;

    public TrackerState(Context context) {
        prefs = context.getSharedPreferences("tracker-v1", Context.MODE_PRIVATE);
        elixir = clamp(prefs.getFloat("elixir", 5f));
        multiplier = Math.max(1, Math.min(3, prefs.getInt("multiplier", 1)));
        updatedAt = prefs.getLong("updatedAt", SystemClock.elapsedRealtime());
        readStrings(prefs.getString("known", "[]"), known);
        readStrings(prefs.getString("recent", "[]"), recent);
        try {
            JSONArray saved = new JSONArray(prefs.getString("custom", "[]"));
            for (int i = 0; i < saved.length(); i++) {
                JSONObject obj = saved.optJSONObject(i);
                if (obj == null) continue;
                String name = obj.optString("name", "").trim();
                int cost = obj.optInt("cost", 0);
                if (!name.isEmpty() && cost >= 1 && cost <= 10) {
                    custom.add(new CardCatalog.Card(name, cost));
                }
            }
        } catch (Exception ignored) {}
        tick();
    }

    private static void readStrings(String encoded, java.util.Collection<String> result) {
        try {
            JSONArray array = new JSONArray(encoded);
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "").trim();
                if (!value.isEmpty()) result.add(value);
            }
        } catch (Exception ignored) {}
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(10.0, value));
    }

    public void tick() {
        long now = SystemClock.elapsedRealtime();
        if (now < updatedAt) updatedAt = now;
        long delta = Math.max(0L, now - updatedAt);
        // Approximate regeneration: 1 elixir per 2.8s in single elixir.
        elixir = clamp(elixir + delta / (2800.0 / multiplier));
        updatedAt = now;
    }

    public double elixir() {
        tick();
        return elixir;
    }

    public int multiplier() {
        return multiplier;
    }

    public List<String> known() {
        return new ArrayList<>(known);
    }

    public List<String> recent() {
        return new ArrayList<>(recent);
    }

    public List<CardCatalog.Card> customCards() {
        return new ArrayList<>(custom);
    }

    public void changeElixir(double amount) {
        tick();
        elixir = clamp(elixir + amount);
        save();
    }

    public void changeMultiplier() {
        tick();
        multiplier = multiplier == 3 ? 1 : multiplier + 1;
        save();
    }

    public boolean record(CardCatalog.Card card) {
        if (!known.contains(card.name) && known.size() >= 8) return false;
        tick();
        known.add(card.name);
        elixir = clamp(elixir - card.cost);
        recent.add(0, card.name);
        while (recent.size() > 4) recent.remove(recent.size() - 1);
        save();
        return true;
    }

    public void removeKnown(String name) {
        known.remove(name);
        recent.removeIf(name::equals);
        save();
    }

    public boolean addCustom(String name, int cost) {
        String cleaned = name.trim();
        if (cleaned.isEmpty() || cleaned.length() > 32 || cost < 1 || cost > 10 ||
                CardCatalog.find(cleaned, custom) != null) return false;
        custom.add(new CardCatalog.Card(cleaned, cost));
        save();
        return true;
    }

    public void reset() {
        known.clear();
        recent.clear();
        elixir = 5.0;
        multiplier = 1;
        updatedAt = SystemClock.elapsedRealtime();
        save();
    }

    private void save() {
        JSONArray customArray = new JSONArray();
        for (CardCatalog.Card card : custom) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", card.name);
                obj.put("cost", card.cost);
                customArray.put(obj);
            } catch (Exception ignored) {}
        }
        prefs.edit()
                .putFloat("elixir", (float) elixir)
                .putInt("multiplier", multiplier)
                .putLong("updatedAt", updatedAt)
                .putString("known", new JSONArray(known).toString())
                .putString("recent", new JSONArray(recent).toString())
                .putString("custom", customArray.toString())
                .apply();
    }
}
