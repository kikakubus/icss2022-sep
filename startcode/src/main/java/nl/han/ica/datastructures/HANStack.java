package nl.han.ica.datastructures;

import java.util.ArrayList;

public class HANStack<T> implements IHANStack<T> {

    private ArrayList<T> items = new ArrayList<>();

    @Override
    public void push(T value) {
        items.add(value);
    }

    @Override
    public T pop() {
        return items.remove(items.size() - 1);
    }

    @Override
    public T peek() {
        return items.get(items.size() - 1);
    }

}