package pokedex.batalha;

/** Condição que um Pokémon carrega entre os turnos (e entre as trocas de Pokémon). */
public enum Status {
	NENHUM("", ""),
	QUEIMADO("QMD", "queimado"),
	PARALISADO("PAR", "paralisado"),
	ENVENENADO("PSN", "envenenado"),
	DORMINDO("SLP", "dormindo"),
	CONGELADO("CGL", "congelado");

	private final String sigla;
	private final String nome;

	Status(String sigla, String nome) {
		this.sigla = sigla;
		this.nome = nome;
	}

	public String getSigla() {
		return sigla;
	}

	public String getNome() {
		return nome;
	}
}
