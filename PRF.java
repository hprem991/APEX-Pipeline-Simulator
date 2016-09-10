import java.io.*;
import java.lang.*;
import java.util.*;

class PRF{
	private int index;
	private String []registers;
	private String []value;
	private boolean []valid;

	PRF(){
		index = 0;
		registers = new String[16];
		valid = new boolean[16];
		value = new String[16];
	}

	void init(){
		for(int i = 0; i < 8;i++){
			registers[i] = "P"+i;
			valid[i] = false;
			value[i] = "0";
		}	
		for(int i = 8; i < 16;i++){
			registers[i] = "P"+i;
			valid[i] = true;
			value[i] = "0";
		}
	}

	boolean canAllocate(){
		for(int i = 0; i < 16 ; i++){
			if(valid[i])
				return true;
		}
		System.out.println("NO PRF");
		return false;
	}

	String allocate(){
		String register = null;
		for(int i = 0; i < 16 ; i++){
			if(valid[i]) {
				valid[i] = false;
				register = registers[i];
				break;
			}
		} 
		return register;
	}

	void deAllocate(String reg){
		for(int i = 0; i < 16 ; i++){
			if(registers[i].equals(reg)) {
				valid[i] = true;
			}
		} 
	}
	
	boolean getValid(String reg){
		boolean validity = false;
		for(int i = 0; i < 16 ; i++){
			if(registers[i].equals(reg)) {
				validity = valid[i];
			}
		} 
		return validity;
	}
	
	void setValid(String reg){
		for(int i = 0; i < 16 ; i++){
			if(registers[i].equals(reg)) {
				valid[i] = true;
			}
		} 
	}

	String getValue(String reg){
		String content = null;
		for(int i = 0; i < 16 ; i++){
			if(registers[i].equals(reg)) {
				content = value[i];
			}
		}
		return content;
	}
	
	void setValue(String reg, String val){
		String content = null;
		for(int i = 0; i < 16 ; i++){
			if(registers[i].equals(reg)) {
				value[i] = val;
			}
		}
	}
	
	List<String> getAllocatedRegisters(){
		List<String> regList = new ArrayList<String>();
		for(int i = 0; i<16; i++){
			if(!valid[i])
				regList.add(registers[i]);
		}
		return regList;
	}
	
	void display(){
		System.out.println("PRF DISPLAY");
		for(int i = 0; i < 16 ; i++){
			System.out.print(registers[i]);
			System.out.print(" : "+value[i]);
			System.out.println(": Valid bit "+valid[i]);
			}
		}
}