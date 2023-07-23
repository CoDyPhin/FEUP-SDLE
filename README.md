# SDLE Project

**SDLE Project for group T4G17 (2021/2022)**

Group members:

1. Carlos Lousada (up201806302@up.pt)
2. José Maçães (up201806622@up.pt)
3. Mariana Ramos (up201806869@up.pt)
4. Tomás Mendes (up201806522@up.pt)

### Build and run 

1. Start RMI Registry:
java -jar RMI.jar

2. Start Proxy:
java -jar Proxy.jar

3. Start Subscribers:
java -jar Client.jar sub subid 

4. Start Publishers:
java -jar Client.jar pub  pubid 

5. Start testing:
java -jar TestApp.jar clientid operation topic 

If the operation is put, also add the message to post in quotation marks
