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
import java.util.Arrays;
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
    private static String DELIM = " ";
 
    public static void main(String[] args) throws IOException {
 
        File original_f = new File("./imgs/monsters.png");
        BufferedImage orig = ImageIO.read(original_f);
        
        BufferedImage justBlackWhite = convertBlackAndWhite(orig);
        List<BufferedImage> splitImages = splitImage(orig,3);
        BufferedImage merged = mergeImages(splitImages);
        int[] counts = getFrequencyCounts(orig);
        counts = deserializeVector(serializeVector(counts));
        
        long size = ((long) merged.getWidth()) * ((long) merged.getHeight());
        int[] newCounts = equalizeFreqs(counts, size);
        merged = applyValuesToImage(newCounts, merged);
        
        
        ImageIO.write(merged, "png", new File("./imgs/blackwhiteeqilized.png"));
        ImageIO.write(justBlackWhite, "png", new File("./imgs/blackwhite.png"));
 
    }
    
    public static BufferedImage convertBlackAndWhite(BufferedImage img) {
      BufferedImage output = new BufferedImage(
        img.getWidth(), img.getHeight(),
        BufferedImage.TYPE_BYTE_GRAY);
      
      Graphics2D graphics = output.createGraphics();
      graphics.drawImage(img, 0, 0, null);
      
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
    
    public static BufferedImage mergeImagesFromCIC(List<ClientImageContainer> images){
      BufferedImage result;
      int width = 0;
      int indwidth = 0;
      int height = 0;
      int type = 0;
      for(int i=0; i<images.size(); i++){
        width += images.get(i).Image.getWidth();
        if(i == 0)
          indwidth = width;
        if(i == (images.size() - 1)){
          height = images.get(i).Image.getHeight();
          type = images.get(i).Image.getType();
        }
      }
      result = new BufferedImage(width, height, type);
      for(int i=0; i<images.size(); i++){
        result.createGraphics().drawImage(images.get(i).Image,indwidth*i,0, null);
      }
      
      return result;
    }
    
    public static int[] getFrequencyCounts(BufferedImage img){
      int[] colorFreqs = new int[256];
      Arrays.fill(colorFreqs, 0);
      
      for(int x=0; x<img.getWidth(); x++){
        for(int y=0; y<img.getHeight(); y++){
          int rgb = img.getRGB(x, y);
          int r = (rgb >> 16) & 0xFF;
          int g = (rgb >> 8) & 0xFF;
          int b = (rgb & 0xFF);
          colorFreqs[(r+g+b)/3]++;
        }
      }
      
      return colorFreqs;
    }
    
    public static int[] sumColorFreqs(int[] a, int[] b){
      int[] res = new int[256];
      for(int i=0; i<256; i++){
        res[i] = a[i] + b[i];
      }
      return res;
    }
    
    public static int[] sumColorFreqs(List<int[]> freqs){
      int[] res = new int[256];
      Arrays.fill(res, 0);
      for(int i=0; i<freqs.size(); i++){
        for(int j=0; j<256; j++){
          res[j] += freqs.get(i)[j];
        }
      }
      return res;
    }
    
    public static int[] sumColorFreqsFromCIC(List<ClientImageContainer> freqs){
      int[] res = new int[256];
      Arrays.fill(res, 0);
      for(int i=0; i<freqs.size(); i++){
        for(int j=0; j<256; j++){
          res[j] += freqs.get(i).FrequencyCounts[j];
        }
      }
      return res;
    }
    
    public static int[] equalizeFreqs(int[] freqs, long imgSiz){
      long[] cdf = new long[256];
      int[] newVals = new int[256];
      for(int i=0; i<256; i++){
        cdf[i] = (long) freqs[i];
      }
      
      long sum = 0;
      long cdfmin = 0;
      for(int i=0; i<256; i++){
        sum += freqs[i];
        cdf[i] = sum;
        if(i == 0)
          cdfmin = sum;
      }
      
      //freqs now contains the cdf, rather than frequency counts
      for(int i=0; i<256; i++){
        int newVal = (int) Math.round((cdf[i]-cdfmin)*255.0/(imgSiz - cdfmin));
        if(newVal > 255)
          newVal = 255;
        if(newVal < 0)
          newVal = 0;
        newVals[i] = newVal;
      }
      return newVals;
    }
    
    public static BufferedImage applyValuesToImage(int[] values, BufferedImage img){
      for(int x=0; x<img.getWidth(); x++){
        for(int y=0; y<img.getHeight(); y++){
          int rgb = img.getRGB(x, y);
          int r = (rgb >> 16) & 0xFF;
          int g = (rgb >> 8) & 0xFF;
          int b = (rgb & 0xFF);
          int indx = (r+g+b)/3;
          int blackwhite = values[indx];
          Color outputColor = new Color(blackwhite, blackwhite, blackwhite);
          img.setRGB(x, y, outputColor.getRGB());
        }
      }
      return img;
    } 
    
    public static String serializeVector(int[] vector){
      if(vector.length != 256){
        System.err.println("invalid vector");
        System.exit(1);
      }
      String serialized = "";
      for(int i=0; i<256; i++){
        serialized += vector[i] + DELIM;
      }
      return serialized.trim();
    }
    
    public static int[] deserializeVector(String serialized){
      String[] split = serialized.split(DELIM);
      int[] deserialized = new int[256];
      if(split.length != 256){
        System.err.println("invalid vector");
        System.exit(1);
      }
      for(int i=0; i<256; i++){
        deserialized[i] = Integer.parseInt(split[i]);
      }
      return deserialized;
    }
}
