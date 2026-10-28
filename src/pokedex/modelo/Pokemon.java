package pokedex.modelo;

public class Pokemon {
	private int numeroDex;
	private String nome;
	private String regiao;
	private String tipo1;
	private String tipo2;
	private Double altura;
	private Double peso;
	private String descricao;
	private String curiosidade;

	public Pokemon(int numeroDex, String nome, String regiao, String tipo1, String tipo2, Double altura, Double peso, String descricao, String curiosidade) {
		this.numeroDex = numeroDex;
		this.nome = nome;
		this.regiao = regiao;
		this.tipo1 = tipo1;
		this.tipo2 = tipo2;
		this.altura = altura;
		this.peso = peso;
		this.descricao = descricao;
		this.curiosidade = curiosidade;
	}

	public int getNumeroDex() {
		return numeroDex;
	}

	public void setNumeroDex(int numeroDex) {
		this.numeroDex = numeroDex;
	}

	public String getNome() {
		return nome;
	}

	public String getRegiao() {
		return regiao;
	}

	public String getTipo1() {
		return tipo1;
	}

	public String getTipo2() {
		return tipo2;
	}

	public Double getAltura() {
		return altura;
	}

	public Double getPeso() {
		return peso;
	}

	public String getDescricao() {
		return descricao;
	}

	public String getCuriosidade() {
		return curiosidade;
	}
}
