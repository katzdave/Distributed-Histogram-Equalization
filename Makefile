compile: */*.java
	mkdir -p out
	javac -d out -cp lib/sigar.jar $^
runConsumer: 
	java -cp out/:lib/sigar.jar imageconsumer.ConsumerServer $(IP) $(PORT) $(MYPORT)
runClient:
	java -classpath out/ imageclient.ImageClient $(IP) $(PORT) $(IMGS)
runMaster:
	java -classpath out/ masterserver.MasterServer $(MP) $(LIP) $(LP)
runImageTest:
	java -classpath out/ -Djava.library.path=lib/ -cp :lib/sigar.jar imageprocessing.ImageProcessing
