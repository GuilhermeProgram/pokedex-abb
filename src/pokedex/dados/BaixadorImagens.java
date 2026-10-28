package pokedex.dados;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import pokedex.modelo.Pokemon;

/**
 * Baixa a arte oficial dos Pokémon do pokemons.csv (PokeAPI, repositório de sprites)
 * e salva em images/{numeroDex}.png.
 *
 * O CSV manda: todo Pokémon que estiver nele (até o limite da API) tem a imagem
 * baixada. Para adicionar Pokémon, basta incluir a linha no CSV e rodar de novo.
 *
 * Rode com internet. Imagens que já existem na pasta são puladas.
 */
public class BaixadorImagens {

	/** Último número Dex que a API tem imagem. Pokémon acima disso são ignorados. */
	public static final int ULTIMO_DEX_API = 1025;

	private static final String BASE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/";
	private static final String PASTA = CaminhosProjeto.resolver("images").toString();

	/** Sprites pixelados 96x96 (usados nas caixas do PC), salvos em images/pixel/{numeroDex}.png. */
	private static final String BASE_PIXEL = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/";
	private static final String PASTA_PIXEL = CaminhosProjeto.resolver("images/pixel").toString();

	private static boolean baixarSprite(String url, Path destino) throws InterruptedException {
		if (Files.exists(destino)) {
			return false;
		}
		try (InputStream in = URI.create(url).toURL().openStream()) {
			Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
			Thread.sleep(100);
			return true;
		} catch (IOException e) {
			System.out.println("Falhou sprite: " + url + " (" + e.getMessage() + ")");
			return false;
		}
	}

	/** Sprites pixelados de frente (images/pixel/N.png) e de costas (images/pixel/costas/N.png), inclusive das Megas. */
	private static void baixarPixel(List<Pokemon> lista) throws Exception {
		Files.createDirectories(Path.of(PASTA_PIXEL, "costas"));
		java.util.List<Integer> ids = new java.util.ArrayList<>();
		for (Pokemon p : lista) {
			if (p.getNumeroDex() >= 1 && p.getNumeroDex() <= ULTIMO_DEX_API) {
				ids.add(p.getNumeroDex());
			}
		}
		Path megas = CaminhosProjeto.resolver("dados/megas.csv");
		if (Files.exists(megas)) {
			List<String> linhas = Files.readAllLines(megas, java.nio.charset.StandardCharsets.UTF_8);
			for (String linha : linhas.subList(1, linhas.size())) {
				if (!linha.isBlank()) {
					ids.add(Integer.parseInt(linha.split(";")[0].trim()));
				}
			}
		}
		int baixadas = 0;
		for (int id : ids) {
			if (baixarSprite(BASE_PIXEL + id + ".png", Path.of(PASTA_PIXEL, id + ".png"))) {
				baixadas++;
			}
			if (baixarSprite(BASE_PIXEL + "back/" + id + ".png", Path.of(PASTA_PIXEL, "costas", id + ".png"))) {
				baixadas++;
			}
		}
		System.out.println("Sprites pixel baixados: " + baixadas);
	}

	/** Baixa também a arte das formas Mega (ids da forma em dados/megas.csv, coluna 1). */
	private static void baixarMegas() throws Exception {
		Path csv = CaminhosProjeto.resolver("dados/megas.csv");
		if (!Files.exists(csv)) {
			return;
		}
		int baixadas = 0;
		for (String linha : Files.readAllLines(csv, java.nio.charset.StandardCharsets.UTF_8).subList(1, Files.readAllLines(csv).size())) {
			if (linha.isBlank()) {
				continue;
			}
			String id = linha.split(";")[0];
			Path destino = Path.of(PASTA, id + ".png");
			if (Files.exists(destino)) {
				continue;
			}
			try (InputStream in = URI.create(BASE + id + ".png").toURL().openStream()) {
				Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
				baixadas++;
				System.out.println("Baixada (mega): " + destino);
			} catch (IOException e) {
				System.out.println("Falhou mega " + id + ": " + e.getMessage());
			}
			Thread.sleep(100);
		}
		System.out.println("Megas baixadas: " + baixadas);
	}

	public static void main(String[] args) throws Exception {
		List<Pokemon> lista = CarregadorPokemon.carregar("pokemons.csv");

		Files.createDirectories(Path.of(PASTA));

		int baixadas = 0;
		int puladas = 0;
		int falhas = 0;
		int ignorados = 0;

		for (Pokemon p : lista) {
			int dex = p.getNumeroDex();

			if (dex < 1 || dex > ULTIMO_DEX_API) {
				ignorados++; // a API não tem imagem desse número
				System.out.println("Ignorado (fora do limite da API 1-" + ULTIMO_DEX_API + "): #" + dex + " " + p.getNome());
				continue;
			}

			Path destino = Path.of(PASTA, dex + ".png");

			if (Files.exists(destino)) {
				puladas++; // já baixada antes
				continue;
			}

			String endereco = BASE + dex + ".png";
			try (InputStream in = URI.create(endereco).toURL().openStream()) {
				Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
				baixadas++;
				System.out.println("Baixada: " + destino + " (" + p.getNome() + ")");
			} catch (IOException e) {
				falhas++;
				System.out.println("Falhou #" + dex + " " + p.getNome() + ": " + e.getMessage());
			}

			Thread.sleep(100); // pausa para não sobrecarregar o servidor
		}

 
		baixarMegas();
		baixarPixel(lista);

		System.out.println();
		System.out.println("Baixadas: " + baixadas + " | Puladas: " + puladas + " | Falhas: " + falhas
				+ " | Ignoradas (fora da API): " + ignorados);
		if (falhas > 0) {
			System.out.println("Rode de novo: só as que falharam serão baixadas.");
		}
	}
}
