package nl.han.ica.datastructures;

import java.util.LinkedList;

public class HANLinkedList<T> implements IHANLinkedList<T> {

    private final LinkedList<T> list = new LinkedList<>();

    @Override
    public void addFirst(T value) {
        list.addFirst(value);
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public void insert(int index, T value) {
        list.add(index, value);
    }

    @Override
    public void delete(int pos) {
        list.remove(pos);
    }

    @Override
    public T get(int pos) {
        return list.get(pos);
    }

    @Override
    public void removeFirst() {
        list.removeFirst();
    }

    @Override
    public T getFirst() {
        return list.getFirst();
    }

    @Override
    public int getSize() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
