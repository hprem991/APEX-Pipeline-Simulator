import java.io.*;
import java.lang.*;
import java.util.*;

//package Simulate;

class Simulator{
	static int PC;
	static int sim_cycle;
	static int calculation;
	static int jumpAddress;
	static String[] registers = new String[8];
	static boolean fetchInst, jumped, dependencyFlag, halt, pipeLineStatus;
	static String dependent;
	static Map<String, Integer> dependency = new HashMap<String, Integer>(); // Register to Value
	static Map<Integer, Instruction> instructions  = new HashMap<Integer, Instruction>(); // PC -> Instruction Object
	static Map<Integer, Integer> memory = new HashMap<Integer, Integer>(); // Memory Available
	static Map<String, Instruction> pipeLine =  new HashMap<String, Instruction>();// Single Stage APEX PipeLine Stage -> Available

	static Map<String, String> arf = new HashMap<String, String>();  // Register -> Value


	static Queue<IssueQueue> ISQ = new LinkedList<IssueQueue>(); 
	static Queue<IssueQueue> LSQ = new LinkedList<IssueQueue>(); // TBD
	static Queue<Instruction> completed  = new LinkedList<Instruction>();
	static RenameTable renameTable = new RenameTable();
	static PRF prf = new PRF();
	static ReOrder  rob = new ReOrder();
	//IssueQueue isq = new IssueQueue;


	static boolean srcOneFlag, srcTwoFlag;
	/*  Rename table [archi reg no, physical reg no, location]
	 *  ARF[arch reg no, value ]
	 *  PRF[phy reg no, value, valid bit] // valid if physcial reg is available to pick
	 *  ROB[phy reg, status ] // status will whether we have to pick it from arch or physical 
	 */
	//static String [] renameTable = new String[16];
	// statROB

	//static Map<String, String> renameTable = new HashMap<String, String>();  //location -> Index
	//static Map<String, String> rob = new HashMap<String, String>(); //physical Reg -> Status

