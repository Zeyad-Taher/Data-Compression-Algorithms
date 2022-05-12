package vectorquantization;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.FileWriter;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 *
 * @author Zeyad
 */

class Image {
    private int imageWidth;
    private int imageHeight;
    private int[][] imagePixels;

    public Image(String path) throws IOException {
        imagePixels=imageToPixels(path);
    }

    public int[][] imageToPixels(String imagePath) throws IOException {
        BufferedImage image=null;
        File input_image=new File(imagePath);
        image= ImageIO.read(input_image);
        imageWidth=image.getWidth();
        imageHeight=image.getHeight();
        int[][] imagePixels=new int[imageWidth][imageHeight];
        for (int i=0;i<imageWidth;i++){
            for (int j=0;j<imageHeight;j++){
                imagePixels[i][j]=image.getRGB(i,j) & 255;
            }
        }
        System.out.println("Reading complete.");
        return imagePixels;
    }

    public static void writeImage(int image[][], int width, int height) throws IOException {
        File newImage = new File(System.getProperty("user.dir")+"\\newImage.jpg");
        BufferedImage compressedImage = new BufferedImage(width, height,BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                compressedImage.setRGB(x, y, (image[x][y] << 16) | (image[x][y] << 8) | (image[x][y]));
            }
        }
        ImageIO.write(compressedImage, "jpg", newImage);
        System.out.println("Writing complete.");
    }

    public int getImageWidth(){
        return imageWidth;
    }

    public int getImageHeight(){
        return imageHeight;
    }

    public int[][] getImagePixels() {
        return imagePixels;
    }
}

class VectorQuantization {

    private Image image;
    private int[][] imagePixels;
    private ArrayList<ArrayList<Integer>> blocks = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> codeBook = new ArrayList<>();
    private ArrayList<Integer> encoded=new ArrayList<>();
    private int scaledHeight;
    private int scaledWidth;
    private int blockWidth;
    private int blockHeight;
    private int[][] scaledImage;
    private int blockSize;
    private int codeBookSize;
    File codeBookFile;
    File encodedFile;

    public VectorQuantization(String path,int blockWidth,int blockHeight,int codeBookSize) throws IOException {
        image=new Image(path);
        this.codeBookSize=codeBookSize;
        this.blockWidth=blockWidth;
        this.blockHeight=blockHeight;
        blockSize=blockWidth*blockHeight;
        imagePixels=image.getImagePixels();
        scaledWidth = image.getImageWidth() % blockWidth == 0 ? image.getImageWidth() : ((image.getImageWidth() / blockWidth) + 1) * blockWidth;
        scaledHeight = image.getImageHeight() % blockHeight == 0 ? image.getImageHeight() : ((image.getImageHeight() / blockHeight) + 1) * blockHeight;
        scaledImage = new int[scaledWidth][scaledHeight];
        for (int i = 0; i < scaledWidth; i++) {
            int x = i >= image.getImageWidth() ? image.getImageWidth() - 1 : i;
            for (int j = 0; j < scaledHeight; j++) {
                int y = j >= image.getImageHeight() ? image.getImageHeight() - 1 : j;
                scaledImage[i][j] = imagePixels[x][y];
            }
        }
        codeBookFile = new File("codeBook.txt");
        encodedFile = new File("encodedImage.txt");
    }

    public void constructBlocks() {
        for (int i = 0; i < scaledWidth; i += blockWidth) {
            for (int j = 0; j < scaledHeight; j += blockHeight) {
                ArrayList<Integer> newBlock = new ArrayList<Integer>();
                for (int x = i; x < i + blockWidth; x++) {
                    for (int y = j; y < j + blockHeight; y++) {
                        newBlock.add(scaledImage[x][y]);
                    }
                }
                blocks.add(newBlock);
            }
        }
    }

