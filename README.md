# BAFREADER v.0.3

Program for querying postalcodes and checking validity of addresses in Finland. 
It can also be used to convert between Finnish and Swedish variants of the addresses.

Input: latest **BAF**-file from https://www.posti.fi/webpcode
     + Tekstifile for quaries

Output: **CSV**

I am not associated with Posti in any way. I just wanted to try to do something usefull with Kotlin. 

## Algorithm: 
1) Read queries into memory
2) Read a line from BAF-file
3) Check if any of the quaries match
4) Read next lines until all quaries have an answer or the BAF-file ends

## Building the program: 
./gradlew buildFatJar

## Command line switches
| Switch (short)| Switch (long) | Mandatory |Description |
|:-------|:-------|:-------|:-------|
|-b | --baf\=\<text\> | Yes | BAF-File |
| -i | --input\=\<text\> | Yes | Query file |
| -c | --city\=\<text\> | No | Code of the city, default 091 (Helsinki) |
| -nh | --no-header | no | Don't print CSV-file header | 
| | --version | no | Print version number |
| -v | --debug | no | Print debug info | 
| -vv | --moredebug | no | Print all debug info (a lot) |
| -h | --help | no | Print help |

## Example usage: 

java -jar build/libs/bafreader.jar --baf=BAF_20240803.dat --input=samplequery.txt 

### STDOUT (Semicolon separated CSV):

| Query | Streetname (FI) | Streetname (SE) | Postalcode | Name of the postal code (FI)|Name of the postal code (SE)|Abbreviation of the postal code (FI)|Abbreviation of the postal code (FI)|Number|End of the address|Address FI|Address SE|BAF entry start|BAF entry start|Run date (YYYY-MM-DD)|
|:------------|:------------|:------------|:------------|:------------|:------------|:------------|:------------|:------------|:------------|:------------|:------------|:------------|:------------|:------------|
| Mannerheimintie 1|Mannerheimintie|Mannerheimvägen|00100|HELSINKI|HELSINGFORS|HKI|HFORS|1||Mannerheimintie 1|Mannerheimvägen 1|1|13|2024-08-03|
| Mannerheimintie 2a|Mannerheimintie|Mannerheimvägen|00100|HELSINKI|HELSINGFORS|HKI|HFORS|2|a|Mannerheimintie 2a|Mannerheimvägen 2a|2|40|2024-08-03|
| Mannerheimintie 17|Mannerheimintie|Mannerheimvägen|00250|HELSINKI|HELSINGFORS|HKI|HFORS|17||Mannerheimintie 17|Mannerheimvägen 17|17|69|2024-08-03|
| Mannerheimintie 69|Mannerheimintie|Mannerheimvägen|00250|HELSINKI|HELSINGFORS|HKI|HFORS|69||Mannerheimintie 69|Mannerheimvägen 69|17|69|2024-08-03|
| Mannerheimvägen 70|Mannerheimintie|Mannerheimvägen|00250|HELSINKI|HELSINGFORS|HKI|HFORS|70||Mannerheimintie 70|Mannerheimvägen 70|70|114|2024-08-03|
| Mannerheimvägen 42|Mannerheimintie|Mannerheimvägen|00260|HELSINKI|HELSINGFORS|HKI|HFORS|42||Mannerheimintie 42|Mannerheimvägen 42|42|68|2024-08-03|
| Mannerheimvägen 71|Mannerheimintie|Mannerheimvägen|00270|HELSINKI|HELSINGFORS|HKI|HFORS|71||Mannerheimintie 71|Mannerheimvägen 71|71|95|2024-08-03|
| Mannerheimintie 95|Mannerheimintie|Mannerheimvägen|00270|HELSINKI|HELSINGFORS|HKI|HFORS|95||Mannerheimintie 95|Mannerheimvägen 95|71|95|2024-08-03|
| Mannerheimvägen 116|Mannerheimintie|Mannerheimvägen|00270|HELSINKI|HELSINGFORS|HKI|HFORS|116||Mannerheimintie 116|Mannerheimvägen 116|116|158|2024-08-03|
| Mannerheimintie 97|Mannerheimintie|Mannerheimvägen|00280|HELSINKI|HELSINGFORS|HKI|HFORS|97||Mannerheimintie 97|Mannerheimvägen 97|97|117|2024-08-03|
| Mannerheimvägen 160|Mannerheimintie|Mannerheimvägen|00300|HELSINKI|HELSINGFORS|HKI|HFORS|160||Mannerheimintie 160|Mannerheimvägen 160|160|172|2024-08-03|
| A.I. Virtasen aukio 1|A.I. Virtasen aukio|A.I. Virtanens plats|00560|HELSINKI|HELSINGFORS|HKI|HFORS|1||A.I. Virtasen aukio 1|A.I. Virtanens plats 1|1||2024-08-03|
| Jan-Magnus Janssonin aukio 2|Jan-Magnus Janssonin aukio|Jan-Magnus Janssons plats|00560|HELSINKI|HELSINGFORS|HKI|HFORS|2||Jan-Magnus Janssonin aukio 2|Jan-Magnus Janssons plats 2|2|6|2024-08-03|
| Klaukkalanpuiston ryhmäpuutarha     113|Klaukkalanpuiston ryhmäpuutarh|Klasasparken grupp trädgård|00680|HELSINKI|HELSINGFORS|HKI|HFORS|113||Klaukkalanpuiston ryhmäpuutarh 113|Klasasparken grupp trädgård 113|1|113| 2024-08-03|
| Poiju 3|Poiju|Bojen|00890|HELSINKI|HELSINGFORS|HKI|HFORS|3||Poiju 3|Bojen 3|3||2024-08-03
| Solviksallén                                                                             4|Aurinkolahden puistotie|Solviksallén|00990|HELSINKI|HELSINGFORS|HKI|HFORS|4||Aurinkolahden puistotie 4|Solviksallén 4|2|12|2024-08-03|

### STDERR:
Mannerheimvägen 119;  Unknown address
