package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.AST;
import nl.han.ica.icss.ast.ASTNode;
import nl.han.ica.icss.ast.Stylesheet;
import nl.han.ica.icss.ast.Stylerule;
import nl.han.ica.icss.ast.Declaration;
import nl.han.ica.icss.ast.Expression;
import nl.han.ica.icss.ast.VariableReference;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.BoolLiteral;

public class Generator {

	// Hier bouwen we de uiteindelijke CSS-code stap voor stap op.
	private final StringBuilder output = new StringBuilder();

	public String generate(AST ast) {
		// Maak de uitvoer leeg, zodat een eerdere generatie geen invloed heeft.
		output.setLength(0);

		// Begin bij de hoofdstructuur van de AST en verwerk de onderdelen.
		generateNodes(ast.root);

		// Geef de volledige gegenereerde CSS-code terug als tekst.
		return output.toString();
	}

	private void generateNodes(ASTNode node) {
		// Een stylesheet bevat de verschillende onderdelen van het ICSS-bestand.
		if (node instanceof Stylesheet) {
			Stylesheet stylesheet = (Stylesheet) node;

			// Verwerk ieder onderdeel van het stylesheet afzonderlijk.
			for (ASTNode child : stylesheet.body) {
				generateNodes(child);
			}
		} else if (node instanceof Stylerule) {
			// Een stylerule bevat een selector en de bijbehorende declaraties.
			generateStylerule((Stylerule) node);
		}
	}

	private void generateStylerule(Stylerule rule) {
		// Voeg alle selectors toe. Meerdere selectors worden gescheiden door een komma.
		for (int i = 0; i < rule.selectors.size(); i++) {
			if (i > 0) {
				output.append(", ");
			}

			output.append(rule.selectors.get(i).toString());
		}

		// Open de CSS-regel en begin op een nieuwe regel met de declaraties.
		output.append(" {\n");

		// Verwerk de declaraties binnen deze stylerule.
		for (ASTNode node : rule.body) {
			if (node instanceof Declaration) {
				generateDeclaration((Declaration) node);
			}
		}

		// Sluit de CSS-regel af.
		output.append("}\n");
	}

	private void generateDeclaration(Declaration declaration) {
		// Voeg de eigenschap toe, bijvoorbeeld 'width', gevolgd door de waarde.
		// De spaties en nieuwe regel zorgen ervoor dat de CSS leesbaar blijft.
		output.append("  ");
		output.append(declaration.property);
		output.append(": ");
		output.append(generateExpression(declaration.expression));
		output.append(";\n");
	}

	private String generateExpression(Expression expression) {
		// Zet een pixelwaarde om naar CSS, bijvoorbeeld 100px.
		if (expression instanceof PixelLiteral) {
			return ((PixelLiteral) expression).value + "px";
		}

		// Zet een percentage om naar CSS, bijvoorbeeld 50%.
		if (expression instanceof PercentageLiteral) {
			return ((PercentageLiteral) expression).value + "%";
		}

		// Een scalar is een getal zonder eenheid, bijvoorbeeld 2.
		if (expression instanceof ScalarLiteral) {
			return String.valueOf(((ScalarLiteral) expression).value);
		}

		// Geef de kleurwaarde terug, bijvoorbeeld #ff0000.
		if (expression instanceof ColorLiteral) {
			return ((ColorLiteral) expression).value;
		}

		// Zet een boolean om naar tekst, bijvoorbeeld true of false.
		if (expression instanceof BoolLiteral) {
			return String.valueOf(((BoolLiteral) expression).value);
		}

		// Geef de naam terug als de expressie nog een variabeleverwijzing is.
		if (expression instanceof VariableReference) {
			return ((VariableReference) expression).name;
		}

		// Als het type niet wordt ondersteund, wordt een lege tekst teruggegeven.
		// Berekeningen moeten bijvoorbeeld eerst door de Evaluator worden verwerkt.
		return "";
	}
}