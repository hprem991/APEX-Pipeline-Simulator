import java.io.*;
import java.lang.*;
import java.util.*;


class ReOrder{
	private int head, tail;
	private Instruction []instuctions;
	private String []rob;
	private boolean []status;
	boolean reuse;

	ReOrder(){
		head = tail = 0;
		rob = new String[16];
		instuctions = new Instruction[16];
		status = new boolean[16];
		reuse = false;
	}

	void initialize(){
		for(int i = 0; i < 16 ;i++){
			rob[i] = "P"+i;
			status[i] = true;
		}
	}

	boolean isAvailable(){
		if((reuse && (tail >= head)) || (!reuse && (tail <= head)))
			return true;
		else
			return false;
	}

	boolean canRemove(){
		if(Math.abs(head - tail) != 0){
			return true;	
		}
		return false;
	}

	void insert(String regNo, Instruction inst){
		if(head == 16 && tail != 0){
			head = 0;
			reuse = true;
		}
		instuctions[head] = inst;
		rob[head] = regNo;
		status[head] = false;
		head++;
	}

	void remove(){
		if(tail == 16){
			tail = 0;
			reuse = false;
		} 
		status[tail] = true;
		tail++;
	}

	boolean check(String regNo){
		for(int i = 0; i < 16; i++ ){
			if(rob[i].equals(regNo))
				return true;
		}
		return false;
	}

	String getFirst(){
		String element = null;
		if(canRemove())
			element = rob[tail];
		return element;
	}

	Instruction getInstruction(){
		return instuctions[tail];
	}

	String getRegister(){
		return rob[tail];
	} 

	void setStatus(String regNo, boolean flag){
		for(int i = 0; i < 16; i++ ){
			if(rob[i].equals(regNo))
				status[i] = flag;
		}
	}

	boolean getStatus(String regNo){
		boolean state = false;
		for(int i = 0; i < 16; i++ ){
			if(rob[i].equals(regNo))
				state = status[i];
		}
		return state;
	}

	void display(){
		System.out.println("ROB DISPLAY");
		//System.out.println("head "+head);
		//System.out.println("tail "+tail);
		for(int i = 0; i < 16 ; i++){
			System.out.print("rob "+rob[i]);
			System.out.println(": status "+status[i]);
		}
	}
}