/*
 * Copyright (C) 2007-2008  James Cook University (http://www.jcu.edu.au).
 * 
 * This program was developed as part of the ARCHER project (Australian
 * (Research Enabling Environment) funded by a Systemic Infrastructure
 * Initiative (SII) grant and supported by the Australian Department of
 * Innovation, Industry, Science and Research.

 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the above
 * copyright notice and the following two paragraphs appear in all copies
 * of this software.

 * IN NO EVENT SHALL THE JAMES COOK UNIVERSITY BE LIABLE TO ANY PARTY FOR 
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING
 *  OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE 
 * JAMES COOK UNIVERSITY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * THE JAMES COOK UNIVERSITY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE JAMES COOK UNIVERSITY 
 * HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS.
 */

package au.edu.jcu.kepler.hydrant;

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.log4j.Logger;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;

public class ReplacementManager extends StringAttribute {
	
	private String logDir = null;
	private String exeResultDir = null;
	static Logger logger = Logger.getLogger(ReplacementManager.class.getName());
	
	
	public ReplacementManager(NamedObj container, String name, String logDir, String exeResultDir)
    throws NameDuplicationException, IllegalActionException {
		super(container, name);
		this.logDir = logDir;
		this.exeResultDir = exeResultDir;
	}
	
	/** Set the output directory. */
	public void setOutputDirectory(String directory) {
		exeResultDir = directory;
	}
	
	//public void writePythonData(PyDictionary data);

