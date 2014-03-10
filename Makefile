compile: */*.java
	mkdir -p out
	javac -d out -cp lib/sigar.jar $^
runConsumer: 
	java -classpath out/ -Djava.library.path=lib/ -cp :lib/sigar.jar imageconsumer.ConsumerServer $(IP) $(PORT) $(MYPORT)
runClient:
	java -classpath out/ -Djava.library.path=lib/ -cp :lib/sigar.jar imageclient.ImageClient $(IP) $(PORT) $(IMGS)
runMaster:
	java -classpath out/ -Djava.library.path=lib/ -cp :lib/sigar.jar masterserver.MasterServer $(MP) $(LIP) $(LP)
runImageTest:
	java -classpath out/ -Djava.library.path=lib/ -cp :lib/sigar.jar imageprocessing.ImageProcessing