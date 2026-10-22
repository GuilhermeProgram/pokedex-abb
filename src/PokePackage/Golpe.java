package PokePackage;

import java.util.ArrayList;
import java.util.List;

/**
 * Um golpe (dados vindos de dados/golpes.csv): pode causar dano (físico ou especial) ou ser só de status
 * (poder 0). Além do dano, pode ter um efeito: mudar atributos, causar uma condição, curar, drenar ou causar recuo.
 */
public class Golpe {

	/** Um aumento ou diminuição de atributo (ex.: ATQ +2). */
	public static class MudancaDeAtributo {
		public final int atributo; // 0 ataque, 1 defesa, 2 ataque especial, 3 defesa especial, 4 velocidade
		public final int niveis;   // positivo sobe, negativo desce

		MudancaDeAtributo(int atributo, int niveis) {
			this.atributo = atributo;
			this.niveis = niveis;
		}
	}

	public static final String[] NOMES_ATRIBUTOS = { "Ataque", "Defesa", "Ataque Esp.", "Defesa Esp.", "Velocidade" };
	private static final String[] CODIGOS = { "ATQ", "DEF", "ATE", "DFE", "VEL" };

	private final int id;
	private final String nome;
	private final String tipo;
	private final boolean fisico; // true = físico (usa Ataque/Defesa), false = especial
	private final boolean status; // true = não causa dano
	private final int poder;
	private final int precisao;   // 1 a 100
	private final int prioridade; // golpes com prioridade maior agem primeiro
	private final int pp;
	private final int chance;     // chance (%) do efeito acontecer
	private final boolean noProprio; // o efeito é no usuário (true) ou no adversário (false)
	private final Status condicao;   // condição que o golpe causa (NENHUM se não causa)
	private final boolean cura, drena, recuo;
	private final List<MudancaDeAtributo> mudancas = new ArrayList<>();

	public Golpe(int id, String nome, String tipo, String categoria, int poder, int precisao, int prioridade, int pp,
			String efeito, int chance, String alvo) {
		this.id = id;
		this.nome = nome;
		this.tipo = tipo;
		this.status = "Status".equals(categoria);
		this.fisico = "Físico".equals(categoria);
		this.poder = poder;
		this.precisao = precisao;
		this.prioridade = prioridade;
		this.pp = pp;
		this.chance = efeito.isEmpty() ? 0 : chance;
		this.noProprio = "eu".equals(alvo);
		Status c = Status.NENHUM;
		boolean cu = false, dr = false, re = false;
		for (String e : efeito.split(",")) {
			e = e.trim();
			switch (e) {
			case "":
				break;
			case "QUEIMAR":
				c = Status.QUEIMADO;
				break;
			case "PARALISAR":
				c = Status.PARALISADO;
				break;
			case "ENVENENAR":
				c = Status.ENVENENADO;
				break;
			case "DORMIR":
				c = Status.DORMINDO;
				break;
			case "CONGELAR":
				c = Status.CONGELADO;
				break;
			case "CURAR":
				cu = true;
				break;
			case "DRENAR":
				dr = true;
				break;
			case "RECOIL":
				re = true;
				break;
			default: // ATQ+2, VEL-1...
				mudancas.add(lerMudanca(e));
			}
		}
		this.condicao = c;
		this.cura = cu;
		this.drena = dr;
		this.recuo = re;
	}

	private static MudancaDeAtributo lerMudanca(String e) {
		String cod = e.substring(0, 3);
		for (int i = 0; i < CODIGOS.length; i++) {
			if (CODIGOS[i].equals(cod)) {
				return new MudancaDeAtributo(i, Integer.parseInt(e.substring(3).replace("+", "")));
			}
		}
		throw new IllegalArgumentException("Efeito desconhecido no golpes.csv: " + e);
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

	/** Golpe de status: não causa dano, só o efeito. */
	public boolean isStatus() {
		return status;
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

	public int getPp() {
		return pp;
	}

	public int getChance() {
		return chance;
	}

	public boolean efeitoNoProprio() {
		return noProprio;
	}

	public Status getCondicao() {
		return condicao;
	}

	public boolean isCura() {
		return cura;
	}

	public boolean isDrena() {
		return drena;
	}

	public boolean isRecuo() {
		return recuo;
	}

	public List<MudancaDeAtributo> getMudancas() {
		return mudancas;
	}

	/** Texto curto para o botão: poder ou "Status". */
	public String resumo() {
		return tipo + " · " + (status ? "Status" : String.valueOf(poder));
	}

	@Override
	public String toString() {
		return nome + " (" + tipo + ", " + (status ? "Status" : fisico ? "Físico" : "Especial") + ", poder " + poder + ")";
	}
}
