package PokePackage;

import java.util.Comparator;
import java.util.List;

/** Mostra a diferença de altura da árvore entre inserir em ordem do CSV e em ordem balanceada. */
public class AlturaDemo {

	// Nó mínimo só para medir a altura; a ABB de verdade é a do grupo.
	private static class No {
		Pokemon p;
		No esq, dir;
		No(Pokemon p) { this.p = p; }
	}

	private static No inserir(No raiz, Pokemon p, Comparator<Pokemon> chave) {
		if (raiz == null) {
			return new No(p);
		}
		if (chave.compare(p, raiz.p) < 0) {
			raiz.esq = inserir(raiz.esq, p, chave);
		} else {
			raiz.dir = inserir(raiz.dir, p, chave);
		}
		return raiz;
	}

	private static int altura(No n) {
		return n == null ? 0 : 1 + Math.max(altura(n.esq), altura(n.dir));
	}

	private static int alturaAoInserir(List<Pokemon> ordem, Comparator<Pokemon> chave) {
		No raiz = null;
		for (Pokemon p : ordem) {
			raiz = inserir(raiz, p, chave);
		}
		return altura(raiz);
	}

	public static void main(String[] args) throws Exception {
		List<Pokemon> lista = CarregadorPokemon.carregar("pokemons.csv");
		Comparator<Pokemon> porDex = Comparator.comparingInt(Pokemon::getNumeroDex);
		Comparator<Pokemon> porNome = Comparator.comparing(Pokemon::getNome, String.CASE_INSENSITIVE_ORDER);

		System.out.println("Pokémon: " + lista.size());
		System.out.println("Altura mínima possível: " + (int) Math.ceil(Math.log(lista.size() + 1) / Math.log(2)));
		System.out.println();
		System.out.println("Por Dex  - ordem do CSV:  " + alturaAoInserir(lista, porDex));
		System.out.println("Por Dex  - balanceada:    " + alturaAoInserir(CarregadorPokemon.ordemBalanceada(lista, porDex), porDex));
		System.out.println("Por nome - ordem do CSV:  " + alturaAoInserir(lista, porNome));
		System.out.println("Por nome - balanceada:    " + alturaAoInserir(CarregadorPokemon.ordemBalanceada(lista, porNome), porNome));
	}
}
