package pokedex.ui.componentes;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import javax.swing.*;

import static pokedex.ui.tema.Tema.*;

/** Botão redondo das setas de navegação. */
public class BotaoRedondo extends JButton {
	private static final long serialVersionUID = 1L;

	private final int direcao; // -1 = seta para a esquerda, +1 = para a direita

	public BotaoRedondo(String simbolo, String dica) {
		super("");
		this.direcao = "‹".equals(simbolo) ? -1 : 1;
		setToolTipText(dica);
		setPreferredSize(new Dimension(42, 42));
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setFocusable(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		qualidade(g2);
		Color fundo = getModel().isPressed() ? escurecer(PAINEL, 0.8f)
				: getModel().isRollover() ? mix(PAINEL, Color.WHITE, 0.10f) : PAINEL;
		g2.setColor(fundo);
		g2.fill(new Ellipse2D.Float(0, 0, getWidth() - 1, getHeight() - 1));
		g2.setColor(BORDA);
		g2.setStroke(new BasicStroke(1.2f));
		g2.draw(new Ellipse2D.Float(0.6f, 0.6f, getWidth() - 2.2f, getHeight() - 2.2f));

		// seta desenhada à mão (não depende da fonte)
		int cx = getWidth() / 2, cy = getHeight() / 2;
		g2.setColor(TEXTO);
		g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.drawLine(cx - direcao * 3, cy - 7, cx + direcao * 4, cy);
		g2.drawLine(cx + direcao * 4, cy, cx - direcao * 3, cy + 7);
		g2.dispose();
	}
}
