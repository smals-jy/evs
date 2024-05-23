package org.imec.ivlab.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imec.ivlab.core.authentication.AuthenticationConfigReader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConsoleUtils {

    private final static Logger log = LogManager.getLogger(AuthenticationConfigReader.class);

    private static final int BOX_LENGTH = 170;

    public static String emphasizeTitle(String message) {
        StringBuilder builder = new StringBuilder();
        builder.append(createBorder(BOX_LENGTH)).append(System.lineSeparator());
        String[] lines = message.split(System.lineSeparator());
        for (String line : lines) {
            builder.append(center(line, BOX_LENGTH)).append(System.lineSeparator());
        }
        builder.append(createBorder(BOX_LENGTH));
        return builder.toString();
    }

    private static String repeat(String str, int length) {
        IntFunction<String> repeater = (l) -> IntStream.range(0, l)
                .mapToObj(i -> str)
                .collect(Collectors.joining());
        return repeater.apply(length);
    }
    
    private static String center(String text, int length) {
        int paddingSize = (length - text.length()) / 2;
        String padding = repeat(" ", Math.max(0, paddingSize));
        return "|" + padding + text + padding + (text.length() % 2 == 0 ? padding : padding + " ") + "|";
    }

    private static String createBorder(int length) {
        return "+" + repeat("-",length) + "+";
    }

    public static String createAsciiMessage(String message) {
        BufferedImage image = new BufferedImage(144, 32, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setFont(new Font("Dialog", Font.PLAIN, 24));
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.drawString(message, 6, 24);

        StringBuffer stringBuffer = new StringBuffer();
        for (int y = 0; y < 32; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < 144; x++) {
                int pixel = image.getRGB(x, y);
                if (pixel == Color.BLACK.getRGB()) {
                    sb.append(" ");
                } else if (pixel == Color.WHITE.getRGB()) {
                    sb.append("#");
                } else {
                    sb.append("*");
                }
            }
            if (sb.toString().trim().isEmpty()) continue;
            stringBuffer.append(sb.toString()).append(System.lineSeparator());
        }
        return stringBuffer.toString();
    }

}
