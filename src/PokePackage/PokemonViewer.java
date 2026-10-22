package PokePackage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Janela da Pokédex.
 *
 * O viewer não conhece a ABB. Ele recebe duas coisas:
 *  - buscador: texto digitado -> Pokemon (ou null). É a busca que o grupo liga na árvore.
 *  - todos:    fornece a lista de Pokémon em ordem (para a lista lateral).
 *              Com a ABB, basta devolver o percurso em ordem da árvore.
 *
 * As imagens são lidas de images/{numeroDex}.png (criadas pelo BaixadorImagens).
 */
public class PokemonViewer extends JFrame {

	private static final long serialVersionUID = 1L;

	private final PainelPokedex painel;

	public PokemonViewer(Function<String, Pokemon> buscador) {
		this(buscador, () -> new ArrayList<Pokemon>());
	}

	public PokemonViewer(Function<String, Pokemon> buscador, Supplier<List<Pokemon>> todos) {
		super("Pokédex");
		painel = new PainelPokedex(buscador, todos);
		setContentPane(painel);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(1200, 760);
		setMinimumSize(new Dimension(1040, 680));
		setLocationRelativeTo(null);
	}

	/** Mostra o Pokémon na tela (pode ser chamado de fora, se precisar). */
	public void exibir(Pokemon p) {
		painel.exibir(p);
	}

	// =====================================================================
	// Tema e utilidades de desenho
	// =====================================================================

	static final Color FUNDO_A = new Color(0x0D, 0x10, 0x1F);
	static final Color FUNDO_B = new Color(0x19, 0x1E, 0x38);
	static final Color PAINEL = new Color(0x1E, 0x24, 0x42);
	static final Color BORDA = new Color(0x2E, 0x36, 0x5C);
	static final Color CAMPO = new Color(0x15, 0x19, 0x30);
	static final Color TEXTO = new Color(0xF4, 0xF6, 0xFC);
	static final Color SUAVE = new Color(0x9B, 0xA4, 0xC4);
	static final Color PADRAO_A = new Color(0x5B, 0x6C, 0xFF);
	static final Color PADRAO_B = new Color(0x9B, 0x4D, 0xFF);

	static final Map<String, Color> CORES_TIPO = new HashMap<>();
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

	static Color corDoTipo(String tipo) {
		if (tipo == null) {
			return new Color(0x9C, 0xA3, 0xAF);
		}
		return CORES_TIPO.getOrDefault(tipo.toLowerCase(), new Color(0x9C, 0xA3, 0xAF));
	}