	/*public void writeData(HashMap data_map) {
		PyDictionary d = new PyDictionary();
		for (Object key: data_map.keySet()) {
			if ((key instanceof String)) {
				Object value = data_map.get(key);
				if (value instanceof String) {
					d.__setitem__((String)key, new PyString((String)value));
				} else if (value instanceof ByteArrayOutputStream) {
					PyArray pa = new PyArray(byte.class, ((ByteArrayOutputStream)value).toByteArray());
					d.__setitem__((String)key, pa);
				}
			}
		}
		writePythonData(d);
	}*/
	
	
	public void writeData(HashMap data_map) {
		try {

			 File file = new File(logDir + this.getValueAsString()+ File.separator + exeResultDir);
			 if(!file.exists())
				 file.mkdirs();

			String dataName = (String)data_map.get("name");
			dataName = dataName.replaceAll(" ", "");
			if (dataName.startsWith("."))
				dataName = dataName.substring(1);
			String dataType = (String)data_map.get("type");
			logger.debug("man value:"+this.getValueAsString());
			//System.out.println("data_map value:"+data_map);
			//int jobExeIndex = PropertyUtil.getInstance().getNewJobExeIndex("D:/wfExeIndex.properties","JobExeIndex");
			logger.debug("dir info:"+file.getAbsolutePath());
	         
			//text output
			if (data_map.containsKey("output"))
			{
				boolean append = true;
				String data = (String)data_map.get("output");
				File outputFile = new File(logDir + this.getValueAsString()+ File.separator + exeResultDir + File.separator + dataName + "." + dataType);
				if (data_map.containsKey("append"))
					append = (Boolean) data_map.get("append");
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, append)));
				pw.println(data);
//				System.out.println("append data:" + data + ", to file:" + outputFile);
//				System.out.println("-----");
//				pw.append(data);
				pw.flush();
				pw.close();
			}
			//plot image
			else if (data_map.containsKey("plotOutput"))
			{
				String fileFormat = (String)data_map.get("format");
				File outputFile = new File(logDir + this.getValueAsString()+ File.separator + exeResultDir + File.separator + dataName + "." + fileFormat);
				if(outputFile.exists())
				//add time stamp to duplicate output files
				{
					outputFile = new File(logDir + this.getValueAsString() + File.separator + exeResultDir + File.separator + dataName + "_" + getTimeStamp() +"." + fileFormat);
					
				}
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
				//PrintWriter pw = new PrintWriter( new BufferedOutputStream(new FileOutputStream(WriteDataDir+this.getValueAsString()+"//" + dataName + "."+fileFormat)));
		        ((ByteArrayOutputStream) data_map.get("plotOutput")).writeTo(out);
		        out.flush();
		        out.close();
		        //pw.print(data_map.get("plotOutput"));
		        //((CharArrayWriter)data_map.get("plotOutput")).writeTo(pw);
		        //pw.writeTo(out)
		        //pw.flush();
		        //pw.close();
		        logger.debug("png file info:"+file.getAbsolutePath());				
			}
			//awt image
			else if(data_map.containsKey("imageData"))
			{
				Image img = (Image)data_map.get("imageData");
				File outputFile = new File(logDir + this.getValueAsString() + File.separator + exeResultDir + File.separator + dataName + "." + dataType);
				if(outputFile.exists())
				//add time stamp to duplicate output files
				{
					outputFile = new File(logDir + this.getValueAsString() + File.separator + exeResultDir + File.separator + dataName + "_" + getTimeStamp() +"." + dataType);
					
				}
				writePNG(img,outputFile.toString());
			}
			//url image
			else if (data_map.containsKey("url"))
			{
				String fileFormat = (String)data_map.get("format");
				URL url = (URL)data_map.get("url");
	        	File outputFile = new File(logDir + this.getValueAsString() + File.separator + exeResultDir + File.separator + dataName + "." + fileFormat);
				if(outputFile.exists())
				//add time stamp to duplicate output files
				{
					outputFile = new File(logDir + this.getValueAsString() + File.separator + exeResultDir + File.separator + dataName + "_" + getTimeStamp() +"." + fileFormat);
					
				}
	        	Image image = null;
	        	try {
	        	    image = ImageIO.read(url);
	        		} catch (IOException e) {
	        			e.printStackTrace();
	        	}
	        	write(image, outputFile.toString(), fileFormat);
			}
			//other image file
			else if(data_map.containsKey("filename"))
			{
				
				logger.debug("filetype:"+dataType);
				
				//String fileFormat = (String)data_map.get("format");
				String fileName = (String)data_map.get("filename");
				logger.debug("fileName:"+fileName);
				File sourceFile = new File (fileName);
				fileName = fileName.substring(fileName.lastIndexOf(File.separator));
				File targetFile = new File(logDir + this.getValueAsString() + File.separator + exeResultDir + File.separator + fileName);
				if(targetFile.exists())
				//add time stamp to duplicate output files
				{
					String filePreffix = fileName.substring(0, fileName.lastIndexOf("."));
					String fileSuffix = fileName.substring(fileName.lastIndexOf(".") + 1);
					targetFile = new File(logDir + this.getValueAsString() + File.separator + exeResultDir + File.separator +filePreffix + "_" + getTimeStamp() +"." + fileSuffix);					
				}				
				this.copyFile(sourceFile, targetFile);
				logger.debug("image file info:"+targetFile.getAbsolutePath());			
			}				 
	         
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void copyFile(File sourceFile, File targetFile)
	{
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(sourceFile));
			byte[] tempch;
			try {
				tempch = new byte[in.available()];
				in.read(tempch);
				out.write(tempch);
				out.flush();
				out.close();
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * write a swt Image into a png file
	 * @param img the swt Image to write
	 * @param filename name of the png file that will be created
	 */
	public static void writePNG(Image img, String filename) throws IOException {
		write(img, filename, "png");
	}
	
	/**
	 * write a swt Image into a png file
	 * @param img the swt Image to write
	 * @param filename name of the png file that will be created
	 */
	public static void writeJPG(Image img, String filename) throws IOException {
		write(img, filename, "jpg");
	}
	
	
	public static void write(Image img, String fileName, String format) 
	throws IOException {
		BufferedImage buffi = toBufferedImage(img);
		File outputFile = new File(fileName);
		ImageIO.write(buffi, format, outputFile);
	
	}
	
	
    // This method returns a buffered image with the contents of an image
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }
    
        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();
    
        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {    
            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                image.getWidth(null), image.getHeight(null));
        } catch (HeadlessException e) {
            // The system does not have a screen
        }
    
        if (bimage == null) {
            // Create a buffered image using the default color model
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        }
    
        // Copy image to buffered image
        Graphics g = bimage.createGraphics();
    
        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();
    
        return bimage;
    }
    
    private long getTimeStamp(){
      Date date = new Date(System.currentTimeMillis());
      return date.getTime();	
    }
		
}
