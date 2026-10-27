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


 // Zet de parse tree van ANTLR om naar een Abstract Syntax Tree (AST).
 // De AST bevat de onderdelen van de ICSS-code in een structuur
 // waarmee bijvoorbeeld de Checker en Evaluator kunnen werken.

public class ASTListener extends ICSSBaseListener {

	// De AST die tijdens het verwerken van de parse tree wordt opgebouwd.
	private AST ast;

	// Houdt bij in welke AST-node nieuwe onderdelen geplaatst moeten worden.
	// De stack maakt het mogelijk om tijdelijk een dieper onderdeel te verwerken.
	private IHANStack<ASTNode> currentContainer;

	public ASTListener() {
		// Maak een lege AST en een lege stack voor de huidige container.
		ast = new AST();
		currentContainer = new HANStack<>();
	}

	public AST getAST() {
		// Geef de opgebouwde AST terug.
		return ast;
	}


	 // Level 1 en hoger:
	 // Maakt een stylerule aan en voegt deze toe aan de stylesheet.

	@Override
	public void enterStylerule(ICSSParser.StyleruleContext ctx) {
		// Maak een nieuwe regel aan, bijvoorbeeld voor de selector 'p'.
		Stylerule stylerule = new Stylerule();

		// Voeg de regel toe aan de hoofdstructuur van de AST.
		ast.root.addChild(stylerule);

		// Maak deze stylerule de huidige container voor selectors en declaraties.
		currentContainer.push(stylerule);
	}


	 // Level 1 en hoger:
	 // Haalt de stylerule van de stack wanneer deze klaar is.

	@Override
	public void exitStylerule(ICSSParser.StyleruleContext ctx) {
		// De stylerule is verwerkt; ga terug naar de vorige container.
		currentContainer.pop();
	}


	 //Level 1 en hoger:
	 //Verwerkt selectors, zoals p, .menu en #menu.

	@Override
	public void exitSelector(ICSSParser.SelectorContext ctx) {
		Selector selector;

		// Een gewone naam, zoals 'p', wordt een TagSelector.
		if (ctx.LOWER_IDENT() != null) {
			selector = new TagSelector(ctx.LOWER_IDENT().getText());

			// Een selector die met een punt begint, wordt een ClassSelector.
		} else if (ctx.CLASS_IDENT() != null) {
			selector = new ClassSelector(ctx.CLASS_IDENT().getText());

			// Een selector die met een hekje begint, wordt een IdSelector.
		} else {
			selector = new IdSelector(ctx.ID_IDENT().getText());
		}

		// Voeg de selector toe aan de stylerule die momenteel wordt verwerkt.
		currentContainer.peek().addChild(selector);
	}


	 // Level 1 en hoger:
	 // Verwerkt declaraties binnen een stylerule.

	@Override
	public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
		// Maak een declaratie aan met de naam van de CSS-property.
		Declaration declaration =
				new Declaration(ctx.LOWER_IDENT().getText());

		// Zet de waarde om naar een AST-expressie en voeg deze toe.
		declaration.addChild(createExpression(ctx.expression()));

