package pokedex.ui.componentes;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

import static pokedex.ui.tema.Tema.*;

public class BarraRolagem extends BasicScrollBarUI {
	@Override
	protected JButton createDecreaseButton(int orientation) {
		return botaoVazio();
	}

	@Override
	protected JButton createIncreaseButton(int orientation) {
		return botaoVazio();
	}

	private JButton botaoVazio() {
		JButton b = new JButton();
		b.setPreferredSize(new Dimension(0, 0));
		return b;
	}

	@Override
	protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
		// sem trilho: só o puxador aparece
	}

	@Override
	protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
		if (r.isEmpty() || !scrollbar.isEnabled()) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		qualidade(g2);
		g2.setColor(new Color(255, 255, 255, isDragging ? 110 : 60));
		g2.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8));
		g2.dispose();
	}
}
