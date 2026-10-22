package PokePackage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static PokePackage.PokemonViewer.*;

/**
 * Janela da batalha em equipe, no estilo dos jogos: cenário com sprites pixelados (o seu de costas, o do
 * adversário de frente), caixas de HP, caixa de texto e menu Lutar / Pokémon / Mega / Fugir. À direita fica o
 * seu time, e dá para trocar de Pokémon no meio da luta (a troca gasta o turno).
 *
 * A lógica de cada confronto fica em {@link Batalha}; aqui ficam o desenho, o menu e a fila de mensagens.
 * Tamanho fixo e cenário em cache, para a animação ficar leve.
 */
public class TelaBatalha extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final int MARGEM = 12;
	private static final int LARG_ARENA = 660, ALT_ARENA = 400, ALT_BAIXO = 170, LARG_LATERAL = 300, ALT_TOPO = 52;
	private static final Pattern PERDEU = Pattern.compile("^(.+) perdeu (\\d+) de HP\\.$");
	private static final Pattern GANHOU = Pattern.compile("^(.+) recuperou (\\d+) de HP\\.$");
	private static final Pattern MUDOU_STATUS = Pattern.compile(".*(se queimou|ficou paralisado|foi envenenado|adormeceu|congelou|acordou|descongelou)[!.].*");
	private static final Pattern EVOLUIU = Pattern.compile("^(.+) Mega Evoluiu para (.+)!$");

	// cores no estilo dos jogos
	private static final Color MOLDURA = new Color(0x2B, 0x3A, 0x63);
	private static final Color CAIXA_CLARA = new Color(0xF6, 0xF1, 0xDD);
	private static final Color TINTA = new Color(0x26, 0x2F, 0x4A);
	private static final Color C_LUTAR = new Color(0xE8, 0x55, 0x3B), C_POKEMON = new Color(0x3F, 0xAE, 0x5C),
			C_MEGA = new Color(0xF0, 0xA9, 0x2B), C_FUGIR = new Color(0x3F, 0x7B, 0xD9);

	private enum Modo { MENU, GOLPES, TIME_ESCOLHA, TIME_FORCADA }

	private final transient DadosBatalha dados;
	private final transient Random sorte = new Random();
	private final List<Pokemon> meuTime, timeOponente;
	private final int[] hpMeu; // HP atual de cada um do seu time (0 = ainda com a vida cheia)
	private final boolean[] caiu;
	private final EstadoPokemon[] estMeu, estOpo; // condição e PP de cada Pokémon dos dois times
	private Status stJog = Status.NENHUM, stOpo = Status.NENHUM; // condição mostrada na caixa de HP
	private int iJog = 0, iOpo = 0;
	private int turnoTotal = 0;
	private transient Batalha batalha;
	private Modo modo = Modo.MENU;

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
	private transient Pokemon pendOpo; // adversário em campo quando o seu Pokémon desmaiou
	private int pendHpOpo;

	private final Map<Integer, BufferedImage> miniaturas = new HashMap<>();

	private final Topo topo = new Topo();
	private final Arena arena = new Arena();
	private final PainelBaixo baixo = new PainelBaixo();
	private final PainelTime lateral = new PainelTime();
	private final javax.swing.Timer relogio;
	private int assinaturaUI = 0, assinaturaArena = 0;

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
		this.hpMeu = new int[meuTime.size()];
		this.caiu = new boolean[meuTime.size()];
		this.estMeu = new EstadoPokemon[meuTime.size()];
		for (int i = 0; i < estMeu.length; i++) {
			estMeu[i] = new EstadoPokemon(dados.golpesDe(meuTime.get(i).getNumeroDex()));
		}
		this.estOpo = new EstadoPokemon[timeOponente.size()];
		for (int i = 0; i < estOpo.length; i++) {
			estOpo[i] = new EstadoPokemon(dados.golpesDe(timeOponente.get(i).getNumeroDex()));
		}
		for (Pokemon p : meuTime) {
			nomesJogador.add(p.getNome());
			for (Pokemon m : dados.megasDe(p.getNumeroDex())) {
				nomesJogador.add(m.getNome());
			}
		}

		JPanel raiz = new JPanel(new BorderLayout());
		raiz.setBackground(FUNDO_A);
		topo.setPreferredSize(new Dimension(MARGEM * 3 + LARG_ARENA + LARG_LATERAL, ALT_TOPO));
		raiz.add(topo, BorderLayout.NORTH);

		JPanel esquerda = new JPanel(new BorderLayout(0, 0));
		esquerda.setOpaque(false);
		esquerda.add(arena, BorderLayout.CENTER);
		esquerda.add(baixo, BorderLayout.SOUTH);
		JPanel centro = new JPanel(new BorderLayout(MARGEM, 0));
		centro.setOpaque(false);
		centro.setBorder(BorderFactory.createEmptyBorder(0, MARGEM, MARGEM, MARGEM));
		centro.add(esquerda, BorderLayout.CENTER);
		centro.add(lateral, BorderLayout.EAST);
		raiz.add(centro, BorderLayout.CENTER);
		setContentPane(raiz);
		setResizable(false);
		pack();
		setLocationRelativeTo(dono);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		configurarTeclas(raiz);

		novaBatalha(this.meuTime.get(0), 0, this.timeOponente.get(0), 0);
		mensagem = "O adversário enviou " + formaOponente.getNome() + "! Vai, " + formaJogador.getNome() + "!";
		fila.add(new Evento("O que " + formaJogador.getNome() + " fará?", null, 0));
		proximoEm = System.currentTimeMillis() + 1700;

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
	// Teclas
	// =====================================================================
	private void configurarTeclas(JComponent raiz) {
		InputMap im = raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = raiz.getActionMap();
		for (int i = 0; i < 4; i++) {
			final int n = i;
			im.put(KeyStroke.getKeyStroke("" + (i + 1)), "opcao" + i);
			am.put("opcao" + i, new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					baixo.acionar(n);
				}
			});
		}
		im.put(KeyStroke.getKeyStroke("M"), "mega");
		am.put("mega", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				alternarMega();
			}
		});
		im.put(KeyStroke.getKeyStroke("ESCAPE"), "voltar");
		am.put("voltar", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (fim) {
					dispose();
				} else if (modo == Modo.GOLPES || modo == Modo.TIME_ESCOLHA) {
					modo = Modo.MENU;
				}
			}
		});
	}

	// =====================================================================
	// Controle da luta
	// =====================================================================

	/** Começa (ou continua) um confronto: quem já estava ferido entra com o HP que tinha. */
	private void novaBatalha(Pokemon jogador, int hpJogador, Pokemon oponente, int hpOponente) {
		batalha = new Batalha(dados, jogador, hpJogador, estMeu[iJog], oponente, hpOponente, estOpo[iOpo], sorte);
		stJog = estMeu[iJog].getStatus();
		stOpo = estOpo[iOpo].getStatus();
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
		modo = Modo.MENU;
		atualizarSprites();
		atualizarFundo();
	}

	private boolean ocupado() {
		return !fila.isEmpty() || System.currentTimeMillis() < proximoEm;
	}

	/** Dá para o jogador agir agora (nada na fila e a luta não acabou). */
	private boolean liberado() {
		return !fim && !ocupado();
	}

	/** Os golpes do menu; se não sobrou PP em nenhum, só o Esforço. */
	private List<Golpe> golpesAtuais() {
		if (estMeu[iJog].semPpNenhum() && dados.golpeEsforco() != null) {
			List<Golpe> so = new ArrayList<>();
			so.add(dados.golpeEsforco());
			return so;
		}
		return dados.golpesDe(formaJogador.getNumeroDex());
	}

	private boolean usavel(Golpe g) {
		return g == dados.golpeEsforco() || estMeu[iJog].temPp(g);
	}

	private List<Pokemon> opcoesMega() {
		return (megaUsadaNaLuta || fim || batalha == null) ? new ArrayList<>() : batalha.megasDisponiveisDoJogador();
	}

	/** Liga/desliga a Mega Evolução do próximo golpe (se houver mais de uma forma, passa para a próxima). */
	private void alternarMega() {
		if (!liberado() || (modo != Modo.MENU && modo != Modo.GOLPES)) {
			return;
		}
		List<Pokemon> opcoes = opcoesMega();
		if (opcoes.isEmpty()) {
			return;
		}
		int i = megaEscolhida == null ? -1 : opcoes.indexOf(megaEscolhida);
		megaEscolhida = i + 1 < opcoes.size() ? opcoes.get(i + 1) : null;
	}

	/** O jogador escolheu um golpe: joga o turno e enfileira o relato (uma linha por vez na tela). */
	private void jogar(Golpe g) {
		if (!liberado() || batalha.terminou()) {
			return;
		}
		Pokemon mega = megaEscolhida;
		megaEscolhida = null;
		if (mega != null) {
			megaUsadaNaLuta = true;
		}
		modo = Modo.MENU;
		executarTurno(g, mega);
	}

	private void executarTurno(Golpe g, Pokemon mega) {
		final Batalha atual = batalha;
		turnoTotal++;
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

	/** Troca por vontade própria: o Pokémon que sai não ataca, e o adversário ataca o que entra. */
	private void trocarVoluntaria(int k) {
		if (!liberado() || k == iJog || caiu[k]) {
			return;
		}
		final Batalha antiga = batalha;
		hpMeu[iJog] = Math.max(1, antiga.getHpJogador());
		final String sai = meuTime.get(iJog).getNome();
		final Pokemon novo = meuTime.get(k);
		final Pokemon opo = antiga.getOponente();
		final int hpOpo = antiga.getHpOponente();
		megaEscolhida = null;
		modo = Modo.MENU;
		fila.add(new Evento("Volta, " + sai + "!", () -> caindoJog = true, 900));
		fila.add(new Evento("Vai, " + novo.getNome() + "!", () -> {
			iJog = k;
			novaBatalha(novo, hpMeu[k], opo, hpOpo);
			executarTurno(null, null);
		}, 900));
	}

	/** Troca obrigatória (o seu Pokémon desmaiou): entra o escolhido, sem gastar turno. */
	private void trocarForcada(int k) {
		if (!liberado() || caiu[k]) {
			return;
		}
		final Pokemon novo = meuTime.get(k);
		final Pokemon opo = pendOpo;
		final int hpOpo = pendHpOpo;
		modo = Modo.MENU;
		fila.add(new Evento("Vai, " + novo.getNome() + "!", () -> {
			iJog = k;
			novaBatalha(novo, hpMeu[k], opo, hpOpo);
			mensagem = "O que " + formaJogador.getNome() + " fará?";
		}, 900));
	}

	private void fugir() {
		if (!liberado()) {
			return;
		}
		int r = JOptionPane.showConfirmDialog(this, "Fugir da luta? Isso conta como derrota.", "Fugir", JOptionPane.YES_NO_OPTION);
		if (r == JOptionPane.YES_OPTION) {
			modo = Modo.MENU;
			fila.add(new Evento("Você fugiu da luta...", null, 1200));
			terminar("Você desistiu da luta.");
		}
	}

	/** Efeitos visuais ligados à linha do relato que acabou de aparecer. */
	private void aplicarLinha(String linha, Batalha b) {
		Matcher ev = EVOLUIU.matcher(linha);
		if (ev.matches()) {
			if (nomesJogador.contains(ev.group(2))) {
				formaJogador = b.getJogador();
			} else {
				formaOponente = b.getOponente();
			}
			atualizarSprites();
			atualizarFundo();
			return;
		}
		if (MUDOU_STATUS.matcher(linha).matches()) {
			stJog = b.getEstadoJogador().getStatus();
			stOpo = b.getEstadoOponente().getStatus();
		}
		Matcher gan = GANHOU.matcher(linha);
		if (gan.matches()) {
			int cura = Integer.parseInt(gan.group(2));
			if (nomesJogador.contains(gan.group(1))) {
				hpJogAlvo = Math.min(hpJogMax, hpJogAlvo + cura);
			} else {
				hpOpoAlvo = Math.min(hpOpoMax, hpOpoAlvo + cura);
			}
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

	/** Acabou de mostrar o turno: ou volta ao menu, ou entra o próximo Pokémon de quem caiu. */
	private void fimDoTurno(Batalha b) {
		if (b.getHpJogador() > 0) {
			hpMeu[iJog] = b.getHpJogador();
		}
		if (!b.terminou()) {
			modo = Modo.MENU;
			mensagem = "O que " + formaJogador.getNome() + " fará?";
			return;
		}
		boolean jogadorCaiu = b.getHpJogador() <= 0;
		final Pokemon sobrevivente = jogadorCaiu ? b.getOponente() : b.getJogador();
		final int hpSobrevivente = jogadorCaiu ? b.getHpOponente() : b.getHpJogador();
		fila.add(new Evento((jogadorCaiu ? formaJogador : formaOponente).getNome() + " desmaiou!", null, 1500));
		if (jogadorCaiu) {
			caiu[iJog] = true;
			List<Integer> vivos = new ArrayList<>();
			for (int i = 0; i < meuTime.size(); i++) {
				if (!caiu[i]) {
					vivos.add(i);
				}
			}
			if (vivos.isEmpty()) {
				terminar("Todo o seu time desmaiou... Você perdeu a luta!");
			} else if (vivos.size() == 1) {
				final int k = vivos.get(0);
				final Pokemon proximo = meuTime.get(k);
				fila.add(new Evento("Vai, " + proximo.getNome() + "!", () -> {
					iJog = k;
					novaBatalha(proximo, hpMeu[k], sobrevivente, hpSobrevivente);
					mensagem = "O que " + formaJogador.getNome() + " fará?";
				}, 900));
			} else {
				pendOpo = sobrevivente;
				pendHpOpo = hpSobrevivente;
				fila.add(new Evento("Escolha o próximo Pokémon do seu time.", () -> modo = Modo.TIME_FORCADA, 0));
			}
		} else {
			iOpo++;
			if (iOpo >= timeOponente.size()) {
				terminar("O time adversário foi derrotado. Você venceu a luta!");
			} else {
				final Pokemon proximo = timeOponente.get(iOpo);
				fila.add(new Evento("O adversário enviou " + proximo.getNome() + "!",
						() -> {
							novaBatalha(sobrevivente, hpSobrevivente, proximo, 0);
							mensagem = "O que " + formaJogador.getNome() + " fará?";
						}, 900));
			}
		}
	}

	private void terminar(String texto) {
		fila.add(new Evento(texto, () -> fim = true, 100));
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
			}
			if (e.acao != null) {
				e.acao.run();
			}
			proximoEm = agora + e.ms;
		}

		// repinta só o que mudou
		int bob = (int) Math.round(Math.sin(fase) * 3);
		int ua = java.util.Objects.hash(modo, liberado(), mensagem, megaEscolhida, iJog, iOpo, fim, Math.round(hpJogMostrado),
				Math.round(hpOpoMostrado), formaJogador, caiu[iJog], turnoTotal, stJog, stOpo);
		if (ua != assinaturaUI) {
			assinaturaUI = ua;
			baixo.repaint();
			lateral.repaint();
			topo.repaint();
		}
		int sa = java.util.Objects.hash(bob, Math.round(hpJogMostrado), Math.round(hpOpoMostrado), tremerJog > 0, tremerOpo > 0,
				Math.round(alfaJog * 20), Math.round(alfaOpo * 20), popups.size(), popups.isEmpty() ? 0 : Math.round(popups.get(0)[1] * 30),
				formaJogador, formaOponente, iJog, iOpo, stJog, stOpo);
		if (sa != assinaturaArena) {
			assinaturaArena = sa;
			arena.repaint();
		}
	}

	// =====================================================================
	// Sprites e cenário (desenhados uma vez, só copiados a cada quadro)
	// =====================================================================
	private void atualizarSprites() {
		spriteJog = pixelPronto(formaJogador, true, 220);
		spriteOpo = pixelPronto(formaOponente, false, 170);
	}

	/** Recorta a borda transparente do sprite (os sprites 96x96 têm tamanhos diferentes dentro da imagem). */
	private static BufferedImage recortar(BufferedImage img) {
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
		return maxX < 0 ? img : img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}

	/** Sprite pixelado ampliado com escala inteira (pixel nítido). De costas, se o arquivo faltar, espelha o da frente. */
	private static BufferedImage pixelPronto(Pokemon p, boolean costas, int alvo) {
		int dex = p.getNumeroDex();
		BufferedImage img = costas ? Imagens.pixelCostas(dex) : Imagens.pixel(dex);
		boolean espelhar = false;
		if (img == null && costas) {
			img = Imagens.pixel(dex);
			espelhar = true;
		}
		if (img == null) { // sem sprite pixel: usa a arte grande
			BufferedImage arte = Imagens.grande(dex);
			if (arte == null) {
				return null;
			}
			double e = Math.min((double) alvo / arte.getWidth(), (double) alvo / arte.getHeight());
			BufferedImage s = new BufferedImage(Math.max(1, (int) (arte.getWidth() * e)), Math.max(1, (int) (arte.getHeight() * e)),
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = s.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(arte, 0, 0, s.getWidth(), s.getHeight(), null);
			g.dispose();
			return s;
		}
		BufferedImage rec = recortar(img);
		int esc = Math.max(1, alvo / Math.max(rec.getWidth(), rec.getHeight()));
		int w = rec.getWidth() * esc, h = rec.getHeight() * esc;
		BufferedImage saida = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = saida.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		if (espelhar) {
			g.drawImage(rec, w, 0, -w, h, null);
		} else {
			g.drawImage(rec, 0, 0, w, h, null);
		}
		g.dispose();
		return saida;
	}

	/** Ícone pequeno (do time na lateral). */
	private BufferedImage miniatura(Pokemon p, int lado) {
		int chave = p.getNumeroDex() * 1000 + lado;
		BufferedImage m = miniaturas.get(chave);
		if (m == null) {
			BufferedImage img = Imagens.pixel(p.getNumeroDex());
			if (img == null) {
				img = Imagens.miniatura(p.getNumeroDex());
			}
			if (img != null) {
				BufferedImage rec = recortar(img);
				double e = (double) lado / Math.max(rec.getWidth(), rec.getHeight());
				e = e >= 1 ? Math.floor(e) : e;
				int w = Math.max(1, (int) Math.round(rec.getWidth() * e)), h = Math.max(1, (int) Math.round(rec.getHeight() * e));
				m = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = m.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						e >= 1 ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(rec, 0, 0, w, h, null);
				g.dispose();
			}
			if (m != null) {
				miniaturas.put(chave, m);
			}
		}
		return m;
	}

	/** Cenário: céu, colinas, campo de grama e as duas plataformas (a cor do campo muda um pouco com o tipo). */
	private void atualizarFundo() {
		BufferedImage f = new BufferedImage(LARG_ARENA, ALT_ARENA, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = f.createGraphics();
		qualidade(g);
		// céu
		g.setPaint(new GradientPaint(0, 0, new Color(0x7F, 0xC8, 0xF8), 0, 190, new Color(0xE3, 0xF5, 0xFF)));
		g.fillRect(0, 0, LARG_ARENA, 200);
		g.setColor(new Color(255, 255, 255, 190));
		g.fill(new Ellipse2D.Float(90, 40, 120, 34));
		g.fill(new Ellipse2D.Float(130, 28, 80, 34));
		g.fill(new Ellipse2D.Float(430, 20, 140, 36));
		g.fill(new Ellipse2D.Float(480, 8, 80, 34));
		// colinas ao fundo
		g.setColor(new Color(0x6F, 0xB8, 0x6A));
		g.fill(new Ellipse2D.Float(-80, 130, 420, 160));
		g.setColor(new Color(0x5F, 0xA9, 0x5E));
		g.fill(new Ellipse2D.Float(300, 120, 520, 180));
		// campo
		Color ca = mix(new Color(0x86, 0xD0, 0x68), corDoTipo(formaOponente.getTipo1()), 0.10f);
		Color cb = mix(new Color(0x4F, 0x9E, 0x47), corDoTipo(formaJogador.getTipo1()), 0.10f);
		g.setPaint(new GradientPaint(0, 170, ca, 0, ALT_ARENA, cb));
		g.fill(new Ellipse2D.Float(-200, 170, LARG_ARENA + 400, 520));
		g.setColor(new Color(255, 255, 255, 14));
		for (int y = 200; y < ALT_ARENA; y += 34) {
			g.fillRect(0, y, LARG_ARENA, 16);
		}
		// plataformas
		plataforma(g, 470, 212, 240, 56);
		plataforma(g, 190, 352, 340, 76);
		g.dispose();
		fundo = f;
	}

	private static void plataforma(Graphics2D g, int cx, int cy, int larg, int alt) {
		g.setColor(new Color(0, 0, 0, 55));
		g.fill(new Ellipse2D.Float(cx - larg / 2f - 4, cy - alt / 2f + 10, larg + 8, alt));
		g.setColor(new Color(0x7C, 0xA8, 0x4A));
		g.fill(new Ellipse2D.Float(cx - larg / 2f, cy - alt / 2f + 6, larg, alt));
		g.setPaint(new GradientPaint(0, cy - alt / 2f, new Color(0xCF, 0xEB, 0x9A), 0, cy + alt / 2f, new Color(0xA5, 0xD0, 0x6E)));
		g.fill(new Ellipse2D.Float(cx - larg / 2f, cy - alt / 2f, larg, alt));
		g.setColor(new Color(255, 255, 255, 90));
		g.setStroke(new BasicStroke(2f));
		g.draw(new Ellipse2D.Float(cx - larg / 2f + 3, cy - alt / 2f + 3, larg - 6, alt - 8));
	}

	// =====================================================================
	// Faixa do topo
	// =====================================================================
	private class Topo extends JComponent {
		private static final long serialVersionUID = 1L;

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			qualidade(g);
			int w = getWidth(), h = getHeight();
			g.setPaint(new GradientPaint(0, 0, new Color(0x3A, 0x5B, 0xD9), 0, h, new Color(0x2A, 0x3F, 0x9E)));
			g.fillRect(0, 0, w, h);
			g.setColor(new Color(255, 255, 255, 40));
			g.fillRect(0, h - 3, w, 3);
			desenharPokebola(g, 36, h / 2f, 13, new Color(0xE8, 0x55, 0x3B), 255);
			g.setFont(fonte(Font.BOLD, 20f));
			g.setColor(Color.WHITE);
			g.drawString("BATALHA POKÉMON", 62, h / 2 + 7);
			g.setFont(fonte(Font.BOLD, 14f));
			String t = "Turno " + turnoTotal + "   ·   Nv. " + Atributos.NIVEL;
			g.setColor(new Color(0xD8, 0xE4, 0xFF));
			g.drawString(t, w - MARGEM - 8 - g.getFontMetrics().stringWidth(t), h / 2 + 5);
			g.dispose();
		}
	}

	// =====================================================================
	// Arena
	// =====================================================================
	private class Arena extends JComponent {
		private static final long serialVersionUID = 1L;

		Arena() {
			setPreferredSize(new Dimension(LARG_ARENA, ALT_ARENA));
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			if (fundo != null) {
				g.drawImage(fundo, 0, 0, null);
			}
			qualidade(g);
			int bob = (int) Math.round(Math.sin(fase) * 3);

			sprite(g, spriteOpo, 470, 214 + bob, tremerOpo, alfaOpo);
			sprite(g, spriteJog, 190, 356 - bob, tremerJog, alfaJog);

			caixaHp(g, formaOponente, hpOpoMostrado, hpOpoMax, 18, 20, 300, 90, false, timeOponente.size() - iOpo, timeOponente.size(), stOpo);
			caixaHp(g, formaJogador, hpJogMostrado, hpJogMax, LARG_ARENA - 318, 284, 300, 98, true, vivosMeus(), meuTime.size(), stJog);

			for (double[] p : popups) {
				boolean doJog = p[0] == 1;
				int x = doJog ? 190 : 470, y = (doJog ? 210 : 100) - (int) (p[1] * 50);
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
	}

	/** Etiqueta colorida com a sigla da condição (QMD, PAR...). */
	private static void etiquetaStatus(Graphics2D g, Status st, int x, int y, int w, int h, float fonte) {
		Color c;
		switch (st) {
		case QUEIMADO:
			c = new Color(0xE8, 0x6A, 0x2B);
			break;
		case PARALISADO:
			c = new Color(0xD9, 0xAE, 0x1F);
			break;
		case ENVENENADO:
			c = new Color(0x9B, 0x4F, 0xC4);
			break;
		case DORMINDO:
			c = new Color(0x7A, 0x84, 0x9E);
			break;
		default:
			c = new Color(0x3F, 0xB5, 0xD6);
		}
		g.setColor(c);
		g.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
		g.setFont(fonte(Font.BOLD, fonte));
		g.setColor(Color.WHITE);
		FontMetrics fm = g.getFontMetrics();
		g.drawString(st.getSigla(), x + (w - fm.stringWidth(st.getSigla())) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
	}

	private int vivosMeus() {
		int n = 0;
		for (boolean c : caiu) {
			if (!c) {
				n++;
			}
		}
		return n;
	}

	/** Caixa de HP no estilo dos jogos: nome, nível, barra e (no seu Pokémon) os números. */
	private static void caixaHp(Graphics2D g, Pokemon p, double hp, int max, int x, int y, int larg, int alt, boolean numeros,
			int vivos, int total, Status status) {
		g.setColor(new Color(0, 0, 0, 50));
		g.fill(new RoundRectangle2D.Float(x + 3, y + 4, larg, alt, 18, 18));
		g.setColor(CAIXA_CLARA);
		g.fill(new RoundRectangle2D.Float(x, y, larg, alt, 18, 18));
		g.setColor(MOLDURA);
		g.setStroke(new BasicStroke(3f));
		g.draw(new RoundRectangle2D.Float(x + 1.5f, y + 1.5f, larg - 3, alt - 3, 18, 18));

		g.setFont(fonte(Font.BOLD, 19f));
		g.setColor(TINTA);
		g.drawString(p.getNome(), x + 16, y + 28);
		g.setFont(fonte(Font.BOLD, 14f));
		String nivel = "Nv" + Atributos.NIVEL;
		g.drawString(nivel, x + larg - 16 - g.getFontMetrics().stringWidth(nivel), y + 27);
		if (status != Status.NENHUM) {
			etiquetaStatus(g, status, x + larg - 16 - g.getFontMetrics().stringWidth(nivel) - 44, y + 12, 38, 18, 12f);
		}

		// rótulo HP + barra
		double frac = Math.max(0, Math.min(1, hp / max));
		int by = y + 40, bh = 14, bx = x + 54, bw = larg - 54 - 16;
		g.setColor(new Color(0xE2, 0x5A, 0x3B));
		g.fill(new RoundRectangle2D.Float(x + 16, by - 1, 32, bh + 2, 8, 8));
		g.setFont(fonte(Font.BOLD, 12f));
		g.setColor(Color.WHITE);
		g.drawString("HP", x + 24, by + 11);
		g.setColor(new Color(0x39, 0x42, 0x5C));
		g.fill(new RoundRectangle2D.Float(bx - 2, by - 2, bw + 4, bh + 4, bh, bh));
		g.setColor(new Color(0x23, 0x2A, 0x40));
		g.fill(new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh));
		Color cor = frac > 0.5 ? new Color(0x3F, 0xD2, 0x7A) : frac > 0.2 ? new Color(0xF4, 0xC4, 0x3B) : new Color(0xEF, 0x4B, 0x4B);
		if (frac > 0) {
			g.setPaint(new GradientPaint(0, by, mix(cor, Color.WHITE, 0.35f), 0, by + bh, escurecer(cor, 0.85f)));
			g.fill(new RoundRectangle2D.Float(bx, by, (float) Math.max(bh, bw * frac), bh, bh, bh));
		}
		if (numeros) {
			g.setFont(fonte(Font.BOLD, 15f));
			g.setColor(TINTA);
			String t = Math.round(hp) + " / " + max;
			g.drawString(t, x + larg - 16 - g.getFontMetrics().stringWidth(t), y + 84);
		}
		// bolinhas do time
		for (int i = 0; i < total; i++) {
			g.setColor(i < vivos ? new Color(0xE8, 0x55, 0x3B) : new Color(0xB8, 0xB4, 0xA4));
			g.fill(new Ellipse2D.Float(x + 18 + i * 18, y + alt - 28, 13, 13));
			g.setColor(new Color(0x26, 0x2F, 0x4A, 160));
			g.setStroke(new BasicStroke(1.5f));
			g.draw(new Ellipse2D.Float(x + 18 + i * 18, y + alt - 28, 13, 13));
		}
	}

	// =====================================================================
	// Parte de baixo: caixa de texto + menu
	// =====================================================================
	private class PainelBaixo extends JComponent {
		private static final long serialVersionUID = 1L;
		private static final int LARG_TEXTO = 404;
		private final Rectangle[] botoes = new Rectangle[4];
		private final Rectangle voltar = new Rectangle();
		private int hover = -1;
		private boolean hoverVoltar = false;

		PainelBaixo() {
			setPreferredSize(new Dimension(LARG_ARENA, ALT_BAIXO));
			for (int i = 0; i < 4; i++) {
				int col = i % 2, lin = i / 2;
				botoes[i] = new Rectangle(LARG_TEXTO + 14 + col * 124, 6 + lin * 80, 118, 72);
			}
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if (voltar.contains(e.getPoint()) && podeVoltar()) {
						modo = Modo.MENU;
						return;
					}
					for (int i = 0; i < 4; i++) {
						if (botoes[i].contains(e.getPoint())) {
							acionar(i);
						}
					}
				}

				@Override
				public void mouseExited(MouseEvent e) {
					hover = -1;
					hoverVoltar = false;
					repaint();
				}
			});
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					int h = -1;
					for (int i = 0; i < 4; i++) {
						if (botoes[i].contains(e.getPoint())) {
							h = i;
						}
					}
					boolean hv = podeVoltar() && voltar.contains(e.getPoint());
					if (h != hover || hv != hoverVoltar) {
						hover = h;
						hoverVoltar = hv;
						setCursor(Cursor.getPredefinedCursor(h >= 0 || hv ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
						repaint();
					}
				}
			});
		}

		private boolean podeVoltar() {
			return liberado() && (modo == Modo.GOLPES || modo == Modo.TIME_ESCOLHA);
		}

		/** Aciona a opção n (0 a 3) do menu atual. */
		void acionar(int n) {
			if (fim) {
				if (n == 0 || n == 3) {
					dispose();
				}
				return;
			}
			if (!liberado()) {
				return;
			}
			if (modo == Modo.MENU) {
				switch (n) {
				case 0:
					modo = Modo.GOLPES;
					break;
				case 1:
					if (vivosMeus() > 1) {
						modo = Modo.TIME_ESCOLHA;
					}
					break;
				case 2:
					alternarMega();
					break;
				default:
					fugir();
				}
			} else if (modo == Modo.GOLPES) {
				List<Golpe> golpes = golpesAtuais();
				if (n < golpes.size() && usavel(golpes.get(n))) {
					jogar(golpes.get(n));
				}
			}
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			qualidade(g);
			int w = getWidth(), h = getHeight();
			g.setColor(new Color(0x12, 0x18, 0x30));
			g.fillRect(0, 0, w, h);

			// caixa de texto
			g.setColor(new Color(0x1C, 0x25, 0x4A));
			g.fill(new RoundRectangle2D.Float(0, 8, LARG_TEXTO, h - 16, 20, 20));
			g.setColor(new Color(0xE8, 0xEE, 0xFF));
			g.setStroke(new BasicStroke(3f));
			g.draw(new RoundRectangle2D.Float(1.5f, 9.5f, LARG_TEXTO - 3, h - 19, 20, 20));
			g.setFont(fonte(Font.BOLD, 20f));
			g.setColor(Color.WHITE);
			FontMetrics fm = g.getFontMetrics();
			String texto = textoAtual();
			List<String> linhas = quebrar(texto, fm, LARG_TEXTO - 48);
			int total = linhas.size() * (fm.getHeight() + 2);
			int y = 8 + (h - 16 - total) / 2 + fm.getAscent() - (podeVoltar() ? 8 : 0);
			for (String l : linhas) {
				g.drawString(l, 24, y);
				y += fm.getHeight() + 2;
			}
			// botão voltar
			if (podeVoltar()) {
				voltar.setBounds(20, h - 46, 110, 26);
				g.setColor(hoverVoltar ? new Color(0x4A, 0x5C, 0x9A) : new Color(0x33, 0x40, 0x72));
				g.fill(new RoundRectangle2D.Float(voltar.x, voltar.y, voltar.width, voltar.height, 26, 26));
				g.setFont(fonte(Font.BOLD, 12f));
				g.setColor(Color.WHITE);
				g.drawString("‹ Voltar (Esc)", voltar.x + 14, voltar.y + 17);
			} else {
				voltar.setBounds(0, 0, 0, 0);
			}

			// menu
			boolean lib = liberado();
			if (fim) {
				botaoMenu(g, botoes[0], "FECHAR", C_FUGIR, true, hover == 0, null);
				return;
			}
			if (modo == Modo.GOLPES) {
				List<Golpe> golpes = golpesAtuais();
				for (int i = 0; i < 4; i++) {
					Golpe gl = i < golpes.size() ? golpes.get(i) : null;
					if (gl == null) {
						g.setColor(new Color(255, 255, 255, 14));
						g.fill(new RoundRectangle2D.Float(botoes[i].x, botoes[i].y, botoes[i].width, botoes[i].height, 16, 16));
					} else {
						boolean esforco = gl == dados.golpeEsforco();
						String pp = esforco ? "sem PP" : "PP " + estMeu[iJog].ppDe(gl) + "/" + gl.getPp();
						String sub = gl.isStatus() ? "Status · " + pp : gl.getTipo() + " " + gl.getPoder() + " · " + pp;
						botaoMenu(g, botoes[i], gl.getNome().toUpperCase(), corDoTipo(gl.getTipo()), lib && usavel(gl), hover == i, sub);
					}
				}
			} else {
				boolean ativoMenu = lib && modo == Modo.MENU;
				List<Pokemon> megas = opcoesMega();
				botaoMenu(g, botoes[0], "LUTAR", C_LUTAR, ativoMenu, hover == 0, "golpes");
				botaoMenu(g, botoes[1], "POKÉMON", C_POKEMON, ativoMenu && vivosMeus() > 1, hover == 1, "trocar");
				String rotMega = megaEscolhida != null ? "✦ MEGA" : "MEGA";
				botaoMenu(g, botoes[2], rotMega, C_MEGA, ativoMenu && !megas.isEmpty(), hover == 2,
						megaEscolhida != null ? "armada" : megas.isEmpty() ? "indisponível" : "evoluir");
				botaoMenu(g, botoes[3], "FUGIR", C_FUGIR, ativoMenu, hover == 3, "desistir");
			}
			g.dispose();
		}

		private String textoAtual() {
			if (!liberado() || fim) {
				return mensagem;
			}
			if (modo == Modo.GOLPES) {
				return "Escolha o golpe de " + formaJogador.getNome() + ".";
			}
			if (modo == Modo.TIME_ESCOLHA) {
				return "Escolha, no seu time, o Pokémon que vai entrar. Trocar gasta o turno.";
			}
			if (modo == Modo.TIME_FORCADA) {
				return "Escolha o próximo Pokémon do seu time.";
			}
			return mensagem;
		}

		private void botaoMenu(Graphics2D g, Rectangle r, String texto, Color base, boolean ativo, boolean hover, String sub) {
			Color c = ativo ? base : new Color(0x4B, 0x52, 0x68);
			Color cima = mix(c, Color.WHITE, hover && ativo ? 0.30f : 0.14f);
			g.setPaint(new GradientPaint(0, r.y, cima, 0, r.y + r.height, escurecer(c, 0.78f)));
			g.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 16, 16));
			g.setColor(escurecer(c, 0.5f));
			g.setStroke(new BasicStroke(2.5f));
			g.draw(new RoundRectangle2D.Float(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 16, 16));
			Color txt = ativo ? Color.WHITE : new Color(0xB5, 0xBB, 0xCC);
			g.setFont(fonte(Font.BOLD, texto.length() > 9 ? 13f : 16f));
			FontMetrics fm = g.getFontMetrics();
			int ty = r.y + (sub == null ? r.height / 2 + fm.getAscent() / 2 - 2 : r.height / 2 - 2);
			g.setColor(new Color(0, 0, 0, 70));
			g.drawString(texto, r.x + (r.width - fm.stringWidth(texto)) / 2 + 1, ty + 1);
			g.setColor(txt);
			g.drawString(texto, r.x + (r.width - fm.stringWidth(texto)) / 2, ty);
			if (sub != null) {
				g.setFont(fonte(Font.PLAIN, 12f));
				FontMetrics f2 = g.getFontMetrics();
				g.setColor(ativo ? new Color(255, 255, 255, 210) : new Color(0xB5, 0xBB, 0xCC, 170));
				g.drawString(sub, r.x + (r.width - f2.stringWidth(sub)) / 2, r.y + r.height / 2 + 18);
			}
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
	// Lateral: seu time (clicável para trocar) e o time adversário
	// =====================================================================
	private class PainelTime extends JComponent {
		private static final long serialVersionUID = 1L;
		private int hover = -1;

		/** Altura do cartão: cabe 6 no painel (compacto) ou 3 (grande). */
		private int cartaoH() {
			return meuTime.size() <= 3 ? 108 : 64;
		}

		private boolean compacto() {
			return meuTime.size() > 3;
		}

		PainelTime() {
			setPreferredSize(new Dimension(LARG_LATERAL, ALT_ARENA + ALT_BAIXO));
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					int k = cartaoEm(e.getPoint());
					if (k < 0) {
						return;
					}
					if (modo == Modo.TIME_ESCOLHA) {
						trocarVoluntaria(k);
					} else if (modo == Modo.TIME_FORCADA) {
						trocarForcada(k);
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
					int k = cartaoEm(e.getPoint());
					if (k != hover) {
						hover = k;
						setCursor(Cursor.getPredefinedCursor(k >= 0 && selecionavel(k) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
						repaint();
					}
				}
			});
		}

		private int cartaoEm(Point p) {
			for (int i = 0; i < meuTime.size(); i++) {
				if (new Rectangle(0, 40 + i * (cartaoH() + (compacto() ? 6 : 8)), LARG_LATERAL, cartaoH()).contains(p)) {
					return i;
				}
			}
			return -1;
		}

		private boolean selecionavel(int i) {
			return liberado() && (modo == Modo.TIME_ESCOLHA || modo == Modo.TIME_FORCADA) && !caiu[i] && !(modo == Modo.TIME_ESCOLHA && i == iJog);
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			qualidade(g);
			int w = getWidth(), h = getHeight();
			g.setPaint(new GradientPaint(0, 0, new Color(0x1E, 0x2A, 0x55), 0, h, new Color(0x14, 0x1C, 0x3A)));
			g.fill(new RoundRectangle2D.Float(0, 0, w, h, 20, 20));
			g.setColor(new Color(0x3F, 0x55, 0xA0));
			g.setStroke(new BasicStroke(2.5f));
			g.draw(new RoundRectangle2D.Float(1.25f, 1.25f, w - 2.5f, h - 2.5f, 20, 20));

			g.setFont(fonte(Font.BOLD, 15f));
			g.setColor(new Color(0xB9, 0xC7, 0xF5));
			g.drawString("SEU TIME", 18, 28);

			for (int i = 0; i < meuTime.size(); i++) {
				desenharCartao(g, i, 10, 40 + i * (cartaoH() + (compacto() ? 6 : 8)), w - 20, cartaoH());
			}

			// time adversário
			int oy = 40 + meuTime.size() * (cartaoH() + (compacto() ? 6 : 8)) + 2;
			g.setFont(fonte(Font.BOLD, 15f));
			g.setColor(new Color(0xB9, 0xC7, 0xF5));
			g.drawString("ADVERSÁRIO", 18, oy + 14);
			int tam = timeOponente.size() > 4 ? 36 : 54, passoOp = tam + 6;
			for (int i = 0; i < timeOponente.size(); i++) {
				int ox = 18 + i * passoOp, oyy = oy + 22;
				boolean visto = i <= iOpo, derrotado = i < iOpo;
				g.setColor(new Color(255, 255, 255, visto ? 28 : 14));
				g.fill(new RoundRectangle2D.Float(ox, oyy, tam, tam, 12, 12));
				if (visto) {
					BufferedImage mi = miniatura(timeOponente.get(i), tam - 10);
					if (mi != null) {
						if (derrotado) {
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
						}
						g.drawImage(mi, ox + (tam - mi.getWidth()) / 2, oyy + (tam - mi.getHeight()) / 2, null);
						g.setComposite(AlphaComposite.SrcOver);
					}
				} else {
					g.setFont(fonte(Font.BOLD, 22f));
					g.setColor(new Color(0xB9, 0xC7, 0xF5, 150));
					g.drawString("?", ox + tam / 2 - 6, oyy + tam / 2 + 8);
				}
			}
			g.dispose();
		}

		private void desenharCartao(Graphics2D g, int i, int x, int y, int w, int h) {
			Pokemon p = meuTime.get(i);
			boolean ativo = i == iJog && !caiu[i];
			boolean sel = selecionavel(i);
			g.setColor(caiu[i] ? new Color(0x26, 0x2A, 0x3C) : ativo ? new Color(0x3A, 0x4C, 0x96) : new Color(0x2A, 0x38, 0x70));
			g.fill(new RoundRectangle2D.Float(x, y, w, h, 16, 16));
			Color borda = ativo ? new Color(0xFF, 0xD8, 0x4D) : sel ? (hover == i ? Color.WHITE : new Color(0x7B, 0xE0, 0x9A)) : new Color(0x4A, 0x5E, 0xB0);
			g.setColor(borda);
			g.setStroke(new BasicStroke(sel || ativo ? 3f : 1.6f));
			g.draw(new RoundRectangle2D.Float(x + 1, y + 1, w - 2, h - 2, 16, 16));

			boolean comp = compacto();
			BufferedImage mi = miniatura(p, comp ? 46 : 72);
			if (mi != null) {
				if (caiu[i]) {
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
				}
				int lado = comp ? 46 : 72;
				g.drawImage(mi, x + (comp ? 8 : 12) + (lado - mi.getWidth()) / 2, y + (h - mi.getHeight()) / 2, null);
				g.setComposite(AlphaComposite.SrcOver);
			}
			int tx = x + (comp ? 66 : 96);
			String estado = caiu[i] ? "DESMAIOU" : ativo ? "EM CAMPO" : sel ? "TOQUE PARA ENTRAR" : "PRONTO";
			g.setFont(fonte(Font.BOLD, comp ? 15f : 17f));
			g.setColor(caiu[i] ? new Color(0x8A, 0x90, 0xA8) : Color.WHITE);
			g.drawString(p.getNome(), tx, y + (comp ? 22 : 30));
			Status stCartao = i == iJog ? stJog : estMeu[i].getStatus();
			if (stCartao != Status.NENHUM && !caiu[i]) {
				int nx = tx + g.getFontMetrics().stringWidth(p.getNome()) + 8;
				etiquetaStatus(g, stCartao, nx, y + (comp ? 9 : 16), 34, 16, 11f);
			}
			g.setFont(fonte(Font.PLAIN, comp ? 11f : 12f));
			g.setColor(new Color(0xB9, 0xC7, 0xF5));
			if (comp) {
				String st = estado;
				g.drawString(st, x + w - 10 - g.getFontMetrics().stringWidth(st), y + 22);
			} else {
				g.drawString("Nv" + Atributos.NIVEL + "  ·  " + estado, tx, y + 48);
			}

			int max = Batalha.hpMaximo(dados, p);
			double hp = caiu[i] ? 0 : (i == iJog ? hpJogMostrado : (hpMeu[i] > 0 ? hpMeu[i] : max));
			if (i == iJog && !caiu[i]) {
				max = Math.max(1, hpJogMax);
			}
			double frac = Math.max(0, Math.min(1, hp / max));
			int bx = tx, by = comp ? y + 34 : y + 62, bw = w - (comp ? 66 : 96) - 14, bh = comp ? 9 : 11;
			g.setColor(new Color(0x14, 0x1A, 0x33));
			g.fill(new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh));
			Color cor = frac > 0.5 ? new Color(0x3F, 0xD2, 0x7A) : frac > 0.2 ? new Color(0xF4, 0xC4, 0x3B) : new Color(0xEF, 0x4B, 0x4B);
			if (frac > 0) {
				g.setColor(cor);
				g.fill(new RoundRectangle2D.Float(bx, by, (float) Math.max(bh, bw * frac), bh, bh, bh));
			}
			g.setFont(fonte(Font.PLAIN, 11f));
			g.setColor(new Color(0xD8, 0xE4, 0xFF));
			g.drawString(Math.round(hp) + " / " + max, bx, by + (comp ? 21 : 26));
		}
	}
}
