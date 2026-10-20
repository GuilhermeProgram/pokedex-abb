package PokePackage;

/** Um golpe que causa dano (dados vindos de dados/golpes.csv). */
public class Golpe {

	private final int id;
	private final String nome;
	private final String tipo;
	private final boolean fisico; // true = físico (usa Ataque/Defesa), false = especial
	private final int poder;
	private final int precisao;   // 1 a 100
	private final int prioridade; // golpes com prioridade maior agem primeiro

	public Golpe(int id, String nome, String tipo, boolean fisico, int poder, int precisao, int prioridade) {
		this.id = id;
		this.nome = nome;
		this.tipo = tipo;
		this.fisico = fisico;
		this.poder = poder;
		this.precisao = precisao;
		this.prioridade = prioridade;
	}

	public int getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getTipo() {
		return tipo;
	}

	public boolean isFisico() {
		return fisico;
	}

	public int getPoder() {
		return poder;
	}

	public int getPrecisao() {
		return precisao;
	}

	public int getPrioridade() {
		return prioridade;
	}

	@Override
	public String toString() {
		return nome + " (" + tipo + ", " + (fisico ? "Físico" : "Especial") + ", poder " + poder + ")";
	}
}
