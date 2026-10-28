package pokedex.ui.tema;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import pokedex.modelo.Pokemon;

import static pokedex.ui.tema.Tema.*;

/**
 * Desenha um medalhão com um padrão próprio de cada tipo (brasas para fogo,
 * ondas para água, raios para elétrico e assim por diante). Cada Pokémon
 * recebe sempre o mesmo desenho, porque a "sorte" vem do número Dex.
 */
public class Ambiente {

	// camadas que não mudam a cada quadro ficam prontas (só o padrão é redesenhado)
	private static BufferedImage baseCache, brilhoCache;
	private static String baseChave = "", brilhoChave = "";

	/** Para Pokémon Normal/Voador, o tipo mais marcante é o segundo. */
	public static String tipoDoCenario(Pokemon p) {
		if ("normal".equals(normalizar(p.getTipo1())) && p.getTipo2() != null) {
			return p.getTipo2();
		}
		return p.getTipo1();
	}

	/** Número pseudoaleatório estável (0 a 1) a partir de uma semente. */
	public static double rnd(long semente, int i) {
		long x = semente * 0x9E3779B97F4A7C15L + (i + 1) * 0xBF58476D1CE4E5B9L;
		x ^= x >>> 30;
		x *= 0xBF58476D1CE4E5B9L;
		x ^= x >>> 27;
		x *= 0x94D049BB133111EBL;
		x ^= x >>> 31;
		return (x >>> 11) * (1.0 / (1L << 53));
	}

