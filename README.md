BAFREADER v.0.3

Program for querying postalcodes of addresses in Finland. 
It can also be used to convert between Finnish and Swedish variants of the addresses.

Input: latest BAF-file from https://www.posti.fi/webpcode
     + Tekstifile for quaries

Output: CSV

I am not associated with Posti in any way. I just wanted to try to do something usefull with Kotlin. 

Algorithm: 
1) Read queries into memory
2) Read a line from BAF-file
3) Check if any of the quaries match
4) Read next lines until all quaries have an answer or the BAF-file ends

Building the program: 
./gradlew buildFatJar

Example usage: 

java -jar build/libs/bafreader.jar --baf=BAF_20240803.dat --input=samplequery.txt 

stdout: gets successfull results in CSV format
stderr: gets invalid addresses 