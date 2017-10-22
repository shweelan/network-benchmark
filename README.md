To compile

        $ make


To run the server

        $ java -classpath ./build/ nbm.server.Server


To run the client

        $ java -classpath ./build/ nbm.client.Client


Many options come with the client

-hosts, -h to specify the host(s), comma separated hosts and ports, i,e "12.29.19.90:2912, localhost:1229".

-port, -p to specify the default port.

-clientscount, -cc to specify the number of clients to be connected to the server, NOTE: each client is a thread!

-chunksize, -cs to specify each message sent body size (in Bytes).

-chunkdelay, -cd to specify the delay after each message is sent (in Milliseconds).

-duration, -d to specify the test duration (in Seconds), NOTE: the duration is per test, not per connection!

-usedownlink, -udl to use use downlink


To run remote and local servers (remote servers must be accessible via ssh, have jvm installed, and the project cloned and compiled)
To run client with multiple tests, first create a file that will contain command line args (for example, check start_conf_example.config)

        $ java -classpath ./build/ nbm.main.Main start_conf_example.config
