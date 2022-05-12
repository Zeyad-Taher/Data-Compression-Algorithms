package lz77;
import java.io.DataOutputStream;
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class LZ77 {
    
    static int windowSize=12;
    
    static boolean charFound(String data, int index){
        if(index==0){
            return false;
        }
        int i;
        if(index<windowSize){
            i=0;
        }else{
            i=index-windowSize;
        }
        
        while(i<index){
            if(data.charAt(i)==data.charAt(index)){
                return true;
            }
            else{
                i++;
            }
        }
        return false;
    }
    
    static void compress(File myInput) throws IOException{
        Scanner input = new Scanner(System.in);
        String data="";  
        int sizeBeforeCompress;
        int sizeAfterCompress;
        int numberOfTags=0;
        int positionBits=0,lengthBits=0;
        FileOutputStream fw=new FileOutputStream("D:\\Java\\LZ77\\src\\lz77\\compressed.txt");
        DataOutputStream dw=new DataOutputStream(fw);
        try {
            Scanner myReader = new Scanner(myInput);
            while (myReader.hasNextLine()) {
                data = myReader.nextLine();
            }
            myReader.close();
        } 
        catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        
        sizeBeforeCompress=data.length()*7;
        
        for(int i=0;i<data.length();i++){
            int position=0;
            int length=0;
            char nextChar=' ';
            boolean found=charFound(data,i);
            if(!found){
                position=0;
                length=0;
                nextChar=data.charAt(i);
            }
            else{
                String searchWindow="";
                int lengthDifference=0;
                int j;
                if(i<windowSize){
                    j=0;
                }
                else{
                    j=i-windowSize;
                    lengthDifference=j;
                }
                while(j<i){
                    searchWindow+=data.charAt(j);
                    j++;
                }
                String prefix="";
                j=i;
                while(j<data.length()){
                    prefix+=data.charAt(j);
                    length++;
                    j++;
                    if(searchWindow.lastIndexOf(prefix)!=-1){
                        position=i-(searchWindow.lastIndexOf(prefix)+lengthDifference);
                    }
                    else{
                        j--;
                        if(j<data.length()){
                            nextChar=data.charAt(j);
                        }
                        length--;
                        break;
                    }
                }
                i=j;
            }
            int charAscii=0;
            if(nextChar==' '){
                String lastChar="NULL";
                System.out.println("<"+position+", "+length+", "+lastChar+">");
            }
            else{
                System.out.println("<"+position+", "+length+", \""+nextChar+"\">");
                charAscii=nextChar;
            }
            dw.writeBytes(Integer.toBinaryString(position));
            dw.writeBytes(" ");
            dw.writeBytes(Integer.toBinaryString(length));
            dw.writeBytes(" ");
            dw.writeBytes(Integer.toBinaryString(charAscii));
            if(i<data.length()-1){
                dw.writeBytes("\n");
            }
            numberOfTags++;
            if(Integer.toBinaryString(position).length()>positionBits){
                positionBits=Integer.toBinaryString(position).length();
            }
            if(Integer.toBinaryString(length).length()>lengthBits){
                lengthBits=Integer.toBinaryString(length).length();
            }
        }
        sizeAfterCompress=numberOfTags*(positionBits+lengthBits+7);
        dw.close();
        fw.close();
        File myOutput = new File("D:\\Java\\LZ77\\src\\lz77\\compressed.txt");
        if (myOutput.exists()) {
            System.out.println("File has been compressed!");
            System.out.println("File size before compression = "+sizeBeforeCompress+" Bits");
            System.out.println("File size after compression = "+sizeAfterCompress+" Bits");
        }
    }
    
    static void decompress(){
        String dataRead="";
        String data="";
        int position,length;
        char nextChar;
        try {
            File file = new File("D:\\Java\\LZ77\\src\\lz77\\compressed.txt");
            Scanner myReader = new Scanner(file);
            while (myReader.hasNextLine()) {
                dataRead = myReader.nextLine();
                String[] arrOfData=dataRead.split(" ");
                position=Integer.parseInt(arrOfData[0],2);
                length=Integer.parseInt(arrOfData[1],2);
                nextChar=(char) Integer.parseInt(arrOfData[2],2);
                
                String subString="";
                int size=data.length();
                for(int j=0;j<length;j++){
                    subString+=data.charAt(size-position);
                    size++;
                }
                subString+=nextChar;
                data+=subString;
            }
            System.out.println("Decompressed data: "+data);
            myReader.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws IOException {
        File file = new File("D:\\Java\\LZ77\\src\\lz77\\data.txt");
        compress(file);
        decompress();
    }
}