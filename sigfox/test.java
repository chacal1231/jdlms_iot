import java.io.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;	

class jPython
{
    public static void main(String[] args)
     {
	try{
		//String prg = "import sys";
		//BufferedWriter out = new BufferedWriter(new FileWriter("./tx.py"));
		//out.write(prg);
		//out.close();
		Process p = Runtime.getRuntime().exec("python ./testp.py");
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                System.out.println(in.readLine());
		//String ret = in.readLine();
		//System.out.println("value is : "+ret);
        	System.out.println("Hello World");
	}catch(Exception e){}
     }
}
