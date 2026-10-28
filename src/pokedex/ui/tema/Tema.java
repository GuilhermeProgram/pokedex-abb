package pokedex.ui.tema;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Cores, fontes e utilidades de desenho compartilhadas pela interface. */
public final class Tema {
	private Tema() {}

	public static final Color FUNDO_A = new Color(0x0D, 0x10, 0x1F);
	public static final Color FUNDO_B = new Color(0x19, 0x1E, 0x38);
	public static final Color PAINEL = new Color(0x1E, 0x24, 0x42);
	public static final Color BORDA = new Color(0x2E, 0x36, 0x5C);
	public static final Color CAMPO = new Color(0x15, 0x19, 0x30);
	public static final Color TEXTO = new Color(0xF4, 0xF6, 0xFC);
	public static final Color SUAVE = new Color(0x9B, 0xA4, 0xC4);
	public static final Color PADRAO_A = new Color(0x5B, 0x6C, 0xFF);
	public static final Color PADRAO_B = new Color(0x9B, 0x4D, 0xFF);

	public static final Map<String, Color> CORES_TIPO = new HashMap<>();
	static {
		CORES_TIPO.put("normal", new Color(0xA8, 0xA7, 0x7A));
		CORES_TIPO.put("fogo", new Color(0xEE, 0x81, 0x30));
		CORES_TIPO.put("água", new Color(0x63, 0x90, 0xF0));
		CORES_TIPO.put("agua", new Color(0x63, 0x90, 0xF0));
		CORES_TIPO.put("elétrico", new Color(0xF2, 0xC4, 0x1B));
		CORES_TIPO.put("eletrico", new Color(0xF2, 0xC4, 0x1B));
		CORES_TIPO.put("planta", new Color(0x7A, 0xC7, 0x4C));
		CORES_TIPO.put("gelo", new Color(0x7F, 0xCF, 0xCB));
		CORES_TIPO.put("lutador", new Color(0xC2, 0x2E, 0x28));
		CORES_TIPO.put("veneno", new Color(0xA3, 0x3E, 0xA1));
		CORES_TIPO.put("terra", new Color(0xD8, 0xB0, 0x55));
		CORES_TIPO.put("voador", new Color(0x8F, 0x7B, 0xE6));
		CORES_TIPO.put("psíquico", new Color(0xF9, 0x55, 0x87));
		CORES_TIPO.put("psiquico", new Color(0xF9, 0x55, 0x87));
		CORES_TIPO.put("inseto", new Color(0xA6, 0xB9, 0x1A));
		CORES_TIPO.put("pedra", new Color(0xB6, 0xA1, 0x36));
		CORES_TIPO.put("fantasma", new Color(0x73, 0x57, 0x97));
		CORES_TIPO.put("dragão", new Color(0x6F, 0x35, 0xFC));
		CORES_TIPO.put("dragao", new Color(0x6F, 0x35, 0xFC));
		CORES_TIPO.put("sombrio", new Color(0x70, 0x57, 0x46));
		CORES_TIPO.put("aço", new Color(0x8E, 0x93, 0xB5));
		CORES_TIPO.put("aco", new Color(0x8E, 0x93, 0xB5));
		CORES_TIPO.put("fada", new Color(0xD6, 0x85, 0xAD));
	}

	public static Color corDoTipo(String tipo) {
		if (tipo == null) {
			return new Color(0x9C, 0xA3, 0xAF);
		}
		return CORES_TIPO.getOrDefault(tipo.toLowerCase(), new Color(0x9C, 0xA3, 0xAF));
	}

	public static Color escurecer(Color c, float f) {
		return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f));
	}

	public static Color mix(Color a, Color b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
				(int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
				(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}

	public static Color comAlpha(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
	}

	public static boolean clara(Color c) {
		return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) > 165;
	}

	public static double easeOut(double t) {
		double u = 1 - Math.max(0, Math.min(1, t));
		return 1 - u * u * u;
	}

	private static final Map<Integer, Font> FONTES = new ConcurrentHashMap<>();

	/** Fontes são guardadas: criar uma Font nova a cada quadro deixava a animação lenta. */
	public static Font fonte(int estilo, float tamanho) {
		int chave = estilo * 100000 + Math.round(tamanho * 10);
		Font f = FONTES.get(chave);
		if (f == null) {
			f = new Font("Segoe UI", estilo, 12).deriveFont(tamanho);
			FONTES.put(chave, f);
		}
		return f;
	}

	public static void qualidade(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	}

	public static String normalizar(String s) {
		String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD);
		return n.replaceAll("\\p{M}", "").toLowerCase().trim();
	}

	/** Etiqueta de tipo (a cor do tipo é a identidade visual de cada Pokémon). */
	public static int desenharPilula(Graphics2D g2, String texto, int x, int y, Color cor) {
		g2.setFont(fonte(Font.BOLD, 15f));
		FontMetrics fm = g2.getFontMetrics();
		int h = 34;
		int w = fm.stringWidth(texto) + 38;
		g2.setPaint(new GradientPaint(x, y, mix(cor, Color.WHITE, 0.14f), x, y + h, escurecer(cor, 0.84f)));
		g2.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
		g2.setColor(comAlpha(Color.WHITE, 60));
		g2.setStroke(new BasicStroke(1.2f));
		g2.draw(new RoundRectangle2D.Float(x + 0.6f, y + 0.6f, w - 1.2f, h - 1.2f, h, h));
		g2.setColor(clara(cor) ? new Color(0x1A, 0x1A, 0x22) : Color.WHITE);
		g2.drawString(texto, x + 19, y + (h - fm.getHeight()) / 2f + fm.getAscent());
		return w;
	}

	/** Desenho simples de uma pokébola, usado na tela inicial. */
	public static void desenharPokebola(Graphics2D g2, float cx, float cy, float r, Color topo, int alpha) {
		g2.setColor(comAlpha(topo, alpha));
		g2.fill(new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, 0, 180, Arc2D.CHORD));
		g2.setColor(new Color(255, 255, 255, (int) (alpha * 0.9)));
		g2.fill(new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, 180, 180, Arc2D.CHORD));
		g2.setColor(new Color(0x12, 0x15, 0x28, alpha));
		g2.fill(new Rectangle2D.Float(cx - r, cy - r * 0.09f, 2 * r, r * 0.18f));
		g2.setStroke(new BasicStroke(r * 0.07f));
		g2.draw(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));
		g2.fill(new Ellipse2D.Float(cx - r * 0.3f, cy - r * 0.3f, r * 0.6f, r * 0.6f));
		g2.setColor(new Color(255, 255, 255, alpha));
		g2.fill(new Ellipse2D.Float(cx - r * 0.18f, cy - r * 0.18f, r * 0.36f, r * 0.36f));
	}

	/** Cenário animado atrás da arte (partículas por tipo). Desligado por ser pesado; mude para true para ver de novo. */
	public static final boolean AMBIENTE_ANIMADO = false;
}
