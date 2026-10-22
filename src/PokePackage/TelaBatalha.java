package PokePackage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static PokePackage.PokemonViewer.PokemonViewer.*;

/**
 * Janela da batalha em equipe: o seu time contra o time adversário, um Pokémon de cada vez.
 * A lógica de cada confronto fica em {@link Batalha}; aqui ficam o desenho, os botões e a fila de mensagens.
 *
 * Tamanho fixo (a janela não cresce com o texto) e sprites/fundo em cache, para a animação ficar leve.
 */
public class TelaBatalha extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final int LARGURA = 920, ALTURA_ARENA = 470, ALTURA_BAIXO = 190;
	private static final Pattern PERDEU = Pattern.compile("^(.+) perdeu (\\d+) de HP\\.$");
	private static final Pattern EVOLUIU = Pattern.compile("^(.+) Mega Evoluiu para (.+)!$");

	private final transient DadosBatalha dados;
	private final transient Random sorte = new Random();
	private final List<Pokemon> meuTime, timeOponente;
	private int iJog = 0, iOpo = 0;
	private transient Batalha batalha;

	// o que está desenhado agora (muda só quando a mensagem correspondente aparece)
	private Pokemon formaJogador, formaOponente;
	private double hpJogMostrado, hpOpoMostrado;
	private int hpJogAlvo, hpOpoAlvo, hpJogMax, hpOpoMax;
	private BufferedImage spriteJog, spriteOpo, fundo;
	private double alfaJog = 1, alfaOpo = 1;
	private boolean caindoJog, caindoOpo;
	private double tremerJog, tremerOpo;
	private final List<double[]> popups = new ArrayList<>(); // {ehJogador, idade, dano}
	private double fase = 0;

	// mensagens e fila de eventos
	private String mensagem = "";
	private final Deque<Evento> fila = new ArrayDeque<>();
	private long proximoEm = 0;
	private boolean fim = false;
	private boolean megaUsadaNaLuta = false;
	private transient Pokemon megaEscolhida;
	private final Set<String> nomesJogador = new HashSet<>();

	private final Arena arena = new Arena();
	private final CaixaMensagem caixaMensagem = new CaixaMensagem();
	private final List<BotaoGolpe> botoesGolpe = new ArrayList<>();
	private final JPanel linhaMega = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
	private final List<BotaoMega> botoesMega = new ArrayList<>();
	private final BotaoTexto botaoFechar = new BotaoTexto("Fechar", "Voltar para a Pokédex (Esc)");
	private final javax.swing.Timer relogio;

	private static class Evento {
		final String texto;
		final Runnable acao;
		final int ms;

		Evento(String texto, Runnable acao, int ms) {
			this.texto = texto;
			this.acao = acao;
			this.ms = ms;
		}
	}

	public TelaBatalha(Frame dono, DadosBatalha dados, List<Pokemon> meuTime, List<Pokemon> timeOponente) {
		super(dono, "Batalha", true);
		this.dados = dados;
		this.meuTime = new ArrayList<>(meuTime);
		this.timeOponente = new ArrayList<>(timeOponente);
		for (Pokemon p : meuTime) {
			nomesJogador.add(p.getNome());
			for (Pokemon m : dados.megasDe(p.getNumeroDex())) {
				nomesJogador.add(m.getNome());
			}
		}

		JPanel raiz = new JPanel(new BorderLayout());
		raiz.setBackground(FUNDO_A);
		raiz.add(arena, BorderLayout.CENTER);
		raiz.add(montarBaixo(), BorderLayout.SOUTH);
		raiz.setPreferredSize(new Dimension(LARGURA, ALTURA_ARENA + ALTURA_BAIXO));
		setContentPane(raiz);
		setResizable(false);
		pack();
		setLocationRelativeTo(dono);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		configurarTeclas(raiz);

		novaBatalha(this.meuTime.get(0), 0, this.timeOponente.get(0), 0);
		mensagem = "O adversário enviou " + formaOponente.getNome() + "! Vai, " + formaJogador.getNome() + "!";

		relogio = new javax.swing.Timer(33, e -> passo());
		relogio.start();
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosed(java.awt.event.WindowEvent e) {
				relogio.stop();
			}
		});
	}

	// =====================================================================
	// Montagem da parte de baixo (mensagem, Mega Evolução e golpes)
	// =====================================================================
	private JComponent montarBaixo() {
		JPanel baixo = new JPanel(new BorderLayout(14, 0));
		baixo.setBackground(escurecer(FUNDO_A, 0.8f));
		baixo.setBorder(new EmptyBorder(14, 18, 14, 18));
		baixo.setPreferredSize(new Dimension(LARGURA, ALTURA_BAIXO));

		JPanel esquerda = new JPanel();
		esquerda.setOpaque(false);
		esquerda.setLayout(new BoxLayout(esquerda, BoxLayout.Y_AXIS));
		esquerda.setPreferredSize(new Dimension(400, 100));
		caixaMensagem.setAlignmentX(Component.LEFT_ALIGNMENT);
		caixaMensagem.setMaximumSize(new Dimension(400, 106));
		caixaMensagem.setPreferredSize(new Dimension(400, 106));
		esquerda.add(caixaMensagem);
		esquerda.add(Box.createVerticalStrut(10));
		linhaMega.setOpaque(false);
		linhaMega.setAlignmentX(Component.LEFT_ALIGNMENT);
		linhaMega.setPreferredSize(new Dimension(400, 42));
		linhaMega.setMaximumSize(new Dimension(400, 42));
		botaoFechar.setPreferredSize(new Dimension(108, 42));
		botaoFechar.addActionListener(e -> dispose());
		botaoFechar.setVisible(false);
		esquerda.add(linhaMega);
		baixo.add(esquerda, BorderLayout.WEST);

		JPanel grade = new JPanel(new GridLayout(2, 2, 10, 10));
		grade.setOpaque(false);
		for (int i = 0; i < 4; i++) {
			BotaoGolpe b = new BotaoGolpe(i);
			botoesGolpe.add(b);
			grade.add(b);
		}
		baixo.add(grade, BorderLayout.CENTER);
		return baixo;
	}

	private void configurarTeclas(JComponent raiz) {
		InputMap im = raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = raiz.getActionMap();
		for (int i = 0; i < 4; i++) {
			final int n = i;
			im.put(KeyStroke.getKeyStroke("" + (i + 1)), "golpe" + i);
			am.put("golpe" + i, new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (botoesGolpe.get(n).isEnabled()) {
						botoesGolpe.get(n).doClick();
					}
				}
			});
		}
		im.put(KeyStroke.getKeyStroke("M"), "mega");
		am.put("mega", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!botoesMega.isEmpty() && botoesMega.get(0).isVisible()) {
					botoesMega.get(0).doClick();
				}
			}
		});
		im.put(KeyStroke.getKeyStroke("ESCAPE"), "sair");
		am.put("sair", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (fim) {
					dispose();
				}
			}
		});
	}

	// =====================================================================
	// Controle da luta
	// =====================================================================

	/** Começa (ou continua) um confronto: quem já estava ferido entra com o HP que tinha. */
	private void novaBatalha(Pokemon jogador, int hpJogador, Pokemon oponente, int hpOponente) {
		batalha = new Batalha(dados, jogador, hpJogador, oponente, hpOponente, sorte);
		formaJogador = batalha.getJogador();
		formaOponente = batalha.getOponente();
		hpJogMax = batalha.getHpMaxJogador();
		hpOpoMax = batalha.getHpMaxOponente();
		hpJogAlvo = batalha.getHpJogador();
		hpOpoAlvo = batalha.getHpOponente();
		hpJogMostrado = hpJogAlvo;
		hpOpoMostrado = hpOpoAlvo;
		alfaJog = alfaOpo = 1;
		caindoJog = caindoOpo = false;
		megaEscolhida = null;
		atualizarSprites();
		atualizarFundo();
		atualizarGolpes();
		atualizarMegas();
		habilitar(true);
	}

	private void atualizarGolpes() {
		List<Golpe> golpes = dados.golpesDe(formaJogador.getNumeroDex());
		for (int i = 0; i < botoesGolpe.size(); i++) {
			botoesGolpe.get(i).definir(i < golpes.size() ? golpes.get(i) : null);
		}
	}

	private void atualizarMegas() {
		linhaMega.removeAll();
		botoesMega.clear();
		List<Pokemon> megas = (megaUsadaNaLuta || fim) ? new ArrayList<>() : batalha.megasDisponiveisDoJogador();
		for (Pokemon m : megas) {
			BotaoMega b = new BotaoMega(m, megas.size() > 1);
			b.addActionListener(e -> {
				megaEscolhida = megaEscolhida == m ? null : m;
				for (BotaoMega o : botoesMega) {
					o.repaint();
				}
			});
			botoesMega.add(b);
			linhaMega.add(b);
		}
		if (fim) {
			linhaMega.add(botaoFechar);
		}
		linhaMega.revalidate();
		linhaMega.repaint();
	}

	private void habilitar(boolean sim) {
		for (BotaoGolpe b : botoesGolpe) {
			b.setEnabled(sim && b.golpe != null);
		}
		for (BotaoMega b : botoesMega) {
			b.setEnabled(sim);
		}
	}

	private boolean ocupado() {
		return !fila.isEmpty() || System.currentTimeMillis() < proximoEm;
	}

	/** O jogador escolheu um golpe: joga o turno e enfileira o relato (uma linha por vez na tela). */
	private void jogar(Golpe g) {
		if (fim || ocupado() || batalha.terminou()) {
			return;
		}
		habilitar(false);
		Pokemon mega = megaEscolhida;
		megaEscolhida = null;
		if (mega != null) {
			megaUsadaNaLuta = true;
		}
		final Batalha atual = batalha;
		List<String> relato = atual.turno(g, mega);
		for (String linha : relato) {
			if (linha.endsWith("venceu a batalha!")) {
				continue; // em equipe, quem vence a luta é decidido no fim
			}
			final String l = linha;
			boolean dano = PERDEU.matcher(l).matches();
			fila.add(new Evento(l, () -> aplicarLinha(l, atual), dano ? 1000 : 850));
		}
		fila.add(new Evento(null, () -> fimDoTurno(atual), 50));
	}

	/** Efeitos visuais ligados à linha do relato que acabou de aparecer. */
	private void aplicarLinha(String linha, Batalha b) {
		Matcher ev = EVOLUIU.matcher(linha);
		if (ev.matches()) {
			if (nomesJogador.contains(ev.group(2))) {
				formaJogador = b.getJogador();
				atualizarSprites();
				atualizarGolpes();
				for (BotaoMega bm : botoesMega) {
					bm.setVisible(false);
				}
			} else {
				formaOponente = b.getOponente();
				atualizarSprites();
			}
			atualizarFundo();
			return;
		}
		Matcher m = PERDEU.matcher(linha);
		if (m.matches()) {
			int dano = Integer.parseInt(m.group(2));
			boolean doJogador = nomesJogador.contains(m.group(1));
			if (doJogador) {
				hpJogAlvo = Math.max(0, hpJogAlvo - dano);
				tremerJog = 1;
				caindoJog = hpJogAlvo == 0;
			} else {
				hpOpoAlvo = Math.max(0, hpOpoAlvo - dano);
				tremerOpo = 1;
				caindoOpo = hpOpoAlvo == 0;
			}
			popups.add(new double[] { doJogador ? 1 : 0, 0, dano });
		}
	}

	/** Acabou de mostrar o turno: ou libera os golpes, ou troca o Pokémon que caiu. */
	private void fimDoTurno(Batalha b) {
		if (!b.terminou()) {
			habilitar(true);
			return;
		}
		boolean jogadorCaiu = b.getHpJogador() <= 0;
		final Pokemon sobrevivente = jogadorCaiu ? b.getOponente() : b.getJogador();
		final int hpSobrevivente = jogadorCaiu ? b.getHpOponente() : b.getHpJogador();
		fila.add(new Evento((jogadorCaiu ? formaJogador : formaOponente).getNome() + " desmaiou!", null, 1500));
		if (jogadorCaiu) {
			iJog++;
			if (iJog >= meuTime.size()) {
				terminar("Todo o seu time desmaiou... Você perdeu a luta!");
			} else {
				final Pokemon proximo = meuTime.get(iJog);
				fila.add(new Evento("Vai, " + proximo.getNome() + "!", () -> novaBatalha(proximo, 0, sobrevivente, hpSobrevivente), 900));
			}
		} else {
			iOpo++;
			if (iOpo >= timeOponente.size()) {
				terminar("O time adversário foi derrotado. Você venceu a luta!");
			} else {
				final Pokemon proximo = timeOponente.get(iOpo);
				fila.add(new Evento("O adversário enviou " + proximo.getNome() + "!",
						() -> novaBatalha(sobrevivente, hpSobrevivente, proximo, 0), 900));
			}
		}
	}

	private void terminar(String texto) {
		fila.add(new Evento(texto, () -> {
			fim = true;
			botaoFechar.setVisible(true);
			atualizarMegas(); // mostra o botão Fechar
		}, 100));
	}

	// =====================================================================
	// Relógio: anima barras, tremor, números de dano e a fila de mensagens
	// =====================================================================
	private void passo() {
		fase += 0.07;
		hpJogMostrado += (hpJogAlvo - hpJogMostrado) * 0.14;
		hpOpoMostrado += (hpOpoAlvo - hpOpoMostrado) * 0.14;
		tremerJog = Math.max(0, tremerJog - 0.07);
		tremerOpo = Math.max(0, tremerOpo - 0.07);
		if (caindoJog) {
			alfaJog = Math.max(0, alfaJog - 0.05);
		}
		if (caindoOpo) {
			alfaOpo = Math.max(0, alfaOpo - 0.05);
		}
		for (int i = popups.size() - 1; i >= 0; i--) {
			popups.get(i)[1] += 0.035;
			if (popups.get(i)[1] >= 1) {
				popups.remove(i);
			}
		}
		long agora = System.currentTimeMillis();
		if (!fila.isEmpty() && agora >= proximoEm) {
			Evento e = fila.poll();
			if (e.texto != null) {
				mensagem = e.texto;
				caixaMensagem.repaint();
			}
			if (e.acao != null) {
				e.acao.run();
			}
			proximoEm = agora + e.ms;
		}
		arena.repaint();
	}

	// =====================================================================
	// Caches (sprites e fundo): desenhados uma vez, só copiados a cada quadro
	// =====================================================================
	private void atualizarSprites() {
		spriteJog = preparar(formaJogador.getNumeroDex(), 310, true);
		spriteOpo = preparar(formaOponente.getNumeroDex(), 250, false);
	}

	private static BufferedImage preparar(int dex, int lado, boolean espelhar) {
		BufferedImage img = Imagens.grande(dex);
		if (img == null) {
			return null;
		}
		double escala = Math.min((double) lado / img.getWidth(), (double) lado / img.getHeight());
		int w = Math.max(1, (int) (img.getWidth() * escala)), h = Math.max(1, (int) (img.getHeight() * escala));
		BufferedImage saida = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = saida.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		if (espelhar) {
			g.drawImage(img, w, 0, -w, h, null);
		} else {
			g.drawImage(img, 0, 0, w, h, null);
		}
		g.dispose();
		return saida;
	}

	private void atualizarFundo() {
		Color co = corDoTipo(formaOponente.getTipo1()), cj = corDoTipo(formaJogador.getTipo1());
		BufferedImage f = new BufferedImage(LARGURA, ALTURA_ARENA, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = f.createGraphics();
		qualidade(g);
		g.setPaint(new GradientPaint(0, 0, mix(FUNDO_A, co, 0.28f), 0, ALTURA_ARENA, mix(FUNDO_A, cj, 0.22f)));
		g.fillRect(0, 0, LARGURA, ALTURA_ARENA);

		// listras diagonais bem suaves
		g.setColor(new Color(255, 255, 255, 9));
		for (int x = -ALTURA_ARENA; x < LARGURA; x += 70) {
			g.fillPolygon(new int[] { x, x + 28, x + 28 + ALTURA_ARENA, x + ALTURA_ARENA }, new int[] { ALTURA_ARENA, ALTURA_ARENA, 0, 0 }, 4);
		}
		// brilhos atrás de cada Pokémon
		brilho(g, 690, 190, 280, co, 120);
		brilho(g, 250, 370, 330, cj, 110);
		// plataformas
		plataforma(g, 690, 250, 330, 64, co);
		plataforma(g, 250, 438, 400, 74, cj);
		// vinheta
		g.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 90), 0, 70, new Color(0, 0, 0, 0)));
		g.fillRect(0, 0, LARGURA, 70);
		g.setPaint(new GradientPaint(0, ALTURA_ARENA - 60, new Color(0, 0, 0, 0), 0, ALTURA_ARENA, new Color(0, 0, 0, 110)));
		g.fillRect(0, ALTURA_ARENA - 60, LARGURA, 60);
		g.dispose();
		fundo = f;
	}

	private static void brilho(Graphics2D g, int cx, int cy, int raio, Color cor, int alfa) {
		g.setPaint(new RadialGradientPaint(cx, cy, raio, new float[] { 0f, 1f },
				new Color[] { comAlpha(cor, alfa), comAlpha(cor, 0) }));
		g.fill(new Ellipse2D.Float(cx - raio, cy - raio, raio * 2, raio * 2));
	}

	private static void plataforma(Graphics2D g, int cx, int cy, int larg, int alt, Color cor) {
		g.setColor(new Color(0, 0, 0, 80));
		g.fill(new Ellipse2D.Float(cx - larg / 2f - 6, cy - alt / 2f + 10, larg + 12, alt));
		g.setPaint(new GradientPaint(0, cy - alt / 2f, mix(cor, Color.WHITE, 0.25f), 0, cy + alt / 2f, escurecer(cor, 0.35f)));
		g.fill(new Ellipse2D.Float(cx - larg / 2f, cy - alt / 2f, larg, alt));
		g.setColor(comAlpha(Color.WHITE, 70));
		g.setStroke(new BasicStroke(2f));
		g.draw(new Ellipse2D.Float(cx - larg / 2f + 1, cy - alt / 2f + 1, larg - 2, alt - 2));
		g.setColor(new Color(0, 0, 0, 60));
		g.fill(new Ellipse2D.Float(cx - larg / 2f + 26, cy - alt / 2f + 9, larg - 52, alt - 18));
	}

	// =====================================================================
	// Arena
	// =====================================================================
	private class Arena extends JPanel {
		private static final long serialVersionUID = 1L;

		Arena() {
			setOpaque(true);
			setBackground(FUNDO_A);
			setPreferredSize(new Dimension(LARGURA, ALTURA_ARENA));
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			if (fundo != null) {
				g.drawImage(fundo, 0, 0, null);
			}
			qualidade(g);

			// adversário (fundo, à direita) e seu Pokémon (frente, à esquerda)
			sprite(g, spriteOpo, 690, 244 + (int) (Math.sin(fase) * 5), tremerOpo, alfaOpo);
			sprite(g, spriteJog, 250, 430 + (int) (Math.sin(fase + 2) * 5), tremerJog, alfaJog);

			caixaHp(g, formaOponente, hpOpoMostrado, hpOpoMax, 22, 20, 340, timeOponente.size() - iOpo, timeOponente.size(), false);
			caixaHp(g, formaJogador, hpJogMostrado, hpJogMax, LARGURA - 362, 330, 340, meuTime.size() - iJog, meuTime.size(), true);

			for (double[] p : popups) {
				boolean doJog = p[0] == 1;
				int x = doJog ? 250 : 690, y = (doJog ? 250 : 80) - (int) (p[1] * 50);
				g.setFont(fonte(Font.BOLD, 34f));
				String t = "-" + (int) p[2];
				int tx = x - g.getFontMetrics().stringWidth(t) / 2;
				float a = (float) Math.max(0, 1 - p[1] * p[1]);
				g.setColor(new Color(0, 0, 0, (int) (200 * a)));
				g.drawString(t, tx + 2, y + 2);
				g.setColor(new Color(255, 90, 90, (int) (255 * a)));
				g.drawString(t, tx, y);
			}
			g.dispose();
		}

		private void sprite(Graphics2D g, BufferedImage img, int cx, int baseY, double tremor, double alfa) {
			if (img == null || alfa <= 0) {
				return;
			}
			int x = cx - img.getWidth() / 2 + (int) (Math.sin(tremor * 40) * tremor * 10);
			int y = baseY - img.getHeight();
			if (alfa < 1) {
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alfa));
				g.drawImage(img, x, y + (int) ((1 - alfa) * 30), null);
				g.setComposite(AlphaComposite.SrcOver);
			} else {
				g.drawImage(img, x, y, null);
			}
		}

		private void caixaHp(Graphics2D g, Pokemon p, double hp, int max, int x, int y, int larg, int vivos, int total,
				boolean mostrarNumeros) {
			int alt = 92;
			g.setColor(new Color(8, 10, 22, 200));
			g.fill(new RoundRectangle2D.Float(x, y, larg, alt, 22, 22));
			g.setPaint(new GradientPaint(x, y, comAlpha(Color.WHITE, 38), x, y + alt, comAlpha(Color.WHITE, 6)));
			g.setStroke(new BasicStroke(1.4f));
			g.draw(new RoundRectangle2D.Float(x + 0.7f, y + 0.7f, larg - 1.4f, alt - 1.4f, 22, 22));

			g.setFont(fonte(Font.BOLD, 20f));
			g.setColor(TEXTO);
			g.drawString(p.getNome(), x + 18, y + 30);
			g.setFont(fonte(Font.PLAIN, 13f));
			g.setColor(SUAVE);
			String nivel = "Nv. " + Atributos.NIVEL;
			g.drawString(nivel, x + larg - 18 - g.getFontMetrics().stringWidth(nivel), y + 29);

			double frac = Math.max(0, Math.min(1, hp / max));
			int bx = x + 18, by = y + 42, bw = larg - 36, bh = 14;
			g.setColor(new Color(255, 255, 255, 28));
			g.fill(new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh));
			Color cor = frac > 0.5 ? new Color(0x3F, 0xD2, 0x7A) : frac > 0.2 ? new Color(0xF4, 0xC4, 0x3B) : new Color(0xEF, 0x4B, 0x4B);
			if (frac > 0) {
				g.setPaint(new GradientPaint(0, by, mix(cor, Color.WHITE, 0.3f), 0, by + bh, escurecer(cor, 0.78f)));
				g.fill(new RoundRectangle2D.Float(bx, by, (float) Math.max(bh, bw * frac), bh, bh, bh));
			}
			g.setFont(fonte(Font.PLAIN, 12f));
			g.setColor(SUAVE);
			g.drawString(mostrarNumeros ? Math.round(hp) + " / " + max : Math.round(frac * 100) + "%", bx, by + bh + 16);

			// bolinhas do time (quantos ainda podem lutar)
			for (int i = 0; i < total; i++) {
				int px = x + larg - 18 - 12 - (total - 1 - i) * 17;
				g.setColor(i < total - vivos ? new Color(255, 255, 255, 40) : new Color(0xFF, 0xD1, 0x66));
				g.fill(new Ellipse2D.Float(px, by + bh + 6, 12, 12));
			}
		}
	}

	// =====================================================================
	// Caixa de mensagem (tamanho fixo; o texto quebra em linhas dentro dela)
	// =====================================================================
	private class CaixaMensagem extends JComponent {
		private static final long serialVersionUID = 1L;

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			qualidade(g);
			int w = getWidth(), h = getHeight();
			g.setColor(new Color(0x14, 0x19, 0x33));
			g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 22, 22));
			g.setColor(BORDA);
			g.setStroke(new BasicStroke(1.6f));
			g.draw(new RoundRectangle2D.Float(0.8f, 0.8f, w - 2.6f, h - 2.6f, 22, 22));
			g.setColor(new Color(0xFF, 0xB8, 0x3D));
			g.fill(new RoundRectangle2D.Float(10, 16, 4, h - 32, 4, 4));

			g.setFont(fonte(Font.BOLD, 19f));
			g.setColor(TEXTO);
			FontMetrics fm = g.getFontMetrics();
			List<String> linhas = quebrar(mensagem, fm, w - 56);
			int total = linhas.size() * (fm.getHeight() + 2);
			int y = (h - total) / 2 + fm.getAscent();
			for (String l : linhas) {
				g.drawString(l, 28, y);
				y += fm.getHeight() + 2;
			}
			g.dispose();
		}

		private List<String> quebrar(String texto, FontMetrics fm, int largura) {
			List<String> linhas = new ArrayList<>();
			StringBuilder atual = new StringBuilder();
			for (String palavra : texto.split(" ")) {
				String tentativa = atual.length() == 0 ? palavra : atual + " " + palavra;
				if (fm.stringWidth(tentativa) > largura && atual.length() > 0) {
					linhas.add(atual.toString());
					atual = new StringBuilder(palavra);
				} else {
					atual = new StringBuilder(tentativa);
				}
			}
			if (atual.length() > 0) {
				linhas.add(atual.toString());
			}
			return linhas;
		}
	}

	// =====================================================================
	// Botões
	// =====================================================================
	private class BotaoGolpe extends JButton {
		private static final long serialVersionUID = 1L;
		private final int posicao;
		private transient Golpe golpe;

		BotaoGolpe(int posicao) {
			this.posicao = posicao;
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setFocusable(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addActionListener(e -> {
				if (golpe != null) {
					jogar(golpe);
				}
			});
		}

		void definir(Golpe g) {
			this.golpe = g;
			if (g == null) {
				setToolTipText(null);
			} else {
				double m = dados.multiplicadorDeTipo(g.getTipo(), formaOponente);
				String efeito = m == 0 ? " · não afeta o adversário" : m > 1 ? " · super efetivo" : m < 1 ? " · pouco efetivo" : "";
				setToolTipText("Precisão " + g.getPrecisao() + "%" + (g.getPrioridade() > 0 ? " · age primeiro" : "") + efeito
						+ "  (tecla " + (posicao + 1) + ")");
			}
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			qualidade(g);
			int w = getWidth(), h = getHeight();
			if (golpe == null) {
				g.setColor(new Color(255, 255, 255, 14));
				g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 20, 20));
				g.dispose();
				return;
			}
			Color base = corDoTipo(golpe.getTipo());
			boolean ativo = isEnabled();
			Color topo = ativo ? mix(base, Color.WHITE, getModel().isRollover() ? 0.28f : 0.12f) : escurecer(base, 0.4f);
			Color fundoBaixo = ativo ? escurecer(base, getModel().isPressed() ? 0.55f : 0.72f) : escurecer(base, 0.28f);
			g.setPaint(new GradientPaint(0, 0, topo, 0, h, fundoBaixo));
			g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 20, 20));
			g.setColor(comAlpha(Color.WHITE, ativo ? 70 : 25));
			g.setStroke(new BasicStroke(1.3f));
			g.draw(new RoundRectangle2D.Float(0.7f, 0.7f, w - 2.4f, h - 2.4f, 20, 20));

			Color texto = ativo ? Color.WHITE : new Color(255, 255, 255, 120);
			g.setColor(texto);
			g.setFont(fonte(Font.BOLD, 18f));
			g.drawString(golpe.getNome(), 16, h / 2 - 4);
			g.setFont(fonte(Font.PLAIN, 12f));
			g.setColor(ativo ? new Color(255, 255, 255, 215) : new Color(255, 255, 255, 100));
			g.drawString(golpe.getTipo() + " · " + (golpe.isFisico() ? "Físico" : "Especial"), 16, h / 2 + 15);
			// poder à direita
			g.setFont(fonte(Font.BOLD, 20f));
			String poder = String.valueOf(golpe.getPoder());
			g.setColor(texto);
			g.drawString(poder, w - 16 - g.getFontMetrics().stringWidth(poder), h / 2 - 2);
			g.setFont(fonte(Font.PLAIN, 11f));
			g.setColor(ativo ? new Color(255, 255, 255, 190) : new Color(255, 255, 255, 90));
			String rot = "PODER";
			g.drawString(rot, w - 16 - g.getFontMetrics().stringWidth(rot), h / 2 + 14);
			g.dispose();
		}
	}

	private class BotaoMega extends JButton {
		private static final long serialVersionUID = 1L;
		private final transient Pokemon mega;

		BotaoMega(Pokemon mega, boolean varias) {
			super(varias ? mega.getNome() : "Mega Evoluir");
			this.mega = mega;
			setToolTipText("Mega Evolui antes de atacar no próximo golpe (uma vez por luta, tecla M)");
			setFont(fonte(Font.BOLD, 14f));
			setPreferredSize(new Dimension(varias ? 186 : 150, 42));
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setFocusable(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			qualidade(g);
			boolean armado = megaEscolhida == mega;
			Color base = armado ? new Color(0xFF, 0xB8, 0x3D) : PAINEL;
			if (getModel().isRollover() && !armado && isEnabled()) {
				base = mix(PAINEL, Color.WHITE, 0.10f);
			}
			g.setColor(isEnabled() ? base : escurecer(base, 0.6f));
			g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight()));
			g.setColor(armado ? new Color(0xFF, 0xE0, 0x8A) : BORDA);
			g.setStroke(new BasicStroke(1.4f));
			g.draw(new RoundRectangle2D.Float(0.7f, 0.7f, getWidth() - 2.4f, getHeight() - 2.4f, getHeight(), getHeight()));
			g.setFont(getFont());
			FontMetrics fm = g.getFontMetrics();
			String txt = (armado ? "✦ " : "") + getText();
			g.setColor(armado ? new Color(0x2A, 0x1A, 0x00) : TEXTO);
			g.drawString(txt, (getWidth() - fm.stringWidth(txt)) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
			g.dispose();
		}
	}
}
