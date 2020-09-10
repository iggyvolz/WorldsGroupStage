javatuples-1.2-dist.zip:
	wget https://github.com/downloads/javatuples/javatuples/javatuples-1.2-dist.zip
javatuples-1.2.jar: javatuples-1.2-dist.zip
	unzip -p $< javatuples-1.2/dist/javatuples-1.2.jar > $@
%.class: %.java javatuples-1.2.jar
	javac -cp .:javatuples-1.2.jar $<
run: Main.class Scenario.class javatuples-1.2.jar
	java -cp .:javatuples-1.2.jar Main
