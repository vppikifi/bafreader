package org.elcheapo

import java.io.File
import java.io.InputStream
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import java.io.BufferedInputStream
import java.io.BufferedReader
import kotlin.text.toUpperCase

data class Field(val name: String, val bytes: Int)

val FIELDS = arrayOf(
    Field("Tietuetunnus", 5),                               // 0
    Field("Ajopäivä", 8),                                   // 1
    Field("Postinumero", 5),                                // 2
    Field("Postinumeron nimi suomeksi",  30),               // 3
    Field("Postinumeron nimi ruotsiksi", 30),               // 4
    Field("Postinumeron nimen lyhenne suomeksi", 12),       // 5
    Field("Postinumeron nimen lyhenne ruotsiksi", 12),      // 6
    Field("Kadun (paikan) nimi suomeksi", 30),              // 7
    Field("Kadun (paikan) nimi ruotsiksi", 30),             // 8
    Field("Tyhjä", 24),                                     // 9
    Field("Kiinteistön tyyppi", 1),                         // 10
    Field("Pienin/Kiinteistönumero 1", 5),                  // 11
    Field("Pienin/Kiinteistön jakokirjain 1", 1),           // 12
    Field("Pienin/Välimerkki", 1),                          // 13
    Field("Pienin/Kiinteistönumero 2", 5),                  // 14
    Field("Pienin/Kiinteistön jakokirjain 2", 1),           // 15
    Field("Suurin/Kiinteistönumero 1", 5),                  // 16
    Field("Suurin/Kiinteistön jakokirjain 1", 1),           // 17
    Field("Suurin/Välimerkki", 1),                          // 18
    Field("Suurin/Kiinteistönumero 2", 5),                  // 19
    Field("Suurin/Kiinteistön jakokirjain 2", 1),           // 20
    Field("Kunnan koodi", 3),                               // 21
    Field("Kunnan nimi suomeksi", 20),                      // 22
    Field("Kunnan nimi ruotsiksi", 20),                     // 23
    Field("rivinvaihto", 1)                                 // 24
)

data class Query(val osoite:String, val katu:String, val nro: Int, val rappu:String, val tyyppi:String, var loyty:Boolean)

val VERSION: String = "0.2"

class bafReader : CliktCommand("Postin BAF_VVVVKKPP.dat tiedoston lukija " + VERSION) {

    // CliktCommand optiot
    val bafTiedosto    by option("-b",  "--baf",  help = "BAF tiedosto")
    val osoiteTiedosto  by option("-i",  "--input",  help = "Kyselyt")
    val kunta       by option("-c",  "--kunta",     help = "Kuntakoodi esim 091").default("091")
    val hiljainen   by option("-q",  "--hiljaa",    help = "Ei tulostusta").flag(default=false)
    val tulostaVersio by option("--versio",        help = "Tulosta versionumero ja lopeta").flag(default=false)
    val debug       by option("-v",  "--debug",     help = "Tulosta debug").flag(default=false)
    val eiHeaderia  by option("-h",  "--ei-header",     help = "Tulosta debug").flag(default=false)
    val moreDebug   by option("-vv", "--moredebug",  help = "Tulosta paljon debuggia").flag(default=false)

    // Omat muuttujat
    var quaries: MutableList<Query> = mutableListOf<Query>()

    // Debug tulostus
    fun debugPrint(msg : String, more : Boolean) {
        // -vv
        if(more) {
            if(moreDebug) {
                println(msg)
            }
        } else if(moreDebug || debug) {
                println(msg)
        }
    }
    // Main, tarkista parametrit
    override fun run() {

        if(tulostaVersio) {
            println ("Version:" + VERSION)
            return 
        }

        if(eiHeaderia == false) {
            println("Kysely;Katu suomeksi;Katu ruotsiksi;Postinumero;Kadun numeron alku;Osoitteen loppu;Osoite suomeksi;Osoite ruotsiksi;Postinumeroalueen alku;Postinumeroalueen loppu;Ajopäivä")
        }

        readQueryFile(osoiteTiedosto ?: "")
       

        try {

            // Avaa BAF - tiedosto
            debugPrint("Avataan :" + bafTiedosto.toString(), true)
            var inputStream: BufferedInputStream
            inputStream = BufferedInputStream(File (bafTiedosto).inputStream())
            debugPrint(bafTiedosto + " sisältää " + inputStream.available() + " tavua",false)

            // Etsi osoitteet
            parseRows(inputStream)
            val notFound = quaries.listIterator()

            // Tulosta osoitteet, joita ei löytynyt
            for(n in notFound) {
                if(n.loyty == false) {
                    System.err.println(n.osoite.toString() + "; Osoite ei tunnettu")
                }
            }

        } catch (e : Exception) {
            System.err.println ("Error")
        }
    }

