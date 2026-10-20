package PokePackage;

import java.util.List;
import java.util.Random;

/** Teste no console do cálculo de dano (sem tela e sem ABB). Rode com a pasta do projeto como diretório de trabalho. */
public class BatalhaDemo {

	private static Pokemon achar(List<Pokemon> lista, String nome) {
		for (Pokemon p : lista) {
			if (p.getNome().equalsIgnoreCase(nome)) {
				return p;
			}
		}
		throw new IllegalArgumentException("Não encontrei " + nome);
	}

	private static void mostrarGolpes(DadosBatalha dados, Pokemon p) {
		Atributos a = dados.atributos(p.getNumeroDex());
		System.out.println(p.getNome() + " (HP " + a.hpNaBatalha() + ", Vel " + a.velocidadeNaBatalha() + ")");
		for (Golpe g : dados.golpesDe(p.getNumeroDex())) {
			System.out.println("   - " + g);
		}
	}

	private static void testar(DadosBatalha dados, Pokemon atq, String golpe, Pokemon def) {
		Golpe g = null;
		for (Golpe x : dados.golpesDe(atq.getNumeroDex())) {
			if (x.getNome().equalsIgnoreCase(golpe)) {
				g = x;
			}
		}
		if (g == null) {
			System.out.println(atq.getNome() + " não tem " + golpe);
			return;
		}
		DadosBatalha.Resultado r = dados.atacar(atq, g, def, new Random(1));
		System.out.printf("%s usa %s em %s: tipo x%.1f, dano esperado %.0f, dano sorteado %d (HP de %s: %d)%n",
				atq.getNome(), g.getNome(), def.getNome(), r.tipo, dados.danoEsperado(atq, g, def), r.dano,
				def.getNome(), dados.atributos(def.getNumeroDex()).hpNaBatalha());
	}

	public static void main(String[] args) throws Exception {
		List<Pokemon> lista = CarregadorPokemon.carregar("pokemons.csv");
		DadosBatalha dados = new DadosBatalha("dados");

		Pokemon charizard = achar(lista, "Charizard"), blastoise = achar(lista, "Blastoise");
		Pokemon gyarados = achar(lista, "Gyarados"), pikachu = achar(lista, "Pikachu");

		mostrarGolpes(dados, charizard);
		mostrarGolpes(dados, blastoise);
		System.out.println();
		testar(dados, charizard, "Overheat", blastoise);   // fogo contra água: não é muito efetivo
		testar(dados, blastoise, "Wave Crash", charizard); // água contra fogo: super efetivo
		testar(dados, pikachu, "Wild Charge", gyarados);   // elétrico contra água/voador: x4
	}
}
