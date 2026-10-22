package PokePackage;

import java.util.Comparator;
import java.util.List;

/** Teste da ABB sem tela: inserir, buscar, remover e percorrer. Rode da raiz do projeto. */
public class ArvoreTeste {
	static int falhas = 0;

	static void confere(String descricao, boolean ok) {
		System.out.println((ok ? "OK    " : "FALHOU ") + descricao);
		if (!ok) {
			falhas++;
		}
	}

	static Pokemon p(int dex, String nome) {
		return new Pokemon(dex, nome, "Kanto", "Normal", null, 1.0, 1.0, "", "");
	}

	public static void main(String[] args) throws Exception {
		// 1) árvore pequena, com resultado conhecido
		ArvoreBinaria a = new ArvoreBinaria();
		int[] ordem = { 25, 10, 40, 5, 15, 30, 50 };
		for (int n : ordem) {
			a.inserir(p(n, "P" + n));
		}
		confere("em ordem = 5 10 15 25 30 40 50", a.emOrdem().stream().map(Pokemon::getNumeroDex).toList().equals(List.of(5, 10, 15, 25, 30, 40, 50)));
		confere("altura = 3", a.altura() == 3);
		confere("buscar(15) acha P15", a.buscar(15) != null && a.buscar(15).getNome().equals("P15"));
		confere("buscar(99) devolve null", a.buscar(99) == null);
		a.inserir(p(25, "Repetido"));
		confere("inserir número repetido não duplica", a.emOrdem().size() == 7 && a.buscar(25).getNome().equals("P25"));
		a.remover(5);
		confere("remover folha (5)", a.buscar(5) == null && a.emOrdem().size() == 6);
		a.remover(10);
		confere("remover nó com 1 filho (10)", a.buscar(10) == null && a.buscar(15) != null);
		a.remover(25);
		confere("remover raiz com 2 filhos (25)", a.buscar(25) == null && a.emOrdem().stream().map(Pokemon::getNumeroDex).toList().equals(List.of(15, 30, 40, 50)));
		a.remover(999);
		confere("remover inexistente não altera", a.emOrdem().size() == 4);

		// 2) os 1025 Pokémon do CSV: ordem do CSV x ordem balanceada
		List<Pokemon> todos = CarregadorPokemon.carregar("pokemons.csv");
		ArvoreBinaria seq = new ArvoreBinaria(), bal = new ArvoreBinaria();
		for (Pokemon x : todos) {
			seq.inserir(x);
		}
		for (Pokemon x : CarregadorPokemon.ordemBalanceada(todos, Comparator.comparingInt(Pokemon::getNumeroDex))) {
			bal.inserir(x);
		}
		System.out.println("altura inserindo na ordem do CSV: " + seq.altura() + " | balanceada: " + bal.altura());
		confere("1025 Pokémon nas duas árvores", seq.emOrdem().size() == 1025 && bal.emOrdem().size() == 1025);
		confere("buscar(6) = Charizard", bal.buscar(6).getNome().equals("Charizard"));
		confere("buscar(1025) acha o último", bal.buscar(1025) != null);
		confere("em ordem da balanceada = Dex crescente", bal.emOrdem().equals(todos));
		System.out.println(falhas == 0 ? "\nTodos os testes passaram." : "\n" + falhas + " teste(s) falharam.");
	}
}
