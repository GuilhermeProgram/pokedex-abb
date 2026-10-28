package pokedex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import javax.swing.*;

import pokedex.abb.ArvoreBinaria;
import pokedex.busca.BuscaPokemon;
import pokedex.dados.BaixadorImagens;
import pokedex.dados.CarregadorPokemon;
import pokedex.modelo.Pokemon;
import pokedex.ui.PokemonViewer;

/**
 * Ponto de entrada: carrega o CSV na ABB do catálogo e abre a janela da Pokédex.
 */
public class Main {

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

		// Catálogo: ABB com os 1025 Pokémon do CSV (usada na busca, balanceada pela ordem de inserção).
		final ArvoreBinaria arvore = new ArvoreBinaria();
		for (Pokemon pokemon : CarregadorPokemon.ordemBalanceada(todos, Comparator.comparingInt(Pokemon::getNumeroDex))) {
			arvore.inserir(pokemon);
		}

		// A View utiliza esta função para buscar por número ou nome (classe BuscaPokemon, em cima da ABB).
		Function<String, Pokemon> buscador = texto -> BuscaPokemon.buscar(arvore, texto);

		// PC: ABB que começa vazia; cada Pokémon cadastrado é inserido nela.
		final ArvoreBinaria pc = new ArvoreBinaria();

		SwingUtilities.invokeLater(() -> {
			PokemonViewer janela = new PokemonViewer(buscador, () -> todos, pc);
			janela.setVisible(true);
			if (!todos.isEmpty()) {
				janela.exibir(todos.get(0));
			}
		});
	}
}
