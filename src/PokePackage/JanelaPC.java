package PokePackage;

import javax.swing.*;
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
 * PC: a caixa onde ficam os Pokémon capturados. Em cima, o painel com os dados do Pokémon selecionado;
 * embaixo, a caixa em grade (6 x 5) com sprites pixelados. Por trás está a ABB ordenada pelo número Dex:
 * a caixa mostra o percurso em ordem da árvore, e o botão Liberar chama {@code remover} na ABB.
 */
public class JanelaPC extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final int COLUNAS = 6;
	private static final int LINHAS = 5;
	private static final int POR_CAIXA = COLUNAS * LINHAS;
	private static final int CELULA = 66;
	private static final int FOLGA = 4;

	// medidas da janela (tudo desenhado em um painel só)
	private static final int LARGURA = 780;
	private static final int ALTURA = 706;
	private static final int MARGEM = 16;
	private static final int INFO_Y = MARGEM, INFO_H = 236;
	private static final int CAIXA_Y = INFO_Y + INFO_H + 16;
	private static final int CAIXA_W = COLUNAS * CELULA + (COLUNAS - 1) * FOLGA + 32;
	private static final int CAIXA_H = ALTURA - MARGEM - CAIXA_Y;
	private static final int GRADE_X = MARGEM + 16, GRADE_Y = CAIXA_Y + 56;
	private static final int LATERAL_X = MARGEM + CAIXA_W + 16;
	private static final int LATERAL_W = LARGURA - MARGEM - LATERAL_X;

	// cores no estilo do PC dos jogos
	private static final Color FUNDO = new Color(0x1B, 0x25, 0x40);
	private static final Color PAINEL_CLARO = new Color(0xE3, 0xF0, 0xFA);
	private static final Color BORDA_PAINEL = new Color(0x5C, 0x8F, 0xBD);
	private static final Color TEXTO_ESCURO = new Color(0x1E, 0x2E, 0x4A);
	private static final Color ROTULO = new Color(0x4F, 0x6F, 0x94);
	private static final Color VERDE_A = new Color(0xC6, 0xE8, 0xA6), VERDE_B = new Color(0xB9, 0xDE, 0x98);

	private final transient ArvoreBinaria pc;
	private final transient Runnable aoMudar;
	private final transient List<Pokemon> itens = new ArrayList<>();
	private int selecionado = 0; // índice em itens
	private int caixa = 0;
	private int hoverCelula = -1;
	private int hoverBotao = -1; // 0 = Liberar, 1 = Fechar
	private String resumoArvore = "";

	private final Rectangle botaoLiberar = new Rectangle();
	private final Rectangle botaoFechar = new Rectangle();
	private final Tela tela = new Tela();

	/**
	 * @param pc      ABB dos Pokémon capturados
	 * @param aoMudar chamado depois de cada liberação, para a Pokédex se atualizar
	 */
	public JanelaPC(Frame dono, ArvoreBinaria pc, Runnable aoMudar) {
		super(dono, "PC", true);
		this.pc = pc;
		this.aoMudar = aoMudar;

		tela.setPreferredSize(new Dimension(LARGURA, ALTURA));
		setContentPane(tela);
		setResizable(false);
		configurarTeclas(tela);
		pack();
		setLocationRelativeTo(dono);
		recarregar();
	}

	private void configurarTeclas(JComponent c) {
		mapear(c, "LEFT", -1);
		mapear(c, "RIGHT", +1);
		mapear(c, "UP", -COLUNAS);
		mapear(c, "DOWN", +COLUNAS);
		mapear(c, "PAGE_UP", -POR_CAIXA);
		mapear(c, "PAGE_DOWN", +POR_CAIXA);
		c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "fechar");
		c.getActionMap().put("fechar", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
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
		tela.repaint();
	}

	private int totalCaixas() {
		return Math.max(1, (itens.size() + POR_CAIXA - 1) / POR_CAIXA);
	}

	private void mudarCaixa(int passo) {
		caixa = Math.floorMod(caixa + passo, totalCaixas());
		if (!itens.isEmpty() && selecionado / POR_CAIXA != caixa) {
			selecionado = Math.min(itens.size() - 1, caixa * POR_CAIXA);
		}
		tela.repaint();
	}

	/** Relê a ABB em ordem (esquerda, raiz, direita) e atualiza as caixas e o resumo. */
	private void recarregar() {
		itens.clear();
		itens.addAll(pc.emOrdem());
		int n = itens.size();
		selecionado = n == 0 ? -1 : Math.max(0, Math.min(selecionado, n - 1));
		caixa = n == 0 ? 0 : selecionado / POR_CAIXA;
		int ideal = 32 - Integer.numberOfLeadingZeros(n); // menor altura possível para n nós
		resumoArvore = n + " no PC|altura da ABB: " + pc.altura() + " (mínima: " + ideal + ")";
		tela.repaint();
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

	private final transient java.util.Map<Integer, BufferedImage> recortes = new java.util.HashMap<>();

	/** Sprite do Pokémon sem a borda transparente (os sprites têm tamanhos diferentes dentro da imagem 96x96). */
	private BufferedImage sprite(Pokemon p) {
		int dex = p.getNumeroDex();
		if (recortes.containsKey(dex)) {
			return recortes.get(dex);
		}
		BufferedImage img = Imagens.pixel(dex);
		BufferedImage res = null;
		if (img != null) {
			int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
			for (int y = 0; y < img.getHeight(); y++) {
				for (int x = 0; x < img.getWidth(); x++) {
					if ((img.getRGB(x, y) >>> 24) > 20) {
						minX = Math.min(minX, x);
						minY = Math.min(minY, y);
						maxX = Math.max(maxX, x);
						maxY = Math.max(maxY, y);
					}
				}
			}
			res = maxX < 0 ? img : img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
		} else {
			res = Imagens.miniatura(dex);
		}
		recortes.put(dex, res);
		return res;
	}

	private int celulaX(int col) {
		return GRADE_X + col * (CELULA + FOLGA);
	}

	private int celulaY(int lin) {
		return GRADE_Y + lin * (CELULA + FOLGA);
	}

	/** Índice (em itens) da célula sob o ponto, ou -1. */
	private int indiceEm(Point pt) {
		int x = pt.x - GRADE_X, y = pt.y - GRADE_Y;
		if (x < 0 || y < 0) {
			return -1;
		}
		int col = x / (CELULA + FOLGA), lin = y / (CELULA + FOLGA);
		if (col >= COLUNAS || lin >= LINHAS || x % (CELULA + FOLGA) >= CELULA || y % (CELULA + FOLGA) >= CELULA) {
			return -1;
		}
		return caixa * POR_CAIXA + lin * COLUNAS + col;
	}

	// =====================================================================
	// A tela inteira: painel de dados, caixa e lateral
	// =====================================================================
	private class Tela extends JComponent {
		private static final long serialVersionUID = 1L;

		Tela() {
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					Point pt = e.getPoint();
					if (botaoLiberar.contains(pt) && atual() != null) {
						liberar();
					} else if (botaoFechar.contains(pt)) {
						dispose();
					} else if (new Rectangle(MARGEM, CAIXA_Y, 80, 48).contains(pt)) {
						mudarCaixa(-1);
					} else if (new Rectangle(MARGEM + CAIXA_W - 80, CAIXA_Y, 80, 48).contains(pt)) {
						mudarCaixa(+1);
					} else {
						int i = indiceEm(pt);
						if (i >= 0 && i < itens.size()) {
							selecionar(i);
						}
					}
				}

				@Override
				public void mouseExited(MouseEvent e) {
					hoverCelula = -1;
					hoverBotao = -1;
					repaint();
				}
			});
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					Point pt = e.getPoint();
					int hc = indiceEm(pt);
					if (hc >= itens.size()) {
						hc = -1;
					}
					int hb = botaoLiberar.contains(pt) ? 0 : botaoFechar.contains(pt) ? 1 : -1;
					if (hc != hoverCelula || hb != hoverBotao) {
						hoverCelula = hc;
						hoverBotao = hb;
						setCursor(Cursor.getPredefinedCursor(hc >= 0 || hb >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
						repaint();
					}
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			qualidade(g);
			g.setPaint(new GradientPaint(0, 0, FUNDO, 0, getHeight(), escurecer(FUNDO, 0.7f)));
			g.fillRect(0, 0, getWidth(), getHeight());
			desenharInfo(g);
			desenharCaixa(g);
			desenharLateral(g);
			g.dispose();
		}

		// ------------------------------------------------------------ painel de dados
		private void desenharInfo(Graphics2D g) {
			int x = MARGEM, y = INFO_Y, w = LARGURA - 2 * MARGEM, h = INFO_H;
			g.setPaint(new GradientPaint(0, y, PAINEL_CLARO, 0, y + h, new Color(0xC4, 0xDD, 0xF2)));
			g.fill(new RoundRectangle2D.Float(x, y, w, h, 20, 20));
			g.setColor(BORDA_PAINEL);
			g.setStroke(new BasicStroke(3f));
			g.draw(new RoundRectangle2D.Float(x + 1.5f, y + 1.5f, w - 3, h - 3, 20, 20));

			Pokemon p = atual();
			if (p == null) {
				g.setFont(fonte(Font.BOLD, 18f));
				g.setColor(ROTULO);
				String t = "O PC está vazio. Cadastre Pokémon na Pokédex.";
				g.drawString(t, x + (w - g.getFontMetrics().stringWidth(t)) / 2, y + h / 2);
				return;
			}

			// sprite grande (2x, pixel perfeito) sobre um disco na cor do tipo
			Color cor = corDoTipo(p.getTipo1());
			int sx = x + 24, sy = y + 22, lado = 192;
			g.setPaint(new RadialGradientPaint(sx + lado / 2f, sy + lado / 2f, lado / 2f, new float[] { 0f, 1f },
					new Color[] { comAlpha(cor, 120), comAlpha(cor, 0) }));
			g.fillOval(sx - 4, sy - 4, lado + 8, lado + 8);
			BufferedImage sprite = sprite(p);
			if (sprite != null) {
				// escala inteira (pixel perfeito) que cabe no quadrado de 192
				int esc = Math.max(1, Math.min(lado / sprite.getWidth(), lado / sprite.getHeight()));
				int dw = sprite.getWidth() * esc, dh = sprite.getHeight() * esc;
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				g.drawImage(sprite, sx + (lado - dw) / 2, sy + (lado - dh) / 2, dw, dh, null);
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			}

			// nome e número
			int tx = x + 244;
			g.setFont(fonte(Font.BOLD, 15f));
			g.setColor(ROTULO);
			g.drawString(String.format("Nº %04d", p.getNumeroDex()), tx, y + 40);
			g.setFont(fonte(Font.BOLD, 32f));
			g.setColor(TEXTO_ESCURO);
			g.drawString(p.getNome(), tx, y + 78);

			// tipos
			int px = tx, py = y + 94;
			px += pilula(g, p.getTipo1(), px, py) + 8;
			if (p.getTipo2() != null) {
				pilula(g, p.getTipo2(), px, py);
			}

			// linhas de dados
			int ly = y + 150;
			linha(g, "Região", p.getRegiao(), tx, ly);
			linha(g, "Altura", p.getAltura() == null ? "-" : String.format("%.1f m", p.getAltura()), tx + 170, ly);
			linha(g, "Peso", p.getPeso() == null ? "-" : String.format("%.1f kg", p.getPeso()), tx + 340, ly);

			// descrição em uma caixa
			int dy = y + 166, dw = w - (tx - x) - 24, dh = h - (dy - y) - 18;
			g.setColor(new Color(255, 255, 255, 170));
			g.fill(new RoundRectangle2D.Float(tx, dy, dw, dh, 12, 12));
			g.setFont(fonte(Font.PLAIN, 14f));
			g.setColor(TEXTO_ESCURO);
			FontMetrics fm = g.getFontMetrics();
			int ty = dy + 8 + fm.getAscent();
			for (String l : quebrar(p.getDescricao() == null ? "" : p.getDescricao(), fm, dw - 20, 3)) {
				g.drawString(l, tx + 10, ty);
				ty += fm.getHeight();
			}
		}

		private int pilula(Graphics2D g, String tipo, int x, int y) {
			g.setFont(fonte(Font.BOLD, 13f));
			FontMetrics fm = g.getFontMetrics();
			int w = fm.stringWidth(tipo) + 24, h = 26;
			Color c = corDoTipo(tipo);
			g.setColor(c);
			g.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
			g.setColor(clara(c) ? new Color(0x20, 0x20, 0x20) : Color.WHITE);
			g.drawString(tipo, x + 12, y + (h - fm.getHeight()) / 2f + fm.getAscent());
			return w;
		}

		private void linha(Graphics2D g, String rotulo, String valor, int x, int y) {
			g.setFont(fonte(Font.PLAIN, 13f));
			g.setColor(ROTULO);
			g.drawString(rotulo, x, y - 20);
			g.setFont(fonte(Font.BOLD, 18f));
			g.setColor(TEXTO_ESCURO);
			g.drawString(valor == null ? "-" : valor, x, y + 2);
		}

		private List<String> quebrar(String texto, FontMetrics fm, int largura, int maxLinhas) {
			List<String> linhas = new ArrayList<>();
			StringBuilder atual = new StringBuilder();
			for (String palavra : texto.split(" ")) {
				String teste = atual.length() == 0 ? palavra : atual + " " + palavra;
				if (fm.stringWidth(teste) > largura && atual.length() > 0) {
					linhas.add(atual.toString());
					atual = new StringBuilder(palavra);
				} else {
					atual = new StringBuilder(teste);
				}
			}
			if (atual.length() > 0) {
				linhas.add(atual.toString());
			}
			return linhas.size() <= maxLinhas ? linhas : linhas.subList(0, maxLinhas);
		}

		// ------------------------------------------------------------ caixa
		private void desenharCaixa(Graphics2D g) {
			int x = MARGEM, y = CAIXA_Y, w = CAIXA_W, h = CAIXA_H;

			// moldura e cabeçalho
			g.setPaint(new GradientPaint(0, y, new Color(0x5F, 0xA8, 0x6A), 0, y + h, new Color(0x3F, 0x84, 0x52)));
			g.fill(new RoundRectangle2D.Float(x, y, w, h, 20, 20));
			g.setColor(new Color(0x2B, 0x62, 0x3C));
			g.setStroke(new BasicStroke(3f));
			g.draw(new RoundRectangle2D.Float(x + 1.5f, y + 1.5f, w - 3, h - 3, 20, 20));

			// pílula do título com as setas
			int pw = 250, ph = 34, px = x + (w - pw) / 2, py = y + 10;
			g.setColor(new Color(0xF2, 0xFB, 0xEA));
			g.fill(new RoundRectangle2D.Float(px, py, pw, ph, ph, ph));
			g.setColor(new Color(0x2B, 0x62, 0x3C));
			g.setStroke(new BasicStroke(2f));
			g.draw(new RoundRectangle2D.Float(px, py, pw, ph, ph, ph));
			g.setFont(fonte(Font.BOLD, 17f));
			String titulo = "CAIXA " + (caixa + 1) + "  /  " + totalCaixas();
			FontMetrics fm = g.getFontMetrics();
			g.drawString(titulo, px + (pw - fm.stringWidth(titulo)) / 2f, py + (ph - fm.getHeight()) / 2f + fm.getAscent());
			g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(new Color(0xF2, 0xFB, 0xEA));
			int cy = py + ph / 2;
			g.drawLine(x + 44, cy - 9, x + 32, cy);
			g.drawLine(x + 32, cy, x + 44, cy + 9);
			g.drawLine(x + w - 44, cy - 9, x + w - 32, cy);
			g.drawLine(x + w - 32, cy, x + w - 44, cy + 9);

			// área da grade (xadrez verde claro)
			int gw = COLUNAS * CELULA + (COLUNAS - 1) * FOLGA, gh = LINHAS * CELULA + (LINHAS - 1) * FOLGA;
			Shape clip = g.getClip();
			g.clip(new RoundRectangle2D.Float(GRADE_X - 8, GRADE_Y - 8, gw + 16, gh + 16, 14, 14));
			for (int lin = 0; lin < LINHAS; lin++) {
				for (int col = 0; col < COLUNAS; col++) {
					g.setColor((lin + col) % 2 == 0 ? VERDE_A : VERDE_B);
					g.fillRect(celulaX(col) - FOLGA / 2, celulaY(lin) - FOLGA / 2, CELULA + FOLGA, CELULA + FOLGA);
				}
			}
			g.fillRect(0, 0, 0, 0);
			g.setClip(clip);
			g.setColor(new Color(0x2B, 0x62, 0x3C));
			g.setStroke(new BasicStroke(2f));
			g.draw(new RoundRectangle2D.Float(GRADE_X - 8, GRADE_Y - 8, gw + 16, gh + 16, 14, 14));

			// Pokémon
			for (int lin = 0; lin < LINHAS; lin++) {
				for (int col = 0; col < COLUNAS; col++) {
					int i = caixa * POR_CAIXA + lin * COLUNAS + col;
					Pokemon p = i < itens.size() ? itens.get(i) : null;
					if (p == null) {
						continue;
					}
					int cx = celulaX(col), cyy = celulaY(lin);
					boolean sel = i == selecionado;
					if (hoverCelula == i && !sel) {
						g.setColor(new Color(255, 255, 255, 80));
						g.fill(new RoundRectangle2D.Float(cx, cyy, CELULA, CELULA, 10, 10));
					}
					if (sel) {
						g.setColor(new Color(255, 255, 255, 120));
						g.fill(new RoundRectangle2D.Float(cx, cyy, CELULA, CELULA, 10, 10));
						g.setColor(new Color(0xE0, 0x3B, 0x30));
						g.setStroke(new BasicStroke(3f));
						g.draw(new RoundRectangle2D.Float(cx + 1.5f, cyy + 1.5f, CELULA - 3, CELULA - 3, 10, 10));
					}
					BufferedImage sprite = sprite(p);
					if (sprite != null) {
						int max = CELULA - 8;
						double esc = Math.min((double) max / sprite.getWidth(), (double) max / sprite.getHeight());
						esc = esc >= 1 ? Math.floor(esc) : esc; // se der, escala inteira (mantém o pixel nítido)
						int dw = (int) Math.round(sprite.getWidth() * esc), dh = (int) Math.round(sprite.getHeight() * esc);
						g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, esc >= 1 ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
								: RenderingHints.VALUE_INTERPOLATION_BILINEAR);
						g.drawImage(sprite, cx + (CELULA - dw) / 2, cyy + (CELULA - dh) / 2 - (sel ? 3 : 0), dw, dh, null);
						g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					}
				}
			}
		}

		// ------------------------------------------------------------ lateral: resumo da ABB e botões
		private void desenharLateral(Graphics2D g) {
			int x = LATERAL_X, y = CAIXA_Y, w = LATERAL_W;

			// cartão com o resumo da árvore
			int ch = 118;
			g.setPaint(new GradientPaint(0, y, PAINEL_CLARO, 0, y + ch, new Color(0xC4, 0xDD, 0xF2)));
			g.fill(new RoundRectangle2D.Float(x, y, w, ch, 18, 18));
			g.setColor(BORDA_PAINEL);
			g.setStroke(new BasicStroke(3f));
			g.draw(new RoundRectangle2D.Float(x + 1.5f, y + 1.5f, w - 3, ch - 3, 18, 18));
			String[] partes = resumoArvore.split("\\|");
			g.setFont(fonte(Font.BOLD, 13f));
			g.setColor(ROTULO);
			g.drawString("ÁRVORE (ABB)", x + 16, y + 28);
			g.setFont(fonte(Font.BOLD, 22f));
			g.setColor(TEXTO_ESCURO);
			g.drawString(partes.length > 0 ? partes[0] : "", x + 16, y + 62);
			g.setFont(fonte(Font.PLAIN, 14f));
			g.drawString(partes.length > 1 ? partes[1] : "", x + 16, y + 90);

			// botões
			int bh = 52, bx = x, bw = w;
			int porY = CAIXA_Y + CAIXA_H - bh;
			botaoFechar.setBounds(bx, porY, bw, bh);
			botaoLiberar.setBounds(bx, porY - bh - 12, bw, bh);
			botao(g, botaoLiberar, "LIBERAR", atual() != null, true, hoverBotao == 0);
			botao(g, botaoFechar, "FECHAR", true, false, hoverBotao == 1);

			g.setFont(fonte(Font.PLAIN, 12f));
			g.setColor(new Color(0xA9, 0xB6, 0xD6));
			g.drawString("Setas: mover  ·  PgUp/PgDn: caixa", x, porY - bh - 28);
		}

		private void botao(Graphics2D g, Rectangle r, String texto, boolean ativo, boolean vermelho, boolean hover) {
			Color base = !ativo ? new Color(0x6B, 0x75, 0x8C) : vermelho ? new Color(0xE0, 0x3B, 0x30) : new Color(0x5C, 0x8F, 0xBD);
			Color topo = hover && ativo ? mix(base, Color.WHITE, 0.2f) : mix(base, Color.WHITE, 0.08f);
			g.setPaint(new GradientPaint(0, r.y, topo, 0, r.y + r.height, escurecer(base, 0.82f)));
			g.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 14, 14));
			g.setColor(escurecer(base, 0.55f));
			g.setStroke(new BasicStroke(2.5f));
			g.draw(new RoundRectangle2D.Float(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 14, 14));
			g.setFont(fonte(Font.BOLD, 18f));
			FontMetrics fm = g.getFontMetrics();
			g.setColor(ativo ? Color.WHITE : new Color(0xC9, 0xCF, 0xDD));
			g.drawString(texto, r.x + (r.width - fm.stringWidth(texto)) / 2f, r.y + (r.height - fm.getHeight()) / 2f + fm.getAscent());
		}
	}
}
