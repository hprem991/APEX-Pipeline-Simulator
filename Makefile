JFLAGS = -g
JC = javac
#
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java
CLASSES = Instructions.java Simulator.java IssueQueue.java LoadStore.java RenameTable.java ReOrder.java Stages.java PRF.java
default: classes
classes: $(CLASSES:.java=.class)
clean:
	$(RM) *.class
