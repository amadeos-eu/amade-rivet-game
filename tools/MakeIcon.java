import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Draws the program icon of Rivet: a red tank on a green hill, its barrel raised, and the dotted arc
 * of its shell over an evening sky. Drawn as shapes on a 48 × 48 grid, so it is sharp at every size.
 *
 * <pre>
 * java tools/MakeIcon.java 48 src/main/resources/icons/amade-rivet-game.png
 * java tools/MakeIcon.java 256 build/icon-256.png
 * python3 -c "from PIL import Image; Image.open('build/icon-256.png').save('packaging/windows/amade-rivet-game.ico',
 *     sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])"
 * </pre>
 *
 * Run it in the repository's folder. The files are part of the repository; run this again only to change
 * the icon.
 */
public class MakeIcon {
    public static void main(String[] args) throws IOException {
        int size = Integer.parseInt(args[0]);
        File file = new File(args[1]);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(size / 48.0, size / 48.0);
        paint(g);
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    static void paint(Graphics2D g) {
        // The sky, a rounded square.
        RoundRectangle2D frame = new RoundRectangle2D.Double(1, 1, 46, 46, 10, 10);
        g.setPaint(new GradientPaint(0, 1, new Color(0x2A3C78), 0, 47, new Color(0xE89A78)));
        g.fill(frame);
        g.setClip(frame);
        // The hill.
        Path2D hill = new Path2D.Double();
        hill.moveTo(0, 48);
        hill.lineTo(0, 36);
        hill.curveTo(6, 33, 12, 32, 18, 32.5);
        hill.curveTo(26, 33, 34, 38, 48, 37);
        hill.lineTo(48, 48);
        hill.closePath();
        g.setColor(new Color(0x5F4A2E));
        g.fill(hill);
        g.setColor(new Color(0x8FB84A));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(topEdge());
        g.setClip(null);
        // The arc of the shell: dots over the sky, the shell at its end.
        g.setColor(new Color(255, 245, 220, 210));
        for (int i = 1; i < 8; i++) {
            double t = i / 8.0, x = 19 + t * 24, y = 25 - 40 * t + 34 * t * t;
            g.fill(new Ellipse2D.Double(x - 0.8, y - 0.8, 1.6, 1.6));
        }
        g.setColor(new Color(0x2A2A2A));
        g.fill(new Ellipse2D.Double(41.5, 17.5, 3.4, 3.4));
        // The barrel.
        g.setColor(new Color(0x1E1E1E));
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(15, 26, 22, 20));
        // The tracks.
        g.setColor(new Color(0x2A2A2A));
        g.fill(new RoundRectangle2D.Double(5, 29, 20, 5, 5, 5));
        g.setColor(new Color(0x6A6A6A));
        for (int i = 0; i < 5; i++) g.fill(new Ellipse2D.Double(6.4 + i * 3.6, 30.2, 2.6, 2.6));
        // The hull and the turret.
        Path2D hull = new Path2D.Double();
        hull.moveTo(4, 29.5);
        hull.lineTo(6.5, 25.5);
        hull.lineTo(23.5, 25.5);
        hull.lineTo(26, 29.5);
        hull.closePath();
        g.setPaint(new GradientPaint(0, 25, new Color(0xF0705E), 0, 30, new Color(0xB8352A)));
        g.fill(hull);
        g.fill(new Ellipse2D.Double(10, 20.5, 10, 9));
        g.setColor(new Color(0x6A1A14));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(hull);
        g.setColor(new Color(255, 255, 255, 90));
        g.fill(new Ellipse2D.Double(12, 21.5, 4, 2));
    }

    /** The lit top edge of the hill. */
    private static Path2D topEdge() {
        Path2D edge = new Path2D.Double();
        edge.moveTo(0, 36);
        edge.curveTo(6, 33, 12, 32, 18, 32.5);
        edge.curveTo(26, 33, 34, 38, 48, 37);
        return edge;
    }
}
