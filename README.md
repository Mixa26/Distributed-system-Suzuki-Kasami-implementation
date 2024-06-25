# Suzuki-Kasami implementation

This project is an implementation of the Suzuki-Kasami token based distributed systems algorithm.<br>
The base is a chord system which is holding servents (server and client at the same time) which<br>
work with textual files. Suzuki-Kasami ensures only one added servent is adding another servent at a time<br>
and that it doesn't interfere with file adding. System also implement a fail recovery system<br>
which in ideal situations should make the system readjust after a servent has died. For more information<br>
a documentation file in serbian is provided.
