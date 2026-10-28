package pokedex.ui.componentes;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import static pokedex.ui.tema.Tema.*;

public class CampoBusca extends JTextField {
	private static final long serialVersionUID = 1L;

	public CampoBusca() {
		super(18);
		setOpaque(false);
		setBorder(new EmptyBorder(0, 44, 0, 16));
		setFont(fonte(Font.PLAIN, 16f));
		setForeground(TEXTO);
		setCaretColor(TEXTO);
		setPreferredSize(new Dimension(100, 44));
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		qualidade(g2);
		int w = getWidth(), h = getHeight();
		g2.setColor(CAMPO);
		g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, h, h));
		g2.setColor(hasFocus() ? new Color(0x7C, 0x8C, 0xFF) : BORDA);
		g2.setStroke(new BasicStroke(hasFocus() ? 1.8f : 1.2f));
		g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, w - 2.6f, h - 2.6f, h, h));

		// lupa
		g2.setColor(SUAVE);
		g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(new Ellipse2D.Float(18, h / 2f - 8, 12, 12));
		g2.drawLine(28, (int) (h / 2f + 3), 33, (int) (h / 2f + 8));
		g2.dispose();

		super.paintComponent(g);

		if (getText().isEmpty()) {
			Graphics2D d = (Graphics2D) g.create();
			qualidade(d);
			d.setColor(new Color(0x71, 0x7A, 0x9C));
			d.setFont(getFont());
			FontMetrics fm = d.getFontMetrics();
			d.drawString("Buscar por nome ou número", getInsets().left, (h - fm.getHeight()) / 2f + fm.getAscent());
			d.dispose();
		}
	}
}
