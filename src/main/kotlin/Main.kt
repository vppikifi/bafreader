package org.vppikifi.bafreader

import java.io.File
import java.io.InputStream
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import java.io.BufferedInputStream
import java.io.BufferedReader
import kotlin.text.toUpperCase

val VERSION: String = "0.3"

// BAF file format, get exact specification from posti.fi
data class Field(val name: String, val bytes: Int)
val FIELDS = arrayOf(
    Field("Tietuetunnus", 5),                               
    Field("Ajopäivä", 8),                                   
    Field("Postinumero", 5),                                
    Field("Postinumeron nimi suomeksi",  30),               
    Field("Postinumeron nimi ruotsiksi", 30),               
    Field("Postinumeron nimen lyhenne suomeksi", 12),       
    Field("Postinumeron nimen lyhenne ruotsiksi", 12),      
    Field("Kadun (paikan) nimi suomeksi", 30),              
    Field("Kadun (paikan) nimi ruotsiksi", 30),             
    Field("Tyhjä", 24),                                     
    Field("Kiinteistön tyyppi", 1),                         
    Field("Pienin/Kiinteistönumero 1", 5),                  
    Field("Pienin/Kiinteistön jakokirjain 1", 1),           
    Field("Pienin/Välimerkki", 1),                          
    Field("Pienin/Kiinteistönumero 2", 5),                  
    Field("Pienin/Kiinteistön jakokirjain 2", 1),           
    Field("Suurin/Kiinteistönumero 1", 5),                  
    Field("Suurin/Kiinteistön jakokirjain 1", 1),           
    Field("Suurin/Välimerkki", 1),                          
    Field("Suurin/Kiinteistönumero 2", 5),                  
    Field("Suurin/Kiinteistön jakokirjain 2", 1),           
    Field("Kunnan koodi", 3),                               
    Field("Kunnan nimi suomeksi", 20),                      
    Field("Kunnan nimi ruotsiksi", 20),                     
    Field("rivinvaihto", 1)                                 
)

// Dataclass for quaries
data class Query(val query:String, val street:String, val number: Int, val staircase:String, val type:String, var found:Boolean)

class bafReader : CliktCommand("Finnish postal code BAF_VVVVKKPP.dat file reader " + VERSION) {

    // Commandline parameters 
    val bafTiedosto     by option("-b",  "--baf",           help = "BAF file")
    val osoiteTiedosto  by option("-i",  "--input",         help = "query file")
    val kunta           by option("-c",  "--kunta",         help = "City code, default is 091 (Helsinki)").default("091")
    val printVersion    by option("--version",              help = "Print version").flag(default=false)
    val debug           by option("-v",  "--debug",         help = "Tulosta debug").flag(default=false)
    val noHeader        by option("-nh", "--no-header",     help = "Don't print CSV header").flag(default=false)
    val moreDebug       by option("-vv", "--moredebug",     help = "Tulosta paljon debuggia").flag(default=false)

    // 
    var quaries: MutableList<Query> = mutableListOf<Query>()

    // Debug tulostus
    fun debugPrint(msg : String, more : Boolean) {
        // -vv
        if(more)  {
            if(moreDebug) {
                println(msg)
            }
        } else if(moreDebug || debug) {
                println(msg)
        }
    }

    // Main
    override fun run() {

        if(printVersion) {
            println ("Version:" + VERSION)
            return 
        }

        if(bafTiedosto == null || osoiteTiedosto == null) {
            System.err.println("Error: Input file(s) missing")
            return
        }

        // Read query file into quaries structure
        if(!readQueryFile(osoiteTiedosto ?: "")) return
    
        // Read BAF file
        try {

            // Open
            debugPrint("Opening :" + bafTiedosto.toString(), false)
            var inputStream: BufferedInputStream
            inputStream = BufferedInputStream(File (bafTiedosto).inputStream())
            debugPrint(bafTiedosto + " contains " + inputStream.available() + " tavua",false)

            // Output CSV file header
            if(!noHeader) {
                println("Query;Street (FI);Street (SE);Postal code (text);Number;End of the address;Address (FI);Address (SE);BAF Entry start;BAF Entry End;Rundate (YYYY-MM-DD)")
            }

            // Walk trough BAF and compare
            parseRows(inputStream)
            val notFound = quaries.listIterator()

            // Print quaries without a result
            for(n in notFound) {
                if(n.found == false) {
                    System.err.println(n.query.toString() + ";  Unknown address")
                }
            }
        } catch (e : Exception) {
            System.err.println ("Error")
        }
    }

