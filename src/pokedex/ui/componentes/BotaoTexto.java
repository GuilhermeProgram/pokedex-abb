package pokedex.ui.componentes;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

import static pokedex.ui.tema.Tema.*;

/** Botão em forma de pílula com texto (ex.: Cadastrar). */
public class BotaoTexto extends JButton {
	private static final long serialVersionUID = 1L;

	public BotaoTexto(String texto, String dica) {
		super(texto);
		setToolTipText(dica);
		setFont(fonte(Font.BOLD, 14f));
		setForeground(TEXTO);
		setPreferredSize(new Dimension(108, 42));
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
		g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight()));
		g2.setColor(BORDA);
		g2.setStroke(new BasicStroke(1.2f));
		g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, getWidth() - 2.2f, getHeight() - 2.2f, getHeight(), getHeight()));
		g2.setFont(getFont());
		FontMetrics fm = g2.getFontMetrics();
		g2.setColor(TEXTO);
		g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2f,
				(getHeight() - fm.getHeight()) / 2f + fm.getAscent());
		g2.dispose();
	}
}
