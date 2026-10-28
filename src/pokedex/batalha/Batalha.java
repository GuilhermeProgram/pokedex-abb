package pokedex.batalha;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pokedex.modelo.Pokemon;

/**
 * Lógica de uma batalha 1 contra 1 por turnos. Não tem tela: a interface chama {@link #turno(Golpe)}
 * com o golpe escolhido pelo jogador e recebe de volta o texto do que aconteceu.
 *
 * Além do dano, a batalha trata: condições (queimar, paralisar, envenenar, dormir, congelar), mudança de atributos
 * (de -6 a +6 níveis), cura, golpes que drenam ou causam recuo e os PP de cada golpe.
 */
public class Batalha {

	/** Chance de a IA ignorar o melhor golpe e escolher outro. */
	private static final double CHANCE_ALEATORIA = 0.2;

	/** Multiplica o HP para a luta durar alguns turnos (no nível 50 puro, um golpe forte já derruba). */
	private static final int FATOR_HP = 3;

	/** Chance de o oponente Mega Evoluir (se tiver Mega). */
	private static final double CHANCE_MEGA_OPONENTE = 0.7;

	private final DadosBatalha dados;
	private final Random sorte;
	private Pokemon jogador, oponente;
	private final EstadoPokemon estadoJogador, estadoOponente;
	private int hpJogador, hpOponente;
	private final int hpMaxJogador, hpMaxOponente;
	/** Níveis de atributo de cada lado (ATQ, DEF, ATE, DFE, VEL), de -6 a +6. Voltam a zero quando o Pokémon sai. */
	private final int[] nivelJogador = new int[5], nivelOponente = new int[5];
	private int numeroTurno = 0;
	private boolean jogadorJaEvoluiu = false, oponenteJaEvoluiu = false;
	/** Mega que o oponente vai usar no primeiro turno (null = não evolui nesta luta). */
	private final Pokemon megaPlanejadaOponente;

	public Batalha(DadosBatalha dados, Pokemon jogador, Pokemon oponente, Random sorte) {
		this(dados, jogador, 0, null, oponente, 0, null, sorte);
	}

	/**
	 * Batalha em que um dos lados já chega ferido (usada no modo em equipe, quando o outro lado
	 * perdeu um Pokémon e entra o próximo). HP menor ou igual a zero significa "vida cheia". Os estados
	 * (condição e PP) vêm de fora para continuarem valendo quando o Pokémon sai e volta; null cria um novo.
	 */
	public Batalha(DadosBatalha dados, Pokemon jogador, int hpJogador, EstadoPokemon estadoJogador,
			Pokemon oponente, int hpOponente, EstadoPokemon estadoOponente, Random sorte) {
		this.dados = dados;
		this.sorte = sorte;
		this.jogador = jogador;
		this.oponente = oponente;
		this.estadoJogador = estadoJogador != null ? estadoJogador : new EstadoPokemon(dados.golpesDe(jogador.getNumeroDex()));
		this.estadoOponente = estadoOponente != null ? estadoOponente : new EstadoPokemon(dados.golpesDe(oponente.getNumeroDex()));
		this.hpMaxJogador = dados.atributos(jogador.getNumeroDex()).hpNaBatalha() * FATOR_HP;
		this.hpMaxOponente = dados.atributos(oponente.getNumeroDex()).hpNaBatalha() * FATOR_HP;
		this.hpJogador = hpJogador > 0 ? Math.min(hpJogador, hpMaxJogador) : hpMaxJogador;
		this.hpOponente = hpOponente > 0 ? Math.min(hpOponente, hpMaxOponente) : hpMaxOponente;
		List<Pokemon> opcoes = dados.megasDe(oponente.getNumeroDex());
		this.megaPlanejadaOponente = (!opcoes.isEmpty() && sorte.nextDouble() < CHANCE_MEGA_OPONENTE)
				? opcoes.get(sorte.nextInt(opcoes.size())) : null;
	}

	/** HP máximo de um Pokémon nesta batalha (usado também pela tela para mostrar o time). */
	public static int hpMaximo(DadosBatalha dados, Pokemon p) {
		return dados.atributos(p.getNumeroDex()).hpNaBatalha() * FATOR_HP;
	}

	/** Megas que o jogador ainda pode usar (vazio se não tem ou se já evoluiu nesta luta). */
	public List<Pokemon> megasDisponiveisDoJogador() {
		return jogadorJaEvoluiu ? new ArrayList<>() : dados.megasDe(jogador.getNumeroDex());
	}

	public Pokemon getJogador() {
		return jogador;
	}

