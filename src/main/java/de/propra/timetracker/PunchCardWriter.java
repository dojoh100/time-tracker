package de.propra.timetracker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PunchCardWriter {
    private static PunchCardWriter instance;
    private int width = 600;
    private int height = 150;
    private static final Path DEFAULT_PATH_TO_TEMP = Paths.get(System.getProperty("user.home"), ".config", "time-tracker", "punchCard.png");
    private PunchCard pc;

    public static PunchCardWriter getInstance() {
        if (instance == null) {
            instance = new PunchCardWriter();
        }
        return instance;
    }

    private PunchCardWriter() { }

    public int savePunchCard(List<Event> events) {
        pc = new PunchCard(events);
        // Prepare fresh punch card image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        drawBackground(g);
        drawGrid(g);
        drawDaysOfWeek(g);
        drawPunchHoles(g);

        if(!Files.exists(DEFAULT_PATH_TO_TEMP)) {
            try {
                Files.createDirectories(DEFAULT_PATH_TO_TEMP.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        File file = new File(String.valueOf(DEFAULT_PATH_TO_TEMP));

        try {
            ImageIO.write(image, "PNG", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            return getDayOfTheWeek(new SimpleDateFormat("yyyy-MM-dd").parse("2022-06-21"));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void drawBackground(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(0, height - 50, width, height - 50);
        int space = width / 14;
        for (int i = 1; i < 15; i += 2) {
            g.drawLine(i*space, height - 50, i*space, height - 75);
        }
    }

    private void drawDaysOfWeek(Graphics2D g) {
        g.setColor(Color.GRAY);
        Font font = new Font("Arial", Font.PLAIN, 12);
        int space = width / 14;
        try {
            Locale locale = Locale.getDefault();
            DateFormat formatter = new SimpleDateFormat("EEEE", locale);

            for (int i = 1; i < 9; i++) {
                int j = i + 1;
                Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2022-05-0" + j);
                String day = formatter.format(date);
                int stringWidth = g.getFontMetrics().stringWidth(day);
                g.drawString(day, ((i-1)*2*space) + space - (stringWidth / 2), height - 25);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void drawPunchHoles(Graphics2D g) {
        g.setColor(Color.GRAY);
        int space = width / 14;
        for (int i = 1; i < 8; i++) {
            int size = 75;
            int maxHoleSize = 50;
            int holeSize; //ThreadLocalRandom.current().nextInt(0, maxHoleSize + 1);
            holeSize = (int) (maxHoleSize * pc.getPercentageOf(i - 1));
            g.fillOval(((i-1)*2*space) + space - (holeSize / 2), (size - holeSize) / 2, holeSize, holeSize);
        }
    }

    private int getDayOfTheWeek(Date date) {
        LocalDate d = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        DayOfWeek day = d.getDayOfWeek();
        return day.getValue();
    }

    private class PunchCard {
        private int max = 0; // max entries for day/hour pair
        private int[] map = {0, 0, 0, 0, 0, 0, 0};

        PunchCard(List<Event> events) {
            events.forEach(event -> {
                int day = -1;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(event.datum());
                    day = getDayOfTheWeek(date);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                if (day > 0 && day < 8) {
                    map[day - 1] += event.minuten();
                }
            });
            for (int i : map) {
                if (max < i) {
                    max = i;
                }
            }
        }

        double getPercentageOf(int dayOfWeek) {
            if (dayOfWeek > -1 && dayOfWeek < 7) {
                return ((double) map[dayOfWeek]) / max;
            }
            return 0;
        }
    }
}
