package PokePackage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CarregadorPokemon {

	public static List<Pokemon> carregar(String caminho) throws IOException {
		List<Pokemon> lista = new ArrayList<>();

		// UTF-8 explícito para os acentos não quebrarem
		try (BufferedReader leitor = new BufferedReader(
				new InputStreamReader(new FileInputStream(CaminhosProjeto.resolver(caminho).toFile()), StandardCharsets.UTF_8))) {

			String linha = leitor.readLine(); // pula o cabeçalho

			while ((linha = leitor.readLine()) != null) {
				if (linha.isBlank()) {
					continue; // ignora linhas vazias
				}

				String[] campos = linha.split(";", -1);

				int numeroDex = Integer.parseInt(campos[0].trim());
				String nome = campos[1].trim();
				String regiao = campos[2].trim();
				String tipo1 = campos[3].trim();
				String tipo2 = campos[4].trim().isEmpty() ? null : campos[4].trim();
				double altura = Double.parseDouble(campos[5].trim());
				double peso = Double.parseDouble(campos[6].trim());
				String descricao = campos[7].trim();
				String curiosidade = campos[8].trim();

				lista.add(new Pokemon(numeroDex, nome, regiao, tipo1, tipo2, altura, peso, descricao, curiosidade));
			}
		}
		return lista;
	}

	/**
	 * Devolve os Pokémon na ordem em que devem ser inseridos na ABB para ela ficar balanceada.
	 *
	 * Inserir em ordem crescente (como o CSV está) faz a ABB virar uma lista encadeada: altura n, busca O(n).
	 * Aqui ordenamos pela chave da árvore e inserimos primeiro o elemento do meio, depois o meio de cada
	 * metade, e assim por diante. A altura fica em torno de log2(n).
	 *
	 * @param lista    Pokémon carregados (não é alterada)
	 * @param chave    a mesma chave da ABB: por Dex ou por nome
	 */
	public static List<Pokemon> ordemBalanceada(List<Pokemon> lista, Comparator<Pokemon> chave) {
		List<Pokemon> ordenada = new ArrayList<>(lista);
		ordenada.sort(chave);
		List<Pokemon> resultado = new ArrayList<>(ordenada.size());
		meioPrimeiro(ordenada, 0, ordenada.size() - 1, resultado);
		return resultado;
	}

	private static void meioPrimeiro(List<Pokemon> ordenada, int ini, int fim, List<Pokemon> saida) {
		if (ini > fim) {
			return;
		}
		int meio = (ini + fim) >>> 1;
		saida.add(ordenada.get(meio));
		meioPrimeiro(ordenada, ini, meio - 1, saida);
		meioPrimeiro(ordenada, meio + 1, fim, saida);
	}
}