	public static void main(String str[]){
		if(str.length < 0){
			System.out.println("Input File Missing "+str[0]);
		} else {
			Simulator simulator = new Simulator();
			File file = new File(str[0]);

			try {

				while (true){
					System.out.println("Please enter your option :- \n\n");
					BufferedReader r1 = new BufferedReader(new InputStreamReader(System.in));
					String input = r1.readLine();
					if(input.toLowerCase().equals("exit")){
						System.out.println("EXITING ...");
						System.exit(-1);
					} else if (input.toLowerCase().equals("init")){
						simulator.initialize(file);
					} else if (input.toLowerCase().contains("simulate")){
						String[] cycleCount = input.split("\\s+");
						simulator.Simulate(Integer.parseInt(cycleCount[1]));
					} else if (input.toLowerCase().equals("display")){
						prf.display();
						rob.display();
						renameTable.display();
						simulator.displayARF();

						simulator.displayPipeLine();
						//simulator.displayRegister();
						//simulator.displayMemory();
					}
				}
			} catch(Exception e){
				System.out.println("Exception Occoured "+e.getMessage());
				simulator.displayRegister();
				simulator.displayDependency();
				System.out.println("PROGRAM COUNTER "+PC);
			}	
		}
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void initialize(File file){
		System.out.println("INITIALIZATION...");
		int count = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String fileStr;

			pipeLineStatus = false;
			fetchInst = true;
			jumped = false;
			dependent = null;
			dependencyFlag = false;
			halt = false;
			PC = 20000; 
			sim_cycle = 0;
			calculation = 0;
			pipeLine.put("FET", null);
			pipeLine.put("D/RF", null);
			pipeLine.put("D2/RF", null);
			pipeLine.put("ISQ", null);
			pipeLine.put("EX", null);
			pipeLine.put("MEM", null);
			pipeLine.put("WB", null);


			srcOneFlag = true;
			srcTwoFlag = false;


			for(int i = 20000;  ((fileStr = br.readLine()) != null); i++) {
				String string[] = fileStr.split("\\s+");
				if(string.length == 2){
					instructions.put(i, new Instruction(string[0], string[1]));
				} else if(string.length == 3){
					instructions.put(i, new Instruction(string[0], string[1], string[2]));
				} else if (string.length == 4){
					instructions.put(i, new Instruction(string[0], string[1], string[2] ,string[3]));
				}
				count++;
			}
			for(int index = 0; index < 8; index++){
				registers[index] = null;
			}
			renameTable.initialize();
			prf.init();
			rob.initialize();

			for(int index = 0; index < 10000; index++){
				memory.put(index, 0);
			}
			System.out.println("Initialization Successful !!!\n PC Value "+PC+" \n No of Instructions "+count);
		} catch(Exception e){
			System.out.println("Initialization Failed"+e.getMessage());
			System.exit(0);
		}
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void Simulate(int cycles){
		System.out.println("SIMULATING ....");
		Instruction inst;
		sim_cycle = 0;
		try {
			while(!instructions.isEmpty() && sim_cycle < cycles && !pipeLineStatus ) {
				//System.out.println("\n\n\n*** COUNTER ***"+PC+"\n ****** CYCLE ***"+sim_cycle+" Cycle "+cycles+"SIZE"+pipeLine.size());
				sim_cycle++;	
				fetchInst = true;

				if(jumped){
					inst = instructions.get(PC);
					PC = jumpAddress;
					jumped = false;
				} else {
					inst =  instructions.get(PC);
				}

				if(halt){
					sim_cycle = sim_cycle + 3;
				}

				retire();
				writeback();
				memory();
				execute();
				Queue();
				decode2();
				decode1();
				fetch(inst);
				displayPipeLine();

				pipeLineStatus = pipeLineEmpty();
				//System.out.println(" Piple Line Stage Value ******* "+pipeLineStatus);

			}	
		} catch(Exception e){
			System.out.println(" Exception "+e.getMessage());
		}
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void forwarding(String register, String value){
		Iterator iterator = ISQ.iterator();
		while(iterator.hasNext()){
			IssueQueue isq =  (IssueQueue) iterator.next();
			Instruction inst = isq.getInstruction();
			if((inst.getsrc1() != null) && (inst.getsrc1().equals(register))){
				inst.setSrc1(value);
			}
			if((inst.getsrc2() != null) && (inst.getsrc2().equals(register))){
				inst.setSrc2(value);
			}
		}

		Iterator lsqiterator = LSQ.iterator();
		while(iterator.hasNext()){
			IssueQueue isq =  (IssueQueue) iterator.next();
			Instruction inst = isq.getInstruction();
			if((inst.getsrc1() != null) && inst.getsrc1().equals(register)){
				inst.setSrc1(value);
			}
			if((inst.getsrc2() != null) && (inst.getsrc2().equals(register))){
				inst.setSrc2(value);
			}
		}
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void retire(){
		//		System.out.println("Ret");
		if(pipeLine.get("WB") != null) {
			Instruction inst = pipeLine.get("WB");
			Instruction robInst = rob.getInstruction();
			if(robInst != null) {
				String robRegister = rob.getFirst(); // This holds the first inst's physical register not archi reg
				if(robInst.getPC() == inst.getPC()) {
					inst.setStage("RET");
					pipeLine.put("WB", null);
					arf.put(inst.getdstArch(), inst.getdst()); // For this reason we need the arch reg
					rob.remove();
					renameTable.setLocation(inst.getdstArch(), "ARF");
				}
			}		
		}
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void writeback(){
		if((pipeLine.get("WB") == null) && (pipeLine.get("MEM") != null)) {
			Instruction inst = pipeLine.get("MEM");
			inst.setStage("WB");
			pipeLine.put("WB", inst);
			pipeLine.put("MEM", null);	
		}
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void memory(){
		if((pipeLine.get("MEM") == null) && !completed.isEmpty()) {
			if(pipeLine.get("INTFU") != null){
				pipeLine.put("INTFU", null);
			} else if(pipeLine.get("MULFU") != null) {
				pipeLine.put("MULFU", null);
			} else if(pipeLine.get("LSFU") != null){
				pipeLine.put("LSFU", null);
			}
			Instruction inst = completed.element();
			completed.remove(inst);
			inst.setStage("MEM");
			pipeLine.put("MEM", inst);

		}

		Iterator lsqiterator = LSQ.iterator();
		while(lsqiterator.hasNext()){
			IssueQueue lsq = (IssueQueue) lsqiterator.next();
			if(lsq.getStatus() && ((pipeLine.get("LSFU") == null))){
				memoryFU(lsq.getInstruction());
				LSQ.remove(lsq);
			}
		}
	}
	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/
	void INTFU(Instruction inst){

		String operation = inst.getType();
		pipeLine.put("INTFU", inst);
		inst.setStage("INTFU");

		if(operation.equals("ADD")){
			calculation = Integer.parseInt(inst.getsrc1()) + Integer.parseInt(inst.getsrc2());
		} else if(operation.equals("SUB")){			
			calculation = Integer.parseInt(inst.getsrc1()) - Integer.parseInt(inst.getsrc2());			
		} else if(operation.equals("MOV")){
			calculation = Integer.parseInt(inst.getsrc1());
		} else if(operation.equals("MOVC")){
			calculation = Integer.parseInt(inst.getsrc1()); 
		}  else if(operation.equals("AND")){
			calculation = Integer.parseInt(inst.getsrc1()) & Integer.parseInt(inst.getsrc2());
		} else if(operation.equals("OR")){
			calculation = Integer.parseInt(inst.getsrc1()) | Integer.parseInt(inst.getsrc2());
		}  if(operation.equals("EX-OR")){
			calculation = Integer.parseInt(inst.getsrc1()) ^ Integer.parseInt(inst.getsrc2());
		} 

		if(inst.getCount() == 1){	
			String register =  inst.getdst();
			inst.setdst(Integer.toString(calculation));
			prf.setValid(register);
			forwarding(register, Integer.toString(calculation));
			prf.deAllocate(register);
		} else {
			completed.add(inst);
			inst.setCount(inst.getCount() + 1);
		}
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void MULFU(Instruction inst){
		String operation = inst.getType();
		pipeLine.put("MULFU", inst);
		inst.setStage("MULFU");

		if(operation.equals("MUL")){
			calculation = Integer.parseInt(inst.getsrc1()) * Integer.parseInt(inst.getsrc2());
		} else if(operation.equals("DIV")){
			calculation = Integer.parseInt(inst.getsrc1()) / Integer.parseInt(inst.getsrc2());
		} 

		if(inst.getCount() == 3){
			completed.add(inst);
			String register =  inst.getdst();
			prf.setValid(register);
			inst.setdst(Integer.toString(calculation));
			forwarding(inst.getdst(), Integer.toString(calculation));
			prf.deAllocate(register);
		} else {
			inst.setCount(inst.getCount() + 1);
		}
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void memoryFU(Instruction inst){
		String operation = inst.getType();
		if(operation.equals("LOAD")){
			calculation = Integer.parseInt(inst.getsrc1()) + Integer.parseInt(inst.getsrc2());
		} else if(operation.equals("STORE")){
			calculation = Integer.parseInt(inst.getsrc1()) + Integer.parseInt(inst.getsrc2());
		}
		if(inst.getCount() == 3){
			completed.add(inst);
			pipeLine.put("LSFU", null);
			String register =  inst.getdst();
			inst.setdst(Integer.toString(calculation));
			forwarding(inst.getdst(), Integer.toString(calculation));
			prf.deAllocate(register);
		} else {
			inst.setCount(inst.getCount() + 1);
		}
	}


	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void execute(){
		/***** Fetch a ready instruction from ISQ
		 * 	Check the opcode for Type of inst
		 * 	Check for availablility of the FU for that instruction
		 * 	issue the instruction to Ex else loop back
		 */
		Iterator iterator = ISQ.iterator();
		while(iterator.hasNext()){
			IssueQueue isq = (IssueQueue) iterator.next();
			if(isq.getStatus()){
				Instruction inst = isq.getInstruction();
				String Operation = inst.getType();
				if( Operation.equals("BZ") || Operation.equals("BNZ") || Operation.equals("JUMP") || Operation.equals("HALT")){
					if(Operation.equals("BZ")){
						fetchInst = false;
						if((Integer.parseInt(inst.getdst()) < 0) && (calculation == 0)){
							Squash();
							if(inst.getsrc1() == null) {
								PC = PC + Integer.parseInt(inst.getdst());
							} else {
								PC = PC + readRegister(inst.getsrc1()) + Integer.parseInt(inst.getsrc2());
							}
						}
					} else if(Operation.equals("BNZ")){
						fetchInst = false;
						if((calculation != 0) && (Integer.parseInt(inst.getdst()) < 0)){
							Squash();
							if(inst.getsrc1() == null) {
								PC = PC + Integer.parseInt(inst.getdst());
							} else {
								PC = PC + readRegister(inst.getsrc1()) + Integer.parseInt(inst.getsrc2());
							}
						}
					} else if(Operation.equals("HALT")){
						halt = true;
					} else if(Operation.equals("BAL")){
						jumped = true;
						jumpAddress = readRegister(inst.getsrc1()) + Integer.parseInt(inst.getsrc2());
					} else if(Operation.equals("JUMP")){
						fetchInst = false;
						jumped = true;
						jumpAddress = readRegister(inst.getsrc1()) + Integer.parseInt(inst.getsrc2());
					} 
				} else if (Operation.equals("MOV") || Operation.equals("MOVC") || Operation.equals("ADD") || Operation.equals("SUB")){
					if(pipeLine.get("INTFU") == null){						
						inst.setStage("INTFU");
						pipeLine.put("INTFU", inst);
						ISQ.remove(isq); 
						INTFU(inst);
					} else {
						Instruction instuction = pipeLine.get("INTFU");
						INTFU(instuction);
					}
				} else if (Operation.equals("MUL") || Operation.equals("DIV")){
					if(pipeLine.get("MULFU") == null){						
						inst.setStage("MULFU");
						pipeLine.put("MULFU", inst);
						ISQ.remove(isq); 
						MULFU(inst);
					} else {
						Instruction instuction = pipeLine.get("MULFU");
						MULFU(instuction);
					}
				}
			}
		}
	}


	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/
	void Queue(){

		if(pipeLine.get("D2/RF") != null) {
			Instruction inst = pipeLine.get("D2/RF");
			String Operation = inst.getType();
			if(Operation.equals("LOAD") || Operation.equals("STORE")) {
				if((LSQ.size() <= 4) && rob.isAvailable()){
					inst.setStage("LSQ");
					pipeLine.put("LSQ", inst);
					pipeLine.put("D2/RF", null);
					IssueQueue lsq = new IssueQueue(inst); // Assuming IssueQueue Type can handle LSQ
					lsq.setSrc1Available(srcOneFlag);
					lsq.setSrc2Available(srcTwoFlag);
					if(srcOneFlag && srcTwoFlag)
						lsq.setStatus(true);
					else 
						lsq.setStatus(false);
					LSQ.add(lsq); //TBD
					rob.insert(inst.getdst(), inst);					
				}				
			} 
			else if((ISQ.size() <= 16) && rob.isAvailable()){
				inst.setStage("ISQ");
				pipeLine.put("ISQ", inst);
				pipeLine.put("D2/RF", null);
				IssueQueue isq = new IssueQueue(inst);
				isq.setSrc1Available(srcOneFlag);
				isq.setSrc2Available(srcTwoFlag);
				if(srcOneFlag && srcTwoFlag)
					isq.setStatus(true);
				else 
					isq.setStatus(false);
				ISQ.add(isq);
				rob.insert(inst.getdst(), inst);
			}
			
//			List<String> regList = prf.getAllocatedRegisters();
//			for(int i = 0; i < regList.size();i++ ) {  
//				String register = regList.get(i);
//				Iterator iterator = ISQ.iterator();
//				boolean canFree = true;
//				while(iterator.hasNext()){
//					IssueQueue isq =  (IssueQueue) iterator.next();
//					Instruction instr = isq.getInstruction();
//					if((instr.getdstPhy().equals(register))){
//						canFree = false;
//					}	
//				}
	//
//				if(canFree){
//					prf.deAllocate(register);
//				}
//			}
		}
	}


	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/
	void decode2(){
		if((pipeLine.get("D2/RF") == null) && (pipeLine.get("D1/RF") != null) && !dependencyFlag) {
			String src = null;
			Instruction inst = pipeLine.get("D1/RF");
			inst.setStage("D2/RF");
			pipeLine.put("D2/RF", inst);
			pipeLine.put("D1/RF", null);

			if((inst.getsrc1() != null) && (inst.getsrc1().contains("P"))) {
				src = renameTable.getLocation(inst.getsrc1());
				if(src.equals("P") && (prf.getValid(inst.getsrc1()))){
					inst.setSrc1(prf.getValue(inst.getsrc1()));
					srcOneFlag = true;
				} else  
					srcOneFlag = false;
			} else { 
				srcOneFlag = true;
			}
			if((inst.getsrc2() != null) && (inst.getsrc2().contains("P"))) {
				src = renameTable.getLocation(inst.getsrc2());
				if(src.equals("P") && (prf.getValid(inst.getsrc2()))){
					inst.setSrc2(prf.getValue(inst.getsrc2()));
					srcTwoFlag = true;
				} else  
					srcTwoFlag = false;
			} else { 
				srcTwoFlag = true;
			}
		}
	}


	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void decode1(){

		if((pipeLine.get("D1/RF") == null) && (pipeLine.get("FET") != null)) {

			Instruction inst = pipeLine.get("FET");

			inst.setStage("D1/RF");
			pipeLine.put("D1/RF", inst);
			pipeLine.put("FET", null);

			if(prf.canAllocate()) {
				if(inst.getType().equals("ADD")) {
					System.out.println("");
					//String dstReg2 = inst.getdst();
				}
				String physicalRegister = prf.allocate();
				String dstReg = inst.getdst();

				if((inst.getsrc1() != null) && (inst.getsrc1().contains("R"))) 
					inst.setSrc1(renameTable.getPhysicalReg(inst.getsrc1()));	

				if((inst.getsrc2() != null) && (inst.getsrc2().contains("R")))
					inst.setSrc2(renameTable.getPhysicalReg(inst.getsrc2()));

				inst.setdst(physicalRegister);
				inst.setdstPhy(physicalRegister);

				renameTable.setLocation(dstReg, "P"); // Look at the PRF
				renameTable.setPhyReg(dstReg, physicalRegister);

				dependencyFlag = false;

			} else {
				dependencyFlag = true;
			}
		} else if (pipeLine.get("D1/RF") != null) {
			Instruction inst = pipeLine.get("D1/RF");
			if(prf.canAllocate()) {
				String physicalRegister = prf.allocate();
				String dstReg = inst.getdst();

				if((inst.getsrc1() != null) && (inst.getsrc1().contains("R")))
					inst.setSrc1(renameTable.getPhysicalReg(inst.getsrc1()));	

				if((inst.getsrc2() != null) && (inst.getsrc1().contains("R")))
					inst.setSrc2(renameTable.getPhysicalReg(inst.getsrc2()));

				inst.setdst(physicalRegister);
				inst.setdstPhy(physicalRegister);

				renameTable.setLocation(dstReg, "P"); // Look at the PRF
				renameTable.setPhyReg(dstReg, physicalRegister);

				dependencyFlag = false;
			} 
		}
	}



	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/
	void fetch(Instruction inst){
		if((pipeLine.get("FET") == null) && !halt && fetchInst && (inst != null)){
			inst.setStage("FET");
			inst.setPC(PC);
			pipeLine.put("FET", inst);
			PC++;
		}
	}


	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	boolean dependecies(Instruction inst){
		return ((dependency.containsKey(inst.getdst())) || (dependency.containsKey(inst.getsrc1())) || (dependency.containsKey(inst.getsrc2())));
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/
	boolean pipeLineEmpty(){
		return ((pipeLine.get("FET") == null) && (pipeLine.get("D/RF") == null) && (pipeLine.get("EX") == null) && 
				(pipeLine.get("MEM") == null) && (pipeLine.get("WB") == null));
	}


	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	int readRegisterIndex(String register){
		return (Integer.parseInt(register.replace("R", "")));
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void writeRegister(String register, String value){
		//System.out.println("********** WRITE REGISTER ****\n "+register+"--"+value);
		int index = Integer.parseInt(register.replace("R", ""));
		registers[index] = value;
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	int readRegister(String register){
		int index = Integer.parseInt(register.replace("R", ""));
		return Integer.parseInt(registers[index]);
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void Squash(){
		// Squash PipeLine
		pipeLine.put("FET", null);
		pipeLine.put("D/RF", null);		
	}


	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void displayRegister() {
		System.out.println("********** DISPLAY REGISTER ********");
		for (int i =0 ;i < 8; i++) {
			if(registers[i] != null)
				System.out.println("REGISTER "+i+" -> "+registers[i]);
		}
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void displayDependency() { 
		System.out.println("********** DISPLAY DEPENDENCY ********");
		for (Map.Entry<String, Integer> entry : dependency.entrySet()) {
			System.out.println(entry.getKey());
		}
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void displayMemory() { 
		System.out.println("********** DISPLAY Memory ********");
		int index = 0;
		for (Map.Entry<Integer, Integer> entry : memory.entrySet()) {
			if(index++ < 100)
				System.out.println("Address -> "+index+" Value "+entry.getValue());
		}
	}


	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void displayISQ() { 
		Iterator iter = ISQ.iterator();
		while(iter.hasNext()){
			IssueQueue isq = (IssueQueue)iter.next();
			System.out.println("ISQ      "+isq.getInstruction().displayInst());
		}
	}


	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void displayARF() { 
		System.out.println("DISPLAY ARF");
		for(Map.Entry<String,String> entry: arf.entrySet()){
			System.out.println(entry.getKey()+" : "+entry.getValue());
		}
	}


	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void displayLSQ() { 
		Iterator iter = LSQ.iterator();
		while(iter.hasNext()){
			IssueQueue isq = (IssueQueue)iter.next();
			System.out.println("LSQ      "+isq.getInstruction().displayInst());
		}
	}

	/********************************************************
	 * 
	 * METHOD NAME : 
	 * INPUT 	   : 
	 * RETURNS	   : 
	 * PURPOSE     :  
	 *
	 ********************************************************/

	void displayPipeLine(){
		System.out.println("\n********** DISPLAY PIPELINE ********");
		if(pipeLine.get("FET")!= null){
			System.out.println("FETCH      "+pipeLine.get("FET").displayInst());
		}
		if(pipeLine.get("D1/RF") != null){
			System.out.println("DECODE1     "+pipeLine.get("D1/RF").displayInst());
		} 
		if(pipeLine.get("D2/RF") != null){
			System.out.println("DECODE2     "+pipeLine.get("D2/RF").displayInst());
		} 
		if(pipeLine.get("ISQ") != null){
			displayISQ();
		} 
		if(pipeLine.get("LSQ") != null){
			displayLSQ();
		}
		if(pipeLine.get("INTFU")!= null){
			System.out.println("INTFU    "+pipeLine.get("INTFU").displayInst());
		}
		if(pipeLine.get("MULFU")!= null){
			System.out.println("MULFU    "+pipeLine.get("MULFU").displayInst());
		} 
		if(pipeLine.get("LSFU")!= null){
			System.out.println("LSFU    "+pipeLine.get("LSFU").displayInst());
		} 
		if(pipeLine.get("MEM")!= null){
			System.out.println("MEMORY     "+pipeLine.get("MEM").displayInst());
		} 
		if(pipeLine.get("WB")!= null){
			System.out.println("WRITE-BACK "+pipeLine.get("WB").displayInst());
		}
		System.out.println("********** END OF PIPELINE ********\n");
	}	
} // End of Class