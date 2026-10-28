package pokedex.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import pokedex.abb.ArvoreBinaria;
import pokedex.batalha.DadosBatalha;
import pokedex.modelo.Pokemon;
import pokedex.ui.batalha.SeletorBatalha;
import pokedex.ui.batalha.TelaBatalha;
import pokedex.ui.componentes.BarraRolagem;
import pokedex.ui.componentes.BotaoRedondo;
import pokedex.ui.componentes.BotaoTexto;
import pokedex.ui.componentes.Cabecalho;
import pokedex.ui.componentes.CampoBusca;
import pokedex.ui.componentes.LinhaPokemon;
import pokedex.ui.componentes.SeletorRegiao;
import pokedex.ui.recursos.Imagens;
import pokedex.ui.tema.Ambiente;

import static pokedex.ui.tema.Tema.*;

public class PainelPokedex extends JPanel {

	private static final long serialVersionUID = 1L;

	private final transient Function<String, Pokemon> buscador;
	private final transient List<Pokemon> lista = new ArrayList<>();
	private final double maxAltura;
	private final double maxPeso;
	private transient Pokemon atual;

	// descobertas (a Pokédex começa vazia; cada Pokémon precisa ser escaneado)
	private final transient ArvoreBinaria pc; // ABB dos capturados (o PC)
	private int scanDex = -1; // Pokémon sendo escaneado agora (-1 = nenhum)
	private double scanT = 0; // progresso do scan, de 0 a 1
	private double flash = 0; // clarão ao revelar

	// animação: troca de cor/entrada do Pokémon e flutuação da arte
	private Color corDeA = PADRAO_A, corDeB = PADRAO_B, corParaA = PADRAO_A, corParaB = PADRAO_B;
	private double t = 1.0;
	private double fase = 0;
	private final javax.swing.Timer relogio;

	// lateral
	private final DefaultListModel<Pokemon> modelo = new DefaultListModel<>();
	private final JList<Pokemon> jlist = new JList<>(modelo);
	private final CampoBusca campo = new CampoBusca();
	private final JLabel lblAviso = new JLabel(" ");
	private final javax.swing.Timer avisoTimer;
	private boolean sincronizando = false;
	private SeletorRegiao seletorRegiao; // null quando os dados têm uma região só

	// principal
	private final Heroi heroi = new Heroi();
	private final JLabel lblContador = new JLabel(" ");
	private final JLabel lblAlturaValor = new JLabel("-");
	private final JLabel lblPesoValor = new JLabel("-");
	private final Barra barraAltura = new Barra();
	private final Barra barraPeso = new Barra();
	private final JTextArea txtDescricao = criarAreaTexto(15f);
	private final JTextArea txtCuriosidade = criarAreaTexto(15f);

	public PainelPokedex(Function<String, Pokemon> buscador, Supplier<List<Pokemon>> todos, ArvoreBinaria pc) {
		this.pc = pc;
		this.buscador = buscador;

		List<Pokemon> origem = (todos == null) ? null : todos.get();
		if (origem != null) {
			lista.addAll(origem);
		}
		lista.sort(Comparator.comparingInt(Pokemon::getNumeroDex));
		double ma = 0, mp = 0;
		for (Pokemon p : lista) {
			if (p.getAltura() != null) {
				ma = Math.max(ma, p.getAltura());
			}
			if (p.getPeso() != null) {
				mp = Math.max(mp, p.getPeso());
			}
		}
		maxAltura = ma > 0 ? ma : 5;
		maxPeso = mp > 0 ? mp : 200;

		setLayout(new BorderLayout());
		add(montarLateral(), BorderLayout.WEST);
		add(montarPrincipal(), BorderLayout.CENTER);

		avisoTimer = new javax.swing.Timer(3500, e -> lblAviso.setText(" "));
		avisoTimer.setRepeats(false);

		relogio = new javax.swing.Timer(25, e -> passo()); // ~40 quadros por segundo
		relogio.start();

		configurarAtalhos();

		Imagens.preCarregarMiniaturas(lista, () -> SwingUtilities.invokeLater(jlist::repaint));
	}

	// ---------- animação ----------

	private long ultimoQuadro = System.nanoTime();

	/** Avança as animações pelo tempo real decorrido (continua suave mesmo se um quadro demorar). */
	private void passo() {
		long agora = System.nanoTime();
		double k = Math.max(0.2, Math.min(4.0, (agora - ultimoQuadro) / 33_000_000.0)); // 1.0 = 33 ms
		ultimoQuadro = agora;

		fase += 0.07 * k;
		if (flash > 0) {
			flash = Math.max(0, flash - 0.06 * k);
		}
		if (scanDex >= 0) {
			scanT += 0.0125 * k; // ~2,4 segundos
			if (scanT >= 1) {
				concluirScan();
			}
		}
		if (t < 1) {
			t = Math.min(1, t + 0.06 * k);
			repaint();
		} else {
			heroi.repaint();
		}
	}

	private Color accentA() {
		return mix(corDeA, corParaA, (float) easeOut(t));
	}

	private Color accentB() {
		return mix(corDeB, corParaB, (float) easeOut(t));
	}

	/** Só para testes: termina a animação de entrada. */
	public void finalizarAnimacao() {
		t = 1;
	}

	// ---------- montagem da tela ----------

	private JComponent montarLateral() {
		JPanel lateral = new JPanel(new BorderLayout()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setColor(new Color(0, 0, 0, 70));
				g2.fillRect(0, 0, getWidth(), getHeight());
				g2.setColor(comAlpha(BORDA, 200));
				g2.fillRect(getWidth() - 1, 0, 1, getHeight());
				g2.dispose();
			}
		};
		lateral.setOpaque(false);
		lateral.setPreferredSize(new Dimension(330, 100));

		JPanel topo = new JPanel(new BorderLayout());
		topo.setOpaque(false);
		topo.add(new Cabecalho(), BorderLayout.NORTH);

