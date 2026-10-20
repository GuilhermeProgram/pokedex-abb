package PokePackage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Carrega os arquivos da pasta dados/ (atributos, golpes, golpes de cada Pokémon e vantagens de tipo)
 * e calcula o dano de um golpe. Não depende da ABB nem da interface.
 */
public class DadosBatalha {

	/** Resultado de um ataque: dano, se acertou, se foi crítico e o multiplicador de tipo. */
	public static class Resultado {
		public final boolean acertou;
		public final boolean critico;
		public final double tipo; // 0 = não afeta, 0.5, 1, 2, 4...
		public final int dano;

		Resultado(boolean acertou, boolean critico, double tipo, int dano) {
			this.acertou = acertou;
			this.critico = critico;
			this.tipo = tipo;
			this.dano = dano;
		}
	}

	private final Map<Integer, Atributos> atributos = new HashMap<>();
	private final Map<Integer, Golpe> golpes = new HashMap<>();
	private final Map<Integer, List<Golpe>> golpesDoPokemon = new HashMap<>();
	private final Map<String, Integer> vantagens = new HashMap<>(); // "Atacante>Defensor" -> fator em %

	/** @param pasta pasta com os CSV, normalmente "dados" */
	public DadosBatalha(String pasta) throws IOException {
		for (String[] c : ler(pasta + "/atributos.csv")) {
			atributos.put(Integer.parseInt(c[0]), new Atributos(num(c[1]), num(c[2]), num(c[3]), num(c[4]), num(c[5]), num(c[6])));
		}
		for (String[] c : ler(pasta + "/golpes.csv")) {
			int id = num(c[0]);
			golpes.put(id, new Golpe(id, c[1], c[2], "Físico".equals(c[3]), num(c[4]), num(c[5]), num(c[6])));
		}
		for (String[] c : ler(pasta + "/golpes_pokemon.csv")) {
			List<Golpe> lista = new ArrayList<>();
			for (int i = 1; i < c.length; i++) {
				if (!c[i].isBlank() && golpes.containsKey(num(c[i]))) {
					lista.add(golpes.get(num(c[i])));
				}
			}
			golpesDoPokemon.put(Integer.parseInt(c[0]), lista);
		}
		for (String[] c : ler(pasta + "/vantagens.csv")) {
			vantagens.put(c[0] + ">" + c[1], num(c[2]));
		}
	}

	private static int num(String s) {
		return Integer.parseInt(s.trim());
	}

	/** Lê um CSV com ';' (UTF-8), sem o cabeçalho. */
	private static List<String[]> ler(String caminho) throws IOException {
		List<String[]> linhas = new ArrayList<>();
		try (BufferedReader leitor = new BufferedReader(
				new InputStreamReader(new FileInputStream(caminho), StandardCharsets.UTF_8))) {
			leitor.readLine(); // cabeçalho
			String linha;
			while ((linha = leitor.readLine()) != null) {
				if (!linha.isBlank()) {
					linhas.add(linha.split(";", -1));
				}
			}
		}
		return linhas;
	}

	public Atributos atributos(int numeroDex) {
		return atributos.get(numeroDex);
	}

	/** Os (até) 4 golpes do Pokémon. */
	public List<Golpe> golpesDe(int numeroDex) {
		List<Golpe> lista = golpesDoPokemon.get(numeroDex);
		return lista == null ? new ArrayList<>() : lista;
	}

	/** Multiplicador do tipo do golpe contra o Pokémon defensor (2 tipos multiplicam entre si). */
	public double multiplicadorDeTipo(String tipoDoGolpe, Pokemon defensor) {
		double m = fator(tipoDoGolpe, defensor.getTipo1());
		if (defensor.getTipo2() != null) {
			m *= fator(tipoDoGolpe, defensor.getTipo2());
		}
		return m;
	}

	private double fator(String atacante, String defensor) {
		Integer f = vantagens.get(atacante + ">" + defensor);
		return f == null ? 1.0 : f / 100.0;
	}

	/**
	 * Dano esperado, sem sorte (sem sorteio de erro, crítico ou variação). Serve para a IA comparar golpes.
	 */
	public double danoEsperado(Pokemon atacante, Golpe g, Pokemon defensor) {
		return danoBase(atacante, g, defensor) * stab(atacante, g) * multiplicadorDeTipo(g.getTipo(), defensor)
				* 0.925 * (g.getPrecisao() / 100.0);
	}

	private double danoBase(Pokemon atacante, Golpe g, Pokemon defensor) {
		Atributos a = atributos(atacante.getNumeroDex()), d = atributos(defensor.getNumeroDex());
		int ataque = g.isFisico() ? a.ataqueNaBatalha() : a.ataqueEspecialNaBatalha();
		int defesa = g.isFisico() ? d.defesaNaBatalha() : d.defesaEspecialNaBatalha();
		int nivel = Atributos.NIVEL;
		return (2 * nivel / 5 + 2) * g.getPoder() * (double) ataque / defesa / 50 + 2;
	}

	private static double stab(Pokemon atacante, Golpe g) {
		boolean mesmoTipo = g.getTipo().equals(atacante.getTipo1()) || g.getTipo().equals(atacante.getTipo2());
		return mesmoTipo ? 1.5 : 1.0;
	}

	/** Executa o ataque de verdade: sorteia acerto, crítico (1 em 24) e variação de 85% a 100%. */
	public Resultado atacar(Pokemon atacante, Golpe g, Pokemon defensor, Random sorte) {
		if (sorte.nextInt(100) >= g.getPrecisao()) {
			return new Resultado(false, false, 1, 0);
		}
		double tipo = multiplicadorDeTipo(g.getTipo(), defensor);
		if (tipo == 0) {
			return new Resultado(true, false, 0, 0);
		}
		boolean critico = sorte.nextInt(24) == 0;
		double variacao = 0.85 + sorte.nextDouble() * 0.15;
		double dano = danoBase(atacante, g, defensor) * stab(atacante, g) * tipo * variacao * (critico ? 1.5 : 1.0);
		return new Resultado(true, critico, tipo, Math.max(1, (int) dano));
	}
}
