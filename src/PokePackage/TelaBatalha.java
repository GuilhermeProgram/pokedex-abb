package PokePackage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static PokePackage.PokemonViewer.*;

/**
 * Janela da batalha: arena com os dois Pokémon e barras de HP, quatro botões de golpe e o relato do turno.
 * A lógica fica em {@link Batalha}; aqui só desenhamos e passamos o golpe clicado.
 */
public class TelaBatalha extends JDialog {

	private static final long serialVersionUID = 1L;
	private static final Pattern PERDEU = Pattern.compile("^(.+) perdeu (\\d+) de HP\\.$");

	private final transient Batalha batalha;
	private final transient DadosBatalha dados;
	private final transient List<Golpe> golpes;

	// HP mostrado (anima até o alvo) e HP "alvo" (atualizado linha a linha do relato)
	private double hpJogadorMostrado, hpOponenteMostrado;
	private int hpJogadorAlvo, hpOponenteAlvo;

	private final Arena arena = new Arena();
	private final JTextArea log = new JTextArea();
	private final List<BotaoGolpe> botoes = new ArrayList<>();
	private final BotaoTexto botaoFechar = new BotaoTexto("Fechar", "Voltar para a Pokédex");
	private javax.swing.Timer relatoTimer;
	private double fase = 0;

