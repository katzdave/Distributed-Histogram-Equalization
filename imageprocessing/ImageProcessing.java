/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package imageprocessing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 *
 * @author David
 */
public class ImageProcessing {
  /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */  
    private static BufferedImage original, equalized;
 
    public static void main(String[] args) throws IOException {
 
        File original_f = new File("monsters.png");
        BufferedImage orig = ImageIO.read(original_f);
        orig = convertBlackAndWhite(orig);
        List<BufferedImage> splitImages = splitImage(orig,3);
        orig = mergeImages(splitImages);
        
        
        ImageIO.write(orig, "png", new File("blackwhite.png"));
 
    }
    
    public static BufferedImage convertBlackAndWhite(BufferedImage img) {
      BufferedImage output = new BufferedImage(
        img.getWidth(), img.getHeight(),
        BufferedImage.TYPE_BYTE_GRAY);
      
      Graphics2D graphics = output.createGraphics();
      graphics.drawImage(img, 0, 0, null);
      
      //ImageIO.write(output, "png", new File("blackwhite.png"));
      return output;
    }
    
    public static List<BufferedImage> splitImage(BufferedImage img, int parts){
      ArrayList<BufferedImage> result = new ArrayList<>();
      int height = img.getHeight();
      int width = img.getWidth() / parts;
      int lastWidth = width + img.getWidth() - width*parts;
      
      for(int i=0; i<parts; i++){
        BufferedImage split;
        if(i != (parts-1))
          split = new BufferedImage(width,height,img.getType());
        else
          split = new BufferedImage(lastWidth,height,img.getType());
        Graphics2D gr = split.createGraphics();
        if(i != (parts-1))
          gr.drawImage(img, 0, 0, width, height,
                width*i,0,width*i + width, height, null);
        else
          gr.drawImage(img, 0, 0, lastWidth, height,
                width*i,0,width*i + lastWidth, height, null);
        result.add(split);
        gr.dispose();
      }
      
      return result;
    }
    
    public static BufferedImage mergeImages(List<BufferedImage> images){
      BufferedImage result;
      int width = 0;
      int indwidth = 0;
      int height = 0;
      int type = 0;
      for(int i=0; i<images.size(); i++){
        width += images.get(i).getWidth();
        if(i == 0)
          indwidth = width;
        if(i == (images.size() - 1)){
          height = images.get(i).getHeight();
          type = images.get(i).getType();
        }
      }
      result = new BufferedImage(width, height, type);
      for(int i=0; i<images.size(); i++){
        result.createGraphics().drawImage(images.get(i),indwidth*i,0, null);
      }
      
      return result;
    }
    
    public
 
    private static void writeImage(String output) throws IOException {
        File file = new File(output+".jpg");
        ImageIO.write(equalized, "jpg", file);
    }
 
    public static BufferedImage histogramEqualization(BufferedImage original) {
 
        int red;
        int green;
        int blue;
        int alpha;
        int newPixel = 0;
 
        // Get the Lookup table for histogram equalization
        ArrayList<int[]> histLUT = histogramEqualizationLUT(original);
 
        BufferedImage histogramEQ = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
 
        for(int i=0; i<original.getWidth(); i++) {
            for(int j=0; j<original.getHeight(); j++) {
 
                // Get pixels by R, G, B
                alpha = new Color(original.getRGB (i, j)).getAlpha();
                red = new Color(original.getRGB (i, j)).getRed();
                green = new Color(original.getRGB (i, j)).getGreen();
                blue = new Color(original.getRGB (i, j)).getBlue();
 
                // Set new pixel values using the histogram lookup table
                red = histLUT.get(0)[red];
                green = histLUT.get(1)[green];
                blue = histLUT.get(2)[blue];
 
                // Return back to original format
                newPixel = colorToRGB(alpha, red, green, blue);
 
                // Write pixels into image
                histogramEQ.setRGB(i, j, newPixel);
 
            }
        }
 
        return histogramEQ;
 
    }
 
    // Get the histogram equalization lookup table for separate R, G, B channels
    private static ArrayList<int[]> histogramEqualizationLUT(BufferedImage input) {
 
        // Get an image histogram - calculated values by R, G, B channels
        ArrayList<int[]> imageHist = imageHistogram(input);
 
        // Create the lookup table
        ArrayList<int[]> imageLUT = new ArrayList<int[]>();
 
        // Fill the lookup table
        int[] rhistogram = new int[256];
        int[] ghistogram = new int[256];
        int[] bhistogram = new int[256];
 
        for(int i=0; i<rhistogram.length; i++) rhistogram[i] = 0;
        for(int i=0; i<ghistogram.length; i++) ghistogram[i] = 0;
        for(int i=0; i<bhistogram.length; i++) bhistogram[i] = 0;
 
        long sumr = 0;
        long sumg = 0;
        long sumb = 0;
 
        // Calculate the scale factor
        float scale_factor = (float) (255.0 / (input.getWidth() * input.getHeight()));
 
        for(int i=0; i<rhistogram.length; i++) {
            sumr += imageHist.get(0)[i];
            int valr = (int) (sumr * scale_factor);
            if(valr > 255) {
                rhistogram[i] = 255;
            }
            else rhistogram[i] = valr;
 
            sumg += imageHist.get(1)[i];
            int valg = (int) (sumg * scale_factor);
            if(valg > 255) {
                ghistogram[i] = 255;
            }
            else ghistogram[i] = valg;
 
            sumb += imageHist.get(2)[i];
            int valb = (int) (sumb * scale_factor);
            if(valb > 255) {
                bhistogram[i] = 255;
            }
            else bhistogram[i] = valb;
        }
 
        imageLUT.add(rhistogram);
        imageLUT.add(ghistogram);
        imageLUT.add(bhistogram);
 
        return imageLUT;
 
    }
 
    // Return an ArrayList containing histogram values for separate R, G, B channels
    public static ArrayList<int[]> imageHistogram(BufferedImage input) {
 
        int[] rhistogram = new int[256];
        int[] ghistogram = new int[256];
        int[] bhistogram = new int[256];
 
        for(int i=0; i<rhistogram.length; i++) rhistogram[i] = 0;
        for(int i=0; i<ghistogram.length; i++) ghistogram[i] = 0;
        for(int i=0; i<bhistogram.length; i++) bhistogram[i] = 0;
 
        for(int i=0; i<input.getWidth(); i++) {
            for(int j=0; j<input.getHeight(); j++) {
 
                int red = new Color(input.getRGB (i, j)).getRed();
                int green = new Color(input.getRGB (i, j)).getGreen();
                int blue = new Color(input.getRGB (i, j)).getBlue();
 
                // Increase the values of colors
                rhistogram[red]++; ghistogram[green]++; bhistogram[blue]++;
 
            }
        }
 
        ArrayList<int[]> hist = new ArrayList<int[]>();
        hist.add(rhistogram);
        hist.add(ghistogram);
        hist.add(bhistogram);
 
        return hist;
 
    }
 
    // Convert R, G, B, Alpha to standard 8 bit
    private static int colorToRGB(int alpha, int red, int green, int blue) {
 
        int newPixel = 0;
        newPixel += alpha; newPixel = newPixel << 8;
        newPixel += red; newPixel = newPixel << 8;
        newPixel += green; newPixel = newPixel << 8;
        newPixel += blue;
 
        return newPixel;
 
    }
 
}
