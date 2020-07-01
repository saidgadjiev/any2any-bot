package ru.gadjini.any2any.model.bot.api.object.replykeyboard.buttons;

import java.util.ArrayList;
import java.util.List;

public class KeyboardRow extends ArrayList<KeyboardButton> {
    public boolean add(String text) {
        return super.add(new KeyboardButton(text));
    }

    public void add(int index, String text) {
        super.add(index, new KeyboardButton(text));
    }

    public void addAll(List<String> buttonNames) {
        buttonNames.forEach(name -> super.add(new KeyboardButton(name)));
    }

    public boolean contains(String text) {
        return super.contains(new KeyboardButton(text));
    }

    public int lastIndexOf(String text) {
        return super.lastIndexOf(new KeyboardButton(text));
    }

    public int indexOf(String text) {
        return super.indexOf(new KeyboardButton(text));
    }

    public KeyboardButton set(int index, String text) {
        return super.set(index, new KeyboardButton(text));
    }

    public boolean remove(String text) {
        return super.remove(new KeyboardButton(text));
    }
}
