package nl.han.ica.datastructures;

import java.util.ArrayList;

public class HANStack<T> implements IHANStack<T> {

    // Bewaar de elementen in een ArrayList.
    // Met <T> kan de stack verschillende typen gegevens opslaan.
    private ArrayList<T> items = new ArrayList<>();

    @Override
    public void push(T value) {
        // Voeg een element boven op de stack toe.
        items.add(value);
    }

    @Override
    public T pop() {
        // Verwijder het bovenste element en geef de waarde terug.
        // De laatste positie is het bovenste element van de stack.
        return items.remove(items.size() - 1);
    }

    @Override
    public T peek() {
        // Geef het bovenste element terug zonder het te verwijderen.
        return items.get(items.size() - 1);
    }
}