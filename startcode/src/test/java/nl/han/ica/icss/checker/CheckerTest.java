package nl.han.ica.icss.checker;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CheckerTest {

    @Test
    void checkerCanBeCreated() {
        // Controleer of er een Checker-object aangemaakt kan worden.
        Checker checker = new Checker();

        assertNotNull(checker);
    }

    @Test
    void undefinedVariableProducesError() {
        AST ast = new AST();
        Stylerule rule = new Stylerule();

        // Gebruik een variabele die nergens is gedefinieerd.
        Declaration declaration = new Declaration();
        declaration.addChild(new PropertyName("width"));
        declaration.addChild(new VariableReference("unknown"));

        rule.addChild(declaration);
        ast.root.addChild(rule);

        // De Checker moet herkennen dat de variabele niet bestaat.
        new Checker().check(ast);

        assertTrue(ast.getErrors().size() > 0,
                "Een niet-gedefinieerde variabele moet een fout opleveren.");
    }

    @Test
    void definedVariableProducesNoError() {
        AST ast = new AST();

        // Definieer de variabele size met een pixelwaarde.
        VariableAssignment assignment = new VariableAssignment();
        assignment.addChild(new VariableReference("size"));
        assignment.addChild(new PixelLiteral(20));
        ast.root.addChild(assignment);

        Stylerule rule = new Stylerule();

        // Gebruik de eerder gedefinieerde variabele voor width.
        Declaration declaration = new Declaration();
        declaration.addChild(new PropertyName("width"));
        declaration.addChild(new VariableReference("size"));

        rule.addChild(declaration);
        ast.root.addChild(rule);

        // Een bestaande variabele met een geschikt type mag geen fout geven.
        new Checker().check(ast);

        assertEquals(0, ast.getErrors().size(),
                "Een gedefinieerde variabele moet gebruikt kunnen worden.");
    }

    @Test
    void variableFromInnerScopeIsNotAvailableOutside() {
        AST ast = new AST();

        Stylerule firstRule = new Stylerule();

        // Definieer localSize binnen de eerste stylerule.
        VariableAssignment assignment = new VariableAssignment();
        assignment.addChild(new VariableReference("localSize"));
        assignment.addChild(new PixelLiteral(20));

        firstRule.addChild(assignment);
        ast.root.addChild(firstRule);

        Stylerule secondRule = new Stylerule();

        // Probeer de lokale variabele in een andere stylerule te gebruiken.
        Declaration declaration = new Declaration();
        declaration.addChild(new PropertyName("width"));
        declaration.addChild(new VariableReference("localSize"));

        secondRule.addChild(declaration);
        ast.root.addChild(secondRule);

        // De variabele hoort niet buiten haar eigen scope beschikbaar te zijn.
        new Checker().check(ast);

        assertTrue(ast.getErrors().size() > 0,
                "Een variabele uit een andere stylerule mag niet beschikbaar zijn.");
    }

    @Test
    void addingDifferentTypesProducesError() {
        AST ast = new AST();
        Stylerule rule = new Stylerule();
        Declaration declaration = new Declaration();
        AddOperation operation = new AddOperation();

        // Pixels en percentages hebben verschillende typen en mogen
        // volgens de checkerregels niet bij elkaar worden opgeteld.
        operation.addChild(new PixelLiteral(10));
        operation.addChild(new PercentageLiteral(20));

        declaration.addChild(new PropertyName("width"));
        declaration.addChild(operation);
        rule.addChild(declaration);
        ast.root.addChild(rule);

        new Checker().check(ast);

        assertTrue(ast.getErrors().size() > 0,
                "Optellen van pixels en percentages moet een fout opleveren.");
    }

    @Test
    void addingSameTypesProducesNoError() {
        AST ast = new AST();
        Stylerule rule = new Stylerule();
        Declaration declaration = new Declaration();
        AddOperation operation = new AddOperation();

        // Twee pixelwaarden mogen bij elkaar worden opgeteld.
        operation.addChild(new PixelLiteral(10));
        operation.addChild(new PixelLiteral(20));

        declaration.addChild(new PropertyName("width"));
        declaration.addChild(operation);
        rule.addChild(declaration);
        ast.root.addChild(rule);

        new Checker().check(ast);

        assertEquals(0, ast.getErrors().size(),
                "Optellen van twee pixelwaarden moet geldig zijn.");
    }

    @Test
    void multiplyingTwoPixelValuesProducesError() {
        AST ast = new AST();
        Stylerule rule = new Stylerule();
        Declaration declaration = new Declaration();
        MultiplyOperation operation = new MultiplyOperation();

        // Twee pixelwaarden mogen niet met elkaar worden vermenigvuldigd.
        // Vermenigvuldigen is hier bedoeld voor een waarde met een scalar.
        operation.addChild(new PixelLiteral(10));
        operation.addChild(new PixelLiteral(20));

        declaration.addChild(new PropertyName("width"));
        declaration.addChild(operation);
        rule.addChild(declaration);
        ast.root.addChild(rule);

        new Checker().check(ast);

        assertTrue(ast.getErrors().size() > 0,
                "Vermenigvuldigen van twee pixelwaarden moet een fout opleveren.");
    }

    @Test
    void addingColorProducesError() {
        AST ast = new AST();
        Stylerule rule = new Stylerule();
        Declaration declaration = new Declaration();
        AddOperation operation = new AddOperation();

        // Een kleur kan niet bij een pixelwaarde worden opgeteld.
        operation.addChild(new PixelLiteral(10));
        operation.addChild(new ColorLiteral("#ff0000"));

        declaration.addChild(new PropertyName("width"));
        declaration.addChild(operation);
        rule.addChild(declaration);
        ast.root.addChild(rule);

        new Checker().check(ast);

        assertTrue(ast.getErrors().size() > 0,
                "Een kleur mag niet worden gebruikt bij optellen.");
    }

    @Test
    void multiplyingColorProducesError() {
        AST ast = new AST();
        Stylerule rule = new Stylerule();
        Declaration declaration = new Declaration();
        MultiplyOperation operation = new MultiplyOperation();

        // Een kleur mag niet met een getal worden vermenigvuldigd.
        operation.addChild(new ColorLiteral("#ff0000"));
        operation.addChild(new ScalarLiteral(2));

        declaration.addChild(new PropertyName("width"));
        declaration.addChild(operation);
        rule.addChild(declaration);
        ast.root.addChild(rule);

        new Checker().check(ast);

        assertTrue(ast.getErrors().size() > 0,
                "Een kleur mag niet worden gebruikt bij vermenigvuldigen.");
    }

    @Test
    void widthWithColorProducesError() {
        AST ast = new AST();
        Stylerule rule = new Stylerule();
        Declaration declaration = new Declaration();

        // De width-property verwacht een pixel- of percentagewaarde,
        // geen kleur.
        declaration.addChild(new PropertyName("width"));
        declaration.addChild(new ColorLiteral("#ff0000"));

        rule.addChild(declaration);
        ast.root.addChild(rule);

        new Checker().check(ast);

        assertTrue(ast.getErrors().size() > 0,
                "De width-property mag geen kleurwaarde bevatten.");
    }

    @Test
    void colorWithPixelValueProducesError() {
        AST ast = new AST();
        Stylerule rule = new Stylerule();
        Declaration declaration = new Declaration();

        // De color-property verwacht een kleurwaarde, geen pixels.
        declaration.addChild(new PropertyName("color"));
        declaration.addChild(new PixelLiteral(12));

        rule.addChild(declaration);
        ast.root.addChild(rule);

        new Checker().check(ast);

        assertTrue(ast.getErrors().size() > 0,
                "De color-property mag geen pixelwaarde bevatten.");
    }

    @Test
    void ifWithTrueConditionProducesNoError() {
        AST ast = new AST();
        IfClause ifClause = new IfClause();

        // Een booleaanse waarde is een geldig type voor een if-voorwaarde.
        ifClause.addChild(new BoolLiteral("TRUE"));
        ast.root.addChild(ifClause);

        new Checker().check(ast);

        assertEquals(0, ast.getErrors().size(),
                "Een booleaanse if-voorwaarde moet geldig zijn.");
    }

    @Test
    void ifWithPixelConditionProducesError() {
        AST ast = new AST();
        IfClause ifClause = new IfClause();

        // Een pixelwaarde is geen boolean en mag daarom niet
        // als voorwaarde voor een if-statement worden gebruikt.
        ifClause.addChild(new PixelLiteral(10));
        ast.root.addChild(ifClause);

        new Checker().check(ast);

        assertTrue(ast.getErrors().size() > 0,
                "Een pixelwaarde mag niet als if-voorwaarde worden gebruikt.");
    }

    @Test
    void ifWithBooleanVariableProducesNoError() {
        AST ast = new AST();

        // Definieer een variabele met een booleaanse waarde.
        VariableAssignment assignment = new VariableAssignment();
        assignment.addChild(new VariableReference("enabled"));
        assignment.addChild(new BoolLiteral("TRUE"));
        ast.root.addChild(assignment);

        // Gebruik de variabele als voorwaarde voor een if-statement.
        IfClause ifClause = new IfClause();
        ifClause.addChild(new VariableReference("enabled"));
        ast.root.addChild(ifClause);

        // De Checker moet het type van de variabele herkennen als BOOL.
        new Checker().check(ast);

        assertEquals(0, ast.getErrors().size(),
                "Een variabele met het type BOOL mag als if-voorwaarde worden gebruikt.");
    }
}