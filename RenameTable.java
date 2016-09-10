import java.io.*;
import java.lang.*;
import java.util.*;

class RenameTable {
	private int index;
	private String arch[];
	private String phy[];
	private String location[];

	RenameTable(){
		index = 0;
		arch = new String[8];
		phy = new String[8];
		location = new String[8];
	}

	void initialize(){
		for(int i = 0; i < 8; i++){
			arch[i] = "R"+i;
			phy[i] = "P"+i;
			location[i] = "P";
		}
	}

	String getPhysicalReg(String reg){
		String address = null;
		for(int i = 0;i < 8; i++){
			if(reg.equals(arch[i])){
				address = phy[i];
			}
		}
		return address;
	}

	String getLocation(String reg){
		String address = null;
		if(!reg.equals(null)){
			for(int i = 0;i < 8; i++){
				if(reg.equals(phy[i])){
					address = location[i];
				}
			}
		}
		return address;
	}

	void setLocation(String dst, String type){
		for(int i=0; i < 8; i++ ){
			if(arch[i].equals(dst)){
				location[i] = type;
			}
		}
	}

	void setPhyReg(String dst, String src){
		for(int i=0; i < 8; i++ ){
			if(arch[i].equals(dst)){
				phy[i] = src;
			}
		}
	}

	void display(){
		System.out.println("RENAME TABLE");
		for(int i = 0; i < 8 ; i++){
			System.out.print(arch[i]);
			System.out.print(" : "+phy[i]);
			System.out.println(" : "+location[i]);
			
		}
	}




}