		// Plaats de declaratie in de huidige stylerule of andere container.
		currentContainer.peek().addChild(declaration);
	}

	/**
	 * Level 2 en hoger:
	 * Verwerkt variabeletoekenningen, zoals ParWidth := 500px;.
	 */
	@Override
	public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		// Maak een nieuwe variabeletoekenning aan.
		VariableAssignment assignment = new VariableAssignment();

		// Het eerste kind bevat de naam van de variabele.
		assignment.addChild(
				new VariableReference(ctx.CAPITAL_IDENT().getText())
		);

		// Het tweede kind bevat de waarde die aan de variabele wordt toegekend.
		assignment.addChild(createExpression(ctx.expression()));

		// Variabeletoekenningen worden aan de hoofdstructuur van de AST toegevoegd.
		ast.root.addChild(assignment);
	}

	/**
	 * Level 2 en hoger:
	 * Verwerkt optellen en aftrekken in expressies.
	 */
	private Expression createExpression(ICSSParser.ExpressionContext ctx) {
		// Begin met het eerste onderdeel, waarbij vermenigvuldiging
		// eerst wordt verwerkt.
		Expression result = createMultiplication(ctx.multiplication(0));

		// Verwerk de overige onderdelen van links naar rechts.
		for (int i = 1; i < ctx.multiplication().size(); i++) {
			Operation operation;

			// Haal het gebruikte teken op, bijvoorbeeld '+' of '-'.
			String operator = ctx.getChild(2 * i - 1).getText();

			// Maak het juiste AST-object voor de bewerking.
			if (operator.equals("+")) {
				operation = new AddOperation();
			} else {
				operation = new SubtractOperation();
			}

			// De eerdere uitkomst wordt de linkerkant van de bewerking.
			operation.addChild(result);

			// Het volgende onderdeel wordt de rechterkant van de bewerking.
			operation.addChild(createMultiplication(ctx.multiplication(i)));

			// Bewaar de bewerking als resultaat voor de volgende stap.
			result = operation;
		}

		return result;
	}


	 // Level 2 en hoger:
	 // Verwerkt vermenigvuldiging in expressies.

	private Expression createMultiplication(ICSSParser.MultiplicationContext ctx) {
		// Begin met het eerste onderdeel van de vermenigvuldiging.
		Expression result = createAtom(ctx.atom(0));

		// Verwerk eventuele volgende onderdelen van links naar rechts.
		for (int i = 1; i < ctx.atom().size(); i++) {
			MultiplyOperation operation = new MultiplyOperation();

			// Voeg de eerdere uitkomst toe als linkerkant.
			operation.addChild(result);

			// Voeg het volgende onderdeel toe als rechterkant.
			operation.addChild(createAtom(ctx.atom(i)));

			// Bewaar de vermenigvuldiging als nieuwe tussenuitkomst.
			result = operation;
		}

		return result;
	}


	 // Level 0 en hoger:
	 // Zet losse waarden en variabelen om naar AST-nodes.

	private Expression createAtom(ICSSParser.AtomContext ctx) {
		// Haal de tekst van het huidige onderdeel op.
		String text = ctx.getText();

		// Herken een kleur, bijvoorbeeld #ff0000.
		if (ctx.COLOR() != null) {
			return new ColorLiteral(text);
		}

		// Herken een pixelwaarde, bijvoorbeeld 100px.
		if (ctx.PIXELSIZE() != null) {
			return new PixelLiteral(text);
		}

		// Herken een percentage, bijvoorbeeld 50%.
		if (ctx.PERCENTAGE() != null) {
			return new PercentageLiteral(text);
		}

		// Herken een getal zonder eenheid, bijvoorbeeld 2.
		if (ctx.SCALAR() != null) {
			return new ScalarLiteral(text);
		}

		// Herken een verwijzing naar een variabele.
		if (ctx.CAPITAL_IDENT() != null) {
			return new VariableReference(text);
		}

		// Als geen van de bovenstaande typen past, wordt de waarde
		// als booleaanse literal verwerkt.
		return new BoolLiteral(text);
	}

	// Level 3: verwerk een if-statement.
	@Override
	public void enterIfStatement(ICSSParser.IfStatementContext ctx) {
		// Maak een nieuwe AST-node voor het if-statement.
		IfClause ifClause = new IfClause();

		// Voeg de voorwaarde als eerste kind toe.
		ifClause.addChild(createExpression(ctx.expression()));

		// Voeg de IfClause toe aan de huidige container.
		currentContainer.peek().addChild(ifClause);

		// Nieuwe onderdelen worden nu aan de IfClause toegevoegd.
		currentContainer.push(ifClause);
	}

	@Override
	public void exitIfStatement(ICSSParser.IfStatementContext ctx) {
		// Het if-statement is verwerkt; ga terug naar de vorige container.
		currentContainer.pop();
	}

	// Level 3: verwerk het else-gedeelte van een if-statement.
	@Override
	public void enterElseBody(ICSSParser.ElseBodyContext ctx) {
		// Maak een node voor de else-clausule.
		ElseClause elseClause = new ElseClause();

		// Voeg de else-clausule toe aan de huidige IfClause.
		currentContainer.peek().addChild(elseClause);

		// Plaats nieuwe onderdelen binnen de ElseClause.
		currentContainer.push(elseClause);
	}

	@Override
	public void exitElseBody(ICSSParser.ElseBodyContext ctx) {
		// Het else-gedeelte is verwerkt; keer terug naar de vorige container.
		currentContainer.pop();
	}
}