package nl.han.ica.icss.parser;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;
import nl.han.ica.icss.parser.ICSSParser;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {

	// Basisstructuur voor alle levels
	private AST ast;

	// Houdt bij aan welke AST-node nieuwe onderdelen worden toegevoegd.
	private IHANStack<ASTNode> currentContainer;

	public ASTListener() {
		ast = new AST();
		currentContainer = new HANStack<>();
	}

	public AST getAST() {
		return ast;
	}

	/**
	 * Level 1 en hoger:
	 * Maakt een stylerule aan en voegt deze toe aan de stylesheet.
	 */
	@Override
	public void enterStylerule(ICSSParser.StyleruleContext ctx) {
		Stylerule stylerule = new Stylerule();
		ast.root.addChild(stylerule);
		currentContainer.push(stylerule);
	}

	/**
	 * Level 1 en hoger:
	 * Haalt de stylerule van de stack wanneer deze klaar is.
	 */
	@Override
	public void exitStylerule(ICSSParser.StyleruleContext ctx) {
		currentContainer.pop();
	}

	/**
	 * Level 1 en hoger:
	 * Verwerkt selectors, zoals p, .menu en #menu.
	 */
	@Override
	public void exitSelector(ICSSParser.SelectorContext ctx) {
		Selector selector;

		if (ctx.LOWER_IDENT() != null) {
			selector = new TagSelector(ctx.LOWER_IDENT().getText());
		} else if (ctx.CLASS_IDENT() != null) {
			selector = new ClassSelector(ctx.CLASS_IDENT().getText());
		} else {
			selector = new IdSelector(ctx.ID_IDENT().getText());
		}

		currentContainer.peek().addChild(selector);
	}

	/**
	 * Level 1 en hoger:
	 * Verwerkt declaraties binnen een stylerule.
	 */
	@Override
	public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
		Declaration declaration =
				new Declaration(ctx.LOWER_IDENT().getText());

		declaration.addChild(createExpression(ctx.expression()));

		currentContainer.peek().addChild(declaration);
	}

	/**
	 * Level 2 en hoger:
	 * Verwerkt variabeletoekenningen, zoals ParWidth := 500px;.
	 */
	@Override
	public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		VariableAssignment assignment = new VariableAssignment();

		assignment.addChild(
				new VariableReference(ctx.CAPITAL_IDENT().getText())
		);

		assignment.addChild(createExpression(ctx.expression()));

		ast.root.addChild(assignment);
	}

	/**
	 * Level 2 en hoger:
	 * Verwerkt optellen en aftrekken in expressies.
	 */
	private Expression createExpression(ICSSParser.ExpressionContext ctx) {
		Expression result = createMultiplication(ctx.multiplication(0));

		for (int i = 1; i < ctx.multiplication().size(); i++) {
			Operation operation;

			String operator = ctx.getChild(2 * i - 1).getText();

			if (operator.equals("+")) {
				operation = new AddOperation();
			} else {
				operation = new SubtractOperation();
			}

			operation.addChild(result);
			operation.addChild(createMultiplication(ctx.multiplication(i)));

			result = operation;
		}

		return result;
	}

	/**
	 * Level 2 en hoger:
	 * Verwerkt vermenigvuldiging in expressies.
	 */
	private Expression createMultiplication(ICSSParser.MultiplicationContext ctx) {
		Expression result = createAtom(ctx.atom(0));

		for (int i = 1; i < ctx.atom().size(); i++) {
			MultiplyOperation operation = new MultiplyOperation();

			operation.addChild(result);
			operation.addChild(createAtom(ctx.atom(i)));

			result = operation;
		}

		return result;
	}

	/**
	 * Level 0 en hoger:
	 * Zet losse waarden en variabelen om naar AST-nodes.
	 */
	private Expression createAtom(ICSSParser.AtomContext ctx) {
		String text = ctx.getText();

		if (ctx.COLOR() != null) {
			return new ColorLiteral(text);
		}

		if (ctx.PIXELSIZE() != null) {
			return new PixelLiteral(text);
		}

		if (ctx.PERCENTAGE() != null) {
			return new PercentageLiteral(text);
		}

		if (ctx.SCALAR() != null) {
			return new ScalarLiteral(text);
		}

		if (ctx.CAPITAL_IDENT() != null) {
			return new VariableReference(text);
		}

		return new BoolLiteral(text);
	}

	//level 3
	@Override
	public void enterIfStatement(ICSSParser.IfStatementContext ctx) {
		IfClause ifClause = new IfClause();

		// Voeg de conditie als eerste kind toe.
		ifClause.addChild(createExpression(ctx.expression()));

		// Voeg de IfClause toe aan de huidige container.
		currentContainer.peek().addChild(ifClause);

		// Nieuwe declaraties komen nu binnen de IfClause.
		currentContainer.push(ifClause);
	}

	@Override
	public void exitIfStatement(ICSSParser.IfStatementContext ctx) {
		currentContainer.pop();
	}

	@Override
	public void enterElseBody(ICSSParser.ElseBodyContext ctx) {
		ElseClause elseClause = new ElseClause();

		currentContainer.peek().addChild(elseClause);
		currentContainer.push(elseClause);
	}

	@Override
	public void exitElseBody(ICSSParser.ElseBodyContext ctx) {
		currentContainer.pop();
	}
}