	public TelaBatalha(Frame dono, DadosBatalha dados, Pokemon jogador, Pokemon oponente) {
		super(dono, "Batalha: " + jogador.getNome() + " x " + oponente.getNome(), true);
		this.dados = dados;
		this.batalha = new Batalha(dados, jogador, oponente, new Random());
		this.golpes = dados.golpesDe(jogador.getNumeroDex());
		hpJogadorMostrado = hpJogadorAlvo = batalha.getHpJogador();
		hpOponenteMostrado = hpOponenteAlvo = batalha.getHpOponente();

		JPanel raiz = new JPanel(new BorderLayout(0, 10));
		raiz.setBackground(FUNDO_A);
		raiz.setBorder(new EmptyBorder(12, 12, 12, 12));
		raiz.add(arena, BorderLayout.CENTER);
		raiz.add(montarBaixo(), BorderLayout.SOUTH);
		setContentPane(raiz);
		setSize(860, 720);
		setLocationRelativeTo(dono);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		// um único relógio para animar barras e flutuação; para quando a janela fecha
		javax.swing.Timer relogio = new javax.swing.Timer(16, e -> passo());
		relogio.start();
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosed(java.awt.event.WindowEvent e) {
				relogio.stop();
				if (relatoTimer != null) {
					relatoTimer.stop();
				}
			}
		});
		escrever("Um " + oponente.getNome() + " selvagem apareceu! Escolha um golpe.");
	}

	private JComponent montarBaixo() {
		JPanel baixo = new JPanel(new BorderLayout(12, 0));
		baixo.setOpaque(false);

		JPanel grade = new JPanel(new GridLayout(2, 2, 10, 10));
		grade.setOpaque(false);
		for (Golpe g : golpes) {
			BotaoGolpe b = new BotaoGolpe(g);
			b.addActionListener(e -> jogar(g));
			botoes.add(b);
			grade.add(b);
		}
		grade.setPreferredSize(new Dimension(470, 130));
		baixo.add(grade, BorderLayout.WEST);

		log.setEditable(false);
		log.setLineWrap(true);
		log.setWrapStyleWord(true);
		log.setBackground(PAINEL);
		log.setForeground(TEXTO);
		log.setFont(fonte(Font.PLAIN, 14f));
		log.setBorder(new EmptyBorder(8, 10, 8, 10));
		JScrollPane rolagem = new JScrollPane(log);
		rolagem.setBorder(BorderFactory.createLineBorder(BORDA));
		baixo.add(rolagem, BorderLayout.CENTER);

		botaoFechar.addActionListener(e -> dispose());
		botaoFechar.setVisible(false);
		JPanel sul = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		sul.setOpaque(false);
		sul.add(botaoFechar);
		JPanel envoltorio = new JPanel(new BorderLayout(0, 8));
		envoltorio.setOpaque(false);
		envoltorio.add(baixo, BorderLayout.CENTER);
		envoltorio.add(sul, BorderLayout.SOUTH);
		return envoltorio;
	}

	private void escrever(String linha) {
		log.append((log.getText().isEmpty() ? "" : "\n") + linha);
		log.setCaretPosition(log.getDocument().getLength());
	}

	private void habilitar(boolean sim) {
		for (BotaoGolpe b : botoes) {
			b.setEnabled(sim);
		}
	}

	/** O jogador clicou num golpe: joga o turno e mostra o relato linha a linha. */
	private void jogar(Golpe g) {
		if (batalha.terminou() || (relatoTimer != null && relatoTimer.isRunning())) {
			return;
		}
		habilitar(false);
		escrever("");
		escrever("— Turno " + (batalha.getNumeroTurno() + 1) + " —");
		final List<String> relato = batalha.turno(g);
		final int[] i = { 0 };
		relatoTimer = new javax.swing.Timer(650, null);
		relatoTimer.addActionListener(e -> {
			if (i[0] >= relato.size()) {
				relatoTimer.stop();
				if (batalha.terminou()) {
					botaoFechar.setVisible(true);
				} else {
					habilitar(true);
				}
				return;
			}
			String linha = relato.get(i[0]++);
			escrever(linha);
			Matcher m = PERDEU.matcher(linha);
			if (m.matches()) { // a barra só cai quando a linha do dano aparece
				int dano = Integer.parseInt(m.group(2));
				if (m.group(1).equals(batalha.getOponente().getNome())) {
					hpOponenteAlvo = Math.max(0, hpOponenteAlvo - dano);
				} else {
					hpJogadorAlvo = Math.max(0, hpJogadorAlvo - dano);
				}
			}
		});
		relatoTimer.setInitialDelay(300);
		relatoTimer.start();
	}

	private void passo() {
		fase += 0.04;
		hpJogadorMostrado += (hpJogadorAlvo - hpJogadorMostrado) * 0.12;
		hpOponenteMostrado += (hpOponenteAlvo - hpOponenteMostrado) * 0.12;
		arena.repaint();
	}

	// =====================================================================
	// Arena: fundo, os dois Pokémon e as barras de HP
	// =====================================================================
	private class Arena extends JPanel {
		private static final long serialVersionUID = 1L;

		Arena() {
			setOpaque(false);
			setPreferredSize(new Dimension(800, 420));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			int w = getWidth(), h = getHeight();
			Pokemon jog = batalha.getJogador(), opo = batalha.getOponente();
			Color cj = corDoTipo(jog.getTipo1()), co = corDoTipo(opo.getTipo1());

			g2.setPaint(new GradientPaint(0, 0, escurecer(co, 0.35f), w, h, escurecer(cj, 0.35f)));
			g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 26, 26));

			// chãos
			g2.setColor(new Color(0, 0, 0, 70));
			g2.fillOval((int) (w * 0.58), (int) (h * 0.46), (int) (w * 0.34), 40);
			g2.fillOval((int) (w * 0.06), (int) (h * 0.90) - 20, (int) (w * 0.36), 44);

			int tam = Math.min(w / 3, (int) (h * 0.55));
			desenharPokemon(g2, opo.getNumeroDex(), (int) (w * 0.60), (int) (h * 0.46) - tam + 30 + (int) (Math.sin(fase) * 5), tam,
					false);
			desenharPokemon(g2, jog.getNumeroDex(), (int) (w * 0.08), (int) (h * 0.90) - tam + 10 + (int) (Math.sin(fase + 2) * 5),
					(int) (tam * 1.15), true);

			caixaHp(g2, opo, hpOponenteMostrado, batalha.getHpMaxOponente(), 18, 18, Math.min(330, w / 2 - 30));
			caixaHp(g2, jog, hpJogadorMostrado, batalha.getHpMaxJogador(), w - Math.min(330, w / 2 - 30) - 18,
					(int) (h * 0.62), Math.min(330, w / 2 - 30));
			g2.dispose();
		}

		private void desenharPokemon(Graphics2D g2, int dex, int x, int y, int tam, boolean costas) {
			BufferedImage img = Imagens.grande(dex);
			if (img == null) {
				return;
			}
			double escala = Math.min((double) tam / img.getWidth(), (double) tam / img.getHeight());
			int dw = (int) (img.getWidth() * escala), dh = (int) (img.getHeight() * escala);
			if (costas) { // o seu Pokémon olha para o adversário (espelha)
				g2.drawImage(img, x + dw, y, -dw, dh, null);
			} else {
				g2.drawImage(img, x, y, dw, dh, null);
			}
		}

		private void caixaHp(Graphics2D g2, Pokemon p, double hp, int max, int x, int y, int larg) {
			g2.setColor(new Color(10, 12, 24, 215));
			g2.fill(new RoundRectangle2D.Float(x, y, larg, 72, 18, 18));
			g2.setColor(BORDA);
			g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, larg - 1, 71, 18, 18));
			g2.setFont(fonte(Font.BOLD, 18f));
			g2.setColor(TEXTO);
			g2.drawString(p.getNome(), x + 16, y + 28);
			g2.setFont(fonte(Font.PLAIN, 13f));
			g2.setColor(SUAVE);
			String nivel = "Nv. " + Atributos.NIVEL;
			g2.drawString(nivel, x + larg - 16 - g2.getFontMetrics().stringWidth(nivel), y + 28);

			double frac = Math.max(0, Math.min(1, hp / max));
			int bx = x + 16, by = y + 38, bw = larg - 32, bh = 12;
			g2.setColor(new Color(255, 255, 255, 30));
			g2.fill(new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh));
			Color cor = frac > 0.5 ? new Color(0x4C, 0xD9, 0x7B) : frac > 0.2 ? new Color(0xF2, 0xC9, 0x4C) : new Color(0xE5, 0x4B, 0x4B);
			g2.setColor(cor);
			g2.fill(new RoundRectangle2D.Float(bx, by, (float) (bw * frac), bh, bh, bh));
			g2.setFont(fonte(Font.PLAIN, 12f));
			g2.setColor(SUAVE);
			g2.drawString(Math.round(hp) + " / " + max, bx, by + bh + 14);
		}
	}

	// =====================================================================
	// Botão de golpe, na cor do tipo
	// =====================================================================
	private class BotaoGolpe extends JButton {
		private static final long serialVersionUID = 1L;
		private final transient Golpe golpe;

		BotaoGolpe(Golpe golpe) {
			this.golpe = golpe;
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setFocusable(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText("Precisão " + golpe.getPrecisao() + "%"
					+ (golpe.getPrioridade() > 0 ? " · age primeiro" : ""));
			// dica de efetividade contra o oponente
			double m = dados.multiplicadorDeTipo(golpe.getTipo(), batalha.getOponente());
			if (m == 0) {
				setToolTipText(getToolTipText() + " · não afeta o adversário");
			} else if (m > 1) {
				setToolTipText(getToolTipText() + " · super efetivo");
			} else if (m < 1) {
				setToolTipText(getToolTipText() + " · pouco efetivo");
			}
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			Color base = corDoTipo(golpe.getTipo());
			if (!isEnabled()) {
				base = escurecer(base, 0.45f);
			} else if (getModel().isPressed()) {
				base = escurecer(base, 0.8f);
			} else if (getModel().isRollover()) {
				base = mix(base, Color.WHITE, 0.15f);
			}
			g2.setColor(base);
			g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 18, 18));
			Color texto = clara(base) ? new Color(0x14, 0x17, 0x2B) : Color.WHITE;
			g2.setColor(isEnabled() ? texto : comAlpha(texto, 150));
			g2.setFont(fonte(Font.BOLD, 16f));
			g2.drawString(golpe.getNome(), 14, getHeight() / 2 - 2);
			g2.setFont(fonte(Font.PLAIN, 12f));
			g2.drawString(golpe.getTipo() + " · " + (golpe.isFisico() ? "Físico" : "Especial") + " · Poder " + golpe.getPoder(), 14,
					getHeight() / 2 + 18);
			g2.dispose();
		}
	}
}