    fun readQueryFile(qf: String) : Boolean{
        var ret: Boolean = true
        var reader: BufferedReader
        try {
            debugPrint("Opening :" + qf, false)
            reader = BufferedReader(File (qf).inputStream().reader())
            var line = reader.readLine()
            while (line != null) {

                // Parse Street address:
                // - Name starts with capital letter
                // - Shortest possible street is 3 letters
                // - Longest possible street name is 35 letters
                // - Comination numbers like 1-3 are allowed, 1st number is significant for comparision
                // - We also capture possible staircase and/or apartment after the number
                val addressRegex = Regex( "^(?:^([A-ZÅÄÖ]\\D{2,34})\\s(\\d{1,4}))(?:([-–]\\d{1,3}))?(.*)$")
                val matchResult = addressRegex.matchEntire(line.toString())
                if( matchResult != null) {

                    val i: Int = (matchResult.groupValues[2]).toInt()
                    var t: String

                    // Which side of the street?
                    if(i % 2 == 1) {
                        t = "1"
                    } else {
                        t = "2"
                    }

                    // Generate query dataclass and add it to the list
                    // Address comparision is done as case insensitive
                    // If streetname is longer than 30 characters, we
                    // truncate before comparision as BAF entry is limited to 
                    // 30 characters                                    
                    var s:String = (matchResult.groupValues[1]).trim().uppercase()
                    if(s.length > 30) {
                        s = s.substring(0,30)
                        debugPrint("Truncating too long street name to \"$s\" since BAF entry can not have more than 30 characters", false)
                    }

                    var q: Query = Query(line,s, i, (matchResult.groupValues[3])+(matchResult.groupValues[4]), t, false)                    
                    debugPrint(q.toString(), true)
                    quaries.add(q)
                } else {
                    if(line != "") {
                        System.err.println (line.toString() + "; Illegal address")
                    }
                }
                line = reader.readLine()                    
            }
        } catch (e : Exception) {
            System.err.println (e.toString())
            ret = false
        }
        return ret;
    }

    // Handle BAF rows
    fun parseRows(inputStream: InputStream) {
        
        while (inputStream.available() > 0) {
            val row = HashMap<String, String>()
            for ((title, bytes) in FIELDS) {
                // Read BAF field and strip extra spaces
                row[title] = inputStream.readNBytes(bytes).toString(Charsets.ISO_8859_1).trim()
            }

            // Incorrect city?
            if(row["Kunnan koodi"] != kunta) continue

            // Browse trough all quaries
            val qit = quaries.listIterator()
            var quariesLeft : Boolean = false
            for(q in qit) {

                // Performance optiomization: check if at lest one query is still without an answer
                if(q.found == false) {
                    quariesLeft = true
                }
                // 
                if(
                // Already found?    
                q.found == false 

                // Right side of the street?
                && q.type == row["Kiinteistön tyyppi"]

                // Case insensitive comparision for both swedish and finnish versions
                &&  (
                    // Case insensitive comparision for both swedish and finnish versions
                    q.street == (row["Kadun (paikan) nimi suomeksi"] ?: "").uppercase()
                    || q.street == (row["Kadun (paikan) nimi ruotsiksi"] ?: "").uppercase()
                    )
                ){                    
                    debugPrint("Street " + q.street + " found", false)
                    // Smallest house number in BAF row
                    val minNumber = (row["Pienin/Kiinteistönumero 1"] ?: "0").toInt()

                    // Largest house number in BAF row, use MAX_VALUE is upperlimit does not exist
                    var maxNumber = Int.MAX_VALUE
                    if ((row["Suurin/Kiinteistönumero 2"] ?: "") == "") {
                        if ((row["Suurin/Kiinteistönumero 1"] ?: "") != "") {
                            maxNumber = (row["Suurin/Kiinteistönumero 1"] ?: "0").toInt()
                        }
                    } else {
                        maxNumber = (row["Suurin/Kiinteistönumero 2"] ?: "0").toInt()
                    }

                    // Is our address in range of thsi BAF row?
                    if (q.number in minNumber..maxNumber) {

                        debugPrint("Match: ${q.number} is between $minNumber and $maxNumber", false)
                        // Don't print range max, if it did not exist in BAF
                        var l:String = ""
                        if(maxNumber != Int.MAX_VALUE) {
                            l = "$maxNumber"
                        }

                        println(
                            "${q.query};" +
                            "${row["Kadun (paikan) nimi suomeksi"]};" +
                            "${row["Kadun (paikan) nimi ruotsiksi"]};" +
                            "${row["Postinumero"]};" +
                            "${q.number};" +
                            "${q.staircase};" +
                            "${row["Kadun (paikan) nimi suomeksi"]} ${q.number}${q.staircase};" +
                            "${row["Kadun (paikan) nimi ruotsiksi"]} ${q.number}${q.staircase};" +
                            "$minNumber;" +
                            l + ";" +
                            "${(row["Ajopäivä"] ?: "").substring(0,4)}" +
                            "-" + "${(row["Ajopäivä"] ?: "").substring(4,6)}" 
                            + "-" + "${(row["Ajopäivä"] ?: "").substring(6,8)}"
                        )
                        q.found = true
                    }
                    else {
                        debugPrint("${q.number} is not between $minNumber and $maxNumber", false)
                    } 
        
                }
            }
            // Stop reading BAF, if all quaries allready have an answer
            if(quariesLeft == false) {
                return;
            }
        }
    }
}


fun main(args: Array<String>) = bafReader().main(args)