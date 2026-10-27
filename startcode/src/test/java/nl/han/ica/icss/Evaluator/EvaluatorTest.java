package nl.han.ica.icss.Evaluator;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.transforms.Evaluator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvaluatorTest {

    @Test
    void addsPixelValues() {
        // Maak een AST met een stylerule en een declaratie voor width.
        AST ast = new AST();
        Stylerule rule = new Stylerule();
        Declaration declaration = new Declaration();
        AddOperation addition = new AddOperation();

        // Stel een optelling samen van twee pixelwaarden: 10px + 20px.
        addition.addChild(new PixelLiteral(10));
        addition.addChild(new PixelLiteral(20));

        // Koppel de optelling aan de width-property.
        declaration.addChild(new PropertyName("width"));
        declaration.addChild(addition);
        rule.addChild(declaration);
        ast.root.addChild(rule);

        // Voer de Evaluator uit om de optelling te berekenen.
        new Evaluator().apply(ast);

        // Controleer of de expressie is vervangen door een pixelwaarde.
        assertTrue(declaration.expression instanceof PixelLiteral);

        // Controleer of de berekende uitkomst 30px is.
        assertEquals(30, ((PixelLiteral) declaration.expression).value);
    }

    @Test
    void multipliesPixelValueByScalar() {
        // Maak een AST met een stylerule en een declaratie voor width.
        AST ast = new AST();
        Stylerule rule = new Stylerule();
        Declaration declaration = new Declaration();
        MultiplyOperation multiplication = new MultiplyOperation();

        // Stel een vermenigvuldiging samen van 10px met het getal 3.
        multiplication.addChild(new PixelLiteral(10));
        multiplication.addChild(new ScalarLiteral(3));

        // Koppel de vermenigvuldiging aan de width-property.
        declaration.addChild(new PropertyName("width"));
        declaration.addChild(multiplication);
        rule.addChild(declaration);
        ast.root.addChild(rule);

        // Voer de Evaluator uit om de vermenigvuldiging te berekenen.
        new Evaluator().apply(ast);

        // Controleer of de uitkomst een pixelwaarde is gebleven.
        assertTrue(declaration.expression instanceof PixelLiteral);

        // Controleer of de berekende uitkomst 30px is.
        assertEquals(30, ((PixelLiteral) declaration.expression).value);
    }
}