	public Pokemon getOponente() {
		return oponente;
	}

	public EstadoPokemon getEstadoJogador() {
		return estadoJogador;
	}

	public EstadoPokemon getEstadoOponente() {
		return estadoOponente;
	}

	/** Nível atual de um atributo (0 = ATQ, 1 = DEF, 2 = ATE, 3 = DFE, 4 = VEL) do jogador (ou do oponente). */
	public int nivelDeAtributo(boolean doJogador, int atributo) {
		return (doJogador ? nivelJogador : nivelOponente)[atributo];
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

	// =====================================================================
	// Cálculos auxiliares
	// =====================================================================

	/** Multiplicador de um nível de atributo: +1 = x1,5; +2 = x2; -1 = x0,67; -2 = x0,5... */
	public static double fatorDoNivel(int nivel) {
		return nivel >= 0 ? (2.0 + nivel) / 2.0 : 2.0 / (2.0 - nivel);
	}

	private double mAtaque(boolean doJogador, Golpe g) {
		int[] n = doJogador ? nivelJogador : nivelOponente;
		Status st = (doJogador ? estadoJogador : estadoOponente).getStatus();
		double m = fatorDoNivel(n[g.isFisico() ? 0 : 2]);
		if (st == Status.QUEIMADO && g.isFisico()) {
			m *= 0.5;
		}
		return m;
	}

	private double mDefesa(boolean doJogador, Golpe g) {
		int[] n = doJogador ? nivelJogador : nivelOponente;
		return fatorDoNivel(n[g.isFisico() ? 1 : 3]);
	}

	private int velocidade(boolean doJogador) {
		Pokemon p = doJogador ? jogador : oponente;
		int v = dados.atributos(p.getNumeroDex()).velocidadeNaBatalha();
		double m = fatorDoNivel((doJogador ? nivelJogador : nivelOponente)[4]);
		if ((doJogador ? estadoJogador : estadoOponente).getStatus() == Status.PARALISADO) {
			m *= 0.5;
		}
		return (int) (v * m);
	}

	/** Esse Pokémon não pode receber essa condição (tipo imune ou já tem uma condição)? */
	private boolean imune(Status condicao, Pokemon alvo, EstadoPokemon estadoAlvo, Golpe g) {
		if (estadoAlvo.getStatus() != Status.NENHUM) {
			return true;
		}
		String t1 = alvo.getTipo1(), t2 = alvo.getTipo2();
		switch (condicao) {
		case QUEIMADO:
			return "Fogo".equals(t1) || "Fogo".equals(t2);
		case PARALISADO:
			return "Elétrico".equals(t1) || "Elétrico".equals(t2) || ("Elétrico".equals(g.getTipo()) && ("Terra".equals(t1) || "Terra".equals(t2)));
		case ENVENENADO:
			return "Veneno".equals(t1) || "Veneno".equals(t2) || "Aço".equals(t1) || "Aço".equals(t2);
		case CONGELADO:
			return "Gelo".equals(t1) || "Gelo".equals(t2);
		default:
			return false;
		}
	}

	/** Golpes que o lado ainda pode usar (com PP). Se acabaram todos, só o Esforço. */
	public List<Golpe> golpesUsaveis(boolean doJogador) {
		Pokemon p = doJogador ? jogador : oponente;
		EstadoPokemon est = doJogador ? estadoJogador : estadoOponente;
		List<Golpe> lista = new ArrayList<>();
		for (Golpe g : dados.golpesDe(p.getNumeroDex())) {
			if (est.temPp(g)) {
				lista.add(g);
			}
		}
		if (lista.isEmpty() && dados.golpeEsforco() != null) {
			lista.add(dados.golpeEsforco());
		}
		return lista;
	}

	// =====================================================================
	// Inteligência do oponente
	// =====================================================================

	/** Escolhe o golpe do lado indicado (o oponente usa isso a cada turno; o console, para os dois). */
	public Golpe escolherGolpeAuto(boolean doJogador) {
		Pokemon atq = doJogador ? jogador : oponente;
		Pokemon def = doJogador ? oponente : jogador;
		int hpMeu = doJogador ? hpJogador : hpOponente, hpMax = doJogador ? hpMaxJogador : hpMaxOponente;
		int hpAlvo = doJogador ? hpOponente : hpJogador;
		EstadoPokemon estAlvo = doJogador ? estadoOponente : estadoJogador;
		int[] meusNiveis = doJogador ? nivelJogador : nivelOponente, niveisAlvo = doJogador ? nivelOponente : nivelJogador;
		List<Golpe> usaveis = golpesUsaveis(doJogador);

		double melhorDano = 1;
		for (Golpe g : usaveis) {
			melhorDano = Math.max(melhorDano, dados.danoEsperado(atq, g, def, mAtaque(doJogador, g), mDefesa(!doJogador, g)));
		}
		double fracao = (double) hpMeu / hpMax;
		boolean maisLento = velocidade(doJogador) < velocidade(!doJogador);

		List<Golpe> candidatos = new ArrayList<>();
		List<Double> notas = new ArrayList<>();
		for (Golpe g : usaveis) {
			double nota = 0;
			if (!g.isStatus()) {
				double dano = dados.danoEsperado(atq, g, def, mAtaque(doJogador, g), mDefesa(!doJogador, g));
				nota = dano >= hpAlvo ? dano * 1.5 : dano;
				if (g.isRecuo()) {
					nota *= 0.9;
				}
			} else {
				double prec = g.getPrecisao() / 100.0;
				if (g.isCura()) {
					nota = fracao < 0.45 ? Math.min(0.5 * hpMax, hpMax - hpMeu) * 1.1 : 0;
				} else if (g.efeitoNoProprio() && !g.getMudancas().isEmpty()) {
					boolean util = false;
					for (Golpe.MudancaDeAtributo m : g.getMudancas()) {
						if (m.niveis > 0 && meusNiveis[m.atributo] < 2 && fracao > 0.5) {
							util = true;
						}
					}
					nota = util ? melhorDano * 0.6 : 0;
				} else if (g.getCondicao() != Status.NENHUM) {
					Pokemon alvo = def;
					boolean ok = !imune(g.getCondicao(), alvo, estAlvo, g);
					double valor = g.getCondicao() == Status.DORMINDO ? 0.8 : g.getCondicao() == Status.PARALISADO && maisLento ? 0.7 : 0.55;
					nota = ok ? melhorDano * valor * prec : 0;
				} else if (!g.getMudancas().isEmpty()) {
					boolean util = false;
					for (Golpe.MudancaDeAtributo m : g.getMudancas()) {
						if (m.niveis < 0 && niveisAlvo[m.atributo] > -2) {
							util = true;
						}
					}
					nota = util ? melhorDano * 0.4 * prec : 0;
				}
			}
			candidatos.add(g);
			notas.add(nota);
		}
		Golpe melhor = candidatos.get(0);
		double notaMelhor = notas.get(0);
		for (int i = 1; i < candidatos.size(); i++) {
			if (notas.get(i) > notaMelhor) {
				notaMelhor = notas.get(i);
				melhor = candidatos.get(i);
			}
		}
		if (candidatos.size() > 1 && sorte.nextDouble() < CHANCE_ALEATORIA) {
			List<Golpe> validos = new ArrayList<>();
			for (int i = 0; i < candidatos.size(); i++) {
				if (notas.get(i) > 0) {
					validos.add(candidatos.get(i));
				}
			}
			if (!validos.isEmpty()) {
				return validos.get(sorte.nextInt(validos.size()));
			}
		}
		return melhor;
	}

	/** Mantido por compatibilidade: golpe automático para o lado cujo Pokémon é "atacante". */
	public Golpe escolherGolpeIA(Pokemon atacante, Pokemon defensor) {
		return escolherGolpeAuto(atacante == jogador || atacante.getNumeroDex() == jogador.getNumeroDex());
	}

	// =====================================================================
	// Turno
	// =====================================================================

	/** Quem age primeiro: maior prioridade do golpe, depois maior velocidade (com paralisia), empate no sorteio. */
	private boolean jogadorPrimeiro(Golpe gJogador, Golpe gOponente) {
		if (gJogador.getPrioridade() != gOponente.getPrioridade()) {
			return gJogador.getPrioridade() > gOponente.getPrioridade();
		}
		int vj = velocidade(true), vo = velocidade(false);
		return vj != vo ? vj > vo : sorte.nextBoolean();
	}

	public List<String> turno(Golpe golpeJogador) {
		return turno(golpeJogador, null);
	}

	/**
	 * Joga um turno completo e devolve as linhas do relato. Com golpeJogador null, o jogador não age (troca de
	 * Pokémon) e só o oponente ataca. Se megaJogador não for null, o seu Pokémon
	 * Mega Evolui antes de agir (uma vez por luta, como nos jogos); o oponente faz o mesmo no 1º turno.
	 */
	public List<String> turno(Golpe golpeJogador, Pokemon megaJogador) {
		List<String> relato = new ArrayList<>();
		if (terminou()) {
			return relato;
		}
		numeroTurno++;
		if (megaJogador != null && !jogadorJaEvoluiu && megasDisponiveisDoJogador().contains(megaJogador)) {
			relato.add(jogador.getNome() + " Mega Evoluiu para " + megaJogador.getNome() + "!");
			jogador = megaJogador;
			jogadorJaEvoluiu = true;
		}
		if (numeroTurno == 1 && megaPlanejadaOponente != null && !oponenteJaEvoluiu) {
			relato.add(oponente.getNome() + " Mega Evoluiu para " + megaPlanejadaOponente.getNome() + "!");
			oponente = megaPlanejadaOponente;
			oponenteJaEvoluiu = true;
		}
		Golpe golpeOponente = escolherGolpeAuto(false);
		if (golpeJogador == null) { // o jogador trocou de Pokémon neste turno: só o oponente age
			agir(false, golpeOponente, relato);
		} else {
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
		}
		fimDeTurno(relato);
		if (terminou()) {
			relato.add(vencedor().getNome() + " venceu a batalha!");
		}
		return relato;
	}

	private void perderHp(boolean doJogador, int dano, List<String> relato) {
		Pokemon p = doJogador ? jogador : oponente;
		int atual = doJogador ? hpJogador : hpOponente;
		int perdido = Math.min(atual, Math.max(0, dano));
		if (doJogador) {
			hpJogador -= perdido;
		} else {
			hpOponente -= perdido;
		}
		relato.add(p.getNome() + " perdeu " + perdido + " de HP.");
	}

	private void ganharHp(boolean doJogador, int cura, List<String> relato) {
		Pokemon p = doJogador ? jogador : oponente;
		int max = doJogador ? hpMaxJogador : hpMaxOponente;
		int atual = doJogador ? hpJogador : hpOponente;
		int ganho = Math.min(max - atual, Math.max(0, cura));
		if (ganho <= 0) {
			relato.add("O HP de " + p.getNome() + " já está no máximo!");
			return;
		}
		if (doJogador) {
			hpJogador += ganho;
		} else {
			hpOponente += ganho;
		}
		relato.add(p.getNome() + " recuperou " + ganho + " de HP.");
	}

	/** Efeitos do fim do turno: queimadura e veneno tiram HP. */
	private void fimDeTurno(List<String> relato) {
		for (int i = 0; i < 2 && !terminou(); i++) {
			boolean doJogador = i == 0;
			Pokemon p = doJogador ? jogador : oponente;
			EstadoPokemon est = doJogador ? estadoJogador : estadoOponente;
			int max = doJogador ? hpMaxJogador : hpMaxOponente;
			if (est.getStatus() == Status.QUEIMADO) {
				relato.add(p.getNome() + " sofreu com a queimadura!");
				perderHp(doJogador, Math.max(1, max / 16), relato);
			} else if (est.getStatus() == Status.ENVENENADO) {
				relato.add(p.getNome() + " sofreu com o veneno!");
				perderHp(doJogador, Math.max(1, max / 8), relato);
			}
		}
	}

	/** O Pokémon consegue agir neste turno? (dormindo, congelado e paralisado podem impedir.) */
	private boolean podeAgir(boolean doJogador, List<String> relato) {
		Pokemon p = doJogador ? jogador : oponente;
		EstadoPokemon est = doJogador ? estadoJogador : estadoOponente;
		switch (est.getStatus()) {
		case DORMINDO:
			est.setSono(est.getSono() - 1);
			if (est.getSono() <= 0) {
				est.setStatus(Status.NENHUM);
				relato.add(p.getNome() + " acordou!");
				return true;
			}
			relato.add(p.getNome() + " está dormindo...");
			return false;
		case CONGELADO:
			if (sorte.nextDouble() < 0.2) {
				est.setStatus(Status.NENHUM);
				relato.add(p.getNome() + " descongelou!");
				return true;
			}
			relato.add(p.getNome() + " está congelado e não consegue se mover!");
			return false;
		case PARALISADO:
			if (sorte.nextDouble() < 0.25) {
				relato.add(p.getNome() + " está paralisado! Não consegue se mover!");
				return false;
			}
			return true;
		default:
			return true;
		}
	}

	private void agir(boolean doJogador, Golpe g, List<String> relato) {
		Pokemon atq = doJogador ? jogador : oponente;
		Pokemon def = doJogador ? oponente : jogador;
		EstadoPokemon estAtq = doJogador ? estadoJogador : estadoOponente;
		EstadoPokemon estDef = doJogador ? estadoOponente : estadoJogador;
		if (!podeAgir(doJogador, relato)) {
			return;
		}
		estAtq.gastar(g);
		relato.add(atq.getNome() + " usou " + g.getNome() + "!");

		if (g.isStatus()) {
			agirStatus(doJogador, g, atq, def, estAtq, estDef, relato);
			return;
		}
		DadosBatalha.Resultado r = dados.atacar(atq, g, def, sorte, mAtaque(doJogador, g), mDefesa(!doJogador, g));
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
		int vidaAntes = doJogador ? hpOponente : hpJogador;
		perderHp(!doJogador, r.dano, relato);
		int causado = vidaAntes - (doJogador ? hpOponente : hpJogador);

		if (g.isDrena()) {
			ganharHp(doJogador, Math.max(1, causado / 2), relato);
		}
		if (g.isRecuo()) {
			relato.add(atq.getNome() + " sofreu com o recuo!");
			perderHp(doJogador, Math.max(1, causado / 3), relato);
		}
		boolean alvoVivo = (doJogador ? hpOponente : hpJogador) > 0;
		boolean euVivo = (doJogador ? hpJogador : hpOponente) > 0;
		boolean temEfeito = g.getCondicao() != Status.NENHUM || !g.getMudancas().isEmpty();
		if (temEfeito && sorte.nextInt(100) < g.getChance() && (g.efeitoNoProprio() ? euVivo : alvoVivo)) {
			aplicarEfeitos(doJogador, g, atq, def, estDef, relato);
		}
	}

	private void agirStatus(boolean doJogador, Golpe g, Pokemon atq, Pokemon def, EstadoPokemon estAtq,
			EstadoPokemon estDef, List<String> relato) {
		if (g.isCura()) {
			int max = doJogador ? hpMaxJogador : hpMaxOponente;
			ganharHp(doJogador, max / 2, relato);
			return;
		}
		if (!g.efeitoNoProprio() && sorte.nextInt(100) >= g.getPrecisao()) {
			relato.add("Mas errou!");
			return;
		}
		if (!g.efeitoNoProprio() && "Planta".equals(g.getTipo()) && ("Planta".equals(def.getTipo1()) || "Planta".equals(def.getTipo2()))) {
			relato.add("Não afeta " + def.getNome() + "...");
			return;
		}
		aplicarEfeitos(doJogador, g, atq, def, estDef, relato);
	}

	/** Aplica a condição e/ou as mudanças de atributo do golpe (em quem usou ou no adversário). */
	private void aplicarEfeitos(boolean doJogador, Golpe g, Pokemon atq, Pokemon def, EstadoPokemon estDef,
			List<String> relato) {
		boolean noMeu = g.efeitoNoProprio();
		boolean ladoJogador = noMeu ? doJogador : !doJogador;
		Pokemon alvo = noMeu ? atq : def;
		if (g.getCondicao() != Status.NENHUM) {
			EstadoPokemon est = ladoJogador ? estadoJogador : estadoOponente;
			if (imune(g.getCondicao(), alvo, est, g)) {
				if (g.isStatus()) {
					relato.add("Mas falhou!");
				}
			} else {
				est.setStatus(g.getCondicao());
				switch (g.getCondicao()) {
				case QUEIMADO:
					relato.add(alvo.getNome() + " se queimou!");
					break;
				case PARALISADO:
					relato.add(alvo.getNome() + " ficou paralisado! Pode não conseguir atacar.");
					break;
				case ENVENENADO:
					relato.add(alvo.getNome() + " foi envenenado!");
					break;
				case DORMINDO:
					est.setSono(1 + sorte.nextInt(3));
					relato.add(alvo.getNome() + " adormeceu!");
					break;
				default:
					relato.add(alvo.getNome() + " congelou!");
				}
			}
		}
		int[] niveis = ladoJogador ? nivelJogador : nivelOponente;
		for (Golpe.MudancaDeAtributo m : g.getMudancas()) {
			String nome = Golpe.NOMES_ATRIBUTOS[m.atributo];
			int novo = Math.max(-6, Math.min(6, niveis[m.atributo] + m.niveis));
			if (novo == niveis[m.atributo]) {
				relato.add("O " + nome + " de " + alvo.getNome() + " não pode " + (m.niveis > 0 ? "aumentar" : "diminuir") + " mais!");
				continue;
			}
			niveis[m.atributo] = novo;
			String forca = Math.abs(m.niveis) >= 2 ? " muito" : "";
			relato.add("O " + nome + " de " + alvo.getNome() + (m.niveis > 0 ? " aumentou" : " diminuiu") + forca + "!");
		}
	}
}
