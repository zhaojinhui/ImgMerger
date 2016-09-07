/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgmerger;


import static imgmerger.ImgMerger.props;
import static imgmerger.ImgMerger.PROPERTYFILENAME;
import static imgmerger.ImgMerger.logInfoArea;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;



/**
 *
 * @author Jinhui Zhao
 */
class PanelDragDropListener implements DropTargetListener{
    private static HashMap<String, String> imgMap = new HashMap<String, String>();
    private static String parentPath = null;
    private static String lPicPath;
    private static String rPicPath;
    private static HashSet<String> fileSet = new HashSet<String>();
    
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        ///throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        //System.out.println("drag over");
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        //System.out.println(dtde.getDropAction());
        //dtde.getDropAction();
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        //System.out.println("drag Exit");
        
    }
    
    private HashMap<String, List<File>> map = new HashMap<String, List<File>>();
    
    @Override
    public void drop(DropTargetDropEvent event) {
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        Date dateobj = new Date();
        ImgMerger.addLogInfo("start at " + dateobj.toString() + "\n");
        
        imgMap.clear();
        fileSet.clear();
        map.clear();
        props = new Properties();
        InputStream is;
        try {
            is = new FileInputStream(new File(PROPERTYFILENAME));
        } catch(Exception e) {
            is = null;
        }
        try {
            if(is == null)
                is = getClass().getResourceAsStream(PROPERTYFILENAME);
            props.load(is);
        } catch(Exception e) {
        }
        lPicPath = props.getProperty("leftPicPath");
        rPicPath = props.getProperty("rightPicPath");
        // Accept copy drops
        event.acceptDrop(DnDConstants.ACTION_COPY);
        // Get the transfer which can provide the dropped item data
        Transferable transferable = event.getTransferable();
        // Get the data formats of the dropped item
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        // Loop through the flavors
        for (DataFlavor flavor : flavors) {
            try {
                // If the drop items are files
                if (flavor.isFlavorJavaFileListType()) {

                    // Get all of the dropped files
                    List files = (List) transferable.getTransferData(flavor);

                    // Loop them through
                    for (int i=0;i<files.size();i++) {
                        File currentFile = (File)files.get(i);
                        parentPath = currentFile.getPath();
                        List<File> temp = new ArrayList<File>();
                        if(map.containsKey(parentPath)) {
                            temp = map.get(parentPath);
                            temp.add(currentFile);
                            map.put(parentPath, temp);
                        } else {
                            temp.add(currentFile);
                            map.put(parentPath, temp);
                        }
                    }
                }
            } catch (Exception e) {

                // Print out the error stack
                e.printStackTrace();

            }
        }
        Thread thread = new Thread(){
            public void run(){
                for(String key: map.keySet()){
                    List<File> temp = map.get(key);
                    for(File f: temp){
                        getImg(f);
                    }
                }
                //getImg(currentFile);
                DateFormat dfEnd = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
                Date dateobjEnd = new Date();
                //ImgMerger.setCaretPosition(currentObj.logInfo.getDocument().getLength());
//                String fileList = "";
//                for(String fileName: fileSet) {
//                    fileList = fileName + "\n";
//                }
                ImgMerger.addLogInfo("finish at " + dateobjEnd.toString() + "\n");
            }
        };
        thread.start();
        // Inform that the drop is complete
        event.dropComplete(true);
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
    }
    
    static void getImg(File file) {
        String fileName = file.getName();
        if(fileName.equals(lPicPath) || fileName.equals(rPicPath)) {
            String filePath = file.getPath();
            if(imgMap.containsKey(parentPath)) {
                if(fileName == lPicPath)
                    mergeImg(filePath, imgMap.get(parentPath));
                else
                    mergeImg(imgMap.get(parentPath), filePath);
            } else {
                imgMap.put(parentPath, filePath);
            }
            return;
        }
        if(!file.isDirectory())
            return;
        parentPath = file.getPath();
        File[] directoryList = file.listFiles();
        for(File child : directoryList) {
            getImg(child);
        }
    }
    // create the target image
    static private void mergeImg(String lpathName, String rpathName) {
        try {
            double lRotateDegree = Double.parseDouble(props.getProperty("lRotateDegree"));
            double rRotateDegree = Double.parseDouble(props.getProperty("rRotateDegree"));
            BufferedImage lPicture = rotateImage(loadImage(lpathName), lRotateDegree);
            BufferedImage rPicture = rotateImage(loadImage(rpathName), rRotateDegree);
            mergeImage(lPicture, rPicture, 38);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    // rotate target image according to the degree
    static private BufferedImage rotateImage (BufferedImage pic, double degree) {
        int w = pic.getWidth(),
            h = pic.getHeight();
        double  sin = Math.abs(Math.sin(Math.toRadians(degree))),
                cos = Math.abs(Math.cos(Math.toRadians(degree)));
        
        int neww = (int) Math.floor(w*cos + h*sin),
            newh = (int) Math.floor(h*cos + w*sin);
        BufferedImage dst = new BufferedImage(neww, newh, pic.getType());
        Graphics2D g2 = dst.createGraphics();
        g2.translate((neww-w)/2, (newh-h)/2);
        g2.rotate(Math.toRadians(degree), w/2, h/2);
        g2.drawImage(pic, null, 0, 0);
        g2.dispose();
        return dst;
    }
    
    // load image from specific file path
    static private BufferedImage loadImage (String fileName) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return img;
    }
    
    // merge left image and right image
    static private void mergeImage(BufferedImage lPic, BufferedImage rPic, int offSet) {
        try{
            int w = lPic.getWidth() + rPic.getWidth() + offSet;
            int h = Math.max(lPic.getHeight(), rPic.getHeight());
            BufferedImage dst = new BufferedImage(w, h, lPic.getType());
            Graphics2D g2 = dst.createGraphics();
            g2.drawImage(lPic, null, 0, 0);
            g2.drawImage(rPic, null, lPic.getWidth()+offSet, 0);
            BufferedImage logo = loadImage(props.getProperty("logoPath"));
            overlayImage(dst, logo);
            g2.dispose();
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    // put the logo and text on the top of the merged left picture and right picture
    static private void overlayImage(BufferedImage background, BufferedImage top) {
        try {
            float alpha = 1.0f;
            AlphaComposite ac;
            BufferedImage overlay = new BufferedImage(background.getWidth(), background.getHeight(),background.getType());
            Graphics2D g2 = overlay.createGraphics();
            ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            g2.drawImage(background, 0, 0, null);
            //g2.setComposite(ac);
            g2.drawImage(top, 6152, 4918, null);
            //g2.setComposite(ac);
            g2.setPaint(Color.black);
            g2.setFont(new Font("Calibri Light", Font.PLAIN, 90));
            //String[] strArray = parentPath.split("\\\\");
            String[] strArray = parentPath.split("/");
            int len = strArray.length;
            String date = strArray[len-3].substring(2);
            String s = "   [" + date + "]" + "\n" + strArray[len-2] + " #" + strArray[len-1];
            int w = top.getWidth();
            int tw = g2.getFontMetrics().stringWidth("  [" + date + "]" + "\n");
            int textTop = 6152 + w - tw-20;
            drawString(g2, s, textTop, 4753);
            String desFilePath = props.getProperty("outputPath");
            String dstFile = null;
            if(fileSet.contains(date)){
            } else {
                fileSet.add(date);
                ImgMerger.addLogInfo(date + "\n");
            }
            if(ImgMerger.getSaveStyle()) {
                //dstFile = desFilePath + "\\" + strArray[len-3].substring(2) + "-" + strArray[len-2] + "-" + strArray[len-1] + ".jpg";
                dstFile = desFilePath + "/" + date + "-" + strArray[len-2] + "-" + strArray[len-1] + ".jpg";
            } else {
                //dstFile = desFilePath + "\\" + strArray[len-3] + "\\" + strArray[len-2] + "\\" + strArray[len-1] + ".jpg";
                //(new File(desFilePath + "\\" + strArray[len-3] + "\\" + strArray[len-2])).mkdirs();
                dstFile = desFilePath + "/" + date + "/" + strArray[len-2] + "/" + strArray[len-1] + ".jpg";
                (new File(desFilePath + "/" + date + "/" + strArray[len-2])).mkdirs();
            }
            //DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            //Date date = new Date();
            //String logMsgm = "[" + dateFormat.format(date).toString() + "]   " + strArray[1] + " - " + strArray[2] + ".jpg succssfully exported!\n";
            try {
                ImageIO.write(overlay, "jpg", new File(dstFile));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            } 
            g2.dispose();
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    // enable the "\n" function in the img
    static private void drawString(Graphics g, String text, int x, int y) {
        for (String line : text.split("\n"))
            g.drawString(line, x, y += g.getFontMetrics().getHeight()- 20);
    }
}
