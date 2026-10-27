package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import java.util.ArrayList;

import java.util.HashMap;

public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
        variableValues = new HANLinkedList<>();
    }

    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<>();
        enterScope();

        transformNodes(ast.root.body);

        exitScope();
    }

    private void enterScope() {
        variableValues.addFirst(new HashMap<>());
    }

    private void exitScope() {
        variableValues.removeFirst();
    }

    private void transformNodes(ArrayList<ASTNode> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            ASTNode node = nodes.get(i);

            if (node instanceof IfClause) {
                IfClause ifClause = (IfClause) node;

                ArrayList<ASTNode> selectedNodes = transformIfClause(ifClause);

                nodes.remove(i);
                nodes.addAll(i, selectedNodes);

                i += selectedNodes.size() - 1;
            } else {
                transformNode(node);
            }
        }
    }

    private void transformNode(ASTNode node) {
        if (node instanceof VariableAssignment) {
            evaluateVariableAssignment((VariableAssignment) node);
        } else if (node instanceof Stylerule) {
            enterScope();
            transformNodes(((Stylerule) node).body);
            exitScope();
        } else if (node instanceof IfClause) {
            transformIfClause((IfClause) node);
        } else if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;
            declaration.expression = evaluateExpression(declaration.expression);
        }
    }

    private void evaluateVariableAssignment(VariableAssignment assignment) {
        Literal value = evaluateExpression(assignment.expression);

        if (value != null) {
            assignment.expression = value;
            variableValues.getFirst().put(assignment.name.name, value);
        }
    }

    private ArrayList<ASTNode> transformIfClause(IfClause ifClause) {
        ArrayList<ASTNode> selectedNodes = new ArrayList<>();

        Literal condition = evaluateExpression(ifClause.conditionalExpression);

        if (!(condition instanceof BoolLiteral)) {
            return selectedNodes;
        }

        boolean isTrue = ((BoolLiteral) condition).value;

        if (isTrue) {
            selectedNodes.addAll(ifClause.body);
        } else if (ifClause.elseClause != null) {
            selectedNodes.addAll(ifClause.elseClause.body);
        }

        enterScope();
        transformNodes(selectedNodes);
        exitScope();

        return selectedNodes;
    }

    private Literal evaluateExpression(Expression expression) {
        if (expression == null) {
            return null;
        }

        if (expression instanceof Literal) {
            return (Literal) expression;
        }

        if (expression instanceof VariableReference) {
            return getVariableValue(((VariableReference) expression).name);
        }

        if (expression instanceof AddOperation
                || expression instanceof SubtractOperation
                || expression instanceof MultiplyOperation) {

            Operation operation = (Operation) expression;

            Literal left = evaluateExpression(operation.lhs);
            Literal right = evaluateExpression(operation.rhs);

            if (left == null || right == null) {
                return null;
            }

            return evaluateOperation(expression, left, right);
        }

        return null;
    }

    private Literal evaluateOperation(
            Expression expression,
            Literal left,
            Literal right) {

        int leftValue;
        int rightValue;
        int result;

        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            leftValue = ((PixelLiteral) left).value;
            rightValue = ((PixelLiteral) right).value;

            if (expression instanceof AddOperation) {
                result = leftValue + rightValue;
            } else if (expression instanceof SubtractOperation) {
                result = leftValue - rightValue;
            } else {
                return null;
            }

            return new PixelLiteral(result);
        }

        if (left instanceof PercentageLiteral
                && right instanceof PercentageLiteral) {

            leftValue = ((PercentageLiteral) left).value;
            rightValue = ((PercentageLiteral) right).value;

            if (expression instanceof AddOperation) {
                result = leftValue + rightValue;
            } else if (expression instanceof SubtractOperation) {
                result = leftValue - rightValue;
            } else {
                return null;
            }

            return new PercentageLiteral(result);
        }

        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            leftValue = ((ScalarLiteral) left).value;
            rightValue = ((ScalarLiteral) right).value;

            if (expression instanceof AddOperation) {
                result = leftValue + rightValue;
            } else if (expression instanceof SubtractOperation) {
                result = leftValue - rightValue;
            } else if (expression instanceof MultiplyOperation) {
                result = leftValue * rightValue;
            } else {
                return null;
            }

            return new ScalarLiteral(result);
        }

        if (expression instanceof MultiplyOperation) {
            if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
                return new PixelLiteral(
                        ((PixelLiteral) left).value
                                * ((ScalarLiteral) right).value);
            }

            if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
                return new PixelLiteral(
                        ((ScalarLiteral) left).value
                                * ((PixelLiteral) right).value);
            }

            if (left instanceof PercentageLiteral
                    && right instanceof ScalarLiteral) {
                return new PercentageLiteral(
                        ((PercentageLiteral) left).value
                                * ((ScalarLiteral) right).value);
            }

            if (left instanceof ScalarLiteral
                    && right instanceof PercentageLiteral) {
                return new PercentageLiteral(
                        ((ScalarLiteral) left).value
                                * ((PercentageLiteral) right).value);
            }
        }

        return null;
    }

    private Literal getVariableValue(String name) {
        for (int i = 0; i < variableValues.getSize(); i++) {
            HashMap<String, Literal> scope = variableValues.get(i);

            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }

        return null;
    }
}