    public ArrayList<Integer> getAverageVector(ArrayList<ArrayList<Integer>> blocks) {
        ArrayList<Integer> averageBlock = new ArrayList<Integer>();
        for (int i = 0; i < blockSize; i++) { // pixels in internal block
            int sum = 0,average=0;
            for (int j = 0; j < blocks.size(); j++) { // each pixel in the constructed block
                int num = blocks.get(j).get(i);
                sum += num;
            }
            average = sum / blocks.size();
            averageBlock.add(average);
        }
        return averageBlock;
    }

    public void split() {
        ArrayList<ArrayList<Integer>> newCodeBook = new ArrayList<>();
        for (ArrayList<Integer> cb : codeBook) {
            ArrayList<Integer> newBlock1 = new ArrayList<Integer>();
            ArrayList<Integer> newBlock2 = new ArrayList<Integer>();
            for (int i = 0; i < cb.size(); i++) {
                newBlock1.add(cb.get(i) + 1);
                newBlock2.add(cb.get(i) - 1);
            }
            newCodeBook.add(newBlock1);
            newCodeBook.add(newBlock2);
        }
        codeBook=newCodeBook;
    }

    public int getDistance(ArrayList<Integer> x, ArrayList<Integer> y)
    {
        int distance = 0;
        for (int i = 0; i < x.size(); i++)
            distance += Math.pow(x.get(i) - y.get(i), 2);
        return (int) Math.sqrt(distance);
    }

    public ArrayList<ArrayList<ArrayList<Integer>>> getNearset(){
        ArrayList<ArrayList<ArrayList<Integer>>> nearset = new ArrayList<>();
        for (int i = 0; i < codeBook.size(); i++) {
            nearset.add(new ArrayList<ArrayList<Integer>>());
        }
        for (int i = 0; i < blocks.size(); i++) {
            int minimumDistance = Integer.MAX_VALUE;
            int nearsetIndex = 0; //nearest codeBook block index
            for (int j = 0; j < codeBook.size(); j++) {
                int distance = getDistance(codeBook.get(j),blocks.get(i));
                if(distance<minimumDistance){
                    minimumDistance = distance;
                    nearsetIndex = j;
                }
            }
            nearset.get(nearsetIndex).add(blocks.get(i));
        }
        return nearset;
    }

    public void quantize() throws IOException {
        codeBook.add(getAverageVector(blocks));
        while(codeBookSize>codeBook.size()){
            split();
            ArrayList<ArrayList<ArrayList<Integer>>> nearsetVectors = getNearset();
            for (int i = 0; i < codeBook.size(); i++) {
                if(nearsetVectors.get(i).size()>0)
                    codeBook.set(i,getAverageVector(nearsetVectors.get(i)));
            }
        }
        ArrayList<ArrayList<Integer>> previousCodeBook = codeBook;
        while(true){
            ArrayList<ArrayList<ArrayList<Integer>>> nearset = getNearset();
            for (int i = 0; i < codeBook.size(); i++) {
                if(nearset.get(i).size()>0)
                    codeBook.set(i,getAverageVector(nearset.get(i)));
            }
            if(codeBook.equals(previousCodeBook)){
                break;
            }
            previousCodeBook = codeBook;
        }
        FileWriter myWriter = new FileWriter("codeBookFile.txt");
        for (int i=0;i<codeBook.size();i++){
            for(int j=0;j<blockSize;j++){
                myWriter.write(codeBook.get(i).get(j)+" ");
            }
            myWriter.write(",");
        }
        myWriter.close();
    }

    public void encode() throws IOException {
        FileWriter myWriter = new FileWriter("encodedFile.txt");
        for (int i = 0; i < blocks.size(); i++) {
            int minimumDistance = Integer.MAX_VALUE;
            int nearsetIndex = 0;
            for (int j = 0; j < codeBook.size(); j++) {
                int distance = getDistance(codeBook.get(j),blocks.get(i));
                if(distance<minimumDistance){
                    minimumDistance = distance;
                    nearsetIndex = j;
                }
            }
            encoded.add(nearsetIndex);
            myWriter.write(nearsetIndex+" ");
        }
        myWriter.close();
    }

