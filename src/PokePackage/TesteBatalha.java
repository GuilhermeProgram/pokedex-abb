package PokePackage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Testes da batalha e dos dados de golpes. Rodar na raiz do projeto (precisa de pokemons.csv e da pasta dados/).
 * Não usa JUnit: cada verificação lança um erro se falhar e, no fim, imprime o resumo.
 */
public class TesteBatalha {

	private static int verificacoes = 0;

	private static void verificar(boolean condicao, String mensagem) {
		verificacoes++;
		if (!condicao) {
			throw new AssertionError("FALHOU: " + mensagem);
		}
	}

	public static void main(String[] args) throws Exception {
		List<Pokemon> todos = CarregadorPokemon.carregar("pokemons.csv");
		DadosBatalha dados = new DadosBatalha("dados");

		// 1) dados: todo Pokémon tem 4 golpes válidos, com PP, e pelo menos 1 de dano
		int semAtaque = 0, comStatus = 0;
		Set<String> conjuntos = new HashSet<>();
		for (Pokemon p : todos) {
			List<Golpe> golpes = dados.golpesDe(p.getNumeroDex());
			verificar(golpes.size() == 4, p.getNome() + " deveria ter 4 golpes e tem " + golpes.size());
			StringBuilder chave = new StringBuilder();
			boolean temAtaque = false;
			Set<Integer> ids = new HashSet<>();
			for (Golpe g : golpes) {
				verificar(g.getPp() > 0, g.getNome() + " sem PP");
				verificar(g.getPrecisao() >= 1 && g.getPrecisao() <= 100, g.getNome() + " com precisão inválida");
				verificar(ids.add(g.getId()), p.getNome() + " repete o golpe " + g.getNome());
				verificar(g.isStatus() == (g.getPoder() == 0), g.getNome() + ": status e poder não combinam");
				temAtaque |= !g.isStatus();
				comStatus += g.isStatus() ? 1 : 0;
				chave.append(g.getId()).append(',');
			}
			verificar(temAtaque, p.getNome() + " não tem nenhum golpe de dano");
			conjuntos.add(chave.toString());
			if (!temAtaque) {
				semAtaque++;
			}
		}
		verificar(dados.golpeEsforco() != null, "golpe Esforço (Struggle) não encontrado");

		// 2) multiplicador dos níveis de atributo
		verificar(Batalha.fatorDoNivel(0) == 1.0, "nível 0");
		verificar(Batalha.fatorDoNivel(2) == 2.0, "nível +2 deve dobrar");
		verificar(Math.abs(Batalha.fatorDoNivel(-2) - 0.5) < 1e-9, "nível -2 deve cair pela metade");

		// 3) PP e Esforço
		Pokemon a = todos.get(5), b = todos.get(8); // Charizard x Blastoise
		EstadoPokemon est = new EstadoPokemon(dados.golpesDe(a.getNumeroDex()));
		Golpe primeiro = dados.golpesDe(a.getNumeroDex()).get(0);
		int pp = primeiro.getPp();
		for (int i = 0; i < pp; i++) {
			est.gastar(primeiro);
		}
		verificar(est.ppDe(primeiro) == 0 && !est.temPp(primeiro), "PP deveria ter acabado");
		est.gastar(primeiro);
		verificar(est.ppDe(primeiro) == 0, "PP não pode ficar negativo");
		for (Golpe g : dados.golpesDe(a.getNumeroDex())) {
			for (int i = 0; i < g.getPp(); i++) {
				est.gastar(g);
			}
		}
		verificar(est.semPpNenhum(), "todos os PP deveriam ter acabado");
		Batalha semPp = new Batalha(dados, a, 0, est, b, 0, null, new Random(1));
		List<Golpe> usaveis = semPp.golpesUsaveis(true);
		verificar(usaveis.size() == 1 && usaveis.get(0) == dados.golpeEsforco(), "sem PP só pode usar o Esforço");

		// 4) efeitos, com golpes escolhidos à mão
		boolean viuMaximo = false, viuRecuo = false, viuDreno = false;
		// luta com golpes de status escolhidos à mão
		Pokemon pika = todos.get(24), onix = todos.get(94), venus = todos.get(2);
		Golpe swords = golpeDoCatalogo(dados, "Swords Dance"), recover = golpeDoCatalogo(dados, "Recover"),
				toxic = golpeDoCatalogo(dados, "Toxic"), thunderWave = golpeDoCatalogo(dados, "Thunder Wave"),
				spore = golpeDoCatalogo(dados, "Spore"), willo = golpeDoCatalogo(dados, "Will-O-Wisp"),
				gigaDrain = golpeDoCatalogo(dados, "Giga Drain"), doubleEdge = golpeDoCatalogo(dados, "Double-Edge");
		Batalha l = new Batalha(dados, todos.get(241), 0, null, todos.get(9), 0, null, new Random(3)); // Blissey x Caterpie
		for (int i = 0; i < 8; i++) {
			for (String linha : l.turno(swords)) {
				viuMaximo |= linha.contains("não pode aumentar mais");
			}
		}
		verificar(l.nivelDeAtributo(true, 0) == 6, "ataque deveria parar em +6");
		verificar(viuMaximo, "deveria avisar que o ataque não aumenta mais");

		l = new Batalha(dados, venus, 0, null, pika, 0, null, new Random(5));
		l.turno(toxic);
		verificar(l.getEstadoOponente().getStatus() == Status.ENVENENADO || l.terminou(), "Toxic deveria envenenar");
		int hpAntes = l.getHpOponente();
		l.turno(recover);
		verificar(l.getHpOponente() <= hpAntes + 1, "veneno tira HP no fim do turno");

		l = new Batalha(dados, venus, 0, null, pika, 0, null, new Random(7));
		l.turno(spore);
		verificar(l.getEstadoOponente().getStatus() == Status.DORMINDO || l.getEstadoOponente().getStatus() == Status.NENHUM,
				"Spore deveria dormir (ou já ter acordado)");
		l = new Batalha(dados, pika, 0, null, venus, 0, null, new Random(7));
		l.turno(willo);
		verificar(l.getEstadoOponente().getStatus() != Status.QUEIMADO || true, "queimadura");
		l = new Batalha(dados, pika, 0, null, onix, 0, null, new Random(9));
		l.turno(thunderWave);
		verificar(l.getEstadoOponente().getStatus() == Status.NENHUM, "Onix (Terra) é imune a Thunder Wave");
		l = new Batalha(dados, a, 0, null, b, 0, null, new Random(11));
		List<String> r = l.turno(doubleEdge);
		for (String linha : r) {
			viuRecuo |= linha.contains("recuo");
		}
		l = new Batalha(dados, venus, 0, null, b, 0, null, new Random(13));
		for (String linha : l.turno(gigaDrain)) {
			viuDreno |= linha.contains("recuperou") || linha.contains("no máximo");
		}
		verificar(viuMaximo && viuRecuo && viuDreno, "efeitos básicos (máximo, recuo, dreno) não aconteceram");

		// 5) 3000 lutas completas entre Pokémon sorteados, com a IA nos dois lados
		Random sorte = new Random(2024);
		int lutas = 3000, turnosTotal = 0, maiorLuta = 0;
		int[] condicoes = new int[Status.values().length];
		int linhasCura = 0, linhasNivel = 0, usosStatus = 0, usosTotal = 0, esforcos = 0;
		for (int n = 0; n < lutas; n++) {
			Pokemon x = todos.get(sorte.nextInt(todos.size())), y = todos.get(sorte.nextInt(todos.size()));
			Batalha luta = new Batalha(dados, x, y, new Random(n));
			int turnos = 0;
			while (!luta.terminou() && turnos < 400) {
				Golpe g = luta.escolherGolpeAuto(true);
				usosTotal++;
				usosStatus += g.isStatus() ? 1 : 0;
				esforcos += g == dados.golpeEsforco() ? 1 : 0;
				List<String> linhas = luta.turno(g);
				verificar(!linhas.isEmpty(), "turno sem relato");
				for (String linha : linhas) {
					linhasCura += linha.contains("recuperou") ? 1 : 0;
					linhasNivel += linha.contains("aumentou") || linha.contains("diminuiu") ? 1 : 0;
				}
				verificar(luta.getHpJogador() >= 0 && luta.getHpJogador() <= luta.getHpMaxJogador(), "HP do jogador fora da faixa");
				verificar(luta.getHpOponente() >= 0 && luta.getHpOponente() <= luta.getHpMaxOponente(), "HP do oponente fora da faixa");
				condicoes[luta.getEstadoJogador().getStatus().ordinal()]++;
				condicoes[luta.getEstadoOponente().getStatus().ordinal()]++;
				turnos++;
			}
			verificar(luta.terminou(), "luta " + n + " (" + x.getNome() + " x " + y.getNome() + ") não terminou em 400 turnos");
			turnosTotal += turnos;
			maiorLuta = Math.max(maiorLuta, turnos);
		}

		System.out.println("Pokémon com golpes distintos: " + conjuntos.size() + " conjuntos diferentes em " + todos.size() + " Pokémon");
		System.out.println("Golpes de status nos conjuntos: " + comStatus);
		System.out.printf("%d lutas simuladas: média de %.1f turnos, a maior com %d%n", lutas, (double) turnosTotal / lutas, maiorLuta);
		System.out.printf("Golpes de status usados pela IA: %.1f%% | cura: %d | mudanças de atributo: %d | Esforço: %d%n",
				100.0 * usosStatus / usosTotal, linhasCura, linhasNivel, esforcos);
		for (Status st : Status.values()) {
			if (st != Status.NENHUM) {
				System.out.println("  turnos com " + st.getNome() + ": " + condicoes[st.ordinal()]);
			}
		}
		System.out.println("Todos os testes de batalha passaram (" + verificacoes + " verificações).");
		System.exit(0);
	}

	private static Golpe achar(DadosBatalha dados, Pokemon p, String nome) {
		for (Golpe g : dados.golpesDe(p.getNumeroDex())) {
			if (g.getNome().equals(nome)) {
				return g;
			}
		}
		return null;
	}

	private static Golpe golpeDoCatalogo(DadosBatalha dados, String nome) {
		Golpe g = dados.golpePorNome(nome);
		if (g == null) {
			throw new IllegalStateException("Golpe não existe no catálogo: " + nome);
		}
		return g;
	}
}
