package pokedex.ui.recursos;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

import pokedex.dados.CaminhosProjeto;
import pokedex.modelo.Pokemon;

public class Imagens {
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

	public static BufferedImage grande(int dex) {
		BufferedImage c = GRANDES.get(dex);
		if (c != null) {
			return c == SEM ? null : c;
		}
		BufferedImage img = ler(dex);
		GRANDES.put(dex, img == null ? SEM : img);
		return img;
	}

	public static BufferedImage miniatura(int dex) {
		return MINIATURAS.get(dex);
	}

	// sprites pixelados das caixas do PC (images/pixel/{dex}.png); guarda só os usados há pouco
	private static final Map<Integer, BufferedImage> PIXELS = Collections
			.synchronizedMap(new LinkedHashMap<Integer, BufferedImage>(16, 0.75f, true) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(Map.Entry<Integer, BufferedImage> e) {
					return size() > 300;
				}
			});

	/** Sprite pixelado do Pokémon (ou null se o arquivo não existe: rode o BaixadorImagens). */
	public static BufferedImage pixel(int dex) {
		return lerPixel(dex, "images/pixel/" + dex + ".png", dex);
	}

	/** Sprite pixelado de costas (usado na batalha); null se não existe. */
	public static BufferedImage pixelCostas(int dex) {
		return lerPixel(-dex, "images/pixel/costas/" + dex + ".png", dex);
	}

	private static BufferedImage lerPixel(int chave, String caminho, int dex) {
		BufferedImage c = PIXELS.get(chave);
		if (c != null) {
			return c == SEM ? null : c;
		}
		BufferedImage img = null;
		File arquivo = CaminhosProjeto.resolver(caminho).toFile();
		if (arquivo.exists()) {
			try {
				img = ImageIO.read(arquivo);
			} catch (IOException e) {
				img = null;
			}
		}
		PIXELS.put(chave, img == null ? SEM : img);
		return img;
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
	public static BufferedImage silhueta(int dex) {
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

	public static BufferedImage silhuetaMiniatura(int dex) {
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
		File arquivo = CaminhosProjeto.resolver("images/" + dex + ".png").toFile();
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
	public static void preCarregarMiniaturas(List<Pokemon> lista, Runnable aoAvancar) {
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
		File arquivo = CaminhosProjeto.resolver("images/mini/" + dex + ".png").toFile();
		File original = CaminhosProjeto.resolver("images/" + dex + ".png").toFile();
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
			File pasta = CaminhosProjeto.resolver("images/mini").toFile();
			if (pasta.isDirectory() || pasta.mkdirs()) {
				ImageIO.write(mini, "png", new File(pasta, dex + ".png"));
			}
		} catch (IOException | RuntimeException e) {
			// sem cache em disco: só fica mais lento na próxima abertura
		}
	}

	/** Só para testes sem janela: espera a pré-carga terminar. */
	public static void esperarPreCarga() throws InterruptedException {
		if (preCarga != null) {
			preCarga.join();
		}
	}

	public static BufferedImage reduzir(BufferedImage origem, int lado) {
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
