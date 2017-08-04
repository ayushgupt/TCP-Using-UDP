The code consists of two files: Sender.java and Receiver.java
To compile the files just use the command 'make'

On the Receiver's End run the Receiver.java executable with the following syntax:
usage: client Port (-h)
       Port = Port Number => Default set to 9876
       -h = to see help


On the Sender's End run the Sender.java executable with the follwing syntax:
usage: client IP Port -l P (-h)
       IP = IP Address => Default set to 127.0.0.1
       Port = Port Number => Default set to 9876
       -l = l flag
       P = flag value => Default set to 0
       -h = to see help