package PokePackage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import static PokePackage.PokemonViewer.PokemonViewer.*;

/**
 * Tela do modo batalha: o jogador monta o time (até 3) com os Pokémon que já capturou (descobriu).
 * Clique num Pokémon da lista para colocar no time e clique de novo para tirar. A ordem do clique é a
 * ordem de entrada em campo.
 */
public class SeletorBatalha extends JDialog {

	private static final long serialVersionUID = 1L;
	public static final int MAXIMO = 3;
	/** Limite real: precisa sobrar pelo menos 1 Pokémon capturado para ser o adversário. */
	private final int limite;

	private final transient List<Pokemon> capturados;
	private final transient List<Pokemon> time = new ArrayList<>();
	private final DefaultListModel<Pokemon> modelo = new DefaultListModel<>();
	private final JList<Pokemon> jlist = new JList<>(modelo);
	private final Slots slots = new Slots();
	private final BotaoTexto botaoLutar = new BotaoTexto("Lutar!", "Começar a batalha");
	private final JLabel contagem = new JLabel();
	private boolean confirmado = false;

	/**
	 * @param capturados Pokémon já descobertos que têm golpes cadastrados
	 * @param inicial    Pokémon que já começa no time (pode ser null)
	 */
	public SeletorBatalha(Frame dono, List<Pokemon> capturados, Pokemon inicial) {
		super(dono, "Modo Batalha", true);
		this.capturados = new ArrayList<>(capturados);
		this.limite = Math.max(1, Math.min(MAXIMO, capturados.size() - 1));
		if (inicial != null && this.capturados.contains(inicial)) {
			time.add(inicial);
		}

		JPanel raiz = new JPanel(new BorderLayout(18, 0));
		raiz.setBackground(FUNDO_A);
		raiz.setBorder(new EmptyBorder(18, 18, 18, 18));
		raiz.add(montarLista(), BorderLayout.WEST);
		raiz.add(montarTime(), BorderLayout.CENTER);
		raiz.setPreferredSize(new Dimension(900, 620));
		setContentPane(raiz);
		setResizable(false);
		pack();
		setLocationRelativeTo(dono);
		atualizar();
	}

	/** Mostra a janela e devolve o time escolhido (vazio se cancelou). */
	public List<Pokemon> escolher() {
		setVisible(true);
		return confirmado ? new ArrayList<>(time) : new ArrayList<>();
	}

	private JComponent montarLista() {
		JPanel col = new JPanel(new BorderLayout(0, 10));
		col.setOpaque(false);
		col.setPreferredSize(new Dimension(380, 100));

		JLabel titulo = new JLabel("Seus Pokémon capturados");
		titulo.setFont(fonte(Font.BOLD, 20f));
		titulo.setForeground(TEXTO);
		CampoBusca campo = new CampoBusca();
		campo.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				filtrar(campo.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				filtrar(campo.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				filtrar(campo.getText());
			}
		});
		JPanel topo = new JPanel(new BorderLayout(0, 10));
		topo.setOpaque(false);
		topo.add(titulo, BorderLayout.NORTH);
		topo.add(campo, BorderLayout.CENTER);
		col.add(topo, BorderLayout.NORTH);