    public void decode() throws IOException {
        ArrayList<ArrayList<Integer>> readCodeBook=new ArrayList();
        ArrayList<Integer> readEncoded = new ArrayList();
        File myCodeBook = new File("D:\\Java\\VectorQuantization\\codeBookFile.txt");
        Scanner myCodeBookReader = new Scanner(myCodeBook);
        String data="";
        while (myCodeBookReader.hasNextLine()) {
            data += myCodeBookReader.nextLine();
        }
        myCodeBookReader.close();
        for(int i=0;i<data.length();i++){
            if(data.charAt(i)==','){
                readCodeBook.add(new ArrayList());
            }
        }
        for(int i=0;i<data.length();i++){
            int j=0;
            if (data.charAt(i)==','){
                j++;
            }
            else if(data.charAt(i)!=' '){
                readCodeBook.get(j).add(data.charAt(i)-'0');
            }
        }
        File myEncoded = new File("D:\\Java\\VectorQuantization\\encodedFile.txt");
        Scanner myEncodedReader = new Scanner(myEncoded);
        String dataEncoded="";
        while (myEncodedReader.hasNextLine()) {
            dataEncoded += myEncodedReader.nextLine();
        }
        myEncodedReader.close();
        
        for(int i=0;i<dataEncoded.length();i++){
            if(dataEncoded.charAt(i)!=' '){
                readEncoded.add(dataEncoded.charAt(i)-'0');
            }
        }
        
        int[][] newImg = new int[scaledWidth][scaledHeight];
        for (int i = 0; i < encoded.size(); i++) {
            int x = i / (scaledHeight/ blockHeight);
            int y = i % (scaledHeight / blockHeight);
            x *= blockWidth;
            y *= blockHeight;
            int v = 0;
            for (int j = x; j < x + blockWidth; j++) {
                for (int k = y; k < y + blockHeight; k++) {
                    newImg[j][k] = codeBook.get(encoded.get(i)).get(v);
                    v++;
                }
            }
        }
        Image.writeImage(newImg, image.getImageWidth(),image.getImageHeight());
    }

}

public class GUI extends javax.swing.JFrame {

