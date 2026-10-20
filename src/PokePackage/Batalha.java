package PokePackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lógica de uma batalha 1 contra 1 por turnos. Não tem tela: a interface chama {@link #turno(Golpe)}
 * com o golpe escolhido pelo jogador e recebe de volta o texto do que aconteceu.
 */
public class Batalha {

	/** Chance de a IA ignorar o melhor golpe e escolher outro. */
	private static final double CHANCE_ALEATORIA = 0.2;

	/** Multiplica o HP para a luta durar alguns turnos (no nível 50 puro, um golpe forte já derruba). */
	private static final int FATOR_HP = 3;

	private final DadosBatalha dados;
	private final Random sorte;
	private final Pokemon jogador, oponente;
	private int hpJogador, hpOponente;
	private final int hpMaxJogador, hpMaxOponente;
	private int numeroTurno = 0;

	public Batalha(DadosBatalha dados, Pokemon jogador, Pokemon oponente, Random sorte) {
		this.dados = dados;
		this.sorte = sorte;
		this.jogador = jogador;
		this.oponente = oponente;
		this.hpMaxJogador = dados.atributos(jogador.getNumeroDex()).hpNaBatalha() * FATOR_HP;
		this.hpMaxOponente = dados.atributos(oponente.getNumeroDex()).hpNaBatalha() * FATOR_HP;
		this.hpJogador = hpMaxJogador;
		this.hpOponente = hpMaxOponente;
	}

	public Pokemon getJogador() {
		return jogador;
	}

	public Pokemon getOponente() {
		return oponente;
	}

	public int getHpJogador() {
		return hpJogador;
	}

	public int getHpOponente() {
		return hpOponente;
	}

	public int getHpMaxJogador() {
		return hpMaxJogador;
	}

	public int getHpMaxOponente() {
		return hpMaxOponente;
	}

	public int getNumeroTurno() {
		return numeroTurno;
	}

	public boolean terminou() {
		return hpJogador <= 0 || hpOponente <= 0;
	}

	/** O vencedor, ou null se a luta ainda não acabou. */
	public Pokemon vencedor() {
		if (!terminou()) {
			return null;
		}
		return hpOponente <= 0 ? jogador : oponente;
	}

	/** Golpe do oponente: quase sempre o de maior dano esperado, às vezes outro. */
	public Golpe escolherGolpeIA(Pokemon atacante, Pokemon defensor) {
		List<Golpe> golpes = dados.golpesDe(atacante.getNumeroDex());
		Golpe melhor = golpes.get(0);
		for (Golpe g : golpes) {
			if (dados.danoEsperado(atacante, g, defensor) > dados.danoEsperado(atacante, melhor, defensor)) {
				melhor = g;
			}
		}
		if (golpes.size() > 1 && sorte.nextDouble() < CHANCE_ALEATORIA) {
			return golpes.get(sorte.nextInt(golpes.size()));
		}
		return melhor;
	}

	/** Quem age primeiro: maior prioridade do golpe, depois maior velocidade, empate no sorteio. */
	private boolean jogadorPrimeiro(Golpe gJogador, Golpe gOponente) {
		if (gJogador.getPrioridade() != gOponente.getPrioridade()) {
			return gJogador.getPrioridade() > gOponente.getPrioridade();
		}
		int vj = dados.atributos(jogador.getNumeroDex()).velocidadeNaBatalha();
		int vo = dados.atributos(oponente.getNumeroDex()).velocidadeNaBatalha();
		return vj != vo ? vj > vo : sorte.nextBoolean();
	}

	/** Joga um turno completo e devolve as linhas do relato. */
	public List<String> turno(Golpe golpeJogador) {
		List<String> relato = new ArrayList<>();
		if (terminou()) {
			return relato;
		}
		numeroTurno++;
		Golpe golpeOponente = escolherGolpeIA(oponente, jogador);
		boolean jogadorFirst = jogadorPrimeiro(golpeJogador, golpeOponente);

		if (jogadorFirst) {
			agir(true, golpeJogador, relato);
			if (!terminou()) {
				agir(false, golpeOponente, relato);
			}
		} else {
			agir(false, golpeOponente, relato);
			if (!terminou()) {
				agir(true, golpeJogador, relato);
			}
		}
		if (terminou()) {
			relato.add(vencedor().getNome() + " venceu a batalha!");
		}
		return relato;
	}

	private void agir(boolean doJogador, Golpe g, List<String> relato) {
		Pokemon atq = doJogador ? jogador : oponente;
		Pokemon def = doJogador ? oponente : jogador;
		relato.add(atq.getNome() + " usou " + g.getNome() + "!");
		DadosBatalha.Resultado r = dados.atacar(atq, g, def, sorte);
		if (!r.acertou) {
			relato.add("Mas errou!");
			return;
		}
		if (r.tipo == 0) {
			relato.add("Não afeta " + def.getNome() + "...");
			return;
		}
		if (r.critico) {
			relato.add("Acerto crítico!");
		}
		if (r.tipo > 1) {
			relato.add("É super efetivo!");
		} else if (r.tipo < 1) {
			relato.add("Não é muito efetivo...");
		}
		if (doJogador) {
			hpOponente = Math.max(0, hpOponente - r.dano);
		} else {
			hpJogador = Math.max(0, hpJogador - r.dano);
		}
		relato.add(def.getNome() + " perdeu " + r.dano + " de HP.");
	}
}
