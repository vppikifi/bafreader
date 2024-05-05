Postinumerotiedoston parseri

Ohjelmito osaa lukea osoitteesta https://www.posti.fi/fi/asiakastuki/postinumerotiedostot löytyvän postinumerotiedoston ja etsiä parametrina annetulle osoitteelle postinumeron.

Esimerkki:

java -jar build/libs/bafreader.jar --tiedosto=BAF_20240413.dat --kunta=091 --katu=Mannerheimintie --numero=30
tulostaa:
Mannerheimintie 30;30;Mannerheimintie;Mannerheimvägen;00100;30;>=2;<=40;20240413