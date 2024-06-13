package org.elcheapo

import java.io.File
import java.io.InputStream
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import java.io.BufferedInputStream

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

val VERSION: String = "0.2"

class bafReader : CliktCommand("Postin BAF_VVVVKKPP.dat tiedoston lukija " + VERSION) {
    val tiedosto    by option("-f",  "--tiedosto",  help = "BAF tiedosto")
    val kunta       by option("-c",  "--kunta",     help = "Kuntakoodi esim 091").default("091")
    val osoite      by option("-a",  "--osoite",    help = "Osoite <katu> <numero> <rappu>")
    val hiljainen   by option("-q",  "--hiljaa",    help = "Ei tulostusta").flag(default=false)
    val tulostaVersio by option("--versio",        help = "Tulosta versionumero ja lopeta").flag(default=false)
    val debug       by option("-v",  "--debug",     help = "Tulosta debug").flag(default=false)
    val moreDebug   by option("-vv", "--moredebug",  help = "Tulosta paljon debuggia").flag(default=false)
    var tyyppi:String = ""
    var nro:Int = 0
    var katu:String = ""
    var rappu:String = ""

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

        debugPrint("Start:"+osoite.toString(), true)
        if(tulostaVersio) {
            println ("Version:" + VERSION)
            return 
        }
        try {
            // katu ja numero erilleen
            val addressRegex = Regex( "(\\D+)\\s*(\\d+)(?:-\\d+)?\\s*(\\D*)")
            val matchResult = addressRegex.matchEntire(osoite.toString())
            if( matchResult != null) {
                katu    = (matchResult.groupValues[1]).trim()
                nro     = (matchResult.groupValues[2]).toInt()
                rappu   = matchResult.groupValues[3] 
            } else {
                println ("Huono osoite")
                return 
            }
         
            // Kunpi puoli kadusta?
            if(nro % 2 == 1) {
                tyyppi = "1"
            } else {
                tyyppi = "2"
            }
            debugPrint("Katu:" + katu + " Numero:" + nro+ " rappu:" + rappu +  " kunta:" + kunta + " tyyppi:" + tyyppi, false)

            // Avaa BAF - tiedosto
            debugPrint("Avataan :" + tiedosto.toString(), true)
            var inputStream: BufferedInputStream
            inputStream = BufferedInputStream(File (tiedosto).inputStream())
            debugPrint(tiedosto + " sisältää " + inputStream.available() + " tavua",false)

            // Etsi osoite
            if(parseRows(inputStream) == false) {
                if(!hiljainen) {
                    println("Osoite ei tunnettu")
                    echoFormattedHelp()
                }
            }
        } catch (e : Exception) {
            if(!hiljainen) {
                echoFormattedHelp()
            }
        }
    }

    // Varsinainen parseri
    fun parseRows(inputStream: InputStream): Boolean {
    while (inputStream.available() > 0) {
        val row = HashMap<String, String>()
        for ((title, bytes) in FIELDS) {
            row[title] = inputStream.readNBytes(bytes).toString(Charsets.ISO_8859_1).trim()
        }
        debugPrint(
            "- ${row["Kunnan koodi"]} vs $kunta - ${row["Kadun (paikan) nimi suomeksi"]} | ${row["Kadun (paikan) nimi ruotsiksi"]} vs $katu - " +
                    "${row["Kiinteistön tyyppi"]} - ${row["Pienin/Kiinteistönumero 1"]} ${row["Suurin/Kiinteistönumero 1"]} vs $nro",
            true
        )

        if (
            row["Kunnan koodi"] == kunta &&
            (row["Kadun (paikan) nimi suomeksi"] == katu || row["Kadun (paikan) nimi ruotsiksi"] == katu) &&
            row["Kiinteistön tyyppi"] == tyyppi
        ) {
            val minNumber = (row["Pienin/Kiinteistönumero 1"] ?: "0").toInt()
            var maxNumber = Int.MAX_VALUE

            if ((row["Suurin/Kiinteistönumero 2"] ?: "") == "") {
                if ((row["Suurin/Kiinteistönumero 1"] ?: "") != "") {
                    maxNumber = (row["Suurin/Kiinteistönumero 1"] ?: "0").toInt()
                }
            } else {
                maxNumber = (row["Suurin/Kiinteistönumero 2"] ?: "0").toInt()
            }
            debugPrint(
                "${row["Kadun (paikan) nimi suomeksi"]} $minNumber<=$nro<=$maxNumber postinro:${row["Postinumero"]}",
                false
            )

            if (nro in minNumber..maxNumber) {
                println(
                    "$osoite;" +
                            "${row["Kadun (paikan) nimi suomeksi"]};" +
                            "${row["Kadun (paikan) nimi ruotsiksi"]};" +
                            "${row["Postinumero"]};" +
                            "$nro;" +
                            "$rappu;" +
                            "${row["Kadun (paikan) nimi suomeksi"]} $nro$rappu;" +
                            "${row["Kadun (paikan) nimi ruotsiksi"]} $nro$rappu;" +
                            ">=$minNumber;" +
                            "<=$maxNumber;" +
                            "${row["Ajopäivä"]}"
                )
                return true
            }
        }
    }
    return false
}

}
fun main(args: Array<String>) = bafReader().main(args)