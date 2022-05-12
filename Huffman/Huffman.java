package huffman;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Scanner;

class HuffmanNode { 
    int frequency;
    char dataChar; 
    HuffmanNode left;
    HuffmanNode right;
}

class MyComparator implements Comparator<HuffmanNode> {
    @Override
    public int compare(HuffmanNode o1, HuffmanNode o2) {
        return o1.frequency-o2.frequency;
    }
}

public class Huffman {
    
    public static int uncompressedSize=0,compressedSize;
    
    public static void printCode(HuffmanNode root, String s,DataOutputStream dw,ArrayList<Character> dictionaryChar,ArrayList<String> dictionaryCode) throws IOException
    {      
        if (root.left== null && root.right== null) {
            dw.writeBytes(root.dataChar + "," + s + ",");
            dictionaryChar.add(root.dataChar);
            dictionaryCode.add(s);
            return;
        }
        printCode(root.left, s + "0",dw,dictionaryChar,dictionaryCode);
        printCode(root.right, s + "1",dw,dictionaryChar,dictionaryCode);
    }
    
    public static void sort(ArrayList<Character> chars,ArrayList<Integer> frequency){
       for(int i=0;i<frequency.size();i++){
            for(int j=0;j<frequency.size();j++){
                if(frequency.get(i)>frequency.get(j)){
                    int intTemp=frequency.get(j);
                    char charTemp=chars.get(j);
                    frequency.set(j,frequency.get(i));
                    chars.set(j,chars.get(i));
                    frequency.set(i,intTemp);
                    chars.set(i,charTemp);
                }
            }
        }
    }
    
    public static String data="";
    public static ArrayList<Character> dictionaryChar=new ArrayList<Character>();
    public static ArrayList<String> dictionaryCode=new ArrayList<String>();
    public static void compress(File file) throws IOException{
        FileOutputStream fw=new FileOutputStream("D:\\Java\\Huffman\\src\\huffman\\compressed.txt");
        DataOutputStream dw=new DataOutputStream(fw);
        dw.writeBytes("Dictionary: \n");
        try {
            Scanner myReader = new Scanner(file);
            while (myReader.hasNextLine()) {
                data = myReader.nextLine();
            }
            myReader.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        ArrayList <Character> chars = new ArrayList<Character>();
        ArrayList <Integer> frequency = new ArrayList<Integer>();
        Scanner input =new Scanner(System.in);
        for(int i=0;i<data.length();i++){
            char current=data.charAt(i);
            if(chars.isEmpty() || chars.lastIndexOf(current)==-1){
                chars.add(current);
                frequency.add(1);
            }
            else{
                int value=frequency.get(chars.lastIndexOf(current));
                value++;
                frequency.set(chars.lastIndexOf(current), value);
            }
        }
        
        sort(chars,frequency);
        System.out.println("Character   Count");
        for(int i=0;i<frequency.size();i++){
            System.out.println("    "+chars.get(i)+"        "+frequency.get(i));
        }
        
        int numOfBits=(int) Math.ceil(Math.log(chars.size()) / Math.log(2));
        uncompressedSize=data.length()*numOfBits;
        
        int size=chars.size();
        PriorityQueue<HuffmanNode> pqueue = new PriorityQueue<HuffmanNode>(size, new MyComparator());
        
        for (int i = 0; i < size; i++) {
            HuffmanNode hn = new HuffmanNode();

            hn.dataChar = chars.get(i);
            hn.frequency = frequency.get(i);

            hn.left = null;
            hn.right = null;
            
            pqueue.add(hn);
        }
        HuffmanNode root = null;
        
        while (pqueue.size() > 1) {
            HuffmanNode x = pqueue.peek();
            // Retrieves, but does not remove, the head of this queue, or returns null if this queue is empty.
            pqueue.poll();
            // Retrieves and removes the head of this queue, or returns null if this queue is empty.
            HuffmanNode y = pqueue.peek();
            pqueue.poll();
            HuffmanNode f = new HuffmanNode();
            f.frequency = x.frequency + y.frequency;
            f.dataChar = '-';
            f.left = x;
            f.right = y;
            root = f;
            pqueue.add(f);
        }
        printCode(root, "",dw,dictionaryChar,dictionaryCode);
        
        dw.writeBytes("\nCompressed:\n");
        for(int i=0;i<data.length();i++){
            int Indexcode=dictionaryChar.lastIndexOf(data.charAt(i));
            String charCode=dictionaryCode.get(Indexcode);
            dw.writeBytes(charCode);
        }
        dw.close();
        fw.close();
        compressedSize=0;
        for(int i=0;i<chars.size();i++){
            int count=frequency.get(chars.lastIndexOf(dictionaryChar.get(i)));
            compressedSize+=count*dictionaryCode.get(i).length();
        }
    }
    public static String decompressed="";
    public static void decompress() throws IOException{
        FileOutputStream fw=new FileOutputStream("D:\\Java\\Huffman\\src\\huffman\\decompressed.txt");
        DataOutputStream dw=new DataOutputStream(fw);
        String compressedData;
        String dictionary;
        String code="";
         try {
            dictionary=Files.readAllLines(Paths.get("D:\\Java\\Huffman\\src\\huffman\\compressed.txt")).get(1);
            compressedData=Files.readAllLines(Paths.get("D:\\Java\\Huffman\\src\\huffman\\compressed.txt")).get(3);
            String[] charCode=dictionary.split(",");
            int k=0;
            while(k<compressedData.length()){
                code+=compressedData.charAt(k);
                k++;
                for(int i=1;i<charCode.length;i+=2){
                    if(code.equals(charCode[i])){
                        decompressed+=charCode[i-1];
                        code="";
                    }
                }
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
        }
        dw.writeBytes(decompressed);
        dw.close();
        fw.close();
    }
    
    public static void main(String[] args) throws IOException {
        NewJFrame gui=new NewJFrame();
        gui.setVisible(true);
    }
}