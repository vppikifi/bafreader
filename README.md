BAFREADER v.0.1

Program for querying postalcodes of addresses in Finland. 
It can also be used to convert between Finnish and Swedish variants of the addresses.

Input: BAF file from https://www.posti.fi/webpcode
     + Tekstifile for quaries

Output: CSV

Building the program: 
./gradlew buildFatJar

Example usage: 
java -jar build/libs/bafreader.jar --baf=BAF_20240803.dat --input=samplequery.txt 
-> stdout:

Query;Street (FI);Street (SE);Postal code (text);Number;End of the address;Address (FI);Address (SE);BAF Entry start;BAF Entry End;Rundate (YYYY-MM-DD)
Mannerheimintie 1;Mannerheimintie;Mannerheimvägen;00100;1;;Mannerheimintie 1;Mannerheimvägen 1;1;13;2024-08-03
Mannerheimintie 2a;Mannerheimintie;Mannerheimvägen;00100;2;a;Mannerheimintie 2a;Mannerheimvägen 2a;2;40;2024-08-03
Mannerheimintie 17;Mannerheimintie;Mannerheimvägen;00250;17;;Mannerheimintie 17;Mannerheimvägen 17;17;69;2024-08-03
Mannerheimintie 69;Mannerheimintie;Mannerheimvägen;00250;69;;Mannerheimintie 69;Mannerheimvägen 69;17;69;2024-08-03
Mannerheimvägen 70;Mannerheimintie;Mannerheimvägen;00250;70;;Mannerheimintie 70;Mannerheimvägen 70;70;114;2024-08-03
Mannerheimvägen 42;Mannerheimintie;Mannerheimvägen;00260;42;;Mannerheimintie 42;Mannerheimvägen 42;42;68;2024-08-03
Mannerheimvägen 71;Mannerheimintie;Mannerheimvägen;00270;71;;Mannerheimintie 71;Mannerheimvägen 71;71;95;2024-08-03
Mannerheimintie 95;Mannerheimintie;Mannerheimvägen;00270;95;;Mannerheimintie 95;Mannerheimvägen 95;71;95;2024-08-03
Mannerheimvägen 116;Mannerheimintie;Mannerheimvägen;00270;116;;Mannerheimintie 116;Mannerheimvägen 116;116;158;2024-08-03
Mannerheimintie 97;Mannerheimintie;Mannerheimvägen;00280;97;;Mannerheimintie 97;Mannerheimvägen 97;97;117;2024-08-03
Mannerheimvägen 160;Mannerheimintie;Mannerheimvägen;00300;160;;Mannerheimintie 160;Mannerheimvägen 160;160;172;2024-08-03
A.I. Virtasen aukio 1;A.I. Virtasen aukio;A.I. Virtanens plats;00560;1;;A.I. Virtasen aukio 1;A.I. Virtanens plats 1;1;;2024-08-03
Jan-Magnus Janssonin aukio 2;Jan-Magnus Janssonin aukio;Jan-Magnus Janssons plats;00560;2;;Jan-Magnus Janssonin aukio 2;Jan-Magnus Janssons plats 2;2;6;2024-08-03
Klaukkalanpuiston ryhmäpuutarha     113;Klaukkalanpuiston ryhmäpuutarh;Klasasparken grupp trädgård;00680;113;;Klaukkalanpuiston ryhmäpuutarh 113;Klasasparken grupp trädgård 113;1;113;2024-08-03
Poiju 3;Poiju;Bojen;00890;3;;Poiju 3;Bojen 3;3;;2024-08-03

--> stderr
Mannerheimvägen 119;  Unknown address