		JPanel caixaCampo = new JPanel(new BorderLayout());
		caixaCampo.setOpaque(false);
		caixaCampo.setBorder(new EmptyBorder(0, 18, 0, 18));
		caixaCampo.add(campo, BorderLayout.CENTER);
		JPanel centro = new JPanel(new BorderLayout());
		centro.setOpaque(false);
		centro.add(caixaCampo, BorderLayout.NORTH);
		List<String> nomesRegioes = regioesPresentes();
		if (nomesRegioes.size() > 1) {
			seletorRegiao = new SeletorRegiao(nomesRegioes);
			seletorRegiao.aoMudar(this::aoTrocarRegiao);
			JPanel caixaRegiao = new JPanel(new BorderLayout());
			caixaRegiao.setOpaque(false);
			caixaRegiao.setBorder(new EmptyBorder(10, 18, 0, 18));
			caixaRegiao.add(seletorRegiao, BorderLayout.CENTER);
			centro.add(caixaRegiao, BorderLayout.CENTER);
			atualizarSeletorRegiao();
		}
		topo.add(centro, BorderLayout.CENTER);

		lblAviso.setFont(fonte(Font.PLAIN, 13f));
		lblAviso.setForeground(new Color(0xFF, 0x9A, 0x9A));
		lblAviso.setBorder(new EmptyBorder(8, 24, 8, 18));
		topo.add(lblAviso, BorderLayout.SOUTH);
		lateral.add(topo, BorderLayout.NORTH);

		for (Pokemon p : lista) {
			modelo.addElement(p);
		}
		jlist.setOpaque(false);
		jlist.setBackground(new Color(0, 0, 0, 0));
		jlist.setFixedCellHeight(64);
		jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jlist.setCellRenderer(new LinhaPokemon(p -> descoberto(p.getNumeroDex())));
		jlist.addListSelectionListener(e -> {
			if (sincronizando || e.getValueIsAdjusting()) {
				return;
			}
			Pokemon sel = jlist.getSelectedValue();
			if (sel != null) {
				exibir(sel);
			}
		});

		JScrollPane rolagem = new JScrollPane(jlist);
		rolagem.setBorder(null);
		rolagem.setOpaque(false);
		rolagem.getViewport().setOpaque(false);
		rolagem.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		rolagem.getVerticalScrollBar().setUI(new BarraRolagem());
		rolagem.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
		rolagem.getVerticalScrollBar().setOpaque(false);
		rolagem.getVerticalScrollBar().setUnitIncrement(24);
		lateral.add(rolagem, BorderLayout.CENTER);