    fun readQueryFile(qf: String) {
        var reader: BufferedReader
        reader = BufferedReader(File (qf).inputStream().reader())
        try {
            var line = reader.readLine()
            while (line != null) {
                val addressRegex = Regex( "^(?:^([A-ZÅÄÖ]\\D{2,})\\s(\\d{1,4}))(?:([-–]\\d{1,3}))?(.*)$")
                val matchResult = addressRegex.matchEntire(line.toString())
                if( matchResult != null) {

                    val i: Int = (matchResult.groupValues[2]).toInt()
                    var t: String
                    if(i % 2 == 1) {
                        t = "1"
                    } else {
                        t = "2"
                    }

                    var q: Query = Query(line,(matchResult.groupValues[1]).trim(), i, (matchResult.groupValues[3])+(matchResult.groupValues[4]), t, false)                    
                    quaries.add(q)
                } else {
                    if(line != "") {
                        System.err.println (line.toString() + "; Huono osoite")
                    }
                }
                line = reader.readLine()                    
            }
        } finally {
           reader.close()
        }
    }

    // Varsinainen parseri
    fun parseRows(inputStream: InputStream) {
        
        while (inputStream.available() > 0) {
            val row = HashMap<String, String>()
            for ((title, bytes) in FIELDS) {
                row[title] = inputStream.readNBytes(bytes).toString(Charsets.ISO_8859_1).trim()
            }

            // Oikea kunta?
            if(row["Kunnan koodi"] != kunta) {
                continue
            }

            // Käy läpi kaikki kyselyt
            val qit = quaries.listIterator()
            var loytymatta : Boolean = false
            for(q in qit) {

                // Onko kaikki jo löydetty?
                if(q.loyty == false) {
                    loytymatta = true
                }
                // Oikealla puolell katua + oikea katu?
                if(q.loyty == false 
                &&  (
                    q.katu.uppercase() == (row["Kadun (paikan) nimi suomeksi"] ?: "").uppercase()
                    || q.katu.uppercase() == (row["Kadun (paikan) nimi ruotsiksi"] ?: "").uppercase()
                    ) 
                && q.tyyppi == row["Kiinteistön tyyppi"]){
                    
                    // BAF tietueen pinenin osoite
                    val minNumber = (row["Pienin/Kiinteistönumero 1"] ?: "0").toInt()

                    // Selvitä BAF tietueen suurin osoite
                    var maxNumber = Int.MAX_VALUE
                    if ((row["Suurin/Kiinteistönumero 2"] ?: "") == "") {
                        if ((row["Suurin/Kiinteistönumero 1"] ?: "") != "") {
                            maxNumber = (row["Suurin/Kiinteistönumero 1"] ?: "0").toInt()
                        }
                    } else {
                        maxNumber = (row["Suurin/Kiinteistönumero 2"] ?: "0").toInt()
                    }

                    // Osuuko numero?
                    if (q.nro in minNumber..maxNumber) {

                        // Älä tulosta max numeroa, jos sitä ei ole
                        var l:String = ""
                        if(maxNumber != Int.MAX_VALUE) {
                            l = "$maxNumber"
                        }

                        println(
                            "${q.osoite};" +
                            "${row["Kadun (paikan) nimi suomeksi"]};" +
                            "${row["Kadun (paikan) nimi ruotsiksi"]};" +
                            "${row["Postinumero"]};" +
                            "${q.nro};" +
                            "${q.rappu};" +
                            "${row["Kadun (paikan) nimi suomeksi"]} ${q.nro}${q.rappu};" +
                            "${row["Kadun (paikan) nimi ruotsiksi"]} ${q.nro}${q.rappu};" +
                            "$minNumber;" +
                            l + ";" +
                            "${(row["Ajopäivä"] ?: "").substring(0,4)}" +
                            "-" + "${(row["Ajopäivä"] ?: "").substring(4,6)}" 
                            + "-" + "${(row["Ajopäivä"] ?: "").substring(6,8)}"
                        )
                        q.loyty = true
                    } 
        
                }
            }
            if(loytymatta == false) {
                // Ei enää etsittävää
                return;
            }
        }
    }
}


fun main(args: Array<String>) = bafReader().main(args)