	public static void desenhar(Graphics2D g2, String tipo, int dex, float cx, float cy, float R, Color a, Color b,
			double fase) {
		String chave = (tipo == null) ? "normal" : normalizar(tipo);
		long semente = dex * 31L + chave.hashCode();
		Color luz = mix(a, Color.WHITE, 0.6f);
		Color sombra = escurecer(a, 0.45f);

		int lado = (int) Math.ceil(2 * R) + 4;
		float bx = lado / 2f, by = lado / 2f;
		Ellipse2D disco = new Ellipse2D.Float(bx - R, by - R, 2 * R, 2 * R);

		// camada 1: fundo em degradê (muda só quando as cores animam)
		String ck = lado + "|" + a.getRGB() + "|" + b.getRGB();
		if (baseCache == null || !ck.equals(baseChave)) {
			baseCache = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = baseCache.createGraphics();
			qualidade(g);
			g.setPaint(new GradientPaint(bx - R, by - R, mix(a, FUNDO_B, 0.30f), bx + R, by + R,
					escurecer(b, 0.42f)));
			g.fill(disco);
			g.dispose();
			baseChave = ck;
		}

		// camada 3: volume (borda escura, reflexo) e aro de luz (só depende do tamanho)
		String bk = String.valueOf(lado);
		if (brilhoCache == null || !bk.equals(brilhoChave)) {
			brilhoCache = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = brilhoCache.createGraphics();
			qualidade(g);
			g.setPaint(new RadialGradientPaint(new Point2D.Float(bx, by), R, new float[] { 0.55f, 1f },
					new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 125) }));
			g.fill(disco);
			g.setPaint(new RadialGradientPaint(new Point2D.Float(bx - R * 0.4f, by - R * 0.5f), R * 0.9f,
					new float[] { 0f, 1f }, new Color[] { new Color(255, 255, 255, 55), new Color(255, 255, 255, 0) }));
			g.fill(disco);
			g.setStroke(new BasicStroke(3f));
			g.setPaint(new GradientPaint(bx - R, by - R, new Color(255, 255, 255, 170), bx + R, by + R,
					new Color(255, 255, 255, 10)));
			g.draw(disco);
			g.dispose();
			brilhoChave = bk;
		}

		int x0 = Math.round(cx - bx), y0 = Math.round(cy - by);
		g2.drawImage(baseCache, x0, y0, null);

		// camada 2: padrão animado do tipo, recortado no círculo
		Shape clipAntes = g2.getClip();
		g2.clip(new Ellipse2D.Float(cx - R, cy - R, 2 * R, 2 * R));
		padrao(g2, chave, cx, cy, R, luz, sombra, semente, fase);
		g2.setClip(clipAntes);

		g2.drawImage(brilhoCache, x0, y0, null);
	}

	private static void padrao(Graphics2D g, String chave, float cx, float cy, float R, Color luz, Color sombra,
			long s, double fase) {
		switch (chave) {
		case "fogo":
			fogo(g, cx, cy, R, luz, s, fase);
			break;
		case "agua":
			agua(g, cx, cy, R, luz, fase);
			break;
		case "planta":
			planta(g, cx, cy, R, luz, sombra, s, fase);
			break;
		case "eletrico":
			eletrico(g, cx, cy, R, luz, s, fase);
			break;
		case "gelo":
			gelo(g, cx, cy, R, luz, s, fase);
			break;
		case "lutador":
			lutador(g, cx, cy, R, luz, s, fase);
			break;
		case "veneno":
			veneno(g, cx, cy, R, luz, s, fase);
			break;
		case "terra":
			terra(g, cx, cy, R, luz, sombra, s, fase);
			break;
		case "pedra":
			pedra(g, cx, cy, R, luz, sombra, s, fase);
			break;
		case "aco":
			aco(g, cx, cy, R, luz, fase);
			break;
		case "voador":
			voador(g, cx, cy, R, luz, s, fase);
			break;
		case "psiquico":
			psiquico(g, cx, cy, R, luz, fase);
			break;
		case "inseto":
			inseto(g, cx, cy, R, luz, s, fase);
			break;
		case "fantasma":
			fantasma(g, cx, cy, R, luz, s, fase);
			break;
		case "dragao":
			dragao(g, cx, cy, R, luz, fase);
			break;
		case "sombrio":
			sombrio(g, cx, cy, R, luz, s, fase);
			break;
		case "fada":
			fada(g, cx, cy, R, luz, s, fase);
			break;
		default:
			normal(g, cx, cy, R, luz, fase);
		}
	}

	// ---------- um desenho para cada tipo ----------

	/** Fogo: línguas de fogo na base e brasas subindo. */
	private static void fogo(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
		for (int camada = 0; camada < 3; camada++) {
			Path2D.Float p = new Path2D.Float();
			p.moveTo(cx - R, cy + R);
			for (float x = cx - R; x <= cx + R; x += 5) {
				double y = cy + R - (R * 0.16 + camada * R * 0.07)
						- R * 0.09 * Math.sin(x * 0.045 + fase * (1.2 + camada * 0.5) + camada * 2)
						- R * 0.05 * Math.sin(x * 0.12 - fase * 2.0);
				p.lineTo(x, y);
			}
			p.lineTo(cx + R, cy + R);
			p.closePath();
			g.setColor(comAlpha(luz, 40 - camada * 9));
			g.fill(p);
		}
		Color brasa = new Color(255, 214, 140);
		for (int i = 0; i < 34; i++) {
			double r1 = rnd(s, i), r2 = rnd(s, i + 100), r3 = rnd(s, i + 200);
			double ciclo = (r1 + fase * 0.045 * (0.6 + r3)) % 1.0;
			float x = (float) (cx + (r2 - 0.5) * 2 * R * 0.95 + Math.sin(fase * 0.8 + i) * 8);
			float y = (float) (cy + R - ciclo * 2 * R * 1.05);
			float tam = (float) ((2 + r3 * 5) * (1 - ciclo * 0.6));
			g.setColor(comAlpha(brasa, (int) (190 * (1 - ciclo))));
			g.fill(new Ellipse2D.Float(x - tam / 2, y - tam / 2, tam, tam));
		}
	}

	/** Água: ondas que deslizam e ondulações que se abrem. */
	private static void agua(Graphics2D g, float cx, float cy, float R, Color luz, double fase) {
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int k = 0; k < 8; k++) {
			float y0 = cy - R + (k + 1) * (2 * R / 9f);
			Path2D.Float p = new Path2D.Float();
			p.moveTo(cx - R, y0);
			for (float x = cx - R; x <= cx + R; x += 6) {
				p.lineTo(x, y0 + (float) (Math.sin(x * 0.034 + fase * 0.9 + k * 0.8) * 7));
			}
			g.setColor(comAlpha(luz, 34 + (k % 3) * 10));
			g.draw(p);
		}
		float rx = cx - R * 0.3f, ry = cy + R * 0.35f;
		g.setStroke(new BasicStroke(2.2f));
		for (int j = 0; j < 3; j++) {
			double c = (fase * 0.2 + j / 3.0) % 1.0;
			float r = (float) (c * R * 1.3);
			g.setColor(comAlpha(Color.WHITE, (int) (120 * (1 - c))));
			g.draw(new Ellipse2D.Float(rx - r, ry - r * 0.4f, 2 * r, r * 0.8f));
		}
	}

	/** Planta: folhas de capim balançando na base. */
	private static void planta(Graphics2D g, float cx, float cy, float R, Color luz, Color sombra, long s,
			double fase) {
		for (int i = 0; i < 26; i++) {
			float x = cx - R + (float) (rnd(s, i) * 2 * R);
			float alt = (float) (R * (0.25 + 0.6 * rnd(s, i + 50)));
			float larg = 7 + (float) rnd(s, i + 90) * 10;
			float balanco = (float) Math.sin(fase * 0.7 + i) * 10;
			Path2D.Float p = new Path2D.Float();
			p.moveTo(x - larg / 2, cy + R);
			p.quadTo(x - larg / 4 + balanco * 0.4f, cy + R - alt * 0.55f, x + balanco, cy + R - alt);
			p.quadTo(x + larg / 4 + balanco * 0.4f, cy + R - alt * 0.55f, x + larg / 2, cy + R);
			p.closePath();
			g.setColor(comAlpha(mix(luz, sombra, (float) rnd(s, i + 7)), 44 + (int) (rnd(s, i + 3) * 56)));
			g.fill(p);
		}
	}

	/** Elétrico: raios que piscam e trocam de forma. */
	private static void eletrico(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
		int troca = (int) (fase * 0.7);
		double pisca = 0.55 + 0.45 * Math.sin(fase * 8.0);
		for (int b = 0; b < 3; b++) {
			long sb = s + troca * 7919L + b * 104729L;
			Path2D.Float p = new Path2D.Float();
			float x = cx + (float) ((rnd(sb, 0) - 0.5) * R * 1.4);
			p.moveTo(x, cy - R);
			int seg = 8;
			for (int i = 1; i <= seg; i++) {
				x += (float) ((rnd(sb, i) - 0.5) * R * 0.5);
				p.lineTo(x, cy - R + i * (2 * R / seg));
			}
			g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(comAlpha(luz, (int) (34 * pisca)));
			g.draw(p);
			g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(comAlpha(Color.WHITE, (int) (190 * pisca)));
			g.draw(p);
		}
		for (int i = 0; i < 16; i++) {
			double tw = 0.5 + 0.5 * Math.sin(fase * 2 + i * 1.9);
			float x = cx + (float) ((rnd(s, i + 300) - 0.5) * 2 * R * 0.9);
			float y = cy + (float) ((rnd(s, i + 400) - 0.5) * 2 * R * 0.9);
			g.setColor(comAlpha(Color.WHITE, (int) (150 * tw)));
			g.fill(new Ellipse2D.Float(x - 1.8f, y - 1.8f, 3.6f, 3.6f));
		}
	}

	/** Gelo: cristais na base e neve caindo. */
	private static void gelo(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
		g.setStroke(new BasicStroke(1.3f));
		for (int i = 0; i < 10; i++) {
			float x = cx - R + (float) ((i + rnd(s, i) * 0.8) * 2 * R / 10);
			float alt = (float) (R * (0.3 + 0.6 * rnd(s, i + 40)));
			float larg = 16 + (float) rnd(s, i + 80) * 22;
			Path2D.Float esq = new Path2D.Float();
			esq.moveTo(x - larg / 2, cy + R);
			esq.lineTo(x, cy + R - alt);
			esq.lineTo(x, cy + R);
			esq.closePath();
			Path2D.Float dir = new Path2D.Float();
			dir.moveTo(x, cy + R);
			dir.lineTo(x, cy + R - alt);
			dir.lineTo(x + larg / 2, cy + R);
			dir.closePath();
			g.setColor(comAlpha(luz, 46));
			g.fill(esq);
			g.setColor(comAlpha(luz, 24));
			g.fill(dir);
			g.setColor(comAlpha(Color.WHITE, 90));
			g.draw(esq);
			g.draw(dir);
		}
		for (int i = 0; i < 30; i++) {
			double r1 = rnd(s, i + 500), r2 = rnd(s, i + 600), r3 = rnd(s, i + 700);
			double ciclo = (r1 + fase * 0.03 * (0.5 + r3)) % 1.0;
			float x = (float) (cx + (r2 - 0.5) * 2 * R + Math.sin(fase * 0.6 + i) * 6);
			float y = (float) (cy - R + ciclo * 2 * R);
			float r = (float) (1.5 + r3 * 2.5);
			g.setColor(comAlpha(Color.WHITE, 150));
			g.fill(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r));
		}
	}

	/** Lutador: linhas de velocidade saindo do centro, como em quadrinhos. */
	private static void lutador(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
		int n = 38;
		for (int i = 0; i < n; i++) {
			double ang = i * 2 * Math.PI / n + (rnd(s, i) - 0.5) * 0.07;
			double pulso = 0.9 + 0.1 * Math.sin(fase * 2 + i);
			double r0 = R * (0.3 + 0.35 * rnd(s, i + 60));
			double r1 = R * (1.15 + 0.2 * rnd(s, i + 120)) * pulso;
			g.setStroke(new BasicStroke(1.5f + (float) (3.5 * rnd(s, i + 200)), BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND));
			g.setColor(comAlpha(luz, 30 + (int) (45 * rnd(s, i + 260))));
			g.drawLine((int) (cx + Math.cos(ang) * r0), (int) (cy + Math.sin(ang) * r0),
					(int) (cx + Math.cos(ang) * r1), (int) (cy + Math.sin(ang) * r1));
		}
	}

	/** Veneno: bolhas subindo. */
	private static void veneno(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
		for (int i = 0; i < 22; i++) {
			double r1 = rnd(s, i), r2 = rnd(s, i + 80), r3 = rnd(s, i + 160);
			float r = (float) (5 + 20 * r1 * r1);
			double ciclo = (r2 + fase * 0.035 * (0.5 + r3)) % 1.0;
			float x = (float) (cx + (r3 - 0.5) * 2 * R + Math.sin(fase + i) * 6);
			float y = (float) (cy + R + r - ciclo * (2 * R + 2 * r));
			g.setColor(comAlpha(luz, 26));
			g.fill(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r));
			g.setStroke(new BasicStroke(1.6f));
			g.setColor(comAlpha(luz, 110));
			g.draw(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r));
			g.setColor(comAlpha(Color.WHITE, 140));
			g.fill(new Ellipse2D.Float(x - r * 0.55f, y - r * 0.6f, r * 0.35f, r * 0.35f));
		}
	}

	/** Terra: camadas de solo. */
	private static void terra(Graphics2D g, float cx, float cy, float R, Color luz, Color sombra, long s,
			double fase) {
		for (int k = 0; k < 6; k++) {
			float y0 = cy - R * 0.25f + k * R * 0.27f;
			Path2D.Float p = new Path2D.Float();
			p.moveTo(cx - R, cy + R);
			for (float x = cx - R; x <= cx + R; x += 6) {
				p.lineTo(x, y0 + (float) (Math.sin(x * 0.02 + k * 1.3) * 10 + Math.sin(x * 0.05 + k) * 4));
			}
			p.lineTo(cx + R, cy + R);
			p.closePath();
			g.setColor(comAlpha(k % 2 == 0 ? luz : sombra, 34));
			g.fill(p);
		}
		for (int i = 0; i < 16; i++) {
			float x = cx + (float) ((rnd(s, i) - 0.5) * 2 * R);
			float y = cy + R * 0.1f + (float) (rnd(s, i + 50) * R * 0.85);
			float r = 3 + (float) rnd(s, i + 99) * 6;
			g.setColor(comAlpha(luz, 60));
			g.fill(new Ellipse2D.Float(x - r, y - r * 0.7f, 2 * r, 1.4f * r));
		}
	}

	/** Pedra: facetas, como uma rocha lapidada. */
	private static void pedra(Graphics2D g, float cx, float cy, float R, Color luz, Color sombra, long s,
			double fase) {
		int n = 7;
		float x0 = cx - R * 1.1f, y0 = cy - R * 1.1f, passo = 2.2f * R / n;
		float[][] px = new float[n + 1][n + 1], py = new float[n + 1][n + 1];
		for (int i = 0; i <= n; i++) {
			for (int j = 0; j <= n; j++) {
				px[i][j] = x0 + i * passo + (float) ((rnd(s, i * 20 + j) - 0.5) * passo * 0.7);
				py[i][j] = y0 + j * passo + (float) ((rnd(s, i * 20 + j + 500) - 0.5) * passo * 0.7);
			}
		}
		g.setStroke(new BasicStroke(1f));
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				int c = i * n + j;
				Path2D.Float t1 = new Path2D.Float();
				t1.moveTo(px[i][j], py[i][j]);
				t1.lineTo(px[i + 1][j], py[i + 1][j]);
				t1.lineTo(px[i + 1][j + 1], py[i + 1][j + 1]);
				t1.closePath();
				Path2D.Float t2 = new Path2D.Float();
				t2.moveTo(px[i][j], py[i][j]);
				t2.lineTo(px[i + 1][j + 1], py[i + 1][j + 1]);
				t2.lineTo(px[i][j + 1], py[i][j + 1]);
				t2.closePath();
				float b1 = (float) rnd(s, c * 2) * 0.8f + (float) (Math.sin(fase * 0.5 + c) * 0.06);
				float b2 = (float) rnd(s, c * 2 + 1) * 0.8f + (float) (Math.sin(fase * 0.5 + c + 2) * 0.06);
				g.setColor(comAlpha(mix(sombra, luz, b1), 78));
				g.fill(t1);
				g.setColor(comAlpha(mix(sombra, luz, b2), 78));
				g.fill(t2);
				g.setColor(comAlpha(Color.WHITE, 26));
				g.draw(t1);
				g.draw(t2);
			}
		}
	}

	/** Aço: engrenagem com marcas de régua girando devagar. */
	private static void aco(Graphics2D g, float cx, float cy, float R, Color luz, double fase) {
		Graphics2D h = (Graphics2D) g.create();
		h.rotate(fase * 0.08, cx, cy);
		h.setStroke(new BasicStroke(2f));
		float[] aneis = { 0.38f, 0.62f, 0.86f };
		for (float k : aneis) {
			h.setColor(comAlpha(luz, 70));
			h.draw(new Ellipse2D.Float(cx - R * k, cy - R * k, 2 * R * k, 2 * R * k));
		}
		for (int i = 0; i < 60; i++) {
			double ang = i * 2 * Math.PI / 60;
			float r1 = R * 0.62f, r2 = r1 + (i % 5 == 0 ? 16 : 8);
			h.setColor(comAlpha(luz, i % 5 == 0 ? 120 : 70));
			h.drawLine((int) (cx + Math.cos(ang) * r1), (int) (cy + Math.sin(ang) * r1),
					(int) (cx + Math.cos(ang) * r2), (int) (cy + Math.sin(ang) * r2));
		}
		for (int i = 0; i < 8; i++) {
			double ang = i * Math.PI / 4;
			float x = (float) (cx + Math.cos(ang) * R * 0.95f), y = (float) (cy + Math.sin(ang) * R * 0.95f);
			h.setColor(comAlpha(luz, 100));
			h.fill(new Ellipse2D.Float(x - 4, y - 4, 8, 8));
		}
		h.dispose();
	}

	/** Voador: rajadas de vento e nuvens passando. */
	private static void voador(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
		for (int i = 0; i < 3; i++) {
			double ciclo = (rnd(s, i) + fase * 0.012 * (1 + i * 0.4)) % 1.0;
			float x = (float) (cx - R * 1.4 + ciclo * R * 2.8);
			float y = cy - R * 0.4f + i * R * 0.45f;
			g.setColor(comAlpha(luz, 26));
			for (int k = 0; k < 4; k++) {
				float r = R * (0.16f + 0.08f * (k % 2));
				g.fill(new Ellipse2D.Float(x + k * R * 0.14f - r, y - r * 0.6f - (k % 2) * 6, 2 * r, 1.3f * r));
			}
		}
		for (int i = 0; i < 14; i++) {
			float y = cy - R + (float) ((0.08 + 0.84 * rnd(s, i + 30)) * 2 * R);
			float comp = (float) (R * (0.5 + rnd(s, i + 70)));
			double vel = 0.02 + 0.03 * rnd(s, i + 110);
			float x = (float) (cx - R + ((rnd(s, i + 150) + fase * vel) % 1.0) * (2 * R + comp) - comp);
			g.setStroke(new BasicStroke(2f + (float) (3 * rnd(s, i + 190)), BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND));
			g.setColor(comAlpha(Color.WHITE, 38 + (int) (60 * rnd(s, i + 230))));
			Path2D.Float p = new Path2D.Float();
			p.moveTo(x, y);
			p.quadTo(x + comp / 2, y - 8, x + comp, y);
			g.draw(p);
		}
	}

	/** Psíquico: ondas se expandindo e pontos em órbita. */
	private static void psiquico(Graphics2D g, float cx, float cy, float R, Color luz, double fase) {
		g.setStroke(new BasicStroke(2.2f));
		for (int j = 0; j < 4; j++) {
			double c = (fase * 0.12 + j / 4.0) % 1.0;
			float r = (float) (c * R * 1.2);
			g.setColor(comAlpha(luz, (int) (120 * (1 - c))));
			g.draw(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));
		}
		g.setStroke(new BasicStroke(1.2f));
		g.setColor(comAlpha(luz, 40));
		g.draw(new Ellipse2D.Float(cx - R * 0.72f, cy - R * 0.28f, R * 1.44f, R * 0.56f));
		for (int k = 0; k < 3; k++) {
			double ang = fase * 0.5 + k * 2 * Math.PI / 3;
			float x = (float) (cx + Math.cos(ang) * R * 0.72f), y = (float) (cy + Math.sin(ang) * R * 0.28f);
			g.setColor(comAlpha(Color.WHITE, 210));
			g.fill(new Ellipse2D.Float(x - 4.5f, y - 4.5f, 9, 9));
			g.setColor(comAlpha(luz, 60));
			g.fill(new Ellipse2D.Float(x - 11, y - 11, 22, 22));
		}
	}

	/** Inseto: favo de mel com células que acendem. */
	private static void inseto(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
		float hs = R * 0.2f;
		g.setStroke(new BasicStroke(1.4f));
		for (int row = -1; row < 12; row++) {
			for (int col = -1; col < 8; col++) {
				float x = cx - R + col * hs * 1.732f + (row % 2 == 0 ? 0 : hs * 0.866f);
				float y = cy - R + row * hs * 1.5f;
				Path2D.Float hex = new Path2D.Float();
				for (int k = 0; k < 6; k++) {
					double ang = Math.PI / 6 + k * Math.PI / 3;
					float hx = x + (float) Math.cos(ang) * hs * 0.93f, hy = y + (float) Math.sin(ang) * hs * 0.93f;
					if (k == 0) {
						hex.moveTo(hx, hy);
					} else {
						hex.lineTo(hx, hy);
					}
				}
				hex.closePath();
				double aceso = rnd(s, (row + 2) * 20 + col + 2);
				if (aceso > 0.62) {
					double pulso = 0.5 + 0.5 * Math.sin(fase * 0.9 + aceso * 20);
					g.setColor(comAlpha(luz, (int) (20 + 50 * pulso)));
					g.fill(hex);
				}
				g.setColor(comAlpha(luz, 46));
				g.draw(hex);
			}
		}
	}

	/** Fantasma: fios de névoa subindo e orbes à deriva. */
	private static void fantasma(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
		for (int i = 0; i < 5; i++) {
			float x0 = cx - R * 0.8f + i * R * 0.4f;
			Path2D.Float p = new Path2D.Float();
			p.moveTo(x0, cy + R);
			for (float y = cy + R; y >= cy - R; y -= 10) {
				p.lineTo(x0 + (float) (Math.sin(y * 0.025 + fase * 0.7 + i * 1.7) * (18 + (i % 2) * 10)), y);
			}
			g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(comAlpha(luz, 20));
			g.draw(p);
			g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(comAlpha(luz, 62));
			g.draw(p);
		}
		for (int i = 0; i < 9; i++) {
			float x = cx + (float) ((rnd(s, i) - 0.5) * 2 * R * 0.85);
			float y = cy + (float) ((rnd(s, i + 40) - 0.5) * 2 * R * 0.85 + Math.sin(fase * 0.8 + i) * 8);
			float r = 4 + (float) rnd(s, i + 80) * 8;
			g.setColor(comAlpha(luz, 34));
			g.fill(new Ellipse2D.Float(x - r * 2, y - r * 2, 4 * r, 4 * r));
			g.setColor(comAlpha(Color.WHITE, 120));
			g.fill(new Ellipse2D.Float(x - r / 2, y - r / 2, r, r));
		}
	}

	/** Dragão: escamas com um brilho que atravessa. */
	private static void dragao(Graphics2D g, float cx, float cy, float R, Color luz, double fase) {
		float sc = R * 0.17f;
		g.setStroke(new BasicStroke(1.6f));
		g.setColor(comAlpha(luz, 62));
		for (int row = -1; row < 16; row++) {
			for (int col = -1; col < 14; col++) {
				float x = cx - R + col * sc + (row % 2 == 0 ? 0 : sc / 2);
				float y = cy - R + row * sc * 0.5f;
				g.draw(new java.awt.geom.Arc2D.Float(x - sc / 2, y - sc / 2, sc, sc, 180, 180,
						java.awt.geom.Arc2D.OPEN));
			}
		}
		float pos = (float) (cx - R * 1.5 + ((fase * 0.06) % 1.0) * 3 * R);
		g.setPaint(new LinearGradientPaint(new Point2D.Float(pos - R * 0.4f, cy - R * 0.4f),
				new Point2D.Float(pos + R * 0.4f, cy + R * 0.4f), new float[] { 0f, 0.5f, 1f },
				new Color[] { new Color(255, 255, 255, 0), new Color(255, 255, 255, 60),
						new Color(255, 255, 255, 0) }));
		g.fill(new Rectangle2D.Float(cx - R, cy - R, 2 * R, 2 * R));
	}

	/** Sombrio: lua minguante, estrelas e anéis de sombra. */
	private static void sombrio(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
		g.setStroke(new BasicStroke(10f));
		g.setColor(new Color(0, 0, 0, 36));
		for (int k = 0; k < 3; k++) {
			float r = R * (0.45f + k * 0.3f);
			g.draw(new Ellipse2D.Float(cx - r + k * 10, cy - r, 2 * r, 2 * r * 0.9f));
		}
		for (int i = 0; i < 26; i++) {
			double tw = 0.5 + 0.5 * Math.sin(fase * 1.1 + i * 2.3);
			float x = cx + (float) ((rnd(s, i) - 0.5) * 2 * R * 0.95);
			float y = cy + (float) ((rnd(s, i + 60) - 0.5) * 2 * R * 0.95);
			float r = 1f + (float) rnd(s, i + 120) * 2f;
			g.setColor(comAlpha(Color.WHITE, (int) (60 + 150 * tw)));
			g.fill(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r));
		}
		Area lua = new Area(new Ellipse2D.Float(cx + R * 0.45f - R * 0.22f, cy - R * 0.5f - R * 0.22f, R * 0.44f,
				R * 0.44f));
		lua.subtract(new Area(new Ellipse2D.Float(cx + R * 0.55f - R * 0.19f, cy - R * 0.54f - R * 0.19f,
				R * 0.38f, R * 0.38f)));
		g.setColor(comAlpha(luz, 190));
		g.fill(lua);
	}

	/** Fada: brilhos de quatro pontas e luzes desfocadas. */
	private static void fada(Graphics2D g, float cx, float cy, float R, Color luz, long s, double fase) {
		for (int i = 0; i < 7; i++) {
			float x = cx + (float) ((rnd(s, i + 200) - 0.5) * 2 * R * 0.9);
			float y = cy + (float) ((rnd(s, i + 260) - 0.5) * 2 * R * 0.9);
			float r = R * (0.08f + (float) rnd(s, i + 320) * 0.14f);
			g.setColor(comAlpha(luz, 26));
			g.fill(new Ellipse2D.Float(x - r, y - r, 2 * r, 2 * r));
		}
		for (int i = 0; i < 16; i++) {
			double tw = 0.5 + 0.5 * Math.sin(fase * 1.2 + i * 1.7);
			float x = cx + (float) ((rnd(s, i) - 0.5) * 2 * R * 0.92);
			float y = cy + (float) ((rnd(s, i + 60) - 0.5) * 2 * R * 0.92);
			float r = (float) ((6 + 12 * rnd(s, i + 120)) * (0.4 + 0.6 * tw));
			Path2D.Float p = new Path2D.Float();
			for (int k = 0; k < 8; k++) {
				double ang = k * Math.PI / 4 - Math.PI / 2;
				float rr = (k % 2 == 0) ? r : r * 0.28f;
				float px = x + (float) Math.cos(ang) * rr, py = y + (float) Math.sin(ang) * rr;
				if (k == 0) {
					p.moveTo(px, py);
				} else {
					p.lineTo(px, py);
				}
			}
			p.closePath();
			g.setColor(comAlpha(Color.WHITE, (int) (60 + 150 * tw)));
			g.fill(p);
		}
	}

	/** Normal: anéis pontilhados girando, simples e limpo. */
	private static void normal(Graphics2D g, float cx, float cy, float R, Color luz, double fase) {
		float[] aneis = { 0.45f, 0.7f, 0.93f };
		for (int i = 0; i < aneis.length; i++) {
			float r = R * aneis[i];
			g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
					new float[] { 0.1f, 13f }, (float) (((fase * 6 * (i % 2 == 0 ? 1 : -1)) % 13.1 + 13.1) % 13.1)));
			g.setColor(comAlpha(luz, 90));
			g.draw(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));
		}
		g.setColor(comAlpha(luz, 22));
		g.fill(new Ellipse2D.Float(cx - R * 0.3f, cy - R * 0.3f, R * 0.6f, R * 0.6f));
	}
}
