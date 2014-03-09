compile: */*.java
	mkdir -p out
	javac -d out $^
runConsumer: 
	java -classpath out/ imageconsumer.ConsumerServer $(IP) $(PORT) $(MYPORT)
runClient:
	java -classpath out/ imageclient.ImageClient $(IP) $(PORT) $(IMGS)
runMaster:
	java -classpath out/ masterserver.MasterServer $(MP) $(LIP) $(LP)
runImageTest:
	java -classpath out/ imageprocessing.ImageProcessing