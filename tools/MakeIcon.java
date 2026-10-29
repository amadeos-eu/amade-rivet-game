import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Draws the program icon of Rivet: a steel plate of the station with a hazard stripe, frost in one corner,
 * and one big brass rivet in the middle, its head lit from the upper left. Drawn as shapes on a 48 × 48
 * grid, so it is sharp at every size.
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
        Shape plate = new RoundRectangle2D.Double(2, 2, 44, 44, 10, 10);
        g.setPaint(new GradientPaint(0, 2, new Color(0x5d6873), 0, 46, new Color(0x2c333a)));
        g.fill(plate);
        g.setClip(plate);
        // the hazard stripe along the bottom
        g.setPaint(new Color(0x1e1f21));
        g.fill(new Rectangle2D.Double(2, 37, 44, 9));
        g.setPaint(new Color(0xd9a423));
        for (int i = -2; i < 8; i++) {
            Path2D.Double stripe = new Path2D.Double();
            stripe.moveTo(i * 7, 46);
            stripe.lineTo(i * 7 + 4, 46);
            stripe.lineTo(i * 7 + 13, 37);
            stripe.lineTo(i * 7 + 9, 37);
            stripe.closePath();
            g.fill(stripe);
        }
        g.setPaint(new Color(0x15191d));
        g.fill(new Rectangle2D.Double(2, 36, 44, 1.2));
        // frost in the upper right corner
        Area frost = new Area(new Ellipse2D.Double(33, -8, 22, 20));
        frost.subtract(new Area(new Ellipse2D.Double(28, 7, 9, 6)));
        g.setPaint(new Color(0x9fd4ec));
        g.fill(frost);
        g.setPaint(new Color(0xe6f8ff));
        g.fill(new Ellipse2D.Double(39, 2, 6, 4));
        // small rivets in the corners
        for (double[] p : new double[][]{{7, 7}, {7, 31}, {41, 31}}) {
            g.setPaint(new Color(0x23292e));
            g.fill(new Ellipse2D.Double(p[0] - 2.2, p[1] - 1.8, 4.4, 4.4));
            g.setPaint(new Color(0x9aa3ab));
            g.fill(new Ellipse2D.Double(p[0] - 1.8, p[1] - 2.2, 3.6, 3.6));
        }
        g.setClip(null);
        // the big rivet: a shadow, the head, a highlight
        g.setPaint(new Color(0, 0, 0, 110));
        g.fill(new Ellipse2D.Double(12.5, 10.5, 25, 25));
        g.setPaint(new RadialGradientPaint(19, 15, 17, new float[]{0, 0.35f, 1},
                new Color[]{new Color(0xfff0c0), new Color(0xd2a550), new Color(0x6a4a1c)}));
        g.fill(new Ellipse2D.Double(11, 8, 25, 25));
        g.setPaint(new Color(255, 255, 255, 170));
        g.fill(new Ellipse2D.Double(16, 12, 6, 4));
        g.setPaint(new Color(0x3a2a10));
        g.draw(new Ellipse2D.Double(11, 8, 25, 25));
    }
}
