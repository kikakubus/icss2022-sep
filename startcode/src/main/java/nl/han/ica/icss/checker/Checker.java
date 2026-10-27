package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.types.ExpressionType;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;

import java.util.HashMap;


public class Checker {

    // Bewaart per scope welke variabelen bestaan en welk type ze hebben.
    // Bijvoorbeeld: BREEDTE -> PIXEL.
    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        // Begin met een lege lijst van scopes wanneer de checker wordt uitgevoerd.
        variableTypes = new HANLinkedList<>();

        // De stylesheet krijgt de globale scope.
        // Variabelen die hier worden gedefinieerd, kunnen ook in stylerules worden gebruikt.
        enterScope();

        // Controleer alle statements in de stylesheet.
        checkNodes(ast.root.getChildren());

        // Verlaat de globale scope nadat alle statements zijn gecontroleerd.
        exitScope();
    }

    private void enterScope() {
        // Voeg een nieuwe scope vooraan toe.
        // De eerste scope in de lijst is altijd de huidige scope.
        variableTypes.addFirst(new HashMap<>());
    }

    private void exitScope() {
        // Verwijder de huidige scope wanneer deze niet meer nodig is.
        variableTypes.removeFirst();
    }

    private void checkNodes(Iterable<ASTNode> nodes) {
        // Controleer iedere AST-node afzonderlijk.
        for (ASTNode node : nodes) {
            checkNode(node);
        }
    }

    private void checkNode(ASTNode node) {

        if (node instanceof VariableAssignment) {
            // Controleer de waarde en registreer het type van de variabele.
            checkVariableAssignment((VariableAssignment) node);
        }

        else if (node instanceof Stylerule) {
            // Controleer een CSS-regel binnen een eigen scope.
            checkStylerule((Stylerule) node);
        }

        else if (node instanceof IfClause) {
            // Controleer de voorwaarde en de inhoud van een if-statement.
            checkIfClause((IfClause) node);
        }

        else if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;

            // Controleer eerst of de expressie geldig is.
            checkExpression(declaration.expression);

            // Controleer daarna of het type bij de CSS-property past.
            checkDeclaration(declaration);
        }
    }

    private void checkVariableAssignment(VariableAssignment assignment) {

        // Controleer eerst de expressie aan de rechterkant.
        // Zo kunnen fouten in gebruikte variabelen en berekeningen worden gevonden.
        checkExpression(assignment.expression);

        // Bepaal het type van de toegewezen waarde.
        ExpressionType type = getExpressionType(assignment.expression);

        // Sla de variabele en het type op in de huidige scope.
        // Latere expressies kunnen deze informatie gebruiken.
        variableTypes.getFirst().put(
                assignment.name.name,
                type
        );
    }

    private void checkStylerule(Stylerule stylerule) {

        // Een stylerule krijgt een eigen scope.
        // Hierdoor blijven variabelen die lokaal worden gedefinieerd beperkt tot deze regel.
        enterScope();

        // Controleer de declaraties en eventuele if-statements in de stylerule.
        checkNodes(stylerule.body);

        // Verwijder de lokale scope na afloop van de stylerule.
        exitScope();
    }

    private void checkIfClause(IfClause ifClause) {

        // Controleer eerst de expressie die de if-voorwaarde bepaalt.
        checkExpression(ifClause.conditionalExpression);

        // CH05: een if-voorwaarde moet een boolean zijn.
        ExpressionType conditionType =
                getExpressionType(ifClause.conditionalExpression);

        // UNDEFINED wordt hier toegelaten om geen extra typefout te geven
        // wanneer het type nog niet bepaald kan worden.
        if (conditionType != ExpressionType.BOOL
                && conditionType != ExpressionType.UNDEFINED) {

            ifClause.setError(
                    "De voorwaarde van een if-statement moet een booleaanse waarde zijn."
            );
        }

        // De if-body krijgt een eigen scope.
        enterScope();
        checkNodes(ifClause.body);
        exitScope();

        // De else-body krijgt een afzonderlijke scope.
        // Variabelen uit de if-body worden daardoor niet automatisch gedeeld.
        if (ifClause.elseClause != null) {
            enterScope();
            checkNodes(ifClause.elseClause.body);
            exitScope();
        }
    }

    private void checkExpression(Expression expression) {

        // Er valt niets te controleren als de expressie ontbreekt.
        if (expression == null) {
            return;
        }

        // CH01: controleer of een gebruikte variabele is gedefinieerd.
        if (expression instanceof VariableReference) {
            VariableReference variable = (VariableReference) expression;

            // Een variabele moet beschikbaar zijn in de huidige of een bovenliggende scope.
            if (!isVariableDefined(variable.name)) {
                variable.setError(
                        "Variabele '" + variable.name
                                + "' is niet gedefinieerd in deze scope."
                );
            }

            return;
        }

        // CH02 en CH03: controleer de typen van rekenkundige operaties.
        if (expression instanceof Operation) {
            Operation operation = (Operation) expression;

            // Controleer eerst beide operand-expressies.
            // Hierdoor worden ook fouten in geneste berekeningen gevonden.
            checkExpression(operation.lhs);
            checkExpression(operation.rhs);

            // Bepaal de typen van de linker- en rechteroperand.
            ExpressionType leftType = getExpressionType(operation.lhs);
            ExpressionType rightType = getExpressionType(operation.rhs);

            if (operation instanceof AddOperation
                    || operation instanceof SubtractOperation) {

                // CH02: optellen en aftrekken vereisen hetzelfde type.
                if (leftType != ExpressionType.UNDEFINED
                        && rightType != ExpressionType.UNDEFINED
                        && leftType != rightType) {
                    operation.setError(
                            "Optellen en aftrekken vereisen operanden van hetzelfde type."
                    );
                }

                // CH03: kleuren mogen niet worden opgeteld of afgetrokken.
                if (leftType == ExpressionType.COLOR
                        || rightType == ExpressionType.COLOR) {
                    operation.setError(
                            "Kleuren mogen niet worden gebruikt in berekeningen."
                    );
                }
            }

            else if (operation instanceof MultiplyOperation) {

                // CH03: kleuren mogen ook niet worden vermenigvuldigd.
                if (leftType == ExpressionType.COLOR
                        || rightType == ExpressionType.COLOR) {
                    operation.setError(
                            "Kleuren mogen niet worden gebruikt in berekeningen."
                    );
                }

                // CH02: bij vermenigvuldigen moet minstens één operand een scalar zijn.
                // Een scalar is een getal zonder pixel- of percentage-eenheid.
                if (leftType != ExpressionType.UNDEFINED
                        && rightType != ExpressionType.UNDEFINED
                        && leftType != ExpressionType.SCALAR
                        && rightType != ExpressionType.SCALAR) {
                    operation.setError(
                            "Bij vermenigvuldigen moet minstens één operand een scalar zijn."
                    );
                }
            }

            return;
        }

        // Controleer ook eventuele andere expressies met onderliggende expressies.
        for (ASTNode child : expression.getChildren()) {
            if (child instanceof Expression) {
                checkExpression((Expression) child);
            }
        }
    }

    private boolean isVariableDefined(String name) {

        // Zoek vanaf de huidige scope naar de bovenliggende scopes.
        // Zo blijven globale variabelen beschikbaar binnen lokale scopes.
        for (int i = 0; i < variableTypes.getSize(); i++) {
            HashMap<String, ExpressionType> scope = variableTypes.get(i);

            if (scope.containsKey(name)) {
                return true;
            }
        }

        // De variabele is in geen enkele beschikbare scope gevonden.
        return false;
    }

    private ExpressionType getExpressionType(Expression expression) {

        // Zonder expressie kan het type niet worden bepaald.
        if (expression == null) {
            return ExpressionType.UNDEFINED;
        }

        // Bepaal het type aan de hand van het soort literal.
        if (expression instanceof PixelLiteral) {
            return ExpressionType.PIXEL;
        }

        if (expression instanceof PercentageLiteral) {
            return ExpressionType.PERCENTAGE;
        }

        if (expression instanceof ColorLiteral) {
            return ExpressionType.COLOR;
        }

        if (expression instanceof ScalarLiteral) {
            return ExpressionType.SCALAR;
        }

        if (expression instanceof BoolLiteral) {
            return ExpressionType.BOOL;
        }

        if (expression instanceof VariableReference) {
            VariableReference variable = (VariableReference) expression;

            // Zoek het eerder geregistreerde type van de variabele.
            for (int i = 0; i < variableTypes.getSize(); i++) {
                HashMap<String, ExpressionType> scope = variableTypes.get(i);

                if (scope.containsKey(variable.name)) {
                    return scope.get(variable.name);
                }
            }

            // Het type is onbekend als de variabele niet is gevonden.
            return ExpressionType.UNDEFINED;
        }

        if (expression instanceof AddOperation
                || expression instanceof SubtractOperation) {

            // Bij optellen en aftrekken is het resultaat hetzelfde type
            // als de operanden, mits beide typen gelijk zijn.
            Operation operation = (Operation) expression;
            ExpressionType leftType = getExpressionType(operation.lhs);
            ExpressionType rightType = getExpressionType(operation.rhs);

            if (leftType == rightType) {
                return leftType;
            }

            return ExpressionType.UNDEFINED;
        }

        if (expression instanceof MultiplyOperation) {
            // Bij vermenigvuldigen met een scalar behoudt het resultaat
            // het type van de andere operand.
            Operation operation = (Operation) expression;
            ExpressionType leftType = getExpressionType(operation.lhs);
            ExpressionType rightType = getExpressionType(operation.rhs);

            if (leftType == ExpressionType.SCALAR) {
                return rightType;
            }

            if (rightType == ExpressionType.SCALAR) {
                return leftType;
            }

            // Zonder scalar kan het resultaat niet geldig worden bepaald.
            return ExpressionType.UNDEFINED;
        }

        // Het type van een niet-herkende expressie is onbekend.
        return ExpressionType.UNDEFINED;
    }

    private void checkDeclaration(Declaration declaration) {

        // Bepaal het type van de waarde die aan de property wordt toegewezen.
        ExpressionType type = getExpressionType(declaration.expression);

        // Lees de naam van de CSS-property, bijvoorbeeld 'width' of 'color'.
        String property = declaration.property.name;

        if (property.equals("width") || property.equals("height")) {

            // CH04: width en height accepteren pixels of percentages.
            // UNDEFINED wordt voorlopig toegelaten wanneer het type onbekend is.
            if (type != ExpressionType.PIXEL
                    && type != ExpressionType.PERCENTAGE
                    && type != ExpressionType.UNDEFINED) {

                declaration.setError(
                        "De property '" + property
                                + "' vereist een pixel- of percentagewaarde."
                );
            }
        }

        else if (property.equals("color")) {

            // CH04: de color-property moet een kleurwaarde krijgen.
            if (type != ExpressionType.COLOR
                    && type != ExpressionType.UNDEFINED) {

                declaration.setError(
                        "De property 'color' vereist een kleurwaarde."
                );
            }
        }
    }

}