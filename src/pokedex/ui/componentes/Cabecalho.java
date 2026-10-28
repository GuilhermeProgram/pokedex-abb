package pokedex.ui.componentes;

import java.awt.*;
import javax.swing.*;

import static pokedex.ui.tema.Tema.*;

public class Cabecalho extends JComponent {
	private static final long serialVersionUID = 1L;

	public Cabecalho() {
		setPreferredSize(new Dimension(100, 86));
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		qualidade(g2);
		desenharPokebola(g2, 42, 44, 17, new Color(0xE8, 0x3B, 0x4B), 255);
		g2.setFont(fonte(Font.BOLD, 26f));
		g2.setColor(TEXTO);
		g2.drawString("Pokédex", 70, 53);
		g2.dispose();
	}
}
