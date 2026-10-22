package PokePackage;

import java.util.List;
import java.util.Random;

/** Teste no console: luta completa com a IA escolhendo os golpes dos dois lados. */
public class BatalhaConsole {

	private static Pokemon achar(List<Pokemon> lista, String nome) {
		for (Pokemon p : lista) {
			if (p.getNome().equalsIgnoreCase(nome)) {
				return p;
			}
		}
		throw new IllegalArgumentException("Não encontrei " + nome);
	}

	public static void main(String[] args) throws Exception {
		List<Pokemon> lista = CarregadorPokemon.carregar("pokemons.csv");
		DadosBatalha dados = new DadosBatalha("dados");
		Pokemon a = achar(lista, args.length > 0 ? args[0] : "Charizard");
		Pokemon b = achar(lista, args.length > 1 ? args[1] : "Blastoise");

		Batalha batalha = new Batalha(dados, a, b, new Random(7));
		System.out.println(a.getNome() + " x " + b.getNome());
		while (!batalha.terminou()) {
			Golpe escolhido = batalha.escolherGolpeAuto(true); // aqui seria o jogador clicando num golpe
			System.out.println("\n--- Turno " + (batalha.getNumeroTurno() + 1) + " ---");
			for (String linha : batalha.turno(escolhido)) {
				System.out.println(linha);
			}
			System.out.println("HP: " + a.getNome() + " " + batalha.getHpJogador() + "/" + batalha.getHpMaxJogador()
					+ " | " + b.getNome() + " " + batalha.getHpOponente() + "/" + batalha.getHpMaxOponente());
		}
	}
}
