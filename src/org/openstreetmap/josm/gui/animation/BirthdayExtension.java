// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Birthday animation game extension.
 * @author Pauline Thiele
 * @since xxx
 */
public class BirthdayExtension extends JPanel implements AnimationExtension, MouseListener, MouseMotionListener {
    private static final int SIZE = 40;
    private final Image myImage;
    private double w;
    private double h;
    private double Mx;
    private double My;
    private int originX;
    private int originY;
    private boolean isClicked;
    private boolean isHappyBirthday;
    private int giftNumber = 1;

    private final ArrayList<Gift> giftList = new ArrayList<>();
    private long startTime;
    private long lastSpawnTime;

    /**
     * Creates a circle with a position, radius and checks if two circles ovelaps
     */
    private static final class Circle {
        double x;
        double y;
        double radius = 1;

        void setPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void setRadius(double radius) {
            this.radius = radius;
        }

        boolean overlaps(Circle other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dr = radius + other.radius;
            return dx*dx + dy*dy < dr*dr;
        }
    }

    /**
     * Creates a player with a position, radius and updates position
     */
    private final class Player {
        Circle circle = new Circle();

        void setPosition(double x, double y) {
            circle.setPosition(x, y);
        }

        void setRadius(double radius) {
            circle.setRadius(radius);
        }

        void update() {
            setPosition(Mx, My);
        }
    }

    /**
     * Creates a gift with a position, radius, updates position and renders the image
     */
    private final class Gift {
        Image myImage = getImage();
        Circle circle = new Circle();
        int offsX = 4;
        int offsY = 4;

        void setPosition(double x, double y) {
            circle.setPosition(x, y);
        }

        void setRadius(double radius) {
            circle.setRadius(radius);
        }

        void update() {
            circle.x += offsX;
            circle.y += offsY;

            if (circle.x <= 0) { // left
                offsX *= -1;
                circle.x = 0;
                if (circle.y == 0) {
                    offsY *= -1;
                }
            } else if (circle.x >= (w - SIZE)) { // right
                offsX *= -1;
                circle.x = w - SIZE;
            }

            if (circle.y <= 0) { // top
                offsY *= -1;
                circle.y = 0;
            } else if (circle.y >= (h - SIZE)) { // bottom
                offsY *= -1;
                circle.y = h - SIZE;
            }
        }

        void render(Graphics g) {
            double x = circle.x;
            double y = circle.y;
            g.drawImage(myImage, (int) x, (int) y, null);
        }
    }

    Player player = new Player();

    BirthdayExtension() {
        this.myImage = new ImageProvider("presets/shop/present").
                setMaxSize(new Dimension(SIZE, SIZE)).get().getImage();
        player.setRadius(11);
        lastSpawnTime = System.currentTimeMillis();
    }

    /**
     * Creates gifts on a random position on screen
     */
    void spawnGifts() {
        final byte maxGifts = 30;
        if (giftList.size() < maxGifts) {
            lastSpawnTime = System.currentTimeMillis();

            Gift gift = new Gift();
            giftList.add(gift);
            gift.myImage = getImage();
            gift.setRadius(32);

            double x = (w - originX) / 2;
            double y = (h - originY) / 2;

            if (giftList.size() > 1) {
                double radius = w / 2;
                double angle = 2 * Math.PI * Math.random();
                x += Math.cos(angle) * radius;
                y += Math.sin(angle) * radius;
                gift.setPosition(x, y);
            } else {
                gift.setPosition(x, y);
            }
        }
    }

