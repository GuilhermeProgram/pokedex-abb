package PokePackage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static PokePackage.PokemonViewer.*;

/**
 * PC: a caixa onde ficam os Pokémon capturados, como no jogo (grade de 6 x 5 por caixa, com sprites pixelados).
 * Por trás dela está a ABB ordenada pelo número Dex: as caixas mostram o percurso em ordem da árvore,
 * e o botão Liberar chama {@code remover} na ABB.
 */
public class JanelaPC extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final int COLUNAS = 6;
	private static final int LINHAS = 5;
	private static final int POR_CAIXA = COLUNAS * LINHAS;
	private static final int CELULA = 96;
	private static final int FOLGA = 6;

	private final transient ArvoreBinaria pc;
	private final transient Runnable aoMudar;
	private final transient List<Pokemon> itens = new ArrayList<>();
	private int selecionado = 0; // índice em itens
	private int caixa = 0;

	private final Caixa grade = new Caixa();
	private final Cartao cartao = new Cartao();
	private final JLabel info = new JLabel();
	private final BotaoTexto botaoLiberar = new BotaoTexto("Liberar", "Soltar este Pokémon (remove da ABB)");

	/**
	 * @param pc      ABB dos Pokémon capturados
	 * @param aoMudar chamado depois de cada liberação, para a Pokédex se atualizar
	 */
	public JanelaPC(Frame dono, ArvoreBinaria pc, Runnable aoMudar) {
		super(dono, "PC", true);
		this.pc = pc;
		this.aoMudar = aoMudar;

		JPanel raiz = new JPanel(new BorderLayout(18, 0));
		raiz.setBackground(FUNDO_A);
		raiz.setBorder(new EmptyBorder(18, 18, 18, 18));
		raiz.add(grade, BorderLayout.WEST);
		raiz.add(montarDetalhe(), BorderLayout.CENTER);
		raiz.setPreferredSize(new Dimension(1000, 660));
		setContentPane(raiz);
		setResizable(false);
		configurarTeclas(raiz);
		pack();
		setLocationRelativeTo(dono);
		recarregar();
	}

	private JComponent montarDetalhe() {
		JPanel col = new JPanel(new BorderLayout(0, 12));
		col.setOpaque(false);

		info.setFont(fonte(Font.PLAIN, 14f));
		info.setForeground(SUAVE);
		info.setPreferredSize(new Dimension(100, 40));
		col.add(info, BorderLayout.NORTH);
		col.add(cartao, BorderLayout.CENTER);

		JPanel baixo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		baixo.setOpaque(false);
		BotaoTexto fechar = new BotaoTexto("Fechar", "Voltar para a Pokédex");
		fechar.addActionListener(e -> dispose());
		botaoLiberar.addActionListener(e -> liberar());
		baixo.add(botaoLiberar);
		baixo.add(fechar);
		col.add(baixo, BorderLayout.SOUTH);
		return col;
	}

	private void configurarTeclas(JComponent c) {
		mapear(c, "LEFT", -1);
		mapear(c, "RIGHT", +1);
		mapear(c, "UP", -COLUNAS);
		mapear(c, "DOWN", +COLUNAS);
		mapear(c, "PAGE_UP", -POR_CAIXA);
		mapear(c, "PAGE_DOWN", +POR_CAIXA);
	}

	private void mapear(JComponent c, String tecla, int passo) {
		c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla), tecla);
		c.getActionMap().put(tecla, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				selecionar(selecionado + passo);
			}
		});
	}

	/** Seleciona o Pokémon de índice i (ignora se estiver fora) e vira a caixa se for preciso. */
	private void selecionar(int i) {
		if (i < 0 || i >= itens.size()) {
			return;
		}
		selecionado = i;
		caixa = i / POR_CAIXA;
		atualizarTela();
	}

	private int totalCaixas() {
		return Math.max(1, (itens.size() + POR_CAIXA - 1) / POR_CAIXA);
	}

	private void mudarCaixa(int passo) {
		int nova = Math.floorMod(caixa + passo, totalCaixas());
		caixa = nova;
		if (selecionado / POR_CAIXA != caixa) {
			selecionado = Math.min(itens.size() - 1, caixa * POR_CAIXA);
		}
		atualizarTela();
	}

	/** Relê a ABB em ordem (esquerda, raiz, direita) e atualiza as caixas e o resumo. */
	private void recarregar() {
		itens.clear();
		itens.addAll(pc.emOrdem());
		int n = itens.size();
		selecionado = n == 0 ? -1 : Math.max(0, Math.min(selecionado, n - 1));
		caixa = n == 0 ? 0 : selecionado / POR_CAIXA;
		int ideal = 32 - Integer.numberOfLeadingZeros(n); // menor altura possível para n nós
		info.setText(n == 0 ? "<html>O PC está vazio.<br>Cadastre Pokémon na Pokédex.</html>"
				: "<html>" + n + " no PC<br>altura da ABB: " + pc.altura() + " (mínima possível: " + ideal + ")</html>");
		atualizarTela();
	}

	private void atualizarTela() {
		botaoLiberar.setEnabled(selecionado >= 0);
		grade.repaint();
		cartao.repaint();
	}

	private Pokemon atual() {
		return selecionado >= 0 && selecionado < itens.size() ? itens.get(selecionado) : null;
	}

	private void liberar() {
		Pokemon p = atual();
		if (p == null) {
			return;
		}
		int r = JOptionPane.showConfirmDialog(this, "Liberar " + p.getNome() + "? Ele sai do PC e volta a ser desconhecido.",
				"Liberar Pokémon", JOptionPane.YES_NO_OPTION);
		if (r != JOptionPane.YES_OPTION) {
			return;
		}
		pc.remover(p.getNumeroDex());
		recarregar(); // o índice selecionado fica no mesmo lugar (o próximo Pokémon sobe)
		aoMudar.run();
	}

	// =====================================================================
	// A caixa: título com setas + grade de 6 x 5
	// =====================================================================
	private class Caixa extends JComponent {
		private static final long serialVersionUID = 1L;

		private static final int TOPO = 52;
		private static final int PAD = 12;
		private int hover = -1; // índice da célula sob o mouse

		Caixa() {
			int larg = COLUNAS * CELULA + (COLUNAS - 1) * FOLGA + PAD * 2;
			int alt = TOPO + LINHAS * CELULA + (LINHAS - 1) * FOLGA + PAD * 2;
			setPreferredSize(new Dimension(larg, alt));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if (e.getY() < TOPO) {
						if (e.getX() < 70) {
							mudarCaixa(-1);
						} else if (e.getX() > getWidth() - 70) {
							mudarCaixa(+1);
						}
						return;
					}
					int i = indiceEm(e.getPoint());
					if (i >= 0 && i < itens.size()) {
						selecionar(i);
					}
				}

				@Override
				public void mouseExited(MouseEvent e) {
					hover = -1;
					repaint();
				}
			});
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					int h = e.getY() < TOPO ? -1 : indiceEm(e.getPoint());
					if (h != hover) {
						hover = h;
						repaint();
					}
				}
			});
		}

		private int indiceEm(Point pt) {
			int x = pt.x - PAD, y = pt.y - TOPO - PAD;
			if (x < 0 || y < 0) {
				return -1;
			}
			int col = x / (CELULA + FOLGA), lin = y / (CELULA + FOLGA);
			if (col >= COLUNAS || lin >= LINHAS || x % (CELULA + FOLGA) >= CELULA || y % (CELULA + FOLGA) >= CELULA) {
				return -1;
			}
			return caixa * POR_CAIXA + lin * COLUNAS + col;
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			qualidade(g);
			int w = getWidth(), h = getHeight();

			// fundo da caixa
			g.setPaint(new GradientPaint(0, 0, new Color(0x2A, 0x4E, 0x6B), 0, h, new Color(0x16, 0x2C, 0x44)));
			g.fill(new RoundRectangle2D.Float(0, 0, w, h, 26, 26));
			g.setColor(new Color(0x4C, 0x7C, 0xA0));
			g.setStroke(new BasicStroke(1.6f));
			g.draw(new RoundRectangle2D.Float(0.8f, 0.8f, w - 1.6f, h - 1.6f, 26, 26));

			// título "Caixa N" com setas
			g.setFont(fonte(Font.BOLD, 20f));
			g.setColor(TEXTO);
			String titulo = "Caixa " + (caixa + 1) + " / " + totalCaixas();
			g.drawString(titulo, (w - g.getFontMetrics().stringWidth(titulo)) / 2, 34);
			g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(totalCaixas() > 1 ? TEXTO : SUAVE);
			g.drawLine(40, 22, 30, 30);
			g.drawLine(30, 30, 40, 38);
			g.drawLine(w - 40, 22, w - 30, 30);
			g.drawLine(w - 30, 30, w - 40, 38);

			// células
			for (int lin = 0; lin < LINHAS; lin++) {
				for (int col = 0; col < COLUNAS; col++) {
					int i = caixa * POR_CAIXA + lin * COLUNAS + col;
					int x = PAD + col * (CELULA + FOLGA), y = TOPO + PAD + lin * (CELULA + FOLGA);
					Pokemon p = i < itens.size() ? itens.get(i) : null;
					boolean sel = i == selecionado && p != null;

					g.setColor(sel ? new Color(0x3F, 0x6F, 0x96) : hover == i && p != null ? new Color(0x2F, 0x58, 0x7A)
							: new Color(0x1D, 0x38, 0x54));
					g.fill(new RoundRectangle2D.Float(x, y, CELULA, CELULA, 14, 14));
					if (sel) {
						g.setColor(new Color(0xFF, 0xD8, 0x4D));
						g.setStroke(new BasicStroke(3f));
						g.draw(new RoundRectangle2D.Float(x + 1.5f, y + 1.5f, CELULA - 3f, CELULA - 3f, 14, 14));
					}
					if (p == null) {
						continue;
					}
					BufferedImage sprite = Imagens.pixel(p.getNumeroDex());
					if (sprite == null) {
						sprite = Imagens.miniatura(p.getNumeroDex()); // sem o sprite pixel: usa a miniatura normal
					}
					if (sprite != null) {
						g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
						int dw = Math.min(CELULA, sprite.getWidth()), dh = dw * sprite.getHeight() / sprite.getWidth();
						g.drawImage(sprite, x + (CELULA - dw) / 2, y + (CELULA - dh) / 2, dw, dh, null);
					}
					g.setFont(fonte(Font.PLAIN, 11f));
					g.setColor(new Color(255, 255, 255, 170));
					g.drawString(String.valueOf(p.getNumeroDex()), x + 7, y + 15);
				}
			}
			g.dispose();
		}
	}

	// =====================================================================
	// Cartão com o Pokémon selecionado
	// =====================================================================
	private class Cartao extends JComponent {
		private static final long serialVersionUID = 1L;

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			qualidade(g);
			int w = getWidth(), h = getHeight();
			Pokemon p = atual();
			Color cor = p == null ? BORDA : corDoTipo(p.getTipo1());
			g.setPaint(new GradientPaint(0, 0, p == null ? PAINEL : mix(PAINEL, cor, 0.35f), 0, h,
					p == null ? escurecer(PAINEL, 0.7f) : escurecer(cor, 0.35f)));
			g.fill(new RoundRectangle2D.Float(0, 0, w, h, 24, 24));
			g.setColor(p == null ? BORDA : comAlpha(cor, 200));
			g.setStroke(new BasicStroke(1.6f));
			g.draw(new RoundRectangle2D.Float(0.8f, 0.8f, w - 1.6f, h - 1.6f, 24, 24));

			if (p == null) {
				g.setFont(fonte(Font.PLAIN, 15f));
				g.setColor(SUAVE);
				String t = "Nenhum Pokémon selecionado";
				g.drawString(t, (w - g.getFontMetrics().stringWidth(t)) / 2, h / 2);
				g.dispose();
				return;
			}
			BufferedImage img = Imagens.grande(p.getNumeroDex());
			if (img != null) {
				int lado = Math.min(w - 30, h - 130);
				double esc = Math.min((double) lado / img.getWidth(), (double) lado / img.getHeight());
				int dw = (int) (img.getWidth() * esc), dh = (int) (img.getHeight() * esc);
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(img, (w - dw) / 2, 18, dw, dh, null);
			}
			g.setFont(fonte(Font.BOLD, 20f));
			g.setColor(TEXTO);
			String nome = String.format("#%04d  %s", p.getNumeroDex(), p.getNome());
			g.drawString(nome, (w - g.getFontMetrics().stringWidth(nome)) / 2, h - 46);
			g.setFont(fonte(Font.PLAIN, 14f));
			g.setColor(new Color(255, 255, 255, 190));
			String tipos = p.getTipo1() + (p.getTipo2() != null ? " · " + p.getTipo2() : "");
			g.drawString(tipos, (w - g.getFontMetrics().stringWidth(tipos)) / 2, h - 22);
			g.dispose();
		}
	}
}
