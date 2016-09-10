import java.io.*;
import java.lang.*;

//import Simulate;

class Instruction{
	String Type; // Types
	String Status; // TBD
	String src1;
	String src2;
	String dst;
	String dstArch;
	String dstPhy;
	int count;
	int programCounter;

	String PipelineStage;

	Instruction(String type){
		this.Type = type;
		this.dst = null;
		this.src1 = null;
		this.src2 = null;	
		this.count = 0;
	}
	
	Instruction(String type, String src1){
		this.Type = type;
		this.dst = src1;
		this.src1 = null;
		this.src2 = null;
		this.dstArch = src1;
		this.dstPhy = src1;
		this.count = 0;
		this.PipelineStage = "TBP"; // To Be Processed
	}

	Instruction(String type, String dst, String src1){
		this.Type = type;
		this.dst = dst;
		this.dstArch = dst;
		this.dstPhy = dst;
		this.src1 = src1;
		this.src2 = null;
		this.count = 0;
		this.PipelineStage = "TBP"; // To Be Processed
	}
	
	Instruction(String type, String dst, String src1, String src2){
		this.Type = type;
		this.dst = dst;
		this.dstArch = dst;
		this.dstPhy = dst;
		this.src1 = src1;
		this.src2 = src2;
		this.count = 0;
		this.PipelineStage = "TBP"; // TO be Processed
	}


	public int getCount(){
		return count;
	}
	
	public void setCount(int count){
		this.count = count;
	}
	
	public String getType(){
		return Type;
	}

	public String getdstPhy(){
		return dstPhy;
	}
	
	public String getdstArch(){
		return dstArch;
	}
	
	public String getdst(){
		return dst;
	}

	public String getsrc1(){
		return src1;
	}

	public String getsrc2(){
		return src2;
	}
	
	public int getPC(){
		return programCounter;
	}

	public String getStage(){
		return PipelineStage;
	}

	public void setSrc1(String src){
		src1 = src;
	}

	public void setSrc2(String src){
		src2 = src;
	}

	public void setdst(String src){
		dst = src;
	}
	
	public void setdstArch(String src){
		dstArch = src;
	}

	public void setdstPhy(String src){
		dstPhy = src;
	}
	
	public void setStage(String stage){
		PipelineStage = stage;
	}
	
	public void setPC(int pc){
		programCounter = pc;
	}

	public String displayInst(){
		if(src2 != null)
			return Type+" "+dstPhy+" "+src1+" "+src2;
		else if(src1 != null)
			return Type+" "+dstPhy+" "+src1;
		else 
			return Type+" "+dstPhy;
	}
	
	public boolean isValid(){
		return true;
	}

	public boolean hasLiteral(){
		return (isNumeric(src1) || isNumeric(src2));
	}

	private boolean isNumeric(String str)
	{
		for (char c : str.toCharArray())
		{
			if (!Character.isDigit(c)) return false;
		}
		return true;
	}
}