		campo.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				filtrar();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				filtrar();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				filtrar();
			}
		});
		campo.addActionListener(e -> buscar());
		return lateral;
	}

	private JComponent montarPrincipal() {
		JPanel principal = new JPanel(new BorderLayout());
		principal.setOpaque(false);

		// barra de cima: posição na lista e botões de navegação
		JPanel barra = new JPanel(new BorderLayout());
		barra.setOpaque(false);
		barra.setBorder(new EmptyBorder(20, 32, 0, 28));
		lblContador.setFont(fonte(Font.PLAIN, 15f));
		lblContador.setForeground(SUAVE);
		barra.add(lblContador, BorderLayout.WEST);

		JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		botoes.setOpaque(false);
		BotaoTexto cadastrar = new BotaoTexto("Cadastrar", "Registrar um Pokémon pelo nome ou número");
		cadastrar.addActionListener(e -> cadastrar());
		botoes.add(cadastrar);
		BotaoTexto abrirPc = new BotaoTexto("PC", "Ver e liberar os Pokémon capturados (ABB)");
		abrirPc.addActionListener(e -> abrirPC());
		botoes.add(abrirPc);
		BotaoTexto batalhar = new BotaoTexto("Batalhar", "Modo batalha: monte um time de até 6 com os Pokémon que você capturou");
		batalhar.addActionListener(e -> batalhar());
		botoes.add(batalhar);
		BotaoRedondo anterior = new BotaoRedondo("‹", "Anterior (seta esquerda)");
		BotaoRedondo proximo = new BotaoRedondo("›", "Próximo (seta direita)");
		anterior.addActionListener(e -> navegar(-1));
		proximo.addActionListener(e -> navegar(+1));
		botoes.add(anterior);
		botoes.add(proximo);
		barra.add(botoes, BorderLayout.EAST);
		principal.add(barra, BorderLayout.NORTH);

		principal.add(heroi, BorderLayout.CENTER);

		// cartões de baixo
		JPanel detalhes = new JPanel(new GridLayout(1, 3, 14, 0));
		detalhes.setOpaque(false);
		detalhes.setBorder(new EmptyBorder(0, 32, 28, 32));
		detalhes.setPreferredSize(new Dimension(100, 230));
		detalhes.add(montarMedidas());
		detalhes.add(montarTexto("Descrição", txtDescricao, false));
		detalhes.add(montarTexto("Curiosidade", txtCuriosidade, true));

		principal.add(detalhes, BorderLayout.SOUTH);
		return principal;
	}

	private JComponent montarMedidas() {
		CartaoPainel cartao = new CartaoPainel(false);
		cartao.setLayout(new BoxLayout(cartao, BoxLayout.Y_AXIS));

		cartao.add(new Titulo("Medidas"));
		cartao.add(Box.createVerticalStrut(12));
		cartao.add(linhaMedida("Altura", lblAlturaValor, barraAltura));
		cartao.add(Box.createVerticalStrut(12));
		cartao.add(linhaMedida("Peso", lblPesoValor, barraPeso));
		cartao.add(Box.createVerticalGlue());
		return cartao;
	}

	private JComponent linhaMedida(String nome, JLabel valor, Barra barra) {
		JPanel linha = new JPanel(new BorderLayout(0, 6));
		linha.setOpaque(false);
		linha.setAlignmentX(Component.LEFT_ALIGNMENT);
		linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

		JPanel textos = new JPanel(new BorderLayout());
		textos.setOpaque(false);
		JLabel lbl = new JLabel(nome);
		lbl.setFont(fonte(Font.PLAIN, 14f));
		lbl.setForeground(SUAVE);
		valor.setFont(fonte(Font.BOLD, 19f));
		valor.setForeground(TEXTO);
		textos.add(lbl, BorderLayout.WEST);
		textos.add(valor, BorderLayout.EAST);

		linha.add(textos, BorderLayout.NORTH);
		linha.add(barra, BorderLayout.CENTER);
		return linha;
	}

	private JComponent montarTexto(String titulo, JTextArea area, boolean tingir) {
		CartaoPainel cartao = new CartaoPainel(tingir);
		cartao.setLayout(new BorderLayout(0, 10));
		cartao.add(new Titulo(titulo), BorderLayout.NORTH);
		cartao.add(area, BorderLayout.CENTER);
		return cartao;
	}

	private static JTextArea criarAreaTexto(float tamanho) {
		JTextArea area = new JTextArea();
		area.setEditable(false);
		area.setFocusable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setOpaque(false);
		area.setForeground(TEXTO);
		area.setFont(fonte(Font.PLAIN, tamanho));
		area.setBorder(null);
		return area;
	}

	private void configurarAtalhos() {
		InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "anterior");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "proximo");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "buscar");
		am.put("anterior", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				navegar(-1);
			}
		});
		am.put("proximo", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				navegar(+1);
			}
		});
		am.put("buscar", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				campo.requestFocusInWindow();
				campo.selectAll();
			}
		});
	}

	// ---------- comportamento ----------

	private void aviso(String texto) {
		lblAviso.setText(texto);
		avisoTimer.restart();
	}

	private transient DadosBatalha dadosBatalha;

	/**
	 * Modo batalha: o jogador monta um time (até 6) com os Pokémon que capturou e luta contra um time do mesmo
	 * tamanho sorteado entre todos os Pokémon (cada adversário é achado pelo buscador, ou seja, pela ABB do catálogo).
	 */
	private void batalhar() {
		try {
			if (dadosBatalha == null) {
				dadosBatalha = new DadosBatalha("dados");
			}
		} catch (IOException ex) {
			aviso("Não achei a pasta dados/ (rode a partir da raiz do projeto).");
			return;
		}
		List<Pokemon> capturados = new ArrayList<>(); // o seu time só pode ter Pokémon do PC
		List<Pokemon> sorteaveis = new ArrayList<>(); // o adversário sorteia entre todos que têm golpes
		for (Pokemon p : lista) {
			if (dadosBatalha.golpesDe(p.getNumeroDex()).isEmpty()) {
				continue;
			}
			sorteaveis.add(p);
			if (descoberto(p.getNumeroDex())) {
				capturados.add(p);
			}
		}
		if (capturados.isEmpty()) {
			aviso("Descubra pelo menos 1 Pokémon para poder batalhar.");
			return;
		}
		Frame dono = (Frame) SwingUtilities.getWindowAncestor(this);
		relogio.stop(); // a Pokédex fica parada enquanto as janelas de batalha estão abertas
		try {
			List<Pokemon> meu = new SeletorBatalha(dono, capturados, capturados.contains(atual) ? atual : null).escolher();
			if (meu.isEmpty()) {
				return;
			}
			List<Pokemon> candidatos = new ArrayList<>(sorteaveis);
			candidatos.removeAll(meu);
			Collections.shuffle(candidatos);
			List<Pokemon> adversarios = new ArrayList<>();
			for (Pokemon c : candidatos.subList(0, Math.min(meu.size(), candidatos.size()))) {
				Pokemon achado = buscador.apply(String.valueOf(c.getNumeroDex())); // busca no catálogo (ABB)
				adversarios.add(achado != null ? achado : c);
			}
			new TelaBatalha(dono, dadosBatalha, meu, adversarios).setVisible(true);
		} finally {
			ultimoQuadro = System.nanoTime();
			relogio.start();
		}
	}

	/** Abre o PC (a ABB dos capturados). Depois de liberar, a Pokédex é atualizada. */
	private void abrirPC() {
		if (scanDex >= 0) {
			aviso("Aguarde o scan terminar.");
			return;
		}
		Frame dono = (Frame) SwingUtilities.getWindowAncestor(this);
		relogio.stop();
		try {
			new JanelaPC(dono, pc, this::aposMudancaNoPC).setVisible(true);
		} finally {
			ultimoQuadro = System.nanoTime();
			relogio.start();
		}
	}

	/** Chamado quando algo sai do PC: refaz a lista lateral, o contador e o painel do Pokémon atual. */
	private void aposMudancaNoPC() {
		filtrar();
		exibir(atual);
	}

	/** Enter no campo: usa o buscador (a ABB, quando estiver ligada). */
	private void buscar() {
		String texto = campo.getText().trim();
		if (texto.isEmpty()) {
			aviso("Digite um nome ou número Dex.");
			return;
		}
		Pokemon p = buscador.apply(texto);
		boolean porNumero = texto.matches("\\d+");
		if (p != null && !porNumero && !descoberto(p.getNumeroDex())) {
			aviso("Esse Pokémon ainda não foi descoberto. Use Cadastrar.");
		} else if (p == null) {
			aviso("Nenhum Pokémon encontrado para \"" + texto + "\".");
		} else {
			campo.setText("");
			lblAviso.setText(" ");
			exibir(p);
		}
	}

	/** Começa a animação de scan do Pokémon (ignora se já descoberto ou se há outro scan). */
	private void iniciarScan(Pokemon p) {
		if (p == null || scanDex >= 0 || descoberto(p.getNumeroDex())) {
			return;
		}
		scanDex = p.getNumeroDex();
		scanT = 0;
		heroi.repaint();
	}

	/** Um Pokémon está descoberto quando está no PC (ABB dos capturados). */
	private boolean descoberto(int dex) {
		return pc.buscar(dex) != null;
	}

	private Pokemon naLista(int dex) {
		for (Pokemon p : lista) {
			if (p.getNumeroDex() == dex) {
				return p;
			}
		}
		return null;
	}

	private void concluirScan() {
		int dex = scanDex;
		scanDex = -1;
		scanT = 0;
		Pokemon revelado = naLista(dex);
		if (revelado != null) {
			pc.inserir(revelado);
		}
		flash = 1;
		if (revelado != null && atual != null && atual.getNumeroDex() == dex) {
			exibir(revelado); // anima a troca de cores já com os dados
		} else {
			atualizarContador();
			if (revelado != null) {
				aviso(revelado.getNome() + " foi registrado na Pokédex!");
			}
		}
		jlist.repaint();
	}

	/** Botão Cadastrar: só aceita Pokémon que existem na Pokédex e têm imagem. */
	private void cadastrar() {
		if (scanDex >= 0) {
			aviso("Aguarde o scan terminar.");
			return;
		}
		String texto = JOptionPane.showInputDialog(this, "Número Dex ou nome do Pokémon:", "Cadastrar Pokémon",
				JOptionPane.PLAIN_MESSAGE);
		if (texto == null || texto.trim().isEmpty()) {
			return;
		}
		Pokemon p = buscador.apply(texto.trim());
		if (p == null) {
			aviso("Não existe Pokémon \"" + texto.trim() + "\" nesta Pokédex.");
		} else if (Imagens.grande(p.getNumeroDex()) == null) {
			aviso("Sem imagem para o Nº " + p.getNumeroDex() + ". Rode o BaixadorImagens.");
		} else if (descoberto(p.getNumeroDex())) {
			aviso(p.getNome() + " já está registrado.");
			exibir(p);
		} else {
			lblAviso.setText(" ");
			exibir(p);
			iniciarScan(p);
		}
	}

	/** Só para testes: marca como descoberto sem animação. */
	public void descobrir(int dex) {
		Pokemon p = naLista(dex);
		if (p != null) {
			pc.inserir(p);
		}
	}

	/** Só para testes: deixa o scan num ponto (0 a 1) sem avançar. */
	public void fixarScan(int dex, double progresso) {
		scanDex = dex;
		scanT = progresso;
	}

	private void navegar(int passo) {
		int dex = (atual == null) ? 1 : atual.getNumeroDex() + passo;
		Pokemon p = buscador.apply(String.valueOf(dex));
		if (p == null) {
			aviso(passo < 0 ? "Este é o primeiro Pokémon." : "Este é o último Pokémon.");
		} else {
			exibir(p);
		}
	}

	private static final List<String> ORDEM_REGIOES = List.of("Kanto", "Johto", "Hoenn", "Sinnoh", "Unova", "Kalos",
			"Alola", "Galar", "Hisui", "Paldea");

	/** Regiões que existem nos dados, na ordem das gerações (as desconhecidas vão para o fim). */
	private List<String> regioesPresentes() {
		Set<String> existentes = new java.util.TreeSet<>();
		for (Pokemon p : lista) {
			if (p.getRegiao() != null && !p.getRegiao().isBlank()) {
				existentes.add(p.getRegiao());
			}
		}
		List<String> resultado = new ArrayList<>();
		for (String r : ORDEM_REGIOES) {
			if (existentes.remove(r)) {
				resultado.add(r);
			}
		}
		resultado.addAll(existentes);
		return resultado;
	}

	private void aoTrocarRegiao() {
		filtrar();
		atualizarSeletorRegiao();
		if (modelo.size() > 0 && (atual == null || modelo.indexOf(atual) < 0)) {
			exibir(modelo.get(0)); // mostra o primeiro da região escolhida
		}
	}

	/** Mostra "descobertos/total" da região escolhida (ou de todas). */
	private void atualizarSeletorRegiao() {
		if (seletorRegiao == null) {
			return;
		}
		String regiao = seletorRegiao.regiao();
		int total = 0, achados = 0;
		for (Pokemon p : lista) {
			if (regiao == null || regiao.equals(p.getRegiao())) {
				total++;
				if (descoberto(p.getNumeroDex())) {
					achados++;
				}
			}
		}
		seletorRegiao.setProgresso(achados + "/" + total);
	}

	/** Só para testes: escolhe a região (null = todas). */
	public void selecionarRegiao(String regiao) {
		if (seletorRegiao != null) {
			seletorRegiao.escolher(regiao);
		}
	}

	public int itensNaLista() {
		return modelo.size();
	}

	/** Filtra a lista lateral enquanto o usuário digita. */
	private void filtrar() {
		String f = normalizar(campo.getText());
		sincronizando = true;
		modelo.clear();
		String regiao = seletorRegiao == null ? null : seletorRegiao.regiao();
		for (Pokemon p : lista) {
			if (regiao != null && !regiao.equals(p.getRegiao())) {
				continue;
			}
			boolean nomeVisivel = descoberto(p.getNumeroDex()); // nome de quem não foi descoberto é segredo
			if (f.isEmpty() || (nomeVisivel && normalizar(p.getNome()).contains(f))
					|| String.valueOf(p.getNumeroDex()).startsWith(f)) {
				modelo.addElement(p);
			}
		}
		sincronizando = false;
		selecionarNaLista(atual);
	}

	private void selecionarNaLista(Pokemon p) {
		sincronizando = true;
		if (p == null) {
			jlist.clearSelection();
		} else {
			int idx = modelo.indexOf(p);
			if (idx >= 0) {
				jlist.setSelectedIndex(idx);
				jlist.ensureIndexIsVisible(idx);
			} else {
				jlist.clearSelection();
			}
		}
		sincronizando = false;
		jlist.repaint();
	}

	public void exibir(Pokemon p) {
		// as barras guardam o valor atual antes de mudar, para animar a troca
		boolean sabido = p != null && descoberto(p.getNumeroDex());
		barraAltura.definir(!sabido || p.getAltura() == null ? 0 : Math.sqrt(p.getAltura() / maxAltura));
		barraPeso.definir(!sabido || p.getPeso() == null ? 0 : Math.sqrt(p.getPeso() / maxPeso));

		boolean conhecido = p != null && descoberto(p.getNumeroDex());
		Color novaA = (!conhecido) ? PADRAO_A : corDoTipo(p.getTipo1());
		Color novaB;
		if (!conhecido) {
			novaB = PADRAO_B;
		} else if (p.getTipo2() != null) {
			novaB = corDoTipo(p.getTipo2());
		} else {
			novaB = mix(novaA, Color.WHITE, 0.25f);
		}
		corDeA = accentA();
		corDeB = accentB();
		corParaA = novaA;
		corParaB = novaB;
		t = 0;

		atual = p;
		heroi.setPokemon(p);

		if (p == null) {
			lblAlturaValor.setText("-");
			lblPesoValor.setText("-");
			txtDescricao.setText("Escolha um Pokémon na lista ao lado ou busque pelo nome ou número.");
			txtCuriosidade.setText("-");
		} else if (!sabido) {
			lblAlturaValor.setText("???");
			lblPesoValor.setText("???");
			txtDescricao.setText("Dados não registrados. Escaneie este Pokémon para revelar as informações.");
			txtCuriosidade.setText("???");
		} else {
			lblAlturaValor.setText(p.getAltura() == null ? "-" : String.format("%.1f m", p.getAltura()));
			lblPesoValor.setText(p.getPeso() == null ? "-" : String.format("%.1f kg", p.getPeso()));
			txtDescricao.setText(p.getDescricao());
			txtCuriosidade.setText(p.getCuriosidade());
		}
		atualizarContador();
		selecionarNaLista(p);
		repaint();
	}

	private void atualizarContador() {
		atualizarSeletorRegiao();
		String progresso = "Descobertos " + pc.percorrerEmOrdem().size() + "/" + lista.size();
		if (atual == null) {
			lblContador.setText(lista.isEmpty() ? " " : progresso);
			return;
		}
		int pos = 0;
		for (int i = 0; i < lista.size(); i++) {
			if (lista.get(i).getNumeroDex() == atual.getNumeroDex()) {
				pos = i + 1;
				break;
			}
		}
		boolean sabido = descoberto(atual.getNumeroDex());
		String regiao = (!sabido || atual.getRegiao() == null) ? "" : atual.getRegiao() + "  ·  ";
		lblContador.setText((pos > 0 ? regiao + pos + " de " + lista.size() : regiao.trim()) + "  ·  " + progresso);
	}

	/** Tamanho da arte na área principal. */
	private int ladoHeroi(int w, int h) {
		return Math.max(220, Math.min(h - 24, (int) (w * 0.46)));
	}

	// Fundo da janela: degradê + dois brilhos grandes. Muda só quando a cor, o tamanho ou a posição da arte
	// mudam, então fica guardado numa imagem pequena (1/4 do tamanho) e é só esticado a cada quadro.
	private BufferedImage fundoCache;
	private int fundoW = -1, fundoH = -1, fundoCx, fundoCy, fundoLado, fundoRgbA, fundoRgbB;

	@Override
	protected void paintComponent(Graphics g) {
		int w = getWidth(), h = getHeight();
		if (w <= 0 || h <= 0) {
			return;
		}
		boolean temArte = heroi.getParent() != null && heroi.getWidth() > 0;
		int cx = 0, cy = 0, lado = 0;
		if (temArte) {
			Rectangle r = SwingUtilities.convertRectangle(heroi.getParent(), heroi.getBounds(), this);
			lado = ladoHeroi(r.width, r.height);
			cx = Math.round(r.x + r.width - lado / 2f - 36);
			cy = Math.round(r.y + r.height / 2f + 6);
		}
		Color a = accentA(), b = accentB();
		if (fundoCache == null || fundoW != w || fundoH != h || fundoCx != cx || fundoCy != cy || fundoLado != lado
				|| fundoRgbA != a.getRGB() || fundoRgbB != b.getRGB()) {
			int bw = Math.max(1, w / 4), bh = Math.max(1, h / 4);
			if (fundoCache == null || fundoCache.getWidth() != bw || fundoCache.getHeight() != bh) {
				fundoCache = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_RGB);
			}
			Graphics2D c = fundoCache.createGraphics();
			c.scale(bw / (double) w, bh / (double) h);
			c.setPaint(new GradientPaint(0, 0, FUNDO_A, w, h, FUNDO_B));
			c.fillRect(0, 0, w, h);
			if (temArte) {
				c.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), lado * 1.05f, new float[] { 0f, 1f },
						new Color[] { comAlpha(a, 90), comAlpha(a, 0) }));
				c.fill(new Rectangle2D.Float(0, 0, w, h));
				c.setPaint(new RadialGradientPaint(new Point2D.Float(cx - lado * 0.3f, cy + lado * 0.25f),
						lado * 0.7f, new float[] { 0f, 1f }, new Color[] { comAlpha(b, 60), comAlpha(b, 0) }));
				c.fill(new Rectangle2D.Float(0, 0, w, h));
			}
			c.dispose();
			fundoW = w;
			fundoH = h;
			fundoCx = cx;
			fundoCy = cy;
			fundoLado = lado;
			fundoRgbA = a.getRGB();
			fundoRgbB = b.getRGB();
		}
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(fundoCache, 0, 0, w, h, null);
		g2.dispose();
	}

	// =============================================================
	// Componentes (classes internas, para usar a cor animada)
	// =============================================================

	/** Área principal: brilho na cor do tipo, nome grande e a arte do Pokémon. */
	class Heroi extends JPanel {
		private static final long serialVersionUID = 1L;
		private transient Pokemon p;
		private transient BufferedImage img;

		// números e artes já prontos (desenhar texto grande e redimensionar a cada quadro deixava lento)
		private transient BufferedImage numImg;
		private int numDex = -1;
		private int numAscentDesc = 0;
		private final transient Map<BufferedImage, BufferedImage> escalas = new java.util.IdentityHashMap<>();
		private int escalaTam = -1;

		private BufferedImage criarNumero(int dex) {
			BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D gm = tmp.createGraphics();
			gm.setFont(fonte(Font.BOLD, 250f));
			FontMetrics fm = gm.getFontMetrics();
			String num = String.format("%03d", dex);
			int largura = fm.stringWidth(num) + 4, altura = fm.getHeight();
			gm.dispose();
			BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			qualidade(g);
			g.setFont(fonte(Font.BOLD, 250f));
			g.setColor(Color.WHITE);
			g.drawString(num, 0, fm.getAscent());
			g.dispose();
			numAscentDesc = fm.getDescent(); // a base do texto fica em h - 4
			return img;
		}

		/** Desenha a arte: redimensionada com qualidade uma vez só (e rápido enquanto a entrada anima). */
		private void desenharArte(Graphics2D g2, BufferedImage src, int x, int y, int tam, boolean pronto) {
			if (!pronto) {
				Object antes = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.drawImage(src, x, y, tam, tam, null);
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, antes);
				return;
			}
			if (escalaTam != tam) {
				escalas.clear();
				escalaTam = tam;
			}
			BufferedImage pronta = escalas.get(src);
			if (pronta == null) {
				pronta = Imagens.reduzir(src, tam);
				escalas.put(src, pronta);
			}
			g2.drawImage(pronta, x, y, null);
		}

		/** Fundo leve e parado atrás da arte: um brilho circular nas cores do tipo (bem mais barato que o cenário animado). */
		private void desenharMedalhaoSimples(Graphics2D g2, float cx, float cy, float raio, Color a, Color b) {
			g2.setPaint(new RadialGradientPaint(cx, cy, raio, new float[] { 0f, 1f },
					new Color[] { comAlpha(a, 110), comAlpha(b, 0) }));
			g2.fill(new Ellipse2D.Float(cx - raio, cy - raio, raio * 2, raio * 2));
			g2.setStroke(new BasicStroke(2f));
			g2.setColor(comAlpha(mix(a, Color.WHITE, 0.4f), 60));
			g2.draw(new Ellipse2D.Float(cx - raio * 0.92f, cy - raio * 0.92f, raio * 1.84f, raio * 1.84f));
		}

		private Rectangle botaoScan = new Rectangle(); // área do botão ESCANEAR (vazia quando não aparece)
		private boolean sobreBotao = false;

		Heroi() {
			setOpaque(false);
			addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					boolean sobre = botaoScan.contains(e.getPoint());
					if (sobre != sobreBotao) {
						sobreBotao = sobre;
						setCursor(Cursor.getPredefinedCursor(sobre ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
					}
				}
			});
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (p != null && botaoScan.contains(e.getPoint())) {
						iniciarScan(p);
					}
				}
			});
		}

		void setPokemon(Pokemon p) {
			this.p = p;
			this.img = (p == null) ? null : Imagens.grande(p.getNumeroDex());
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			int w = getWidth(), h = getHeight();
			double e = easeOut(t);
			Color a = accentA(), b = accentB();

			int lado = ladoHeroi(w, h);
			int cx = w - lado / 2 - 36;
			int cy = h / 2 + 6;

			if (p == null) {
				desenharVazio(g2, w, h, cx, cy, lado);
				g2.dispose();
				return;
			}

			// número gigante ao fundo
			if (numImg == null || numDex != p.getNumeroDex()) {
				numImg = criarNumero(p.getNumeroDex());
				numDex = p.getNumeroDex();
			}
			Composite compNum = g2.getComposite();
			g2.setComposite(AlphaComposite.SrcOver.derive((float) (16 * e / 255.0)));
			g2.drawImage(numImg, w - numImg.getWidth() - 12, h - 4 - numImg.getHeight() + numAscentDesc, null);
			g2.setComposite(compNum);

			// medalhão com cenário do tipo, atrás da arte
			float raio = lado * 0.46f * (float) (0.9 + 0.1 * e);
			Composite antes = g2.getComposite();
			g2.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0.0, Math.min(1.0, e))));
			final boolean conhecido = descoberto(p.getNumeroDex());
			final boolean escaneando = scanDex == p.getNumeroDex();
			if (AMBIENTE_ANIMADO) {
				Ambiente.desenhar(g2, conhecido ? Ambiente.tipoDoCenario(p) : null, p.getNumeroDex(), cx, cy, raio, a, b, fase);
			} else {
				desenharMedalhaoSimples(g2, cx, cy, raio, a, b);
			}
			g2.setComposite(antes);

			// arte flutuando
			double bob = Math.sin(fase) * 7;
			int tam = (int) (lado * (0.86 + 0.14 * e));
			g2.setColor(new Color(0, 0, 0, (int) (70 * e)));
			float larguraSombra = (float) (lado * 0.5 - bob * 1.6);
			g2.fill(new Ellipse2D.Float(cx - larguraSombra / 2, cy + lado / 2f - 18, larguraSombra, 16));

			Composite original = g2.getComposite();
			g2.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0.0, Math.min(1.0, e))));
			if (img != null) {
				int ix = cx - tam / 2, iy = (int) (cy - tam / 2 + bob);
				boolean pronto = e >= 1;
				if (conhecido) {
					desenharArte(g2, img, ix, iy, tam, pronto);
				} else {
					BufferedImage sil = Imagens.silhueta(p.getNumeroDex());
					desenharArte(g2, sil != null ? sil : img, ix, iy, tam, pronto);
					if (escaneando) {
						// a parte já varrida pela linha de scan aparece colorida
						double linhaY = iy + tam * scanT;
						Shape clipAntes = g2.getClip();
						g2.clip(new Rectangle2D.Double(ix, iy, tam, linhaY - iy));
						desenharArte(g2, img, ix, iy, tam, pronto);
						g2.setClip(clipAntes);
						desenharLinhaScan(g2, cx, cy, raio, ix, tam, (float) linhaY);
					}
				}
			} else {
				g2.setColor(comAlpha(Color.WHITE, 60));
				g2.setStroke(new BasicStroke(3f));
				g2.draw(new Ellipse2D.Float(cx - lado * 0.3f, cy - lado * 0.3f, lado * 0.6f, lado * 0.6f));
				g2.setFont(fonte(Font.BOLD, 16f));
				FontMetrics fm = g2.getFontMetrics();
				String aviso = "sem imagem";
				g2.setColor(comAlpha(Color.WHITE, 200));
				g2.drawString(aviso, cx - fm.stringWidth(aviso) / 2f, cy + fm.getAscent() / 2f - 2);
			}
			g2.setComposite(original);

			// bloco de texto à esquerda (entra deslizando)
			int dx = (int) ((1 - e) * -26);
			g2.setComposite(AlphaComposite.SrcOver.derive((float) (0.2 + 0.8 * e)));
			int x = 44 + dx;
			int larguraTexto = Math.max(240, cx - lado / 2 - 56);

			g2.setFont(fonte(Font.BOLD, 17f));
			g2.setColor(mix(a, Color.WHITE, 0.55f));
			g2.drawString(String.format("Nº %03d", p.getNumeroDex()), x, 62);

			float tamNome = 64f;
			g2.setFont(fonte(Font.BOLD, tamNome));
			final String nomeMostrado = conhecido ? p.getNome() : "???";
			while (g2.getFontMetrics().stringWidth(nomeMostrado) > larguraTexto && tamNome > 30f) {
				tamNome -= 2f;
				g2.setFont(fonte(Font.BOLD, tamNome));
			}
			g2.setColor(TEXTO);
			int baseNome = 62 + 12 + (int) (tamNome * 0.95f);
			g2.drawString(nomeMostrado, x, baseNome);

			int yPilulas = baseNome + 22;
			botaoScan = new Rectangle();
			if (!conhecido) {
				if (escaneando) {
					desenharProgresso(g2, x, yPilulas);
				} else {
					botaoScan = new Rectangle(x, yPilulas, 190, 42);
					desenharBotaoScan(g2, botaoScan);
				}
				g2.setFont(fonte(Font.PLAIN, 15f));
				g2.setColor(SUAVE);
				g2.drawString("Região", x, yPilulas + 34 + 36);
				g2.setFont(fonte(Font.BOLD, 24f));
				g2.setColor(comAlpha(TEXTO, 140));
				g2.drawString("???", x, yPilulas + 34 + 66);
				g2.setComposite(original);
				desenharClarao(g2, cx, cy, raio);
				g2.dispose();
				return;
			}
			int px = x;
			px += desenharPilula(g2, p.getTipo1(), px, yPilulas, corDoTipo(p.getTipo1())) + 10;
			if (p.getTipo2() != null) {
				desenharPilula(g2, p.getTipo2(), px, yPilulas, corDoTipo(p.getTipo2()));
			}

			if (p.getRegiao() != null) {
				g2.setFont(fonte(Font.PLAIN, 15f));
				g2.setColor(SUAVE);
				g2.drawString("Região", x, yPilulas + 34 + 36);
				g2.setFont(fonte(Font.BOLD, 24f));
				g2.setColor(TEXTO);
				g2.drawString(p.getRegiao(), x, yPilulas + 34 + 66);
			}
			g2.setComposite(original);
			desenharClarao(g2, cx, cy, raio);
			g2.dispose();
		}

		/** Clarão branco ao revelar um Pokémon. */
		private void desenharClarao(Graphics2D g2, float cx, float cy, float raio) {
			if (flash <= 0) {
				return;
			}
			g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), raio * 1.15f, new float[] { 0f, 1f },
					new Color[] { new Color(255, 255, 255, (int) (200 * flash)), new Color(255, 255, 255, 0) }));
			g2.fill(new Ellipse2D.Float(cx - raio * 1.15f, cy - raio * 1.15f, raio * 2.3f, raio * 2.3f));
		}

		private void desenharBotaoScan(Graphics2D g2, Rectangle r) {
			Color ciano = new Color(0x38, 0xD8, 0xF2);
			float pulso = (float) (0.5 + 0.5 * Math.sin(fase * 1.6));
			g2.setColor(comAlpha(ciano, (int) (30 + 40 * pulso)));
			g2.fill(new RoundRectangle2D.Float(r.x - 5, r.y - 5, r.width + 10, r.height + 10, 26, 26));
			g2.setPaint(new GradientPaint(r.x, r.y, mix(ciano, Color.BLACK, sobreBotao ? 0.10f : 0.25f), r.x + r.width,
					r.y + r.height, mix(new Color(0x4A, 0x7B, 0xFF), Color.BLACK, 0.2f)));
			g2.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 22, 22));
			g2.setColor(comAlpha(Color.WHITE, 120));
			g2.setStroke(new BasicStroke(1.2f));
			g2.draw(new RoundRectangle2D.Float(r.x + 0.6f, r.y + 0.6f, r.width - 1.2f, r.height - 1.2f, 22, 22));

			// ícone de mira
			float ix = r.x + 26, iy = r.y + r.height / 2f;
			g2.setColor(Color.WHITE);
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(new Ellipse2D.Float(ix - 8, iy - 8, 16, 16));
			g2.draw(new java.awt.geom.Line2D.Float(ix - 12, iy, ix - 4, iy));
			g2.draw(new java.awt.geom.Line2D.Float(ix + 4, iy, ix + 12, iy));
			g2.draw(new java.awt.geom.Line2D.Float(ix, iy - 12, ix, iy - 4));
			g2.draw(new java.awt.geom.Line2D.Float(ix, iy + 4, ix, iy + 12));

			g2.setFont(fonte(Font.BOLD, 16f));
			g2.drawString("ESCANEAR", r.x + 50, r.y + r.height / 2 + 6);
		}

		private void desenharProgresso(Graphics2D g2, int x, int y) {
			Color ciano = new Color(0x38, 0xD8, 0xF2);
			g2.setFont(fonte(Font.BOLD, 15f));
			g2.setColor(ciano);
			int pontos = (int) (fase * 1.5) % 4;
			g2.drawString("ESCANEANDO" + ".".repeat(pontos) + String.format("   %d%%", (int) (scanT * 100)), x, y + 16);
			g2.setColor(new Color(255, 255, 255, 30));
			g2.fill(new RoundRectangle2D.Float(x, y + 28, 190, 8, 8, 8));
			g2.setColor(ciano);
			g2.fill(new RoundRectangle2D.Float(x, y + 28, (float) (190 * scanT), 8, 8, 8));
		}

		/** Linha luminosa e grade azulada da varredura, recortadas no medalhão. */
		private void desenharLinhaScan(Graphics2D g2, float cx, float cy, float raio, int ix, int tam, float linhaY) {
			Color ciano = new Color(0x38, 0xD8, 0xF2);
			Shape antes = g2.getClip();
			g2.clip(new Ellipse2D.Float(cx - raio, cy - raio, raio * 2, raio * 2));
			// brilho que fica para trás da linha
			g2.setPaint(new GradientPaint(0, linhaY - 70, comAlpha(ciano, 0), 0, linhaY, comAlpha(ciano, 110)));
			g2.fill(new Rectangle2D.Float(cx - raio, linhaY - 70, raio * 2, 70));
			// grade
			g2.setColor(comAlpha(ciano, 40));
			g2.setStroke(new BasicStroke(1f));
			for (float gy = cy - raio; gy < cy + raio; gy += 18) {
				g2.draw(new java.awt.geom.Line2D.Float(cx - raio, gy, cx + raio, gy));
			}
			for (float gx = cx - raio; gx < cx + raio; gx += 18) {
				g2.draw(new java.awt.geom.Line2D.Float(gx, cy - raio, gx, cy + raio));
			}
			// a linha
			g2.setColor(Color.WHITE);
			g2.setStroke(new BasicStroke(2.5f));
			g2.draw(new java.awt.geom.Line2D.Float(cx - raio, linhaY, cx + raio, linhaY));
			g2.setClip(antes);
		}

		private void desenharVazio(Graphics2D g2, int w, int h, int cx, int cy, int lado) {
			Graphics2D g = (Graphics2D) g2.create();
			g.rotate(Math.sin(fase * 0.5) * 0.12, cx, cy);
			desenharPokebola(g, cx, cy, lado * 0.3f, PADRAO_A, 210);
			g.dispose();

			g2.setFont(fonte(Font.BOLD, 64f));
			g2.setColor(TEXTO);
			g2.drawString("Pokédex", 44, h / 2 - 10);
			g2.setFont(fonte(Font.PLAIN, 18f));
			g2.setColor(SUAVE);
			g2.drawString("Escolha um Pokémon na lista", 46, h / 2 + 28);
			g2.drawString("ou busque pelo nome ou número.", 46, h / 2 + 54);
		}
	}

	/** Cartão de fundo arredondado; o de curiosidade ganha um toque da cor do tipo. */
	class CartaoPainel extends JPanel {
		private static final long serialVersionUID = 1L;
		private final boolean tingir;

		CartaoPainel(boolean tingir) {
			this.tingir = tingir;
			setOpaque(false);
			setBorder(new EmptyBorder(18, 20, 16, 20));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			Color fundo = tingir ? mix(PAINEL, accentA(), 0.20f) : PAINEL;
			Color borda = tingir ? mix(BORDA, accentA(), 0.55f) : BORDA;
			g2.setColor(fundo);
			g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 22, 22));
			g2.setColor(borda);
			g2.setStroke(new BasicStroke(1.2f));
			g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, getWidth() - 2.2f, getHeight() - 2.2f, 22, 22));
			g2.dispose();
			super.paintComponent(g);
		}
	}

	/** Título de cartão: ponto na cor do tipo + texto. */
	class Titulo extends JComponent {
		private static final long serialVersionUID = 1L;
		private final String texto;

		Titulo(String texto) {
			this.texto = texto;
			setAlignmentX(Component.LEFT_ALIGNMENT);
			setPreferredSize(new Dimension(100, 22));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			g2.setColor(accentA());
			g2.fill(new Ellipse2D.Float(0, 6, 10, 10));
			g2.setFont(fonte(Font.BOLD, 16f));
			g2.setColor(TEXTO);
			FontMetrics fm = g2.getFontMetrics();
			g2.drawString(texto, 20, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
			g2.dispose();
		}
	}

	/** Barra de medida que cresce até o valor novo quando o Pokémon muda. */
	class Barra extends JComponent {
		private static final long serialVersionUID = 1L;
		private double de = 0;
		private double para = 0;

		Barra() {
			setPreferredSize(new Dimension(100, 10));
			setMinimumSize(new Dimension(10, 10));
		}

		void definir(double razao) {
			de = valor();
			para = Math.max(0, Math.min(1, razao));
		}

		private double valor() {
			return de + (para - de) * easeOut(t);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			int w = getWidth(), h = getHeight();
			g2.setColor(comAlpha(Color.BLACK, 70));
			g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
			float largura = (float) Math.max(h, w * valor());
			if (para > 0 || de > 0) {
				g2.setPaint(new GradientPaint(0, 0, accentA(), w, 0, accentB()));
				g2.fill(new RoundRectangle2D.Float(0, 0, largura, h, h, h));
			}
			g2.dispose();
		}
	}
}
