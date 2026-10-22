package PokePackage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static PokePackage.PokemonViewer.*;

/**
 * PC: a caixa onde ficam os Pokémon capturados. Por trás dela está a ABB (ordenada pelo número Dex).
 * A lista mostra o percurso em ordem da árvore, e o botão Liberar chama {@code remover} na ABB.
 */
public class JanelaPC extends JDialog {

	private static final long serialVersionUID = 1L;

	private final transient ArvoreBinaria pc;
	private final transient Runnable aoMudar;
	private final DefaultListModel<Pokemon> modelo = new DefaultListModel<>();
	private final JList<Pokemon> jlist = new JList<>(modelo);
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
		raiz.add(montarLista(), BorderLayout.WEST);
		raiz.add(montarDetalhe(), BorderLayout.CENTER);
		raiz.setPreferredSize(new Dimension(820, 560));
		setContentPane(raiz);
		setResizable(false);
		pack();
		setLocationRelativeTo(dono);
		recarregar();
	}

	private JComponent montarLista() {
		JPanel col = new JPanel(new BorderLayout(0, 10));
		col.setOpaque(false);
		col.setPreferredSize(new Dimension(360, 100));

		JLabel titulo = new JLabel("Pokémon no PC");
		titulo.setFont(fonte(Font.BOLD, 20f));
		titulo.setForeground(TEXTO);
		col.add(titulo, BorderLayout.NORTH);

		LinhaPokemon linha = new LinhaPokemon(p -> true);
		jlist.setFixedCellHeight(64);
		jlist.setOpaque(false);
		jlist.setBackground(new Color(0, 0, 0, 0));
		jlist.setCellRenderer(linha);
		jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jlist.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				cartao.repaint();
				botaoLiberar.setEnabled(jlist.getSelectedValue() != null);
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

	private JComponent montarDetalhe() {
		JPanel col = new JPanel(new BorderLayout(0, 12));
		col.setOpaque(false);

		info.setFont(fonte(Font.PLAIN, 14f));
		info.setForeground(SUAVE);
		info.setPreferredSize(new Dimension(100, 24));
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

	/** Relê a ABB em ordem (esquerda, raiz, direita) e atualiza a lista e o resumo. */
	private void recarregar() {
		Pokemon selecionado = jlist.getSelectedValue();
		modelo.clear();
		List<Pokemon> emOrdem = pc.emOrdem();
		for (Pokemon p : emOrdem) {
			modelo.addElement(p);
		}
		int n = emOrdem.size();
		int ideal = 32 - Integer.numberOfLeadingZeros(n); // menor altura possível para n nós
		info.setText(n == 0 ? "O PC está vazio. Cadastre Pokémon na Pokédex."
				: n + " no PC · altura da ABB: " + pc.altura() + " (mínima possível: " + ideal + ")");
		if (selecionado != null && modelo.contains(selecionado)) {
			jlist.setSelectedValue(selecionado, true);
		} else if (n > 0) {
			jlist.setSelectedIndex(0);
		}
		botaoLiberar.setEnabled(jlist.getSelectedValue() != null);
		cartao.repaint();
	}

	private void liberar() {
		Pokemon p = jlist.getSelectedValue();
		if (p == null) {
			return;
		}
		int r = JOptionPane.showConfirmDialog(this, "Liberar " + p.getNome() + "? Ele sai do PC e volta a ser desconhecido.",
				"Liberar Pokémon", JOptionPane.YES_NO_OPTION);
		if (r != JOptionPane.YES_OPTION) {
			return;
		}
		int indice = jlist.getSelectedIndex();
		pc.remover(p.getNumeroDex());
		recarregar();
		if (modelo.size() > 0) {
			jlist.setSelectedIndex(Math.min(indice, modelo.size() - 1));
		}
		aoMudar.run();
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
			Pokemon p = jlist.getSelectedValue();
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
				int lado = Math.min(w - 40, h - 120);
				double esc = Math.min((double) lado / img.getWidth(), (double) lado / img.getHeight());
				int dw = (int) (img.getWidth() * esc), dh = (int) (img.getHeight() * esc);
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(img, (w - dw) / 2, 18, dw, dh, null);
			}
			g.setFont(fonte(Font.BOLD, 22f));
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
