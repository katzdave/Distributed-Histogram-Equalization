cloudcomp4
==========
Harrison Zhao
David Katz
Eli Friedman

A distributed histogram equalization program. The clients connect to a master computer and receive the ip address and port of a consumer which will process their images. The client connects to that consumer, sends the image and receives the histogram equalized image!

To run the master:
==========
make runMaster SP=(masters_serverSocketPort) SEIP=(leader's_ipAddress) SEP=(leader's_port)

NOTE: if SP == SEP && leader's_ipAddress==localhost, this will make the master the leader

To run the consumer:
==========
make runConsumer IP=(masters_ip_address) PORT=(masters_serverSocketPort)

To run the client:
==========
make runClient IP=(masters_ip_address) PORT=(masters_serverSocketPort) IMGS=(the filename containing a list of newline separated images that you want processed)

