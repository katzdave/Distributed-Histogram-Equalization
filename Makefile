compile: */*.java
	mkdir -p out
	javac -d out $^
runConsumer: 
	java -classpath out/ imageconsumer.ConsumerServer $(IP) $(PORT)
runClient:
	java -classpath out/ imageclient.ImageClient $(IP) $(PORT) $(IMGS)
runMaster:
	java -classpath out/ masterserver.MasterServer $(SP) $(SEIP) $(SEP)