    /**
     * Manages the creation, updating and deletion of gifts
     */
    private void gameLogic() {
        if ((giftList.isEmpty() && !isHappyBirthday) ||
            (System.currentTimeMillis() - lastSpawnTime > 5000 && !isHappyBirthday)) {
            spawnGifts();
            lastSpawnTime = System.currentTimeMillis();
        }

        player.update();

        for (Gift gift : giftList) {
            gift.update();
        }

        int index = 0;
        for (Gift gift : giftList) {
            if (gift.circle.overlaps(player.circle) && isClicked) {
                startTime = System.currentTimeMillis();
                giftList.remove(index);
                break;
            }
            index += 1;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        isClicked = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        isClicked = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        Mx = e.getXOnScreen()- originX;
        My = e.getYOnScreen()- originY;

        player.setPosition(Mx, My);

        huntGift();
    }

    @Override
    public final void adjustForSize(int w, int h, int x, int y) {
        this.w = w;
        this.h = h;
        originX = x;
        originY = y;
    }

    /**
     * Visualizes the gifts and the "Happy Birthday JOSM!" text
     */
    @Override
    public void paint(Graphics g) {
        gameLogic();
        for (Gift gift : giftList) {
            gift.render(g);
        }

        Graphics2D g2d = (Graphics2D) g;

        /* keep a bit of debugging code
        if (Logging.isTraceEnabled()) {
            // draws circles around mouse and gifts
            for (Gift gift : giftList) {
                Ellipse2D.Double circleG = new Ellipse2D.Double(gift.circle.x - gift.circle.radius / 2,
                        gift.circle.y - gift.circle.radius / 2, gift.circle.radius * 2, gift.circle.radius * 2);
                g2d.draw(circleG);
            }
            Ellipse2D.Double circleM = new Ellipse2D.Double(player.circle.x - player.circle.radius / 2,
                    player.circle.y - player.circle.radius / 2, player.circle.radius * 2, player.circle.radius * 2);
            g2d.draw(circleM);
        }
        */

        if (giftList.isEmpty()) {
            if (System.currentTimeMillis() - startTime <= 5000) {
                int fontSize = SIZE /2;
                g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
                g2d.setColor(Color.RED);

                String text = "Happy Birthday JOSM! ";

                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int x = 0;
                int y = SIZE /2;

                for (int i = 0; i < (w / textWidth) + 1; i++) {
                    g2d.drawString(text, x, y);
                    g2d.drawString(text, x, (int) h-y);
                    x += textWidth;
                }
                isHappyBirthday = true;
            } else {
                giftNumber += 1;
                for (int i = 0; i < giftNumber; i += 1) {
                    spawnGifts();
                }
                startTime = System.currentTimeMillis();
                isHappyBirthday = false;
            }
        }
    }

    @Override
    public void animate() {
    }

    /**
     * Hunts the gift
     */
    private void huntGift() {
        for (Gift gift : giftList) {
            double dx = gift.circle.x - player.circle.x;
            double dy = gift.circle.y - player.circle.y;

            double len = Math.sqrt(dx*dx + dy*dy);

            if ((dx < -100 || dx > 100) && (dy < -100 || dy > 100)) {
                dx = 0;
                dy = 0;
            } else if (len > 0 && dx != 0 && dy != 0) {
                dx /= len;
                dy /= len;
            }

            double giftSpeed;

            if (dx != 0 && dy != 0 && gift.circle.x == 0 && gift.circle.y == 0) { // top left corner
                giftSpeed = 5.0f;
                dy += 5;
            } else if (dx != 0 && dy != 0 && gift.circle.x >= w - SIZE && gift.circle.y == 0) { // top right corner
                giftSpeed = 5.0f;
                dy += 5;
            } else if (dx != 0 && dy != 0 && gift.circle.x == 0 && gift.circle.y >= h - SIZE) { // bottom left corner
                giftSpeed = 5.0f;
                dx += 5;
            } else if (dx != 0 && dy != 0 && gift.circle.x >= w - SIZE && gift.circle.y >= h - SIZE) { // bottom right corner
                giftSpeed = 5.0f;
                dx -= 5;
            } else {
                giftSpeed = 3.0f;
            }

            gift.circle.x += dx * giftSpeed;
            gift.circle.y += dy * giftSpeed;
        }
    }

    private Image getImage() {
        return this.myImage;
    }
}
