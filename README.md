# Nio chat client

Console chat client with end-to-end encryption.
So that users can securely exchange messages with peers

## Nio server
- capable of relaying messages from one client to another
- assign clients unique IDs and allow them to send messages to each other
- runs on port 8090
- non-blocking and allow multiple simultaneous connections
- single-threaded
- buffer size is configurable


## End-to-end encryption algorythm
1. User_1 wants to send message  to User_2. He establishes connection to the server.
2. User_1 chooses a one-time key and uses XOR operation to get an encrypted message. User_1 sends message using server.
3. User_2 gets an encrypted message and uses his own one-time key  to get double-encrypted message. User_2 sends this message back to User_1.
4. User_1 removes her encryption.He sends this message back to User_2 through the server.
5. User_2 finally decrypts the message to get User_1 message.

# Author
**Ilona Sofianidi**
