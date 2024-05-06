package org.elcheapo
import java.io.File
import java.io.InputStream
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import java.io.BufferedInputStream


data class t (val name: String, val bytes: Int)

val rakenne =arrayOf(
    t("Tietuetunnus", 5),                               // 0
    t("Ajopäivä", 8),                                   // 1
    t("Postinumero", 5),                                // 2
    t("Postinumeron nimi suomeksi",  30),               // 3
    t("Postinumeron nimi ruotsiksi", 30),               // 4
    t("Postinumeron nimen lyhenne suomeksi", 12),       // 5
    t("Postinumeron nimen lyhenne ruotsiksi", 12),      // 6
    t("Kadun (paikan) nimi suomeksi", 30),              // 7
    t("Kadun (paikan) nimi ruotsiksi", 30),             // 8
    t("Tyhjä", 24),                                     // 9
    t("Kiinteistön tyyppi", 1),                         // 10
    t("Pienin/Kiinteistönumero 1", 5),                  // 11
    t("Pienin/Kiinteistön jakokirjain 1", 1),           // 12
    t("Pienin/Välimerkki", 1),                          // 13
    t("Pienin/Kiinteistönumero 2", 5),                  // 14
    t("Pienin/Kiinteistön jakokirjain 2", 1),           // 15
    t("Suurin/Kiinteistönumero 1", 5),                  // 16
    t("Suurin/Kiinteistön jakokirjain 1", 1),           // 17
    t("Suurin/Välimerkki", 1),                          // 18
    t("Suurin/Kiinteistönumero 2", 5),                  // 19
    t("Suurin/Kiinteistön jakokirjain 2", 1),           // 20
    t("Kunnan koodi", 3),                               // 21
    t("Kunnan nimi suomeksi", 20),                      // 22
    t("Kunnan nimi ruotsiksi", 20),                     // 23
    t("rivinvaihto", 1)                                 // 24
)

val version:String = "0.2"

class bafReader : CliktCommand("Postin BAF_VVVVKKPP.dat tiedoston lukija " + version) {
    val tiedosto    by option("-f",  "--tiedosto",  help = "BAF tiedosto")
    val kunta       by option("-c",  "--kunta",     help = "Kuntakoodi esim 091").default("091")
    val osoite      by option("-a",  "--osoite",    help = "Osoite <katu> <numero> <rappu>")
    val hiljainen   by option("-q",  "--hiljaa",    help = "Ei tulostusta").flag(default=false)
    val tulostaVersio by option("--versio",        help = "Tulosta versionumero ja lopeta").flag(default=false)
    val debug       by option("-v",  "--debug",     help = "Tulosta debug").flag(default=false)
    val moreDebug   by option("-vv", "--moredebug",  help = "Tulosta paljon debuggia").flag(default=false)
    var tyyppi:String = ""
    var nro:Int = 0
    var katu:String =""

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

        try {
            debugPrint("Start:"+osoite.toString(), true)
            if(tulostaVersio) {
                println("bafreader " + version)
                return 
            }

            // Parsi osoite
            var match = Regex("(\\D+) (\\d+)").find(osoite.toString())!!
                val(k, numero) = match.destructured
            katu = k
        
            debugPrint("Katu:" + katu + " Numero:" + numero+ " kunta:" + kunta, true)

            nro = numero.toInt()
            // Kunpi puoli kadusta?
            if(nro % 2 == 1) {
                 tyyppi = "1"
            } else {
                 tyyppi = "2"
            }
            debugPrint("tyyppi=" + tyyppi, true)

            // Avaa BAF
            var inputStream: BufferedInputStream
            debugPrint("Avataan :" + tiedosto.toString(), true)
            inputStream = BufferedInputStream(File (tiedosto).inputStream())
            debugPrint(tiedosto + " sisältää " + inputStream.available() + " tavua",false)

            // Etsi osoite
            if(parseRows(inputStream) == false) {
                if(!hiljainen) {
                    println("Osoite ei tunnettu")
                    echoFormattedHelp()
                }
                return
            }
        } catch (e : Exception) {
            if(!hiljainen) {
                echoFormattedHelp()
            }
            return
        }
    }

    // Varsinainen parseri
    fun parseRows(inputStream: InputStream): Boolean {
        
        while (inputStream.available() > 0) {

            // Lue dat tiedoston rivi speksin mukaan sisän hashiin
            val rivi = HashMap<String, String> ()
            for(b in rakenne) {
                val (title, bytes) = b
                rivi[title] = inputStream.readNBytes(bytes).toString(Charsets.ISO_8859_1).trim()
            }
            debugPrint( "- " + rivi["Kunnan koodi"] + " vs " + kunta + "- " + rivi["Kadun (paikan) nimi suomeksi"] + "|" + rivi["Kadun (paikan) nimi ruotsiksi"] + " vs " + katu + " - "
                    + rivi["Kiinteistön tyyppi"] + " - " + rivi["Pienin/Kiinteistönumero 1"] + rivi["Suurin/Kiinteistönumero 1"] + " vs " + nro, true)
    
            // Oikea katu ja oikea puol?
            if(     rivi["Kunnan koodi"] == kunta
                && (rivi["Kadun (paikan) nimi suomeksi"]    == katu || rivi["Kadun (paikan) nimi ruotsiksi"] == katu)
                &&  rivi["Kiinteistön tyyppi"] == tyyppi){
    
                val pienin: Int = (rivi["Pienin/Kiinteistönumero 1"] ?: "0").toInt()
                var suurin: Int

                if( (rivi["Suurin/Kiinteistönumero 2"] ?: "") == "") {
                    if( (rivi["Suurin/Kiinteistönumero 1"] ?: "") == "") {
                        // Kaikki kadun numerot kuuluvat
                        suurin = Int.MAX_VALUE
                    }
                    else {
                        // Yksiosainen osoite tyyliin katu 1
                        suurin = (rivi["Suurin/Kiinteistönumero 1"] ?: "0").toInt()
                    }
                } else {
                    // Kaksi osainen osoite, tyylii 34-36
                    suurin = (rivi["Suurin/Kiinteistönumero 2"] ?: "0").toInt()
                }
                debugPrint(rivi["Kadun (paikan) nimi suomeksi"] + " " + pienin + "<=" + nro + "<=" + suurin + " postinro:" + rivi["Postinumero"], false)
    
                // Oikea numero?
                if(nro >= pienin && nro <= suurin) {
                    println(
                                osoite
                                + ";" + rivi["Kadun (paikan) nimi suomeksi"]
                                + ";" + rivi["Kadun (paikan) nimi ruotsiksi"]
                                + ";" + rivi["Postinumero"]
                                + ";" + nro
                                + ";>=" + pienin
                                + ";<=" + suurin
                                + ";" + rivi["Ajopäivä"])
                    return true
                }
            }
        }
        return false
    }
}
fun main(args: Array<String>) = bafReader().main(args)