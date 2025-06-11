package io.github.wj9806;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ANSI {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String CLEAR_SCREEN = "\u001B[2J\u001B[H";
    private static final String DENSITY = "Ñ@#W$9876543210?!abc;:+=-,._ ";
    private static final int MIN_FRAME_TIME = 50; // 最小帧间隔(ms)

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java ANSI <file-path> [color] [scale]");
            System.out.println("Supported formats: GIF, PNG, JPG, JPEG");
            System.out.println("Parameters:");
            System.out.println("  color : true/false to enable/disable color (default: true)");
            System.out.println("  scale : Image scaling factor (default: 1.0)");
            return;
        }

        String filePath = args[0];
        boolean useColor = args.length <= 1 || Boolean.parseBoolean(args[1]);
        double scale = args.length > 2 ? Double.parseDouble(args[2]) : 0.5;

        File mediaFile = new File(filePath);
        if (!mediaFile.exists() && !filePath.contains("\\")) {
            FileSystemView fileSystemView = FileSystemView.getFileSystemView();
            File desktop = fileSystemView.getHomeDirectory();
            mediaFile = new File(desktop, filePath);
        }

        String extension = getFileExtension(mediaFile).toLowerCase();

        switch (extension) {
            case "gif":
                playGifAnimation(mediaFile, useColor, scale);
                break;
            case "png":
            case "jpg":
            case "jpeg":
                displayStaticImage(mediaFile, useColor, scale);
                break;
            default:
                System.err.println("Unsupported media format: " + extension);
        }
    }

    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot == -1 ? "" : name.substring(lastDot + 1);
    }

    private static void playGifAnimation(File gifFile, boolean useColor, double scale) throws IOException {
        ImageInputStream input = ImageIO.createImageInputStream(gifFile);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

        if (!readers.hasNext()) {
            throw new IOException("No reader found for GIF");
        }

        ImageReader reader = readers.next();
        reader.setInput(input);

        int frameCount = reader.getNumImages(true);
        StringBuilder[] frameBuffer = new StringBuilder[frameCount];
        int[] frameLineCounts = new int[frameCount];
        int[] delays = new int[frameCount];

        // 获取第一帧确定原始尺寸
        BufferedImage firstFrame = reader.read(0);
        int targetWidth = (int)(firstFrame.getWidth() * scale);

        // 预渲染所有帧到内存并记录每帧行数
        for (int i = 0; i < frameCount; i++) {
            BufferedImage frame = reader.read(i);
            frameBuffer[i] = new StringBuilder(convertToANSI(frame, useColor, targetWidth));
            frameLineCounts[i] = countLines(frameBuffer[i].toString());
            delays[i] = getGifFrameDelay(reader, i);
        }
        reader.dispose();
        input.close();

        // 动画播放循环
        System.out.print(CLEAR_SCREEN);
        int prevLineCount = 0;

        while (true) {
            for (int i = 0; i < frameCount; i++) {
                long frameStartTime = System.currentTimeMillis();
                int currentLineCount = frameLineCounts[i];

                // 智能清屏策略
                if (currentLineCount < prevLineCount) {
                    System.out.print(CLEAR_SCREEN);
                    System.out.print(frameBuffer[i]);
                } else {
                    System.out.print("\u001B[H");
                    System.out.print(frameBuffer[i]);
                }

                prevLineCount = currentLineCount;

                // 帧率控制
                long renderTime = System.currentTimeMillis() - frameStartTime;
                long sleepTime = Math.max(delays[i] - renderTime, 0);

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static int countLines(String str) {
        return str.split("\n").length;
    }

    private static int getGifFrameDelay(ImageReader reader, int frameIndex) {
        try {
            IIOMetadata metadata = reader.getImageMetadata(frameIndex);
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
            IIOMetadataNode extension = findNode(root, "GraphicControlExtension");
            String attribute = (extension != null) ? extension.getAttribute("delayTime") : null;
            return (attribute != null) ? Integer.parseInt(attribute) * 10 : 0;
        } catch (Exception e) {
            // 如果无法获取延迟，使用默认值
        }
        return MIN_FRAME_TIME;
    }

    private static IIOMetadataNode findNode(IIOMetadataNode rootNode, String nodeName) {
        if (rootNode == null) {
            return null;
        }
        for (int i = 0; i < rootNode.getLength(); i++) {
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return ((IIOMetadataNode) rootNode.item(i));
            }
        }
        return null;
    }

    private static void displayStaticImage(File imageFile, boolean useColor, double scale) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        int targetWidth = (int)(image.getWidth() * scale);
        String asciiArt = convertToANSI(image, useColor, targetWidth);
        System.out.print(CLEAR_SCREEN);
        System.out.println(asciiArt);
    }

    public static String convertToANSI(BufferedImage image, boolean useColor, int targetWidth) {
        // 计算高度，考虑终端字符的宽高比(通常字符高度是宽度的约2倍)
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        int targetHeight = (int)((double)originalHeight / originalWidth * targetWidth * 0.5);

        // 高质量缩放
        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        StringBuilder sb = new StringBuilder((targetWidth + 1) * targetHeight);

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int pixel = scaledImage.getRGB(x, y);
                Color c = new Color(pixel);

                // 计算灰度值
                int gray = (int)(0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                int index = (int)((gray / 255.0) * (DENSITY.length() - 1));
                char symbol = DENSITY.charAt(DENSITY.length() - 1 - index);

                if (useColor) {
                    sb.append(String.format("\u001B[38;2;%d;%d;%dm%c", c.getRed(), c.getGreen(), c.getBlue(), symbol));
                } else {
                    sb.append(symbol);
                }
            }
            sb.append('\n');
        }

        return useColor ? sb.append(ANSI_RESET).toString() : sb.toString();
    }
}
