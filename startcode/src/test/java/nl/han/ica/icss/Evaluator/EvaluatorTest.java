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
        AST ast = new AST();
        Stylerule rule = new Stylerule();
        Declaration declaration = new Declaration();
        AddOperation addition = new AddOperation();

        addition.addChild(new PixelLiteral(10));
        addition.addChild(new PixelLiteral(20));

        declaration.addChild(new PropertyName("width"));
        declaration.addChild(addition);
        rule.addChild(declaration);
        ast.root.addChild(rule);

        new Evaluator().apply(ast);

        assertTrue(declaration.expression instanceof PixelLiteral);
        assertEquals(30, ((PixelLiteral) declaration.expression).value);
    }

    @Test
    void multipliesPixelValueByScalar() {
        AST ast = new AST();
        Stylerule rule = new Stylerule();
        Declaration declaration = new Declaration();
        MultiplyOperation multiplication = new MultiplyOperation();

        multiplication.addChild(new PixelLiteral(10));
        multiplication.addChild(new ScalarLiteral(3));

        declaration.addChild(new PropertyName("width"));
        declaration.addChild(multiplication);
        rule.addChild(declaration);
        ast.root.addChild(rule);

        new Evaluator().apply(ast);

        assertTrue(declaration.expression instanceof PixelLiteral);
        assertEquals(30, ((PixelLiteral) declaration.expression).value);
    }
}
