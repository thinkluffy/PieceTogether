// package com.xiaodai;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class PieceTogether {

    private static final int CANVAS_WIDTH = 469;
    private static final int CANVAS_HEIGHT = 640;
    private static final int CANVAS_PADDING = 10;

    private String mSrcFolderPath;
    private String mOutputFolderPath;

    private BufferedImage prepareImage(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        if (width > height) {
            return img;
        }

        @SuppressWarnings("SuspiciousNameCombination")
        BufferedImage rotatedImg = new BufferedImage(height, width, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotatedImg.createGraphics();

        AffineTransform at = new AffineTransform();

        at.translate(height/2, width/2);
        at.rotate(Math.PI/2);
        at.translate(-width/2, -height/2);

        g2d.drawImage(img, at, null);

        g2d.dispose();

        return rotatedImg;
    }

    private BufferedImage joinBufferedImage(BufferedImage img1, BufferedImage img2) {
        //create a new buffer and draw two image into the new image
        BufferedImage newImage = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = newImage.createGraphics();
        Color oldColor = g2d.getColor();
        //fill background
        g2d.setPaint(Color.WHITE);
        g2d.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        g2d.setColor(oldColor);

        int oneImageWidth = CANVAS_WIDTH - CANVAS_PADDING * 2;
        int oneImageHeight = (CANVAS_HEIGHT - CANVAS_PADDING * 3) / 2;

        g2d.drawImage(img1,
                CANVAS_PADDING, CANVAS_PADDING, oneImageWidth + CANVAS_PADDING, oneImageHeight + CANVAS_PADDING,
                0, 0, img1.getWidth(), img1.getHeight(),
                null);
        g2d.drawImage(img2,
                CANVAS_PADDING, oneImageHeight + CANVAS_PADDING * 2, oneImageWidth + CANVAS_PADDING, oneImageHeight * 2 + CANVAS_PADDING * 2,
                0, 0, img2.getWidth(), img2.getHeight(),
                null);

        g2d.dispose();

        return newImage;
    }

    private void joinImages(List<File> imageFiles) {
        File outputDir = new File(mOutputFolderPath);
        if (!outputDir.exists()) {
            if (!outputDir.mkdir()) {
                System.err.println("生成文件夹失败！");
                return;
            }
        }

        for (int i = 0; i < imageFiles.size(); i += 2) {
            if (i >= imageFiles.size() - 1) {
                System.out.println("单个图片，没找到配对图片: " + imageFiles.get(i).getName());
                break;
            }

            try {
                BufferedImage img1 = ImageIO.read(imageFiles.get(i));
                BufferedImage img2 = ImageIO.read(imageFiles.get(i + 1));

                System.out.println("合成: " + imageFiles.get(i).getName() + ", " + imageFiles.get(i + 1).getName());
                BufferedImage joinedImg = joinBufferedImage(prepareImage(img1), prepareImage(img2));

                boolean success = ImageIO.write(joinedImg, "png", new File(outputDir,
                        imageFiles.get(i).getName() + "_" + imageFiles.get(i + 1).getName() + ".png"));

                if (success) {
                    System.out.println("-> 成功");

                } else {
                    System.out.println("-> 失败");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean readConfig() {
        JSONParser parser = new JSONParser();
        JSONObject config;
        try {
            config = (JSONObject) parser.parse(new FileReader("config.json"));

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        mSrcFolderPath = (String) config.get("src_folder");
        mOutputFolderPath = (String) config.get("output_folder");
        return true;
    }

    private List<File> buildImageFiles() {
        List<File> imageFiles = new ArrayList<>();
        File[] files = new File(mSrcFolderPath).listFiles();

        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith("jpg") || fileName.endsWith("jped") || fileName.endsWith("png")) {
                        imageFiles.add(file);
                    }
                }
            }
        }
        return imageFiles;
    }

    public static void main(String args[]) {
        PieceTogether pieceTogether = new PieceTogether();
        if (!pieceTogether.readConfig()) {
            System.err.println("读取 config.json 失败");
            return;
        }

        List<File> imageFiles = pieceTogether.buildImageFiles();
        if (imageFiles == null || imageFiles.size() <= 0) {
            System.out.println("没找到需要处理的图片");
            return;
        }
        pieceTogether.joinImages(imageFiles);
    }
}