		for (Pokemon p : capturados) {
			modelo.addElement(p);
		}
		LinhaPokemon linha = new LinhaPokemon(p -> true);
		jlist.setFixedCellHeight(64);
		jlist.setOpaque(false);
		jlist.setBackground(new Color(0, 0, 0, 0));
		jlist.setCellRenderer((lista, valor, indice, sel, foco) -> linha.getListCellRendererComponent(lista, valor, indice, time.contains(valor), foco));
		jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jlist.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int i = jlist.locationToIndex(e.getPoint());
				if (i < 0 || !jlist.getCellBounds(i, i).contains(e.getPoint())) {
					return;
				}
				alternar(modelo.get(i));
				jlist.clearSelection();
			}
		});
		JScrollPane rolagem = new JScrollPane(jlist);
		rolagem.setBorder(BorderFactory.createLineBorder(BORDA));
		rolagem.setOpaque(false);
		rolagem.getViewport().setOpaque(false);
		rolagem.getVerticalScrollBar().setUI(new BarraRolagem());
		rolagem.getVerticalScrollBar().setBackground(FUNDO_A);
		rolagem.getVerticalScrollBar().setUnitIncrement(24);
		col.add(rolagem, BorderLayout.CENTER);
		return col;
	}

	private JComponent montarTime() {
		JPanel col = new JPanel(new BorderLayout(0, 12));
		col.setOpaque(false);

		JPanel topo = new JPanel();
		topo.setOpaque(false);
		topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
		JLabel titulo = new JLabel("Seu time");
		titulo.setFont(fonte(Font.BOLD, 20f));
		titulo.setForeground(TEXTO);
		titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
		contagem.setFont(fonte(Font.PLAIN, 14f));
		contagem.setForeground(SUAVE);
		contagem.setAlignmentX(Component.LEFT_ALIGNMENT);
		topo.add(titulo);
		topo.add(Box.createVerticalStrut(4));
		topo.add(contagem);
		col.add(topo, BorderLayout.NORTH);

		col.add(slots, BorderLayout.CENTER);

		JPanel baixo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		baixo.setOpaque(false);
		BotaoTexto cancelar = new BotaoTexto("Cancelar", "Voltar sem lutar");
		cancelar.addActionListener(e -> dispose());
		botaoLutar.setPreferredSize(new Dimension(140, 46));
		botaoLutar.addActionListener(e -> {
			if (!time.isEmpty()) {
				confirmado = true;
				dispose();
			}
		});
		baixo.add(cancelar);
		baixo.add(botaoLutar);
		col.add(baixo, BorderLayout.SOUTH);
		return col;
	}

	private void alternar(Pokemon p) {
		if (time.contains(p)) {
			time.remove(p);
		} else if (time.size() < limite) {
			time.add(p);
		}
		atualizar();
	}

	private void atualizar() {
		contagem.setText(time.size() + " de " + limite + " no time · o adversário terá o mesmo número"
				+ (limite < MAXIMO ? " (descubra mais Pokémon para montar times maiores)" : ""));
		botaoLutar.setEnabled(!time.isEmpty());
		jlist.repaint();
		slots.repaint();
	}

	private void filtrar(String texto) {
		String alvo = normalizar(texto.trim());
		modelo.clear();
		for (Pokemon p : capturados) {
			if (alvo.isEmpty() || normalizar(p.getNome()).contains(alvo) || String.valueOf(p.getNumeroDex()).equals(alvo)) {
				modelo.addElement(p);
			}
		}
	}

	private static String normalizar(String s) {
		return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
	}

	// =====================================================================
	// Cartões do time
	// =====================================================================
	private class Slots extends JComponent {
		private static final long serialVersionUID = 1L;

		Slots() {
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					int i = indiceEm(e.getPoint());
					if (i >= 0 && i < time.size()) {
						time.remove(i);
						atualizar();
					}
				}
			});
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		private int indiceEm(Point pt) {
			int lado = getWidth() / MAXIMO;
			return Math.min(MAXIMO - 1, pt.x / Math.max(1, lado));
		}

		@Override
		protected void paintComponent(Graphics g0) {
			Graphics2D g = (Graphics2D) g0.create();
			qualidade(g);
			int gap = 14, larg = (getWidth() - gap * (MAXIMO - 1)) / MAXIMO, alt = Math.min(getHeight(), 360);
			for (int i = 0; i < MAXIMO; i++) {
				int x = i * (larg + gap), y = 0;
				Pokemon p = i < time.size() ? time.get(i) : null;
				Color cor = p == null ? BORDA : corDoTipo(p.getTipo1());
				g.setPaint(new GradientPaint(0, y, p == null ? PAINEL : mix(PAINEL, cor, 0.35f), 0, y + alt,
						p == null ? escurecer(PAINEL, 0.7f) : escurecer(cor, 0.35f)));
				g.fill(new RoundRectangle2D.Float(x, y, larg, alt, 24, 24));
				g.setColor(p == null ? BORDA : comAlpha(cor, 200));
				g.setStroke(new BasicStroke(1.6f));
				g.draw(new RoundRectangle2D.Float(x + 0.8f, y + 0.8f, larg - 1.6f, alt - 1.6f, 24, 24));

				g.setFont(fonte(Font.BOLD, 14f));
				g.setColor(p == null ? SUAVE : comAlpha(Color.WHITE, 200));
				g.drawString((i + 1) + "º a entrar", x + 14, y + 26);
				if (p == null) {
					g.setFont(fonte(Font.PLAIN, 14f));
					g.setColor(SUAVE);
					String t = "vazio";
					g.drawString(t, x + (larg - g.getFontMetrics().stringWidth(t)) / 2, y + alt / 2);
					continue;
				}
				BufferedImage img = Imagens.grande(p.getNumeroDex());
				if (img != null) {
					int lado = larg - 20;
					double esc = Math.min((double) lado / img.getWidth(), (double) lado / img.getHeight());
					int dw = (int) (img.getWidth() * esc), dh = (int) (img.getHeight() * esc);
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					g.drawImage(img, x + (larg - dw) / 2, y + 40, dw, dh, null);
				}
				g.setFont(fonte(Font.BOLD, 18f));
				g.setColor(TEXTO);
				String nome = p.getNome();
				g.drawString(nome, x + (larg - g.getFontMetrics().stringWidth(nome)) / 2, y + alt - 40);
				g.setFont(fonte(Font.PLAIN, 12f));
				g.setColor(new Color(255, 255, 255, 190));
				String tipos = p.getTipo1() + (p.getTipo2() != null ? " · " + p.getTipo2() : "");
				g.drawString(tipos, x + (larg - g.getFontMetrics().stringWidth(tipos)) / 2, y + alt - 20);
				g.setColor(new Color(255, 255, 255, 120));
				g.fill(new Ellipse2D.Float(x + larg - 28, y + 12, 16, 16));
				g.setColor(new Color(0x14, 0x17, 0x2B));
				g.setFont(fonte(Font.BOLD, 13f));
				g.drawString("×", x + larg - 24, y + 25);
			}
			g.dispose();
		}
	}
}
