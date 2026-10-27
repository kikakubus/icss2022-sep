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

    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        variableTypes = new HANLinkedList<>();

        // De stylesheet krijgt de globale scope.
        enterScope();

        // Controleer alle statements in de stylesheet.
        checkNodes(ast.root.getChildren());

        // Verlaat de globale scope.
        exitScope();
    }

    private void enterScope() {
        // De nieuwe scope komt vooraan te staan.
        variableTypes.addFirst(new HashMap<>());
    }

    private void exitScope() {
        // Verwijder de huidige scope.
        variableTypes.removeFirst();
    }

    private void checkNodes(Iterable<ASTNode> nodes) {
        for (ASTNode node : nodes) {
            checkNode(node);
        }
    }

    private void checkNode(ASTNode node) {

        if (node instanceof VariableAssignment) {
            checkVariableAssignment((VariableAssignment) node);
        }

        else if (node instanceof Stylerule) {
            checkStylerule((Stylerule) node);
        }

        else if (node instanceof IfClause) {
            checkIfClause((IfClause) node);
        }

        else if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;

            checkExpression(declaration.expression);
            checkDeclaration(declaration);
        }
    }

    private void checkVariableAssignment(VariableAssignment assignment) {

        // Controleer eerst de expressie aan de rechterkant.
        checkExpression(assignment.expression);

        // Registreer daarna de variabele in de huidige scope.
        // Het type wordt later door CH02 en andere checks bepaald.
        ExpressionType type = getExpressionType(assignment.expression);

        variableTypes.getFirst().put(
                assignment.name.name,
                type
        );
    }

    private void checkStylerule(Stylerule stylerule) {

        // Een stylerule krijgt een eigen scope.
        enterScope();

        // Controleer de inhoud van de stylerule.
        checkNodes(stylerule.body);

        // Variabelen uit deze stylerule zijn hierna niet meer beschikbaar.
        exitScope();
    }

    private void checkIfClause(IfClause ifClause) {

        // Controleer de conditie.
        checkExpression(ifClause.conditionalExpression);

        // CH05: de conditie moet een booleaanse waarde hebben.
        ExpressionType conditionType =
                getExpressionType(ifClause.conditionalExpression);

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
        if (ifClause.elseClause != null) {
            enterScope();
            checkNodes(ifClause.elseClause.body);
            exitScope();
        }
    }

    private void checkExpression(Expression expression) {

        if (expression == null) {
            return;
        }

        // CH01: controleer of een gebruikte variabele bestaat.
        if (expression instanceof VariableReference) {
            VariableReference variable = (VariableReference) expression;

            if (!isVariableDefined(variable.name)) {
                variable.setError(
                        "Variabele '" + variable.name
                                + "' is niet gedefinieerd in deze scope."
                );
            }

            return;
        }

        // CH02 en CH03: controleer de typen van berekeningen.
        if (expression instanceof Operation) {
            Operation operation = (Operation) expression;

            checkExpression(operation.lhs);
            checkExpression(operation.rhs);

            ExpressionType leftType = getExpressionType(operation.lhs);
            ExpressionType rightType = getExpressionType(operation.rhs);

            if (operation instanceof AddOperation
                    || operation instanceof SubtractOperation) {

                if (leftType != ExpressionType.UNDEFINED
                        && rightType != ExpressionType.UNDEFINED
                        && leftType != rightType) {
                    operation.setError(
                            "Optellen en aftrekken vereisen operanden van hetzelfde type."
                    );
                }

                if (leftType == ExpressionType.COLOR
                        || rightType == ExpressionType.COLOR) {
                    operation.setError(
                            "Kleuren mogen niet worden gebruikt in berekeningen."
                    );
                }
            }

            else if (operation instanceof MultiplyOperation) {

                if (leftType == ExpressionType.COLOR
                        || rightType == ExpressionType.COLOR) {
                    operation.setError(
                            "Kleuren mogen niet worden gebruikt in berekeningen."
                    );
                }

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
        for (int i = 0; i < variableTypes.getSize(); i++) {
            HashMap<String, ExpressionType> scope = variableTypes.get(i);

            if (scope.containsKey(name)) {
                return true;
            }
        }

        return false;
    }

    private ExpressionType getExpressionType(Expression expression) {

        if (expression == null) {
            return ExpressionType.UNDEFINED;
        }

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

            for (int i = 0; i < variableTypes.getSize(); i++) {
                HashMap<String, ExpressionType> scope = variableTypes.get(i);

                if (scope.containsKey(variable.name)) {
                    return scope.get(variable.name);
                }
            }

            return ExpressionType.UNDEFINED;
        }

        if (expression instanceof AddOperation
                || expression instanceof SubtractOperation) {

            Operation operation = (Operation) expression;
            ExpressionType leftType = getExpressionType(operation.lhs);
            ExpressionType rightType = getExpressionType(operation.rhs);

            if (leftType == rightType) {
                return leftType;
            }

            return ExpressionType.UNDEFINED;
        }

        if (expression instanceof MultiplyOperation) {
            Operation operation = (Operation) expression;
            ExpressionType leftType = getExpressionType(operation.lhs);
            ExpressionType rightType = getExpressionType(operation.rhs);

            if (leftType == ExpressionType.SCALAR) {
                return rightType;
            }

            if (rightType == ExpressionType.SCALAR) {
                return leftType;
            }

            return ExpressionType.UNDEFINED;
        }

        return ExpressionType.UNDEFINED;
    }

    private void checkDeclaration(Declaration declaration) {

        ExpressionType type = getExpressionType(declaration.expression);
        String property = declaration.property.name;

        if (property.equals("width") || property.equals("height")) {

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

            if (type != ExpressionType.COLOR
                    && type != ExpressionType.UNDEFINED) {

                declaration.setError(
                        "De property 'color' vereist een kleurwaarde."
                );
            }
        }
    }
    
}
