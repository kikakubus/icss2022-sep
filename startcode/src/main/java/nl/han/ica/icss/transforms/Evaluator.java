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

    // Bewaart de waarden van variabelen per scope.
    // Iedere scope heeft een eigen HashMap met variabelen en hun waarden.
    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
        variableValues = new HANLinkedList<>();
    }

    @Override
    public void apply(AST ast) {
        // Begin iedere transform met een lege administratie van variabelen.
        variableValues = new HANLinkedList<>();

        // Maak een globale scope voor variabelen die buiten een stylerule staan.
        enterScope();

        // Verwerk alle onderdelen van de stylesheet.
        transformNodes(ast.root.body);

        // Verwijder de globale scope nadat de transform klaar is.
        exitScope();
    }

    private void enterScope() {
        // Voeg een nieuwe scope vooraan toe.
        // Hierdoor kunnen variabelen lokaal worden opgeslagen.
        variableValues.addFirst(new HashMap<>());
    }

    private void exitScope() {
        // Verwijder de meest recente scope wanneer deze niet meer nodig is.
        variableValues.removeFirst();
    }

    private void transformNodes(ArrayList<ASTNode> nodes) {
        // Loop door de AST-nodes. De lijst kan tijdens deze loop veranderen.
        for (int i = 0; i < nodes.size(); i++) {
            ASTNode node = nodes.get(i);

            if (node instanceof IfClause) {
                // Bepaal welke tak van de if-structuur gekozen moet worden.
                IfClause ifClause = (IfClause) node;
                ArrayList<ASTNode> selectedNodes = transformIfClause(ifClause);

                // Verwijder de if-structuur en plaats de gekozen inhoud op dezelfde plek.
                // Zo blijft alleen de geselecteerde tak in de AST staan.
                nodes.remove(i);
                nodes.addAll(i, selectedNodes);

                // Sla de zojuist ingevoegde nodes over, omdat deze al zijn verwerkt.
                i += selectedNodes.size() - 1;
            } else {
                // Verwerk andere soorten nodes op basis van hun type.
                transformNode(node);
            }
        }
    }

    private void transformNode(ASTNode node) {
        if (node instanceof VariableAssignment) {
            // Bereken de waarde van een variabele en sla deze op.
            evaluateVariableAssignment((VariableAssignment) node);

        } else if (node instanceof Stylerule) {
            // Een stylerule krijgt een eigen scope voor lokale variabelen.
            enterScope();

            // Verwerk de declaraties en eventuele if-structuren binnen de stylerule.
            transformNodes(((Stylerule) node).body);

            // Lokale variabelen zijn na de stylerule niet meer beschikbaar.
            exitScope();

        } else if (node instanceof IfClause) {
            // Verwerk een if-structuur.
            transformIfClause((IfClause) node);

        } else if (node instanceof Declaration) {
            // Vervang de oorspronkelijke expressie door de berekende literal.
            Declaration declaration = (Declaration) node;
            declaration.expression = evaluateExpression(declaration.expression);
        }
    }

    private void evaluateVariableAssignment(VariableAssignment assignment) {
        // Bereken de waarde aan de rechterkant van de variabeletoekenning.
        Literal value = evaluateExpression(assignment.expression);

        if (value != null) {
            // Vervang de expressie door de berekende waarde in de AST.
            assignment.expression = value;

            // Sla de waarde op in de huidige scope onder de variabelenaam.
            variableValues.getFirst().put(assignment.name.name, value);
        }
    }

    private ArrayList<ASTNode> transformIfClause(IfClause ifClause) {
        // Deze lijst bevat de nodes die de if-structuur gaan vervangen.
        ArrayList<ASTNode> selectedNodes = new ArrayList<>();

        // Bereken de voorwaarde van de if-structuur.
        Literal condition = evaluateExpression(ifClause.conditionalExpression);

        // Alleen een boolean kan bepalen welke tak uitgevoerd moet worden.
        if (!(condition instanceof BoolLiteral)) {
            return selectedNodes;
        }

        // Lees de waarde van de boolean.
        boolean isTrue = ((BoolLiteral) condition).value;

        if (isTrue) {
            // Bij TRUE wordt de inhoud van het if-blok gekozen.
            selectedNodes.addAll(ifClause.body);
        } else if (ifClause.elseClause != null) {
            // Bij FALSE wordt de inhoud van het else-blok gekozen,
            // als er een else-blok aanwezig is.
            selectedNodes.addAll(ifClause.elseClause.body);
        }

        // Verwerk ook de inhoud van de gekozen tak.
        // Hierdoor worden bijvoorbeeld geneste if-structuren afgehandeld.
        enterScope();
        transformNodes(selectedNodes);
        exitScope();

        return selectedNodes;
    }

    private Literal evaluateExpression(Expression expression) {
        // Er is niets te evalueren als de expressie ontbreekt.
        if (expression == null) {
            return null;
        }

        // Een literal, zoals 10px of een kleur, heeft al een concrete waarde.
        if (expression instanceof Literal) {
            return (Literal) expression;
        }

        if (expression instanceof VariableReference) {
            // Vervang een variabelereferentie door de opgeslagen waarde.
            return getVariableValue(((VariableReference) expression).name);
        }

        if (expression instanceof AddOperation
                || expression instanceof SubtractOperation
                || expression instanceof MultiplyOperation) {

            // Een rekenkundige operatie heeft een linker- en rechterexpressie.
            Operation operation = (Operation) expression;

            // Bereken beide kanten eerst, zodat ook geneste berekeningen werken.
            Literal left = evaluateExpression(operation.lhs);
            Literal right = evaluateExpression(operation.rhs);

            // De berekening kan niet worden uitgevoerd als een waarde ontbreekt.
            if (left == null || right == null) {
                return null;
            }

            // Voer de juiste berekening uit op basis van de operatortype en waarden.
            return evaluateOperation(expression, left, right);
        }

        // Niet-ondersteunde expressies kunnen hier niet worden geëvalueerd.
        return null;
    }

    private Literal evaluateOperation(
            Expression expression,
            Literal left,
            Literal right) {

        int leftValue;
        int rightValue;
        int result;

        // Optellen en aftrekken van twee pixelwaarden.
        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            leftValue = ((PixelLiteral) left).value;
            rightValue = ((PixelLiteral) right).value;

            if (expression instanceof AddOperation) {
                result = leftValue + rightValue;
            } else if (expression instanceof SubtractOperation) {
                result = leftValue - rightValue;
            } else {
                // Twee pixelwaarden met elkaar vermenigvuldigen is niet ondersteund.
                return null;
            }

            return new PixelLiteral(result);
        }

        // Optellen en aftrekken van twee percentagewaarden.
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

        // Optellen, aftrekken en vermenigvuldigen van twee scalarwaarden.
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

        // Bij vermenigvuldigen mag een pixel- of percentagewaarde
        // met een scalar worden vermenigvuldigd.
        if (expression instanceof MultiplyOperation) {

            // Pixelwaarde × scalar.
            if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
                return new PixelLiteral(
                        ((PixelLiteral) left).value
                                * ((ScalarLiteral) right).value);
            }

            // Scalar × pixelwaarde.
            if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
                return new PixelLiteral(
                        ((ScalarLiteral) left).value
                                * ((PixelLiteral) right).value);
            }

            // Percentage × scalar.
            if (left instanceof PercentageLiteral
                    && right instanceof ScalarLiteral) {
                return new PercentageLiteral(
                        ((PercentageLiteral) left).value
                                * ((ScalarLiteral) right).value);
            }

            // Scalar × percentage.
            if (left instanceof ScalarLiteral
                    && right instanceof PercentageLiteral) {
                return new PercentageLiteral(
                        ((ScalarLiteral) left).value
                                * ((PercentageLiteral) right).value);
            }
        }

        // De combinatie van waarden en operatie wordt niet ondersteund.
        return null;
    }

    private Literal getVariableValue(String name) {
        // Zoek vanaf de meest recente scope naar oudere scopes.
        // Daardoor worden lokale variabelen vóór globale variabelen gevonden.
        for (int i = 0; i < variableValues.getSize(); i++) {
            HashMap<String, Literal> scope = variableValues.get(i);

            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }

        // De variabele is niet gevonden in een van de scopes.
        return null;
    }
}