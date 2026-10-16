import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

class Complex {
    final double real;
    final double imaginary;

    Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    Complex add(Complex other) {
        return new Complex(this.real + other.real, this.imaginary + other.imaginary);
    }

    Complex multiply(Complex other) {
        return new Complex(
            this.real * other.real - this.imaginary * other.imaginary,
            this.real * other.imaginary + this.imaginary * other.real
        );
    }

    double magnitudeSquared() {
        return real * real + imaginary * imaginary;
    }

    @Override
    public String toString() {
        return real + " + " + imaginary + "i";
    }
}

class View {
    double centerRe, centerIm, scale;

    View(double centerRe, double centerIm, double scale) {
        this.centerRe = centerRe;
        this.centerIm = centerIm;
        this.scale = scale;
    }

    double toRe(int px, int width)  { return centerRe + (px - width / 2.0) * scale; }
    double toIm(int py, int height) { return centerIm - (py - height / 2.0) * scale; }

    void zoomAt(int px, int py, int width, int height, double factor) {
        double pRe = toRe(px, width);      // the point under the mouse, before zooming
        double pIm = toIm(py, height);
        scale *= factor;
        centerRe = pRe - (px - width / 2.0) * scale;   // new center keeps that point under the mouse
        centerIm = pIm + (py - height / 2.0) * scale;
    }

    void panBy(int dx, int dy) {
        centerRe -= dx * scale;
        centerIm += dy * scale;
    }
}

class FractalPanel extends JPanel {
    final int width, height;
    final BufferedImage image;
    final View view;
    int lastX, lastY;

    FractalPanel(int width, int height) {
        this.width = width;
        this.height = height;
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        view = new View(-0.5, 0, 3.0 / width);
        setPreferredSize(new Dimension(width, height));

        addMouseWheelListener(e -> {
            double factor = Math.pow(1.25, e.getPreciseWheelRotation()); // scroll up = below 1 = zoom in
            view.zoomAt(e.getX(), e.getY(), width, height, factor);
            render();
        });

        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
            }
            @Override public void mouseDragged(MouseEvent e) {
                view.panBy(e.getX() - lastX, e.getY() - lastY);
                lastX = e.getX();
                lastY = e.getY();
                render();
            }
        };
        addMouseListener(drag);
        addMouseMotionListener(drag);

        render();
    }

    int currentMaxIter() {
        double zoom = (3.0 / width) / view.scale;
        double log2 = Math.log(zoom) / Math.log(2);
        int it = (int) (100 + 40 * log2);
        return Math.max(100, Math.min(2000, it));
    }

    void render() {
        int maxIter = currentMaxIter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double re = view.toRe(x, width);
                double im = view.toIm(y, height);
                int n = fractal.escapeTime(new Complex(re, im), maxIter);
                int v = (n == maxIter) ? 0 : (int) (255 * Math.sqrt((double) n / maxIter));
                image.setRGB(x, y, ((v / 3) << 16) | ((v / 2) << 8) | v);
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null);
    }
}

public class fractal {
    static int escapeTime(Complex c, int maxIter) {
        Complex z = new Complex(0, 0);
        for (int i = 0; i < maxIter; i++) {
            if (z.magnitudeSquared() > 4) {
                return i;
            }
            z = z.multiply(z).add(c);
        }
        return maxIter;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FractalPanel panel = new FractalPanel(800, 600);
            JFrame frame = new JFrame("Fractal Explorer");
            frame.add(panel);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}