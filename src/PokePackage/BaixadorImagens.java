package PokePackage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

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
	private static final String PASTA = "images";

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

		System.out.println();
		System.out.println("Baixadas: " + baixadas + " | Puladas: " + puladas + " | Falhas: " + falhas
				+ " | Ignoradas (fora da API): " + ignorados);
		if (falhas > 0) {
			System.out.println("Rode de novo: só as que falharam serão baixadas.");
		}
	}
}
