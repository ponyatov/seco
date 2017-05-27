CLASSPATH += :.:./lib/antlr-4.7-complete.jar
JAVA  = java  -cp $(CLASSPATH)
JAVAC = javac -cp $(CLASSPATH)
GRUN = $(JAVA) org.antlr.v4.gui.TestRig
test: gLexer.class gParser.class
	echo "this is sample code 01 +02.340 5e-6 " \
	| $(GRUN) g repl -tree
gLexer.class gParser.class: gLexer.java gParser.java
	$(JAVAC) g*.java
gLexer.java gParser.java: g.g
	$(JAVA) -jar lib/antlr-4.7-complete.jar g.g
g.g: ~/seco/docs/src/script.g4
	ln -s $< $@
