package PokePackage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CarregadorPokemon {

	public static List<Pokemon> carregar(String caminho) throws IOException {
		List<Pokemon> lista = new ArrayList<>();

		// UTF-8 explícito para os acentos não quebrarem
		try (BufferedReader leitor = new BufferedReader(
				new InputStreamReader(new FileInputStream(caminho), StandardCharsets.UTF_8))) {

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
}