	static Color escurecer(Color c, float f) {
		return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f));
	}

	static Color mix(Color a, Color b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
				(int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
				(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}

	static Color comAlpha(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
	}

	static boolean clara(Color c) {
		return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) > 165;
	}

	static double easeOut(double t) {
		double u = 1 - Math.max(0, Math.min(1, t));
		return 1 - u * u * u;
	}

	private static final Map<Integer, Font> FONTES = new ConcurrentHashMap<>();

	/** Fontes são guardadas: criar uma Font nova a cada quadro deixava a animação lenta. */
	static Font fonte(int estilo, float tamanho) {
		int chave = estilo * 100000 + Math.round(tamanho * 10);
		Font f = FONTES.get(chave);
		if (f == null) {
			f = new Font("Segoe UI", estilo, 12).deriveFont(tamanho);
			FONTES.put(chave, f);
		}
		return f;
	}

	static void qualidade(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	}

	static String normalizar(String s) {
		String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD);
		return n.replaceAll("\\p{M}", "").toLowerCase().trim();
	}

	/** Etiqueta de tipo (a cor do tipo é a identidade visual de cada Pokémon). */
	static int desenharPilula(Graphics2D g2, String texto, int x, int y, Color cor) {
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
	static void desenharPokebola(Graphics2D g2, float cx, float cy, float r, Color topo, int alpha) {
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

	// =====================================================================
	// Imagens (arte grande, miniaturas e cache)
	// =====================================================================
	static class Imagens {
		private static final BufferedImage SEM = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		// guarda só as últimas artes grandes, para não encher a memória
		private static final Map<Integer, BufferedImage> GRANDES = Collections
				.synchronizedMap(new LinkedHashMap<Integer, BufferedImage>(16, 0.75f, true) {
					private static final long serialVersionUID = 1L;

					@Override
					protected boolean removeEldestEntry(Map.Entry<Integer, BufferedImage> e) {
						return size() > 16;
					}
				});

		private static final Map<Integer, BufferedImage> MINIATURAS = new ConcurrentHashMap<>();
		private static Thread preCarga;

		static BufferedImage grande(int dex) {
			BufferedImage c = GRANDES.get(dex);
			if (c != null) {
				return c == SEM ? null : c;
			}
			BufferedImage img = ler(dex);
			GRANDES.put(dex, img == null ? SEM : img);
			return img;
		}

		static BufferedImage miniatura(int dex) {
			return MINIATURAS.get(dex);
		}

		private static final Map<Integer, BufferedImage> SIL_GRANDES = Collections
				.synchronizedMap(new LinkedHashMap<Integer, BufferedImage>(16, 0.75f, true) {
					private static final long serialVersionUID = 1L;

					@Override
					protected boolean removeEldestEntry(Map.Entry<Integer, BufferedImage> e) {
						return size() > 8;
					}
				});
		private static final Map<Integer, BufferedImage> SIL_MINIATURAS = new ConcurrentHashMap<>();

		/** Silhueta escura da arte (Pokémon ainda não descoberto). */
		static BufferedImage silhueta(int dex) {
			BufferedImage c = SIL_GRANDES.get(dex);
			if (c == null) {
				BufferedImage origem = grande(dex);
				if (origem == null) {
					return null;
				}
				c = escurecerImagem(origem);
				SIL_GRANDES.put(dex, c);
			}
			return c;
		}

		static BufferedImage silhuetaMiniatura(int dex) {
			BufferedImage c = SIL_MINIATURAS.get(dex);
			if (c == null) {
				BufferedImage origem = MINIATURAS.get(dex);
				if (origem == null) {
					return null;
				}
				c = escurecerImagem(origem);
				SIL_MINIATURAS.put(dex, c);
			}
			return c;
		}

		/** Mantém só o contorno (transparência) da imagem e pinta tudo de azul bem escuro. */
		private static BufferedImage escurecerImagem(BufferedImage origem) {
			BufferedImage dst = new BufferedImage(origem.getWidth(), origem.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = dst.createGraphics();
			g.drawImage(origem, 0, 0, null);
			g.setComposite(AlphaComposite.SrcIn);
			g.setColor(new Color(10, 13, 30));
			g.fillRect(0, 0, dst.getWidth(), dst.getHeight());
			g.dispose();
			return dst;
		}

		private static BufferedImage ler(int dex) {
			File arquivo = new File("images/" + dex + ".png");
			if (!arquivo.exists()) {
				return null;
			}
			try {
				return ImageIO.read(arquivo);
			} catch (IOException e) {
				return null; // arquivo corrompido: o viewer mostra "sem imagem"
			}
		}

		/** Gera as miniaturas da lista lateral em segundo plano. */
		static void preCarregarMiniaturas(List<Pokemon> lista, Runnable aoAvancar) {
			final List<Integer> dexs = new ArrayList<>();
			for (Pokemon p : lista) {
				dexs.add(p.getNumeroDex());
			}
			preCarga = new Thread(() -> {
				int n = 0;
				for (int dex : dexs) {
					if (MINIATURAS.containsKey(dex)) {
						continue;
					}
					BufferedImage mini = lerMiniaturaGuardada(dex);
					if (mini == null) {
						BufferedImage img = ler(dex);
						if (img != null) {
							mini = reduzir(img, 96);
							guardarMiniatura(dex, mini);
						}
					}
					if (mini != null) {
						MINIATURAS.put(dex, mini);
					}
					if (++n % 12 == 0) {
						aoAvancar.run();
					}
				}
				aoAvancar.run();
			}, "miniaturas");
			preCarga.setDaemon(true);
			preCarga.setPriority(Thread.MIN_PRIORITY); // não disputa CPU com a animação
			preCarga.start();
		}

		/**
		 * Miniaturas prontas ficam em images/mini/: da segunda abertura em diante a lista carrega em instantes,
		 * em vez de decodificar e reduzir as 1025 artes grandes de novo.
		 */
		private static BufferedImage lerMiniaturaGuardada(int dex) {
			File arquivo = new File("images/mini/" + dex + ".png");
			File original = new File("images/" + dex + ".png");
			if (!arquivo.exists() || arquivo.lastModified() < original.lastModified()) {
				return null; // não existe, ou a arte foi baixada de novo depois
			}
			try {
				return ImageIO.read(arquivo);
			} catch (IOException e) {
				return null;
			}
		}

		private static void guardarMiniatura(int dex, BufferedImage mini) {
			try {
				File pasta = new File("images/mini");
				if (pasta.isDirectory() || pasta.mkdirs()) {
					ImageIO.write(mini, "png", new File(pasta, dex + ".png"));
				}
			} catch (IOException | RuntimeException e) {
				// sem cache em disco: só fica mais lento na próxima abertura
			}
		}

		/** Só para testes sem janela: espera a pré-carga terminar. */
		static void esperarPreCarga() throws InterruptedException {
			if (preCarga != null) {
				preCarga.join();
			}
		}

		static BufferedImage reduzir(BufferedImage origem, int lado) {
			BufferedImage atual = origem;
			int w = atual.getWidth(), h = atual.getHeight();
			while (w > lado * 2) { // reduz pela metade várias vezes: fica mais nítido
				w /= 2;
				h /= 2;
				atual = redimensionar(atual, w, h);
			}
			return redimensionar(atual, lado, Math.max(1, lado * h / w));
		}

		private static BufferedImage redimensionar(BufferedImage src, int w, int h) {
			BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = dst.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(src, 0, 0, w, h, null);
			g.dispose();
			return dst;
		}
	}

	// =====================================================================
	// Cenário dos tipos: o medalhão atrás de cada Pokémon
	// =====================================================================

	/**
	 * Desenha um medalhão com um padrão próprio de cada tipo (brasas para fogo,
	 * ondas para água, raios para elétrico e assim por diante). Cada Pokémon
	 * recebe sempre o mesmo desenho, porque a "sorte" vem do número Dex.
	 */
	static class Ambiente {

		// camadas que não mudam a cada quadro ficam prontas (só o padrão é redesenhado)
		private static BufferedImage baseCache, brilhoCache;
		private static String baseChave = "", brilhoChave = "";

		/** Para Pokémon Normal/Voador, o tipo mais marcante é o segundo. */
		static String tipoDoCenario(Pokemon p) {
			if ("normal".equals(normalizar(p.getTipo1())) && p.getTipo2() != null) {
				return p.getTipo2();
			}
			return p.getTipo1();
		}

		/** Número pseudoaleatório estável (0 a 1) a partir de uma semente. */
		static double rnd(long semente, int i) {
			long x = semente * 0x9E3779B97F4A7C15L + (i + 1) * 0xBF58476D1CE4E5B9L;
			x ^= x >>> 30;
			x *= 0xBF58476D1CE4E5B9L;
			x ^= x >>> 27;
			x *= 0x94D049BB133111EBL;
			x ^= x >>> 31;
			return (x >>> 11) * (1.0 / (1L << 53));
		}

		static void desenhar(Graphics2D g2, String tipo, int dex, float cx, float cy, float R, Color a, Color b,
				double fase) {
			String chave = (tipo == null) ? "normal" : normalizar(tipo);
			long semente = dex * 31L + chave.hashCode();
			Color luz = mix(a, Color.WHITE, 0.6f);
			Color sombra = escurecer(a, 0.45f);

			int lado = (int) Math.ceil(2 * R) + 4;
			float bx = lado / 2f, by = lado / 2f;
			Ellipse2D disco = new Ellipse2D.Float(bx - R, by - R, 2 * R, 2 * R);

			// camada 1: fundo em degradê (muda só quando as cores animam)
			String ck = lado + "|" + a.getRGB() + "|" + b.getRGB();
			if (baseCache == null || !ck.equals(baseChave)) {
				baseCache = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = baseCache.createGraphics();
				qualidade(g);
				g.setPaint(new GradientPaint(bx - R, by - R, mix(a, FUNDO_B, 0.30f), bx + R, by + R,
						escurecer(b, 0.42f)));
				g.fill(disco);
				g.dispose();
				baseChave = ck;
			}

			// camada 3: volume (borda escura, reflexo) e aro de luz (só depende do tamanho)
			String bk = String.valueOf(lado);
			if (brilhoCache == null || !bk.equals(brilhoChave)) {
				brilhoCache = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = brilhoCache.createGraphics();
				qualidade(g);
				g.setPaint(new RadialGradientPaint(new Point2D.Float(bx, by), R, new float[] { 0.55f, 1f },
						new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 125) }));
				g.fill(disco);
				g.setPaint(new RadialGradientPaint(new Point2D.Float(bx - R * 0.4f, by - R * 0.5f), R * 0.9f,
						new float[] { 0f, 1f }, new Color[] { new Color(255, 255, 255, 55), new Color(255, 255, 255, 0) }));
				g.fill(disco);
				g.setStroke(new BasicStroke(3f));
				g.setPaint(new GradientPaint(bx - R, by - R, new Color(255, 255, 255, 170), bx + R, by + R,
						new Color(255, 255, 255, 10)));
				g.draw(disco);
				g.dispose();
				brilhoChave = bk;
			}

			int x0 = Math.round(cx - bx), y0 = Math.round(cy - by);
			g2.drawImage(baseCache, x0, y0, null);

			// camada 2: padrão animado do tipo, recortado no círculo
			Shape clipAntes = g2.getClip();
			g2.clip(new Ellipse2D.Float(cx - R, cy - R, 2 * R, 2 * R));
			padrao(g2, chave, cx, cy, R, luz, sombra, semente, fase);
			g2.setClip(clipAntes);

			g2.drawImage(brilhoCache, x0, y0, null);
		}

		private static void padrao(Graphics2D g, String chave, float cx, float cy, float R, Color luz, Color sombra,
				long s, double fase) {
			switch (chave) {
			case "fogo":
				fogo(g, cx, cy, R, luz, s, fase);
				break;
			case "agua":
				agua(g, cx, cy, R, luz, fase);
				break;
			case "planta":
				planta(g, cx, cy, R, luz, sombra, s, fase);
				break;
			case "eletrico":
				eletrico(g, cx, cy, R, luz, s, fase);
				break;
			case "gelo":
				gelo(g, cx, cy, R, luz, s, fase);
				break;
			case "lutador":
				lutador(g, cx, cy, R, luz, s, fase);
				break;
			case "veneno":
				veneno(g, cx, cy, R, luz, s, fase);
				break;
			case "terra":
				terra(g, cx, cy, R, luz, sombra, s, fase);
				break;
			case "pedra":
				pedra(g, cx, cy, R, luz, sombra, s, fase);
				break;
			case "aco":
				aco(g, cx, cy, R, luz, fase);
				break;
			case "voador":
				voador(g, cx, cy, R, luz, s, fase);
				break;
			case "psiquico":
				psiquico(g, cx, cy, R, luz, fase);
				break;
			case "inseto":
				inseto(g, cx, cy, R, luz, s, fase);
				break;
			case "fantasma":
				fantasma(g, cx, cy, R, luz, s, fase);
				break;
			case "dragao":
				dragao(g, cx, cy, R, luz, fase);
				break;
			case "sombrio":
				sombrio(g, cx, cy, R, luz, s, fase);
				break;
			case "fada":
				fada(g, cx, cy, R, luz, s, fase);
				break;
			default:
				normal(g, cx, cy, R, luz, fase);
			}
		}

		// ---------- um desenho para cada tipo ----------

		/** Fogo: línguas de fogo na base e brasas subindo. */
		private static void fogo(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
			for (int camada = 0; camada < 3; camada++) {
				Path2D.Float p = new Path2D.Float();
				p.moveTo(cx - R, cy + R);
				for (float x = cx - R; x <= cx + R; x += 5) {
					double y = cy + R - (R * 0.16 + camada * R * 0.07)
							- R * 0.09 * Math.sin(x * 0.045 + fase * (1.2 + camada * 0.5) + camada * 2)
							- R * 0.05 * Math.sin(x * 0.12 - fase * 2.0);
					p.lineTo(x, y);
				}
				p.lineTo(cx + R, cy + R);
				p.closePath();
				g.setColor(comAlpha(luz, 40 - camada * 9));
				g.fill(p);
			}
			Color brasa = new Color(255, 214, 140);
			for (int i = 0; i < 34; i++) {
				double r1 = rnd(s, i), r2 = rnd(s, i + 100), r3 = rnd(s, i + 200);
				double ciclo = (r1 + fase * 0.045 * (0.6 + r3)) % 1.0;
				float x = (float) (cx + (r2 - 0.5) * 2 * R * 0.95 + Math.sin(fase * 0.8 + i) * 8);
				float y = (float) (cy + R - ciclo * 2 * R * 1.05);
				float tam = (float) ((2 + r3 * 5) * (1 - ciclo * 0.6));
				g.setColor(comAlpha(brasa, (int) (190 * (1 - ciclo))));
				g.fill(new Ellipse2D.Float(x - tam / 2, y - tam / 2, tam, tam));
			}
		}

		/** Água: ondas que deslizam e ondulações que se abrem. */
		private static void agua(Graphics2D g, float cx, float cy, float R, Color luz, double fase) {
			g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			for (int k = 0; k < 8; k++) {
				float y0 = cy - R + (k + 1) * (2 * R / 9f);
				Path2D.Float p = new Path2D.Float();
				p.moveTo(cx - R, y0);
				for (float x = cx - R; x <= cx + R; x += 6) {
					p.lineTo(x, y0 + (float) (Math.sin(x * 0.034 + fase * 0.9 + k * 0.8) * 7));
				}
				g.setColor(comAlpha(luz, 34 + (k % 3) * 10));
				g.draw(p);
			}
			float rx = cx - R * 0.3f, ry = cy + R * 0.35f;
			g.setStroke(new BasicStroke(2.2f));
			for (int j = 0; j < 3; j++) {
				double c = (fase * 0.2 + j / 3.0) % 1.0;
				float r = (float) (c * R * 1.3);
				g.setColor(comAlpha(Color.WHITE, (int) (120 * (1 - c))));
				g.draw(new Ellipse2D.Float(rx - r, ry - r * 0.4f, 2 * r, r * 0.8f));
			}
		}

		/** Planta: folhas de capim balançando na base. */
		private static void planta(Graphics2D g, float cx, float cy, float R, Color luz, Color sombra, long s,
				double fase) {
			for (int i = 0; i < 26; i++) {
				float x = cx - R + (float) (rnd(s, i) * 2 * R);
				float alt = (float) (R * (0.25 + 0.6 * rnd(s, i + 50)));
				float larg = 7 + (float) rnd(s, i + 90) * 10;
				float balanco = (float) Math.sin(fase * 0.7 + i) * 10;
				Path2D.Float p = new Path2D.Float();
				p.moveTo(x - larg / 2, cy + R);
				p.quadTo(x - larg / 4 + balanco * 0.4f, cy + R - alt * 0.55f, x + balanco, cy + R - alt);
				p.quadTo(x + larg / 4 + balanco * 0.4f, cy + R - alt * 0.55f, x + larg / 2, cy + R);
				p.closePath();
				g.setColor(comAlpha(mix(luz, sombra, (float) rnd(s, i + 7)), 44 + (int) (rnd(s, i + 3) * 56)));
				g.fill(p);
			}
		}

		/** Elétrico: raios que piscam e trocam de forma. */
		private static void eletrico(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
			int troca = (int) (fase * 0.7);
			double pisca = 0.55 + 0.45 * Math.sin(fase * 8.0);
			for (int b = 0; b < 3; b++) {
				long sb = s + troca * 7919L + b * 104729L;
				Path2D.Float p = new Path2D.Float();
				float x = cx + (float) ((rnd(sb, 0) - 0.5) * R * 1.4);
				p.moveTo(x, cy - R);
				int seg = 8;
				for (int i = 1; i <= seg; i++) {
					x += (float) ((rnd(sb, i) - 0.5) * R * 0.5);
					p.lineTo(x, cy - R + i * (2 * R / seg));
				}
				g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(comAlpha(luz, (int) (34 * pisca)));
				g.draw(p);
				g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(comAlpha(Color.WHITE, (int) (190 * pisca)));
				g.draw(p);
			}
			for (int i = 0; i < 16; i++) {
				double tw = 0.5 + 0.5 * Math.sin(fase * 2 + i * 1.9);
				float x = cx + (float) ((rnd(s, i + 300) - 0.5) * 2 * R * 0.9);
				float y = cy + (float) ((rnd(s, i + 400) - 0.5) * 2 * R * 0.9);
				g.setColor(comAlpha(Color.WHITE, (int) (150 * tw)));
				g.fill(new Ellipse2D.Float(x - 1.8f, y - 1.8f, 3.6f, 3.6f));
			}
		}

		/** Gelo: cristais na base e neve caindo. */
		private static void gelo(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
			g.setStroke(new BasicStroke(1.3f));
			for (int i = 0; i < 10; i++) {
				float x = cx - R + (float) ((i + rnd(s, i) * 0.8) * 2 * R / 10);
				float alt = (float) (R * (0.3 + 0.6 * rnd(s, i + 40)));
				float larg = 16 + (float) rnd(s, i + 80) * 22;
				Path2D.Float esq = new Path2D.Float();
				esq.moveTo(x - larg / 2, cy + R);
				esq.lineTo(x, cy + R - alt);
				esq.lineTo(x, cy + R);
				esq.closePath();
				Path2D.Float dir = new Path2D.Float();
				dir.moveTo(x, cy + R);
				dir.lineTo(x, cy + R - alt);
				dir.lineTo(x + larg / 2, cy + R);
				dir.closePath();
				g.setColor(comAlpha(luz, 46));
				g.fill(esq);
				g.setColor(comAlpha(luz, 24));
				g.fill(dir);
				g.setColor(comAlpha(Color.WHITE, 90));
				g.draw(esq);
				g.draw(dir);
			}
			for (int i = 0; i < 30; i++) {
				double r1 = rnd(s, i + 500), r2 = rnd(s, i + 600), r3 = rnd(s, i + 700);
				double ciclo = (r1 + fase * 0.03 * (0.5 + r3)) % 1.0;
				float x = (float) (cx + (r2 - 0.5) * 2 * R + Math.sin(fase * 0.6 + i) * 6);
				float y = (float) (cy - R + ciclo * 2 * R);
				float r = (float) (1.5 + r3 * 2.5);
				g.setColor(comAlpha(Color.WHITE, 150));
				g.fill(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r));
			}
		}

		/** Lutador: linhas de velocidade saindo do centro, como em quadrinhos. */
		private static void lutador(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
			int n = 38;
			for (int i = 0; i < n; i++) {
				double ang = i * 2 * Math.PI / n + (rnd(s, i) - 0.5) * 0.07;
				double pulso = 0.9 + 0.1 * Math.sin(fase * 2 + i);
				double r0 = R * (0.3 + 0.35 * rnd(s, i + 60));
				double r1 = R * (1.15 + 0.2 * rnd(s, i + 120)) * pulso;
				g.setStroke(new BasicStroke(1.5f + (float) (3.5 * rnd(s, i + 200)), BasicStroke.CAP_ROUND,
						BasicStroke.JOIN_ROUND));
				g.setColor(comAlpha(luz, 30 + (int) (45 * rnd(s, i + 260))));
				g.drawLine((int) (cx + Math.cos(ang) * r0), (int) (cy + Math.sin(ang) * r0),
						(int) (cx + Math.cos(ang) * r1), (int) (cy + Math.sin(ang) * r1));
			}
		}

		/** Veneno: bolhas subindo. */
		private static void veneno(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
			for (int i = 0; i < 22; i++) {
				double r1 = rnd(s, i), r2 = rnd(s, i + 80), r3 = rnd(s, i + 160);
				float r = (float) (5 + 20 * r1 * r1);
				double ciclo = (r2 + fase * 0.035 * (0.5 + r3)) % 1.0;
				float x = (float) (cx + (r3 - 0.5) * 2 * R + Math.sin(fase + i) * 6);
				float y = (float) (cy + R + r - ciclo * (2 * R + 2 * r));
				g.setColor(comAlpha(luz, 26));
				g.fill(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r));
				g.setStroke(new BasicStroke(1.6f));
				g.setColor(comAlpha(luz, 110));
				g.draw(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r));
				g.setColor(comAlpha(Color.WHITE, 140));
				g.fill(new Ellipse2D.Float(x - r * 0.55f, y - r * 0.6f, r * 0.35f, r * 0.35f));
			}
		}

		/** Terra: camadas de solo. */
		private static void terra(Graphics2D g, float cx, float cy, float R, Color luz, Color sombra, long s,
				double fase) {
			for (int k = 0; k < 6; k++) {
				float y0 = cy - R * 0.25f + k * R * 0.27f;
				Path2D.Float p = new Path2D.Float();
				p.moveTo(cx - R, cy + R);
				for (float x = cx - R; x <= cx + R; x += 6) {
					p.lineTo(x, y0 + (float) (Math.sin(x * 0.02 + k * 1.3) * 10 + Math.sin(x * 0.05 + k) * 4));
				}
				p.lineTo(cx + R, cy + R);
				p.closePath();
				g.setColor(comAlpha(k % 2 == 0 ? luz : sombra, 34));
				g.fill(p);
			}
			for (int i = 0; i < 16; i++) {
				float x = cx + (float) ((rnd(s, i) - 0.5) * 2 * R);
				float y = cy + R * 0.1f + (float) (rnd(s, i + 50) * R * 0.85);
				float r = 3 + (float) rnd(s, i + 99) * 6;
				g.setColor(comAlpha(luz, 60));
				g.fill(new Ellipse2D.Float(x - r, y - r * 0.7f, 2 * r, 1.4f * r));
			}
		}

		/** Pedra: facetas, como uma rocha lapidada. */
		private static void pedra(Graphics2D g, float cx, float cy, float R, Color luz, Color sombra, long s,
				double fase) {
			int n = 7;
			float x0 = cx - R * 1.1f, y0 = cy - R * 1.1f, passo = 2.2f * R / n;
			float[][] px = new float[n + 1][n + 1], py = new float[n + 1][n + 1];
			for (int i = 0; i <= n; i++) {
				for (int j = 0; j <= n; j++) {
					px[i][j] = x0 + i * passo + (float) ((rnd(s, i * 20 + j) - 0.5) * passo * 0.7);
					py[i][j] = y0 + j * passo + (float) ((rnd(s, i * 20 + j + 500) - 0.5) * passo * 0.7);
				}
			}
			g.setStroke(new BasicStroke(1f));
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					int c = i * n + j;
					Path2D.Float t1 = new Path2D.Float();
					t1.moveTo(px[i][j], py[i][j]);
					t1.lineTo(px[i + 1][j], py[i + 1][j]);
					t1.lineTo(px[i + 1][j + 1], py[i + 1][j + 1]);
					t1.closePath();
					Path2D.Float t2 = new Path2D.Float();
					t2.moveTo(px[i][j], py[i][j]);
					t2.lineTo(px[i + 1][j + 1], py[i + 1][j + 1]);
					t2.lineTo(px[i][j + 1], py[i][j + 1]);
					t2.closePath();
					float b1 = (float) rnd(s, c * 2) * 0.8f + (float) (Math.sin(fase * 0.5 + c) * 0.06);
					float b2 = (float) rnd(s, c * 2 + 1) * 0.8f + (float) (Math.sin(fase * 0.5 + c + 2) * 0.06);
					g.setColor(comAlpha(mix(sombra, luz, b1), 78));
					g.fill(t1);
					g.setColor(comAlpha(mix(sombra, luz, b2), 78));
					g.fill(t2);
					g.setColor(comAlpha(Color.WHITE, 26));
					g.draw(t1);
					g.draw(t2);
				}
			}
		}

		/** Aço: engrenagem com marcas de régua girando devagar. */
		private static void aco(Graphics2D g, float cx, float cy, float R, Color luz, double fase) {
			Graphics2D h = (Graphics2D) g.create();
			h.rotate(fase * 0.08, cx, cy);
			h.setStroke(new BasicStroke(2f));
			float[] aneis = { 0.38f, 0.62f, 0.86f };
			for (float k : aneis) {
				h.setColor(comAlpha(luz, 70));
				h.draw(new Ellipse2D.Float(cx - R * k, cy - R * k, 2 * R * k, 2 * R * k));
			}
			for (int i = 0; i < 60; i++) {
				double ang = i * 2 * Math.PI / 60;
				float r1 = R * 0.62f, r2 = r1 + (i % 5 == 0 ? 16 : 8);
				h.setColor(comAlpha(luz, i % 5 == 0 ? 120 : 70));
				h.drawLine((int) (cx + Math.cos(ang) * r1), (int) (cy + Math.sin(ang) * r1),
						(int) (cx + Math.cos(ang) * r2), (int) (cy + Math.sin(ang) * r2));
			}
			for (int i = 0; i < 8; i++) {
				double ang = i * Math.PI / 4;
				float x = (float) (cx + Math.cos(ang) * R * 0.95f), y = (float) (cy + Math.sin(ang) * R * 0.95f);
				h.setColor(comAlpha(luz, 100));
				h.fill(new Ellipse2D.Float(x - 4, y - 4, 8, 8));
			}
			h.dispose();
		}

		/** Voador: rajadas de vento e nuvens passando. */
		private static void voador(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
			for (int i = 0; i < 3; i++) {
				double ciclo = (rnd(s, i) + fase * 0.012 * (1 + i * 0.4)) % 1.0;
				float x = (float) (cx - R * 1.4 + ciclo * R * 2.8);
				float y = cy - R * 0.4f + i * R * 0.45f;
				g.setColor(comAlpha(luz, 26));
				for (int k = 0; k < 4; k++) {
					float r = R * (0.16f + 0.08f * (k % 2));
					g.fill(new Ellipse2D.Float(x + k * R * 0.14f - r, y - r * 0.6f - (k % 2) * 6, 2 * r, 1.3f * r));
				}
			}
			for (int i = 0; i < 14; i++) {
				float y = cy - R + (float) ((0.08 + 0.84 * rnd(s, i + 30)) * 2 * R);
				float comp = (float) (R * (0.5 + rnd(s, i + 70)));
				double vel = 0.02 + 0.03 * rnd(s, i + 110);
				float x = (float) (cx - R + ((rnd(s, i + 150) + fase * vel) % 1.0) * (2 * R + comp) - comp);
				g.setStroke(new BasicStroke(2f + (float) (3 * rnd(s, i + 190)), BasicStroke.CAP_ROUND,
						BasicStroke.JOIN_ROUND));
				g.setColor(comAlpha(Color.WHITE, 38 + (int) (60 * rnd(s, i + 230))));
				Path2D.Float p = new Path2D.Float();
				p.moveTo(x, y);
				p.quadTo(x + comp / 2, y - 8, x + comp, y);
				g.draw(p);
			}
		}

		/** Psíquico: ondas se expandindo e pontos em órbita. */
		private static void psiquico(Graphics2D g, float cx, float cy, float R, Color luz, double fase) {
			g.setStroke(new BasicStroke(2.2f));
			for (int j = 0; j < 4; j++) {
				double c = (fase * 0.12 + j / 4.0) % 1.0;
				float r = (float) (c * R * 1.2);
				g.setColor(comAlpha(luz, (int) (120 * (1 - c))));
				g.draw(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));
			}
			g.setStroke(new BasicStroke(1.2f));
			g.setColor(comAlpha(luz, 40));
			g.draw(new Ellipse2D.Float(cx - R * 0.72f, cy - R * 0.28f, R * 1.44f, R * 0.56f));
			for (int k = 0; k < 3; k++) {
				double ang = fase * 0.5 + k * 2 * Math.PI / 3;
				float x = (float) (cx + Math.cos(ang) * R * 0.72f), y = (float) (cy + Math.sin(ang) * R * 0.28f);
				g.setColor(comAlpha(Color.WHITE, 210));
				g.fill(new Ellipse2D.Float(x - 4.5f, y - 4.5f, 9, 9));
				g.setColor(comAlpha(luz, 60));
				g.fill(new Ellipse2D.Float(x - 11, y - 11, 22, 22));
			}
		}

		/** Inseto: favo de mel com células que acendem. */
		private static void inseto(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
			float hs = R * 0.2f;
			g.setStroke(new BasicStroke(1.4f));
			for (int row = -1; row < 12; row++) {
				for (int col = -1; col < 8; col++) {
					float x = cx - R + col * hs * 1.732f + (row % 2 == 0 ? 0 : hs * 0.866f);
					float y = cy - R + row * hs * 1.5f;
					Path2D.Float hex = new Path2D.Float();
					for (int k = 0; k < 6; k++) {
						double ang = Math.PI / 6 + k * Math.PI / 3;
						float hx = x + (float) Math.cos(ang) * hs * 0.93f, hy = y + (float) Math.sin(ang) * hs * 0.93f;
						if (k == 0) {
							hex.moveTo(hx, hy);
						} else {
							hex.lineTo(hx, hy);
						}
					}
					hex.closePath();
					double aceso = rnd(s, (row + 2) * 20 + col + 2);
					if (aceso > 0.62) {
						double pulso = 0.5 + 0.5 * Math.sin(fase * 0.9 + aceso * 20);
						g.setColor(comAlpha(luz, (int) (20 + 50 * pulso)));
						g.fill(hex);
					}
					g.setColor(comAlpha(luz, 46));
					g.draw(hex);
				}
			}
		}

		/** Fantasma: fios de névoa subindo e orbes à deriva. */
		private static void fantasma(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
			for (int i = 0; i < 5; i++) {
				float x0 = cx - R * 0.8f + i * R * 0.4f;
				Path2D.Float p = new Path2D.Float();
				p.moveTo(x0, cy + R);
				for (float y = cy + R; y >= cy - R; y -= 10) {
					p.lineTo(x0 + (float) (Math.sin(y * 0.025 + fase * 0.7 + i * 1.7) * (18 + (i % 2) * 10)), y);
				}
				g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(comAlpha(luz, 20));
				g.draw(p);
				g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(comAlpha(luz, 62));
				g.draw(p);
			}
			for (int i = 0; i < 9; i++) {
				float x = cx + (float) ((rnd(s, i) - 0.5) * 2 * R * 0.85);
				float y = cy + (float) ((rnd(s, i + 40) - 0.5) * 2 * R * 0.85 + Math.sin(fase * 0.8 + i) * 8);
				float r = 4 + (float) rnd(s, i + 80) * 8;
				g.setColor(comAlpha(luz, 34));
				g.fill(new Ellipse2D.Float(x - r * 2, y - r * 2, 4 * r, 4 * r));
				g.setColor(comAlpha(Color.WHITE, 120));
				g.fill(new Ellipse2D.Float(x - r / 2, y - r / 2, r, r));
			}
		}

		/** Dragão: escamas com um brilho que atravessa. */
		private static void dragao(Graphics2D g, float cx, float cy, float R, Color luz, double fase) {
			float sc = R * 0.17f;
			g.setStroke(new BasicStroke(1.6f));
			g.setColor(comAlpha(luz, 62));
			for (int row = -1; row < 16; row++) {
				for (int col = -1; col < 14; col++) {
					float x = cx - R + col * sc + (row % 2 == 0 ? 0 : sc / 2);
					float y = cy - R + row * sc * 0.5f;
					g.draw(new java.awt.geom.Arc2D.Float(x - sc / 2, y - sc / 2, sc, sc, 180, 180,
							java.awt.geom.Arc2D.OPEN));
				}
			}
			float pos = (float) (cx - R * 1.5 + ((fase * 0.06) % 1.0) * 3 * R);
			g.setPaint(new LinearGradientPaint(new Point2D.Float(pos - R * 0.4f, cy - R * 0.4f),
					new Point2D.Float(pos + R * 0.4f, cy + R * 0.4f), new float[] { 0f, 0.5f, 1f },
					new Color[] { new Color(255, 255, 255, 0), new Color(255, 255, 255, 60),
							new Color(255, 255, 255, 0) }));
			g.fill(new Rectangle2D.Float(cx - R, cy - R, 2 * R, 2 * R));
		}

		/** Sombrio: lua minguante, estrelas e anéis de sombra. */
		private static void sombrio(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
			g.setStroke(new BasicStroke(10f));
			g.setColor(new Color(0, 0, 0, 36));
			for (int k = 0; k < 3; k++) {
				float r = R * (0.45f + k * 0.3f);
				g.draw(new Ellipse2D.Float(cx - r + k * 10, cy - r, 2 * r, 2 * r * 0.9f));
			}
			for (int i = 0; i < 26; i++) {
				double tw = 0.5 + 0.5 * Math.sin(fase * 1.1 + i * 2.3);
				float x = cx + (float) ((rnd(s, i) - 0.5) * 2 * R * 0.95);
				float y = cy + (float) ((rnd(s, i + 60) - 0.5) * 2 * R * 0.95);
				float r = 1f + (float) rnd(s, i + 120) * 2f;
				g.setColor(comAlpha(Color.WHITE, (int) (60 + 150 * tw)));
				g.fill(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r));
			}
			Area lua = new Area(new Ellipse2D.Float(cx + R * 0.45f - R * 0.22f, cy - R * 0.5f - R * 0.22f, R * 0.44f,
					R * 0.44f));
			lua.subtract(new Area(new Ellipse2D.Float(cx + R * 0.55f - R * 0.19f, cy - R * 0.54f - R * 0.19f,
					R * 0.38f, R * 0.38f)));
			g.setColor(comAlpha(luz, 190));
			g.fill(lua);
		}

		/** Fada: brilhos de quatro pontas e luzes desfocadas. */
		private static void fada(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
			for (int i = 0; i < 7; i++) {
				float x = cx + (float) ((rnd(s, i + 200) - 0.5) * 2 * R * 0.9);
				float y = cy + (float) ((rnd(s, i + 260) - 0.5) * 2 * R * 0.9);
				float r = R * (0.08f + (float) rnd(s, i + 320) * 0.14f);
				g.setColor(comAlpha(luz, 26));
				g.fill(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r));
			}
			for (int i = 0; i < 16; i++) {
				double tw = 0.5 + 0.5 * Math.sin(fase * 1.2 + i * 1.7);
				float x = cx + (float) ((rnd(s, i) - 0.5) * 2 * R * 0.92);
				float y = cy + (float) ((rnd(s, i + 60) - 0.5) * 2 * R * 0.92);
				float r = (float) ((6 + 12 * rnd(s, i + 120)) * (0.4 + 0.6 * tw));
				Path2D.Float p = new Path2D.Float();
				for (int k = 0; k < 8; k++) {
					double ang = k * Math.PI / 4 - Math.PI / 2;
					float rr = (k % 2 == 0) ? r : r * 0.28f;
					float px = x + (float) Math.cos(ang) * rr, py = y + (float) Math.sin(ang) * rr;
					if (k == 0) {
						p.moveTo(px, py);
					} else {
						p.lineTo(px, py);
					}
				}
				p.closePath();
				g.setColor(comAlpha(Color.WHITE, (int) (60 + 150 * tw)));
				g.fill(p);
			}
		}

		/** Normal: anéis pontilhados girando, simples e limpo. */
		private static void normal(Graphics2D g, float cx, float cy, float R, Color luz, double fase) {
			float[] aneis = { 0.45f, 0.7f, 0.93f };
			for (int i = 0; i < aneis.length; i++) {
				float r = R * aneis[i];
				g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
						new float[] { 0.1f, 13f }, (float) (((fase * 6 * (i % 2 == 0 ? 1 : -1)) % 13.1 + 13.1) % 13.1)));
				g.setColor(comAlpha(luz, 90));
				g.draw(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));
			}
			g.setColor(comAlpha(luz, 22));
			g.fill(new Ellipse2D.Float(cx - R * 0.3f, cy - R * 0.3f, R * 0.6f, R * 0.6f));
		}
	}

	// =====================================================================
	// Painel principal
	// =====================================================================
	static class PainelPokedex extends JPanel {

		private static final long serialVersionUID = 1L;

		private final transient Function<String, Pokemon> buscador;
		private final transient List<Pokemon> lista = new ArrayList<>();
		private final double maxAltura;
		private final double maxPeso;
		private transient Pokemon atual;

		// descobertas (a Pokédex começa vazia; cada Pokémon precisa ser escaneado)
		private final Set<Integer> descobertos = new HashSet<>();
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

		PainelPokedex(Function<String, Pokemon> buscador, Supplier<List<Pokemon>> todos) {
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

			relogio = new javax.swing.Timer(16, e -> passo());
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
		void finalizarAnimacao() {
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
			jlist.setCellRenderer(new LinhaPokemon(p -> descobertos.contains(p.getNumeroDex())));
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
			BotaoTexto batalhar = new BotaoTexto("Batalhar", "Modo batalha: monte um time com os Pokémon que você capturou");
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
		 * Modo batalha: o jogador monta um time com os Pokémon já descobertos (capturados) e luta contra
		 * um time sorteado entre os outros descobertos (cada adversário é achado pelo buscador, ou seja, pela ABB).
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
			List<Pokemon> capturados = new ArrayList<>();
			for (Pokemon p : lista) {
				if (descobertos.contains(p.getNumeroDex()) && !dadosBatalha.golpesDe(p.getNumeroDex()).isEmpty()) {
					capturados.add(p);
				}
			}
			if (capturados.size() < 2) {
				aviso("Descubra pelo menos 2 Pokémon para poder batalhar.");
				return;
			}
			Frame dono = (Frame) SwingUtilities.getWindowAncestor(this);
			relogio.stop(); // a Pokédex fica parada enquanto as janelas de batalha estão abertas
			try {
				List<Pokemon> meu = new SeletorBatalha(dono, capturados, capturados.contains(atual) ? atual : null).escolher();
				if (meu.isEmpty()) {
					return;
				}
				List<Pokemon> candidatos = new ArrayList<>(capturados);
				candidatos.removeAll(meu);
				if (candidatos.isEmpty()) {
					aviso("Falta um adversário: descubra mais um Pokémon além do seu time.");
					return;
				}
				Collections.shuffle(candidatos);
				List<Pokemon> adversarios = new ArrayList<>();
				for (Pokemon c : candidatos.subList(0, Math.min(meu.size(), candidatos.size()))) {
					Pokemon achado = buscador.apply(String.valueOf(c.getNumeroDex())); // busca na ABB
					adversarios.add(achado != null ? achado : c);
				}
				new TelaBatalha(dono, dadosBatalha, meu, adversarios).setVisible(true);
			} finally {
				ultimoQuadro = System.nanoTime();
				relogio.start();
			}
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
			if (p != null && !porNumero && !descobertos.contains(p.getNumeroDex())) {
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
			if (p == null || scanDex >= 0 || descobertos.contains(p.getNumeroDex())) {
				return;
			}
			scanDex = p.getNumeroDex();
			scanT = 0;
			heroi.repaint();
		}

		private void concluirScan() {
			int dex = scanDex;
			scanDex = -1;
			scanT = 0;
			descobertos.add(dex);
			flash = 1;
			Pokemon revelado = null;
			for (Pokemon p : lista) {
				if (p.getNumeroDex() == dex) {
					revelado = p;
					break;
				}
			}
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
			} else if (descobertos.contains(p.getNumeroDex())) {
				aviso(p.getNome() + " já está registrado.");
				exibir(p);
			} else {
				lblAviso.setText(" ");
				exibir(p);
				iniciarScan(p);
			}
		}

		/** Só para testes: marca como descoberto sem animação. */
		void descobrir(int dex) {
			descobertos.add(dex);
		}

		/** Só para testes: deixa o scan num ponto (0 a 1) sem avançar. */
		void fixarScan(int dex, double progresso) {
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
					if (descobertos.contains(p.getNumeroDex())) {
						achados++;
					}
				}
			}
			seletorRegiao.setProgresso(achados + "/" + total);
		}

		/** Só para testes: escolhe a região (null = todas). */
		void selecionarRegiao(String regiao) {
			if (seletorRegiao != null) {
				seletorRegiao.escolher(regiao);
			}
		}

		int itensNaLista() {
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
				boolean nomeVisivel = descobertos.contains(p.getNumeroDex()); // nome de quem não foi descoberto é segredo
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

		void exibir(Pokemon p) {
			// as barras guardam o valor atual antes de mudar, para animar a troca
			boolean sabido = p != null && descobertos.contains(p.getNumeroDex());
			barraAltura.definir(!sabido || p.getAltura() == null ? 0 : Math.sqrt(p.getAltura() / maxAltura));
			barraPeso.definir(!sabido || p.getPeso() == null ? 0 : Math.sqrt(p.getPeso() / maxPeso));

			boolean conhecido = p != null && descobertos.contains(p.getNumeroDex());
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
			String progresso = "Descobertos " + descobertos.size() + "/" + lista.size();
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
			boolean sabido = descobertos.contains(atual.getNumeroDex());
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
				final boolean conhecido = descobertos.contains(p.getNumeroDex());
				final boolean escaneando = scanDex == p.getNumeroDex();
				Ambiente.desenhar(g2, conhecido ? Ambiente.tipoDoCenario(p) : null, p.getNumeroDex(), cx, cy, raio, a, b, fase);
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

	// =====================================================================
	// Lateral: cabeçalho, campo de busca, linha da lista, rolagem
	// =====================================================================

	static class Cabecalho extends JComponent {
		private static final long serialVersionUID = 1L;

		Cabecalho() {
			setPreferredSize(new Dimension(100, 86));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			desenharPokebola(g2, 42, 44, 17, new Color(0xE8, 0x3B, 0x4B), 255);
			g2.setFont(fonte(Font.BOLD, 26f));
			g2.setColor(TEXTO);
			g2.drawString("Pokédex", 70, 53);
			g2.dispose();
		}
	}

	static class CampoBusca extends JTextField {
		private static final long serialVersionUID = 1L;

		CampoBusca() {
			super(18);
			setOpaque(false);
			setBorder(new EmptyBorder(0, 44, 0, 16));
			setFont(fonte(Font.PLAIN, 16f));
			setForeground(TEXTO);
			setCaretColor(TEXTO);
			setPreferredSize(new Dimension(100, 44));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			int w = getWidth(), h = getHeight();
			g2.setColor(CAMPO);
			g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, h, h));
			g2.setColor(hasFocus() ? new Color(0x7C, 0x8C, 0xFF) : BORDA);
			g2.setStroke(new BasicStroke(hasFocus() ? 1.8f : 1.2f));
			g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, w - 2.6f, h - 2.6f, h, h));

			// lupa
			g2.setColor(SUAVE);
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(new Ellipse2D.Float(18, h / 2f - 8, 12, 12));
			g2.drawLine(28, (int) (h / 2f + 3), 33, (int) (h / 2f + 8));
			g2.dispose();

			super.paintComponent(g);

			if (getText().isEmpty()) {
				Graphics2D d = (Graphics2D) g.create();
				qualidade(d);
				d.setColor(new Color(0x71, 0x7A, 0x9C));
				d.setFont(getFont());
				FontMetrics fm = d.getFontMetrics();
				d.drawString("Buscar por nome ou número", getInsets().left, (h - fm.getHeight()) / 2f + fm.getAscent());
				d.dispose();
			}
		}
	}

	/** Uma linha da lista: miniatura, número, nome e pontos dos tipos. */
	static class LinhaPokemon extends JComponent implements ListCellRenderer<Pokemon> {
		private static final long serialVersionUID = 1L;
		private transient Pokemon p;
		private boolean selecionado;
		private final transient Predicate<Pokemon> descoberto;

		LinhaPokemon(Predicate<Pokemon> descoberto) {
			this.descoberto = descoberto;
			setPreferredSize(new Dimension(100, 64));
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends Pokemon> list, Pokemon value, int index,
				boolean isSelected, boolean cellHasFocus) {
			this.p = value;
			this.selecionado = isSelected;
			return this;
		}

		@Override
		protected void paintComponent(Graphics g) {
			if (p == null) {
				return;
			}
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			int w = getWidth(), h = getHeight();
			boolean conhecido = descoberto.test(p);
			Color cor = conhecido ? corDoTipo(p.getTipo1()) : PADRAO_A;

			if (selecionado) {
				g2.setColor(mix(PAINEL, cor, 0.32f));
				g2.fill(new RoundRectangle2D.Float(10, 4, w - 20, h - 8, 20, 20));
				g2.setColor(comAlpha(cor, 170));
				g2.setStroke(new BasicStroke(1.4f));
				g2.draw(new RoundRectangle2D.Float(10.7f, 4.7f, w - 21.4f, h - 9.4f, 20, 20));
			}

			// miniatura
			int tam = 48, mx = 22, my = (h - tam) / 2;
			g2.setColor(comAlpha(cor, selecionado ? 120 : 70));
			g2.fill(new Ellipse2D.Float(mx, my, tam, tam));
			BufferedImage mini = conhecido ? Imagens.miniatura(p.getNumeroDex())
					: Imagens.silhuetaMiniatura(p.getNumeroDex());
			if (mini != null) {
				int lado = 56;
				g2.drawImage(mini, mx + (tam - lado) / 2, my + (tam - lado) / 2 - 2, lado, lado, null);
			}

			// textos
			g2.setFont(fonte(Font.PLAIN, 13f));
			g2.setColor(SUAVE);
			g2.drawString(String.format("Nº %03d", p.getNumeroDex()), 84, h / 2 - 5);
			g2.setFont(fonte(Font.BOLD, 17f));
			g2.setColor(TEXTO);
			g2.drawString(conhecido ? p.getNome() : "???", 84, h / 2 + 15);

			// pontos dos tipos
			if (!conhecido) {
				g2.dispose();
				return;
			}
			int px = w - 36;
			if (p.getTipo2() != null) {
				g2.setColor(corDoTipo(p.getTipo2()));
				g2.fill(new Ellipse2D.Float(px, h / 2f - 5, 11, 11));
				px -= 16;
			}
			g2.setColor(cor);
			g2.fill(new Ellipse2D.Float(px, h / 2f - 5, 11, 11));
			g2.dispose();
		}
	}

	static class BarraRolagem extends BasicScrollBarUI {
		@Override
		protected JButton createDecreaseButton(int orientation) {
			return botaoVazio();
		}

		@Override
		protected JButton createIncreaseButton(int orientation) {
			return botaoVazio();
		}

		private JButton botaoVazio() {
			JButton b = new JButton();
			b.setPreferredSize(new Dimension(0, 0));
			return b;
		}

		@Override
		protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
			// sem trilho: só o puxador aparece
		}

		@Override
		protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
			if (r.isEmpty() || !scrollbar.isEnabled()) {
				return;
			}
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			g2.setColor(new Color(255, 255, 255, isDragging ? 110 : 60));
			g2.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8));
			g2.dispose();
		}
	}

	/** Botão redondo das setas de navegação. */
	static class BotaoRedondo extends JButton {
		private static final long serialVersionUID = 1L;

		private final int direcao; // -1 = seta para a esquerda, +1 = para a direita

		BotaoRedondo(String simbolo, String dica) {
			super("");
			this.direcao = "‹".equals(simbolo) ? -1 : 1;
			setToolTipText(dica);
			setPreferredSize(new Dimension(42, 42));
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setFocusable(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			Color fundo = getModel().isPressed() ? escurecer(PAINEL, 0.8f)
					: getModel().isRollover() ? mix(PAINEL, Color.WHITE, 0.10f) : PAINEL;
			g2.setColor(fundo);
			g2.fill(new Ellipse2D.Float(0, 0, getWidth() - 1, getHeight() - 1));
			g2.setColor(BORDA);
			g2.setStroke(new BasicStroke(1.2f));
			g2.draw(new Ellipse2D.Float(0.6f, 0.6f, getWidth() - 2.2f, getHeight() - 2.2f));

			// seta desenhada à mão (não depende da fonte)
			int cx = getWidth() / 2, cy = getHeight() / 2;
			g2.setColor(TEXTO);
			g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.drawLine(cx - direcao * 3, cy - 7, cx + direcao * 4, cy);
			g2.drawLine(cx + direcao * 4, cy, cx - direcao * 3, cy + 7);
			g2.dispose();
		}
	}

	/**
	 * Seletor de região da lista lateral: setas para trocar e, no centro, o nome da região com o progresso
	 * (ex.: Kanto  3/151). A primeira opção é "Todas".
	 */
	static class SeletorRegiao extends JComponent {
		private static final long serialVersionUID = 1L;
		private static final int ZONA_SETA = 40;

		private final List<String> regioes = new ArrayList<>(); // posição 0 = todas (null)
		private int indice = 0;
		private String progresso = "";
		private int hover = 0; // -1 seta esquerda, 1 seta direita, 2 centro, 0 nenhum
		private transient Runnable aoMudar = () -> { };

		SeletorRegiao(List<String> nomes) {
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

		void aoMudar(Runnable acao) {
			this.aoMudar = acao;
		}

		/** Região escolhida, ou null para todas. */
		String regiao() {
			return regioes.get(indice);
		}

		void escolher(String regiao) {
			int i = regioes.indexOf(regiao);
			if (i >= 0 && i != indice) {
				indice = i;
				aoMudar.run();
				repaint();
			}
		}

		void setProgresso(String texto) {
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

	/** Botão em forma de pílula com texto (ex.: Cadastrar). */
	static class BotaoTexto extends JButton {
		private static final long serialVersionUID = 1L;

		BotaoTexto(String texto, String dica) {
			super(texto);
			setToolTipText(dica);
			setFont(fonte(Font.BOLD, 14f));
			setForeground(TEXTO);
			setPreferredSize(new Dimension(108, 42));
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setFocusable(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			qualidade(g2);
			Color fundo = getModel().isPressed() ? escurecer(PAINEL, 0.8f)
					: getModel().isRollover() ? mix(PAINEL, Color.WHITE, 0.10f) : PAINEL;
			g2.setColor(fundo);
			g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight()));
			g2.setColor(BORDA);
			g2.setStroke(new BasicStroke(1.2f));
			g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, getWidth() - 2.2f, getHeight() - 2.2f, getHeight(), getHeight()));
			g2.setFont(getFont());
			FontMetrics fm = g2.getFontMetrics();
			g2.setColor(TEXTO);
			g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2f,
					(getHeight() - fm.getHeight()) / 2f + fm.getAscent());
			g2.dispose();
		}
	}

	// =====================================================================
	// Execução (usa o CSV; troque o buscador e a lista pela ABB quando ela estiver pronta)
	// =====================================================================
	public static void main(String[] args) {
		List<Pokemon> lista;
		try {
			lista = CarregadorPokemon.carregar("pokemons.csv");
		} catch (IOException e) {
			System.out.println("Erro ao ler pokemons.csv: " + e.getMessage());
			return;
		}

		// A API só tem imagens até BaixadorImagens.ULTIMO_DEX_API: o resto do CSV é ignorado.
		final List<Pokemon> todos = new ArrayList<>();
		for (Pokemon p : lista) {
			if (p.getNumeroDex() >= 1 && p.getNumeroDex() <= BaixadorImagens.ULTIMO_DEX_API) {
				todos.add(p);
			} else {
				System.out.println("Ignorado (fora do limite da API): #" + p.getNumeroDex() + " " + p.getNome());
			}
		}

		// Cria a ABB e insere os Pokémon carregados do CSV.
		final ArvoreBinaria arvore = new ArvoreBinaria();
		for (Pokemon pokemon : CarregadorPokemon.ordemBalanceada(todos, Comparator.comparingInt(Pokemon::getNumeroDex))) {
			arvore.inserir(pokemon);
		}

		Function<String, Pokemon> buscador = texto -> BuscaPokemon.buscar(arvore, texto);

		SwingUtilities.invokeLater(() -> {
			PokemonViewer janela = new PokemonViewer(buscador, () -> todos);
			janela.setVisible(true);
			if (!todos.isEmpty()) {
				janela.exibir(todos.get(0));
			}
		});
	}
}
