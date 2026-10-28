package pokedex.batalha;

import java.util.ArrayList;
import java.util.List;

/**
 * O que muda em um Pokémon durante a luta e precisa sobreviver às trocas: a condição (queimado, paralisado...)
 * e os PP (usos restantes) de cada golpe. Um objeto por Pokémon do time; a Mega Evolução usa o mesmo objeto.
 */
public class EstadoPokemon {

	private final List<Golpe> golpes;
	private final int[] pp;
	private Status status = Status.NENHUM;
	/** Turnos de sono que ainda faltam (só vale com Status.DORMINDO). */
	private int sono = 0;

	public EstadoPokemon(List<Golpe> golpes) {
		this.golpes = new ArrayList<>(golpes);
		this.pp = new int[this.golpes.size()];
		for (int i = 0; i < pp.length; i++) {
			pp[i] = this.golpes.get(i).getPp();
		}
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public int getSono() {
		return sono;
	}

	public void setSono(int sono) {
		this.sono = sono;
	}

	/** PP que sobram do golpe (0 se o golpe não é um dos deste Pokémon). */
	public int ppDe(Golpe g) {
		int i = indice(g);
		return i < 0 ? 0 : pp[i];
	}

	public boolean temPp(Golpe g) {
		return ppDe(g) > 0;
	}

	/** Todos os golpes sem PP: o Pokémon só consegue usar o Esforço. */
	public boolean semPpNenhum() {
		for (int v : pp) {
			if (v > 0) {
				return false;
			}
		}
		return true;
	}

	/** Gasta 1 PP (golpes que não estão na lista, como o Esforço, não gastam nada). */
	public void gastar(Golpe g) {
		int i = indice(g);
		if (i >= 0 && pp[i] > 0) {
			pp[i]--;
		}
	}

	private int indice(Golpe g) {
		for (int i = 0; i < golpes.size(); i++) {
			if (golpes.get(i).getId() == g.getId()) {
				return i;
			}
		}
		return -1;
	}
}
