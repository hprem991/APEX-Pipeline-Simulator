PROJECT STRUCTURE AND EXECUTION STEPS 

 You are currently at the present working directory of the project.This folder should contain following files and folders for proper execution.
1> It should contain filename "Makefile".
2> It should contain filename “.java” files
3> It should of course contains the README.txt.

COMMANDS FOR EXECUTION

1> Please type "make" at the command prompt from this location.
   Example :- admin$ make <ENTER>
        This will create a .class file of the name Instruction.class and Simulator.class in the same directory.

2> Now please issue the following command from the same location of the command prompt.
    $java Simulator <filename with correct path of the file>
    Example :- admin$ java Simulator test.txt <ENTER>

    
To Start Simulator Issue 

1> init command: This is initialize the simulator with instructions
2> simulate <no of inst> : This is simulate the no of instruction and display the pipeline stages for each cycle
3> display : This will display the Pipeline Stage at which the simulator is currently in alone with the memory content.
4> Exit : To get out of the simulator environment. Issue exit command.