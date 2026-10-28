package pokedex.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.*;

import pokedex.abb.ArvoreBinaria;
import pokedex.modelo.Pokemon;

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
		this(buscador, todos, new ArvoreBinaria());
	}

	/** O "pc" é a ABB dos Pokémon capturados: cadastrar insere nela e (em breve) liberar remove. */
	public PokemonViewer(Function<String, Pokemon> buscador, Supplier<List<Pokemon>> todos, ArvoreBinaria pc) {
		super("Pokédex");
		painel = new PainelPokedex(buscador, todos, pc);
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
}
