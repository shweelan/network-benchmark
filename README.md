To compile

        $ rm -rf ./build/*
        $ javac -d ./build/ *.java


To run the server

        $ java -classpath ./build/ nbm.server.Server


To run the client

        $ java -classpath ./build/ nbm.client.Client

Many options come with the client

-host, -h                      to specify the host.
-port, -p                      to specify the port.
-clientscount, -cc             to specify the number of clients to be connected to the server, NOTE: each client is a thread!
-chunksize, -cs                to specify each message sent body size (in Bytes).
-chunkdelay, -cd               to specify the delay after each message is sent (in Milliseconds).
-duration, -d                  to specify the test duration (in Seconds), NOTE: the duration is per test, not per connection!
