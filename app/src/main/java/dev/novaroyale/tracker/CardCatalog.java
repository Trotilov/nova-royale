package dev.novaroyale.tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CardCatalog {
    public static final class Card {
        public final String name;
        public final int cost;

        public Card(String name, int cost) {
            this.name = name;
            this.cost = cost;
        }
    }

    private static final List<Card> BASE;

    static {
        List<Card> cards = new ArrayList<>();
        add(cards, "Рыцарь", 3);
        add(cards, "Лучницы", 3);
        add(cards, "Мушкетёр", 4);
        add(cards, "Гигант", 5);
        add(cards, "Мини П.Е.К.К.А.", 4);
        add(cards, "Всадник на кабане", 4);
        add(cards, "Шар", 5);
        add(cards, "Принц", 5);
        add(cards, "Тёмный принц", 4);
        add(cards, "Мегарыцарь", 7);
        add(cards, "П.Е.К.К.А.", 7);
        add(cards, "Электрогигант", 7);
        add(cards, "Ведьма", 5);
        add(cards, "Ночная ведьма", 4);
        add(cards, "Палач", 5);
        add(cards, "Ледяной маг", 3);
        add(cards, "Электромаг", 4);
        add(cards, "Маг", 5);
        add(cards, "Гоблины", 2);
        add(cards, "Гоблины-копейщики", 2);
        add(cards, "Банда гоблинов", 3);
        add(cards, "Гоблинская бочка", 3);
        add(cards, "Скелеты", 1);
        add(cards, "Армия скелетов", 3);
        add(cards, "Стражи", 3);
        add(cards, "Летучие мыши", 2);
        add(cards, "Миньоны", 3);
        add(cards, "Орда миньонов", 5);
        add(cards, "Ледяной дух", 1);
        add(cards, "Электродух", 1);
        add(cards, "Огненный дух", 1);
        add(cards, "Лечебный дух", 1);
        add(cards, "Разряд", 2);
        add(cards, "Бревно", 2);
        add(cards, "Стрелы", 3);
        add(cards, "Огненный шар", 4);
        add(cards, "Яд", 4);
        add(cards, "Ракета", 6);
        add(cards, "Молния", 6);
        add(cards, "Заморозка", 4);
        add(cards, "Торнадо", 3);
        add(cards, "Землетрясение", 3);
        add(cards, "Гигантский скелет", 6);
        add(cards, "Королевский гигант", 6);
        add(cards, "Королевские рекруты", 7);
        add(cards, "Королевские кабаны", 5);
        add(cards, "Охотник", 4);
        add(cards, "Рыбак", 3);
        add(cards, "Феникс", 4);
        add(cards, "Маленький принц", 3);
        add(cards, "Монах", 5);
        add(cards, "Королева лучниц", 5);
        add(cards, "Золотой рыцарь", 4);
        add(cards, "Король скелетов", 4);
        add(cards, "Могучий шахтёр", 4);
        add(cards, "Шахтёр", 3);
        add(cards, "Стенобои", 2);
        add(cards, "Дровосек", 4);
        add(cards, "Бочка с варваром", 2);
        add(cards, "Варвары", 5);
        add(cards, "Элитные варвары", 6);
        add(cards, "Бомбёр", 2);
        add(cards, "Пушка", 3);
        add(cards, "Тесла", 4);
        add(cards, "Адская башня", 5);
        add(cards, "Хижина гоблинов", 5);
        add(cards, "Надгробие", 3);
        add(cards, "Хижина варваров", 6);
        add(cards, "Печь", 4);
        add(cards, "Клетка с гоблином", 4);
        add(cards, "Валькирия", 4);
        add(cards, "Голем", 8);
        add(cards, "Голем из эликсира", 3);
        add(cards, "Ледяной голем", 2);
        add(cards, "Королевский призрак", 3);
        add(cards, "Гоблинский бур", 4);
        add(cards, "Кладбище", 5);
        add(cards, "Гигантский снежок", 2);
        add(cards, "Клон", 3);
        Collections.sort(cards, (a, b) -> a.name.compareToIgnoreCase(b.name));
        BASE = Collections.unmodifiableList(cards);
    }

    private CardCatalog() {}

    private static void add(List<Card> target, String name, int cost) {
        target.add(new Card(name, cost));
    }

    public static List<Card> baseCards() {
        return BASE;
    }

    public static Card find(String name, List<Card> custom) {
        for (Card card : BASE) if (card.name.equals(name)) return card;
        for (Card card : custom) if (card.name.equals(name)) return card;
        return null;
    }
}