    private final JFileChooser openFileChooser;
    private File image;
    private String imagePath;
    private ImageIcon inputImage,outputImage;
    private int blockWidth,blockHeight,codeBookSize;
    private VectorQuantization vq;
    /**
     * Creates new form GUI
     */
    public GUI() {
        initComponents();
        openFileChooser=new JFileChooser();
        openFileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        openFileChooser.setFileFilter(new FileNameExtensionFilter("Images","jpg","png","jpeg"));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        BlockWidthSpinner = new javax.swing.JSpinner();
        BlockWidthMessage = new javax.swing.JLabel();
        BlockHeightMessage = new javax.swing.JLabel();
        BlockHeightSpinner = new javax.swing.JSpinner();
        CodeBookSizeMessage = new javax.swing.JLabel();
        CodeBookSizeSpinner = new javax.swing.JSpinner();
        BrowseButton = new javax.swing.JButton();
        ImageMessage = new javax.swing.JLabel();
        CompressButton = new javax.swing.JButton();
        OutputImage = new javax.swing.JLabel();
        InputImage = new javax.swing.JLabel();
        inputImageMessage = new javax.swing.JLabel();
        outputImageMessage = new javax.swing.JLabel();
        DecompressButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        BlockWidthMessage.setText("Block Width:");

        BlockHeightMessage.setText("Block Height:");

        CodeBookSizeMessage.setText("Code Book Size:");

        BrowseButton.setText("Browse");
        BrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BrowseButtonActionPerformed(evt);
            }
        });

        ImageMessage.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        CompressButton.setText("Compress");
        CompressButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CompressButtonActionPerformed(evt);
            }
        });

        OutputImage.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        InputImage.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        inputImageMessage.setText("Input Image");

        outputImageMessage.setText("Output Image");

        DecompressButton.setText("Decompress");
        DecompressButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DecompressButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 19, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(ImageMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 569, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)
                        .addComponent(BrowseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(InputImage, javax.swing.GroupLayout.PREFERRED_SIZE, 324, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(OutputImage, javax.swing.GroupLayout.PREFERRED_SIZE, 323, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(19, 19, 19))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(112, 112, 112)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(BlockWidthMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(BlockWidthSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(46, 46, 46)
                                .addComponent(BlockHeightMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(BlockHeightSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(37, 37, 37)
                                .addComponent(CodeBookSizeMessage)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CodeBookSizeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGap(57, 57, 57)
                                .addComponent(CompressButton, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(74, 74, 74)
                                .addComponent(DecompressButton, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(140, 140, 140)
                        .addComponent(inputImageMessage)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(outputImageMessage)
                        .addGap(11, 11, 11)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(BrowseButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ImageMessage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(BlockWidthMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(BlockHeightSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(CodeBookSizeMessage)
                        .addComponent(CodeBookSizeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(BlockHeightMessage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(BlockWidthSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CompressButton, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(DecompressButton, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(inputImageMessage)
                    .addComponent(outputImageMessage))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(OutputImage, javax.swing.GroupLayout.DEFAULT_SIZE, 293, Short.MAX_VALUE)
                    .addComponent(InputImage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(21, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void BrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BrowseButtonActionPerformed
        // TODO add your handling code here:
        int returnValue=openFileChooser.showOpenDialog(this);
        if(returnValue==JFileChooser.APPROVE_OPTION){
            image=openFileChooser.getSelectedFile();
            imagePath=image.getAbsolutePath();
            ImageMessage.setText("Image has successfully loaded!");
            inputImage=new ImageIcon(imagePath);
            java.awt.Image img=inputImage.getImage().getScaledInstance(InputImage.getWidth(), InputImage.getHeight(),java.awt.Image.SCALE_SMOOTH);
            InputImage.setIcon(new ImageIcon(img));
        }
        else{
            ImageMessage.setText("No image chosen!");
        }
    }//GEN-LAST:event_BrowseButtonActionPerformed

    private void CompressButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CompressButtonActionPerformed
        try {
            // TODO add your handling code here:
            blockWidth=(int) BlockWidthSpinner.getValue();
            blockHeight=(int) BlockHeightSpinner.getValue();
            codeBookSize=(int) CodeBookSizeSpinner.getValue();
            vq=new VectorQuantization(imagePath,blockWidth,blockHeight,codeBookSize);
            vq.constructBlocks();
            vq.quantize();
            vq.encode();
            JFrame frame = new JFrame("Swing Tester");
            JOptionPane.showMessageDialog(frame, "Image has been compressed");
        } catch (IOException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_CompressButtonActionPerformed

    private void DecompressButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DecompressButtonActionPerformed
        try {
            // TODO add your handling code here:
            vq.decode();
            outputImage=new ImageIcon(System.getProperty("user.dir")+"\\newImage.jpg");
            java.awt.Image newImg=outputImage.getImage().getScaledInstance(OutputImage.getWidth(), OutputImage.getHeight(),java.awt.Image.SCALE_SMOOTH);
            OutputImage.setIcon(new ImageIcon(newImg));
        } catch (IOException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_DecompressButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                GUI gui=new GUI();
                gui.setVisible(true);
                gui.setTitle("Vector Quantization");
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel BlockHeightMessage;
    private javax.swing.JSpinner BlockHeightSpinner;
    private javax.swing.JLabel BlockWidthMessage;
    private javax.swing.JSpinner BlockWidthSpinner;
    private javax.swing.JButton BrowseButton;
    private javax.swing.JLabel CodeBookSizeMessage;
    private javax.swing.JSpinner CodeBookSizeSpinner;
    private javax.swing.JButton CompressButton;
    private javax.swing.JButton DecompressButton;
    private javax.swing.JLabel ImageMessage;
    private javax.swing.JLabel InputImage;
    private javax.swing.JLabel OutputImage;
    private javax.swing.JLabel inputImageMessage;
    private javax.swing.JLabel outputImageMessage;
    // End of variables declaration//GEN-END:variables
}
