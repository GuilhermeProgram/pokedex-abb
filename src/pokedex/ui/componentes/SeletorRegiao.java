package pokedex.ui.componentes;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import static pokedex.ui.tema.Tema.*;

/**
 * Seletor de região da lista lateral: setas para trocar e, no centro, o nome da região com o progresso
 * (ex.: Kanto  3/151). A primeira opção é "Todas".
 */
public class SeletorRegiao extends JComponent {
	private static final long serialVersionUID = 1L;
	private static final int ZONA_SETA = 40;

	private final List<String> regioes = new ArrayList<>(); // posição 0 = todas (null)
	private int indice = 0;
	private String progresso = "";
	private int hover = 0; // -1 seta esquerda, 1 seta direita, 2 centro, 0 nenhum
	private transient Runnable aoMudar = () -> { };

	public SeletorRegiao(List<String> nomes) {
		regioes.add(null);
		regioes.addAll(nomes);
		setPreferredSize(new Dimension(100, 40));
		setToolTipText("Filtrar a lista por região");
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				mudarHover(zona(e.getX()));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				mudarHover(0);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				trocar(zona(e.getX()) == -1 ? -1 : 1);
			}

			@Override
			public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
				trocar(e.getWheelRotation() > 0 ? 1 : -1);
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);
	}

	private int zona(int x) {
		return x < ZONA_SETA ? -1 : x > getWidth() - ZONA_SETA ? 1 : 2;
	}

	private void mudarHover(int z) {
		if (z != hover) {
			hover = z;
			repaint();
		}
	}

	public void aoMudar(Runnable acao) {
		this.aoMudar = acao;
	}

	/** Região escolhida, ou null para todas. */
	public String regiao() {
		return regioes.get(indice);
	}

	public void escolher(String regiao) {
		int i = regioes.indexOf(regiao);
		if (i >= 0 && i != indice) {
			indice = i;
			aoMudar.run();
			repaint();
		}
	}

	public void setProgresso(String texto) {
		if (!texto.equals(progresso)) {
			progresso = texto;
			repaint();
		}
	}

	private void trocar(int passo) {
		indice = (indice + passo + regioes.size()) % regioes.size();
		aoMudar.run();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		qualidade(g2);
		int w = getWidth(), h = getHeight();
		g2.setColor(PAINEL);
		g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, h, h));
		g2.setColor(BORDA);
		g2.setStroke(new BasicStroke(1.2f));
		g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 2.2f, h - 2.2f, h, h));

		// setas desenhadas à mão (não dependem da fonte)
		g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int lado = -1; lado <= 1; lado += 2) {
			g2.setColor(hover == lado ? TEXTO : SUAVE);
			int cx = lado < 0 ? ZONA_SETA / 2 + 2 : w - ZONA_SETA / 2 - 2;
			int cy = h / 2;
			g2.drawLine(cx - lado * 2, cy - 5, cx + lado * 3, cy);
			g2.drawLine(cx + lado * 3, cy, cx - lado * 2, cy + 5);
		}

		// nome + progresso, centralizados juntos
		String nome = regiao() == null ? "Todas" : regiao();
		Font fNome = fonte(Font.BOLD, 14f), fProg = fonte(Font.PLAIN, 13f);
		FontMetrics mNome = g2.getFontMetrics(fNome), mProg = g2.getFontMetrics(fProg);
		int larguraNome = mNome.stringWidth(nome);
		int larguraProg = progresso.isEmpty() ? 0 : mProg.stringWidth(progresso) + 10;
		float x = (w - larguraNome - larguraProg) / 2f;
		float y = (h - mNome.getHeight()) / 2f + mNome.getAscent();
		g2.setFont(fNome);
		g2.setColor(TEXTO);
		g2.drawString(nome, x, y);
		if (larguraProg > 0) {
			g2.setFont(fProg);
			g2.setColor(SUAVE);
			g2.drawString(progresso, x + larguraNome + 10, y);
		}
		g2.dispose();
	}
}
