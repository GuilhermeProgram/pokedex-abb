package pokedex.ui.componentes;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Predicate;
import javax.swing.*;

import pokedex.modelo.Pokemon;
import pokedex.ui.recursos.Imagens;

import static pokedex.ui.tema.Tema.*;

/** Uma linha da lista: miniatura, número, nome e pontos dos tipos. */
public class LinhaPokemon extends JComponent implements ListCellRenderer<Pokemon> {
	private static final long serialVersionUID = 1L;
	private transient Pokemon p;
	private boolean selecionado;
	private final transient Predicate<Pokemon> descoberto;

	public LinhaPokemon(Predicate<Pokemon> descoberto) {
		this.descoberto = descoberto;
		setPreferredSize(new Dimension(100, 64));
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends Pokemon> list, Pokemon value, int index,
			boolean isSelected, boolean cellHasFocus) {
		this.p = value;
		this.selecionado = isSelected;
		return this;
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (p == null) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		qualidade(g2);
		int w = getWidth(), h = getHeight();
		boolean conhecido = descoberto.test(p);
		Color cor = conhecido ? corDoTipo(p.getTipo1()) : PADRAO_A;

		if (selecionado) {
			g2.setColor(mix(PAINEL, cor, 0.32f));
			g2.fill(new RoundRectangle2D.Float(10, 4, w - 20, h - 8, 20, 20));
			g2.setColor(comAlpha(cor, 170));
			g2.setStroke(new BasicStroke(1.4f));
			g2.draw(new RoundRectangle2D.Float(10.7f, 4.7f, w - 21.4f, h - 9.4f, 20, 20));
		}

		// miniatura
		int tam = 48, mx = 22, my = (h - tam) / 2;
		g2.setColor(comAlpha(cor, selecionado ? 120 : 70));
		g2.fill(new Ellipse2D.Float(mx, my, tam, tam));
		BufferedImage mini = conhecido ? Imagens.miniatura(p.getNumeroDex())
				: Imagens.silhuetaMiniatura(p.getNumeroDex());
		if (mini != null) {
			int lado = 56;
			g2.drawImage(mini, mx + (tam - lado) / 2, my + (tam - lado) / 2 - 2, lado, lado, null);
		}

		// textos
		g2.setFont(fonte(Font.PLAIN, 13f));
		g2.setColor(SUAVE);
		g2.drawString(String.format("Nº %03d", p.getNumeroDex()), 84, h / 2 - 5);
		g2.setFont(fonte(Font.BOLD, 17f));
		g2.setColor(TEXTO);
		g2.drawString(conhecido ? p.getNome() : "???", 84, h / 2 + 15);

		// pontos dos tipos
		if (!conhecido) {
			g2.dispose();
			return;
		}
		int px = w - 36;
		if (p.getTipo2() != null) {
			g2.setColor(corDoTipo(p.getTipo2()));
			g2.fill(new Ellipse2D.Float(px, h / 2f - 5, 11, 11));
			px -= 16;
		}
		g2.setColor(cor);
		g2.fill(new Ellipse2D.Float(px, h / 2f - 5, 11, 11));
		g2.dispose();
	}
}
