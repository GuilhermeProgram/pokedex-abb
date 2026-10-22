package PokePackage;

import java.io.BufferedReader;
import java.io.File;
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
	/** Golpe usado quando todos os PP acabaram (no jogo se chama Struggle / Esforço). */
	private Golpe esforco;
	private final Map<Integer, List<Golpe>> golpesDoPokemon = new HashMap<>();
	private final Map<String, Integer> vantagens = new HashMap<>(); // "Atacante>Defensor" -> fator em %
	/** Formas Mega de cada Pokémon (chave = número Dex original). Cada forma é um Pokemon com numeroDex = id da forma (>10000). */
	private final Map<Integer, List<Pokemon>> megas = new HashMap<>();

	/** @param pasta pasta com os CSV, normalmente "dados" */
	public DadosBatalha(String pasta) throws IOException {
		for (String[] c : ler(pasta + "/atributos.csv")) {
			atributos.put(Integer.parseInt(c[0]), new Atributos(num(c[1]), num(c[2]), num(c[3]), num(c[4]), num(c[5]), num(c[6])));
		}
		for (String[] c : ler(pasta + "/golpes.csv")) {
			int id = num(c[0]);
			golpes.put(id, new Golpe(id, c[1], c[2], c[3], num(c[4]), num(c[5]), num(c[6]), num(c[7]), c[8].trim(),
					c[9].isBlank() ? 0 : num(c[9]), c[10].trim()));
			if ("Struggle".equals(c[1])) {
				esforco = golpes.get(id);
			}
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
		// megas.csv é opcional: sem ele a batalha funciona, só não há Mega Evolução
		if (new File(pasta + "/megas.csv").exists()) {
			for (String[] c : ler(pasta + "/megas.csv")) {
				int idForma = num(c[0]), dex = num(c[1]);
				Pokemon mega = new Pokemon(idForma, c[2], "", c[3], c[4].isBlank() ? null : c[4], null, null, "", "");
				atributos.put(idForma, new Atributos(num(c[5]), num(c[6]), num(c[7]), num(c[8]), num(c[9]), num(c[10])));
				golpesDoPokemon.put(idForma, golpesDe(dex)); // a Mega usa os mesmos golpes da forma normal
				megas.computeIfAbsent(dex, k -> new ArrayList<>()).add(mega);
			}
		}
	}

	/** Formas Mega do Pokémon (lista vazia se não tiver). */
	public List<Pokemon> megasDe(int numeroDex) {
		List<Pokemon> lista = megas.get(numeroDex);
		return lista == null ? new ArrayList<>() : lista;
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

	/** Procura um golpe do catálogo pelo nome (em inglês, como no golpes.csv); null se não existir. */
	public Golpe golpePorNome(String nome) {
		for (Golpe g : golpes.values()) {
			if (g.getNome().equalsIgnoreCase(nome)) {
				return g;
			}
		}
		return null;
	}

	/** O golpe que o Pokémon é obrigado a usar quando não sobra PP em nenhum dos seus golpes. */
	public Golpe golpeEsforco() {
		return esforco;
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
	 * Golpes de status não causam dano (devolve 0).
	 */
	public double danoEsperado(Pokemon atacante, Golpe g, Pokemon defensor) {
		return danoEsperado(atacante, g, defensor, 1, 1);
	}

	/** Igual ao anterior, mas com os multiplicadores de atributo (níveis e queimadura) do atacante e do defensor. */
	public double danoEsperado(Pokemon atacante, Golpe g, Pokemon defensor, double mAtaque, double mDefesa) {
		if (g.isStatus()) {
			return 0;
		}
		return danoBase(atacante, g, defensor, mAtaque, mDefesa) * stab(atacante, g)
				* multiplicadorDeTipo(g.getTipo(), defensor) * 0.925 * (g.getPrecisao() / 100.0);
	}

	private double danoBase(Pokemon atacante, Golpe g, Pokemon defensor, double mAtaque, double mDefesa) {
		Atributos a = atributos(atacante.getNumeroDex()), d = atributos(defensor.getNumeroDex());
		int ataque = g.isFisico() ? a.ataqueNaBatalha() : a.ataqueEspecialNaBatalha();
		int defesa = g.isFisico() ? d.defesaNaBatalha() : d.defesaEspecialNaBatalha();
		int nivel = Atributos.NIVEL;
		return (2 * nivel / 5 + 2) * g.getPoder() * (ataque * mAtaque) / Math.max(1, defesa * mDefesa) / 50 + 2;
	}

	private static double stab(Pokemon atacante, Golpe g) {
		boolean mesmoTipo = g.getTipo().equals(atacante.getTipo1()) || g.getTipo().equals(atacante.getTipo2());
		return mesmoTipo ? 1.5 : 1.0;
	}

	public Resultado atacar(Pokemon atacante, Golpe g, Pokemon defensor, Random sorte) {
		return atacar(atacante, g, defensor, sorte, 1, 1);
	}

	/**
	 * Executa o ataque de verdade: sorteia acerto, crítico (1 em 24) e variação de 85% a 100%.
	 * mAtaque e mDefesa são os multiplicadores (níveis de atributo, queimadura) do atacante e do defensor.
	 */
	public Resultado atacar(Pokemon atacante, Golpe g, Pokemon defensor, Random sorte, double mAtaque, double mDefesa) {
		if (sorte.nextInt(100) >= g.getPrecisao()) {
			return new Resultado(false, false, 1, 0);
		}
		double tipo = multiplicadorDeTipo(g.getTipo(), defensor);
		if (tipo == 0) {
			return new Resultado(true, false, 0, 0);
		}
		boolean critico = sorte.nextInt(24) == 0;
		double variacao = 0.85 + sorte.nextDouble() * 0.15;
		double dano = danoBase(atacante, g, defensor, mAtaque, mDefesa) * stab(atacante, g) * tipo * variacao
				* (critico ? 1.5 : 1.0);
		return new Resultado(true, critico, tipo, Math.max(1, (int) dano));
	}
}
