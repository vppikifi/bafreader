package org.elcheapo
import java.io.File
import java.io.InputStream


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
    t("Kunnan nimi ruotsiksi", 20)                      // 23
)

val katunumeroRegex = "\\D".toRegex()

fun printUsage() {
    println("Käyttö: bafreader <tiedosto> <kunta> <katu> <numero>")
}

fun main(args: Array<String>) {

    val parser = ArgParser("example")


    if(args.size < 4 ) {

        printUsage()
        return
    }
    var match:String = (katunumeroRegex.split(args[3]))[0]
    if(match == "") {
        printUsage()
        return       
    }

    var numero:Int = match.toInt()

    //println("Reading:" + args[0])
    val kunta:String = args[1]
    val katu:String = args[2]
    val alkupNum:String = args[3]
    var inputStream: InputStream
    try {
        inputStream = File (args[0]).inputStream()
    } catch (e : Exception) {
        println("Tiedosto ei aukea: " + args[0] )
        printUsage()
        return       
        
    }

    

    // Parillinen vari pariton talo
    var tyyppi:String
    if(numero % 2 == 1) {
        tyyppi = "1"
    } else {
        tyyppi = "2"
    }

    while (inputStream.available() > 0) {
        val rivi = HashMap<String, String> ()
        for(b in rakenne) {
            val (title, bytes) = b
            rivi[title] = inputStream.readNBytes(bytes).toString(Charsets.ISO_8859_1).trim()
        }
        inputStream.readNBytes(1) // Skippaa rivinvaihto


        if(     rivi["Kunnan koodi"] == kunta
            && (rivi["Kadun (paikan) nimi suomeksi"]    == katu || rivi["Kadun (paikan) nimi ruotsiksi"] == katu)
            &&  rivi["Kiinteistön tyyppi"] == tyyppi){

            val pienin: Int = (rivi["Pienin/Kiinteistönumero 1"] ?: "0").toInt()
            var suurin: Int

            // Onko suurin numero tyyliin 34-36
            if( (rivi["Suurin/Kiinteistönumero 2"] ?: "") == "") {
                suurin = (rivi["Suurin/Kiinteistönumero 1"] ?: "0").toInt()
            } else {
                suurin = (rivi["Suurin/Kiinteistönumero 2"] ?: "0").toInt()
            }

            if(numero >= pienin && numero <= suurin) {
                println(
                    katu + " " + alkupNum
                            + ";"+ alkupNum
                            + ";" + rivi["Kadun (paikan) nimi suomeksi"]
                            + ";" + rivi["Kadun (paikan) nimi ruotsiksi"]
                            + ";" + rivi["Postinumero"]
                            + ";" + numero
                            + ";>=" + pienin
                            + ";<=" + suurin
                            + ";" + rivi["Ajopäivä"])
                return
            }
        }
    }
    println("Ei löydy")
    printUsage()
}

