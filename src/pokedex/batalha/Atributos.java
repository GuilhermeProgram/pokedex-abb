package pokedex.batalha;

/**
 * Atributos base de um Pokémon (dados/atributos.csv). Os valores usados na batalha são calculados
 * para o nível 50, como nas batalhas oficiais.
 */
public class Atributos {

	public static final int NIVEL = 50;

	private final int hp, ataque, defesa, ataqueEspecial, defesaEspecial, velocidade;

	public Atributos(int hp, int ataque, int defesa, int ataqueEspecial, int defesaEspecial, int velocidade) {
		this.hp = hp;
		this.ataque = ataque;
		this.defesa = defesa;
		this.ataqueEspecial = ataqueEspecial;
		this.defesaEspecial = defesaEspecial;
		this.velocidade = velocidade;
	}

	public int getHp() {
		return hp;
	}

	public int getAtaque() {
		return ataque;
	}

	public int getDefesa() {
		return defesa;
	}

	public int getAtaqueEspecial() {
		return ataqueEspecial;
	}

	public int getDefesaEspecial() {
		return defesaEspecial;
	}

	public int getVelocidade() {
		return velocidade;
	}

	/** Valor de um atributo comum no nível 50 (IV 31, sem esforço). */
	private static int no50(int base) {
		return (2 * base + 31) * NIVEL / 100 + 5;
	}

	public int hpNaBatalha() {
		return (2 * hp + 31) * NIVEL / 100 + NIVEL + 10;
	}

	public int ataqueNaBatalha() {
		return no50(ataque);
	}

	public int defesaNaBatalha() {
		return no50(defesa);
	}

	public int ataqueEspecialNaBatalha() {
		return no50(ataqueEspecial);
	}

	public int defesaEspecialNaBatalha() {
		return no50(defesaEspecial);
	}

	public int velocidadeNaBatalha() {
		return no50(velocidade);
	}
}
