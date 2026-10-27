package nl.han.ica.datastructures;

import java.util.LinkedList;

public class HANLinkedList<T> implements IHANLinkedList<T> {

    // Gebruik een standaard LinkedList om de elementen in op te slaan.
    // Met <T> kan de lijst verschillende typen gegevens bevatten.
    private final LinkedList<T> list = new LinkedList<>();

    @Override
    public void addFirst(T value) {
        // Voeg een element vooraan de lijst toe.
        list.addFirst(value);
    }

    @Override
    public void clear() {
        // Verwijder alle elementen uit de lijst.
        list.clear();
    }

    @Override
    public void insert(int index, T value) {
        // Voeg een element toe op de opgegeven positie.
        list.add(index, value);
    }

    @Override
    public void delete(int pos) {
        // Verwijder het element op de opgegeven positie.
        list.remove(pos);
    }

    @Override
    public T get(int pos) {
        // Geef het element op de opgegeven positie terug.
        return list.get(pos);
    }

    @Override
    public void removeFirst() {
        // Verwijder het eerste element van de lijst.
        list.removeFirst();
    }

    @Override
    public T getFirst() {
        // Geef het eerste element terug zonder het te verwijderen.
        return list.getFirst();
    }

    @Override
    public int getSize() {
        // Geef het aantal elementen in de lijst terug.
        return list.size();
    }
}