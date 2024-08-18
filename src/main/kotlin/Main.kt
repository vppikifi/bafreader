package org.vppikifi.bafreader

import java.io.File
import java.io.InputStream
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import java.io.BufferedInputStream
import java.io.BufferedReader
import kotlin.text.toUpperCase
import org.vppikifi.bafreader.FieldNames
import org.vppikifi.bafreader.FIELDS

val VERSION: String = "0.3"

enum class FieldNames (val printableName: String){
    ID("Record id"),               
    DATE("Date"),             
    POSTALCODE("Postalcode"),       
    POSTALCODE_NAME_FI("Name of the postal code (FI)"),
    POSTALCODE_NAME_SE("Name of the postal code (SE)"),      
    POSTALCODE_ABRR_FI("Abbreviation of the postal code (FI)"),
    POSTALCODE_ABRR_SE("Abbreviation of the postal code (FI)"),
    STREETNAME_FI("Streetname (FI)"),
    STREETNAME_SE("Streetname (SE)"),
    EMPTY("EMPTY"),
    ADRESS_TYPE("Side of the street"),
    SMALL_NUM1("Small number 1"),
    SMALL_DIV1("Small number division letter"),
    SMALL_PUNCT("Small number punctation"),
    SMALL_NUM2("Small number 2"),
    SMALL_DIV2("Small number 2division letter"),
    HIGH_NUM1("High number 1"),
    HIGH_DIV1("High number divition letter"),
    HIGH_PUNCT("High number punctation"),
    HIGH_NUM2("High number 2"),
    HIGH_DIV2("High number 2 division letter"),
    CITY_CODE("City code"),
    CITY_NAME_FI("City name (FI)"),
    CITY_NAME_SE("City name (SE)"),
    EOL("");
    override fun toString() :String {
        return printableName
    }
}

// BAF file format, get exact specification from posti.fi
data class Field(val name: FieldNames, val bytes: Int)

val FIELDS = arrayOf(
    Field(FieldNames.ID,                     5), // Starts at position 1, "Tietuetunnus"
    Field(FieldNames.DATE,                   8), // YYYYMMDD, Starts at position 6, "Ajopäivämäärä"
    Field(FieldNames.POSTALCODE,             5), // Starts at position 14, "Postinumero"
    Field(FieldNames.POSTALCODE_NAME_FI,     30),// Starts at position 19, "Postinumeron nimi suomeksi"
    Field(FieldNames.POSTALCODE_NAME_SE,     30),// Starts at position 49, "Postinumeron nimi ruotsiksi"
    Field(FieldNames.POSTALCODE_ABRR_FI,     12),// Starts at position 79, "Postinumeron nimen lyhenne suomeksi"
    Field(FieldNames.POSTALCODE_ABRR_SE,     12),// Starts at position 91, "Postinumeron nimen lyhenne ruotsiksi"
    Field(FieldNames.STREETNAME_FI,          30),// Starts at position 103, "Kadun (paikan) nimi suomeksi"
    Field(FieldNames.STREETNAME_SE,          30),// Starts at position 133, "Kadun (paikan) nimi ruotsiksi"    
    Field(FieldNames.EMPTY,                  24),// Starts at position 163, "Tyhjä"
    Field(FieldNames.ADRESS_TYPE,            1), // Starts at position 187, "Kiinteistötietojen tyyppi"
                                            // 1 = even side of the street
                                            // 2 = odd side  of the street

    // Pienin kiinteistönumero (parillisen/parittoman kiinteistön tiedot)
    Field(FieldNames.SMALL_NUM1,             5), // Starts at position 188, "Kiinteistönumero 1"
    Field(FieldNames.SMALL_DIV1,             1), // Starts at position 193, example 12a, "Kiinteistön jakokirjain 1"
    Field(FieldNames.SMALL_PUNCT,            1), // Starts at position 194, example 30-32, "Välimerkki"
    Field(FieldNames.SMALL_NUM2,             5), // Starts at position 195, example 30-32, "Kiinteistönumero 2"
    Field(FieldNames.SMALL_DIV2,             1), // Starts at position 200, example 30a-30c, "Kiinteistön jakokirjain 2"   

    // Suurin kiinteistönumero (parillisen/parittoman kiinteistön tiedot)
    Field(FieldNames.HIGH_NUM1,              5), // Starts at position 201, example 72, "Kiinteistönumero 1"
    Field(FieldNames.HIGH_DIV1,              1), // Starts at position 206, example 72a, "Kiinteistön jakokirjain 1"
    Field(FieldNames.HIGH_PUNCT,             1), // Starts at position 207, example 72 - 74, "Välimerkki"
    Field(FieldNames.HIGH_NUM2,              5), // Starts at position 208, example 72 - 74, "Kiinteistönumero 2"   
    Field(FieldNames.HIGH_DIV2,              1), // Starts at position 213, example 72a - 72c, "Kiinteistön jakokirjain 2"   
    Field(FieldNames.CITY_CODE,              3), // Starts at position 214, "Kunnan koodi"   
    Field(FieldNames.CITY_NAME_FI,           20),// Starts at position 217, "Kunnan nimi suomeksi"   
    Field(FieldNames.CITY_NAME_SE,           20),// Starts at position 237, Kunnan nimi ruotsiksi   
    Field(FieldNames.EOL,                    1)  // Starts at position 257   
)

// Dataclass for quaries
data class Query(val query:String, val street:String, val number: Int, val staircase:String, val type:String, var found:Boolean)

class bafReader : CliktCommand("Finnish postal code BAF_VVVVKKPP.dat file reader " + VERSION 
+ "\u0085Copyright (r) Ville Pitkola 2024 / MIT License (https://opensource.org/license/mit)"
+ "\u0085\u0085Example usage:"
+ "\u0085java -jar bafreader.jar --baf=BAF_20240803.dat --input=samplequery.txt"
) {

    // Commandline parameters 
    val bafFile         by option("-b",     "--baf",           help = "BAF-file (get yours from https://www.posti.fi/webpcode)")
    val queryFile       by option("-i",     "--input",         help = "Query file")
    val city            by option("-c",     "--city",          help = "City code, default is 091 (Helsinki)").default("091")
    val printVersion    by option("--version",                 help = "Print version").flag(default=false)
    val debug           by option("-v",     "--debug",         help = "Print debug").flag(default=false)
    val moreDebug       by option("-vv",    "--moredebug",     help = "More debug").flag(default=false)
    val noHeader        by option("-nh",    "--no-header",     help = "Don't print CSV header").flag(default=false)
    

    // List of quaries
    var quaries: MutableList<Query> = mutableListOf<Query>()

    fun debugPrint(msg : String, more : Boolean = false) {
        // -vv?
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

        if(bafFile == null || queryFile == null) {
            System.err.println("Error: Input file(s) missing. Use --help to get help")
            return
        }

        // Read query file into quaries structure
        if(!readQueryFile(queryFile ?: "")) return
    
        // Read BAF file
        try {

            // Open
            debugPrint("Opening :" + bafFile.toString())
            var inputStream: BufferedInputStream
            inputStream = BufferedInputStream(File (bafFile).inputStream())
            debugPrint(bafFile + " contains " + inputStream.available() + " tavua")

            // Output CSV file header
            if(!noHeader) {
                println("Query"
                + ";${FieldNames.STREETNAME_FI}"
                + ";${FieldNames.STREETNAME_SE}"
                + ";${FieldNames.POSTALCODE}"
                + ";${FieldNames.POSTALCODE_NAME_FI}"
                + ";${FieldNames.POSTALCODE_NAME_SE}"
                + ";${FieldNames.POSTALCODE_ABRR_FI}"
                + ";${FieldNames.POSTALCODE_ABRR_SE}"
                + ";Number"
                + ";End of the address"
                + ";Address FI"
                + ";Address SE"
                + ";BAF entry start"
                + ";BAF entry start"
                + ";Run date (YYYY-MM-DD)"
                )
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
            debugPrint(e.toString())
        }
    }

    fun readQueryFile(qf: String) : Boolean{
        var ret: Boolean = true
        var reader: BufferedReader
        try {
            debugPrint("Opening :" + qf)
            reader = BufferedReader(File (qf).inputStream().reader())
            var line = reader.readLine()
            while (line != null) {

                // Parse Street address:
                // - Name starts with capital letter
                // - Shortest possible street is 3 letters
                // - Longest possible street name is 35 letters
                // - Comination numbers like 1-3 are allowed, 1st number is significant for comparision
                // - We also capture possible staircase and/or apartment after the number
                val addressRegex = Regex( "^(?:^([A-ZÅÄÖ]\\D{2,34})\\s+(\\d{1,4}))(?:([-–]\\d{1,3}))?(.*)$")
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
                    // Address comparision is done as case insensitive so store streetname in uppercase
                    // If streetname is longer than 30 characters, we
                    // truncate before comparision as BAF entry is limited to 
                    // 30 characters                                    
                    var s:String = (matchResult.groupValues[1]).trim().uppercase()
                    if(s.length > 30) {
                        s = s.substring(0,30)
                        debugPrint("Truncating too long street name to \"$s\" since BAF entry can not have more than 30 characters")
                    }                    
                    // Generate query dataclass object and add it to the list of quaries
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
            val row = Array(FieldNames.values().size) {""}
            for ((title, bytes) in FIELDS) {
                // Read BAF field and strip extra spaces
                row[title.ordinal] = inputStream.readNBytes(bytes).toString(Charsets.ISO_8859_1).trim()
            }

            // Incorrect city?
            if(row[FieldNames.CITY_CODE.ordinal] != city) {
                debugPrint("Skipping incorrect city" + row[FieldNames.CITY_CODE.ordinal], true)
                continue
            } 

            // Browse trough all quaries
            val qit = quaries.listIterator()
            var quariesLeft : Boolean = false
            for(q in qit) {

                // Performance optiomization: check if at lest one query is still without an answer
                if(q.found == false) {
                    quariesLeft = true
                }
                // 
                if( q.found == false // Allready found
                && q.type == row[FieldNames.ADRESS_TYPE.ordinal] // Right side of the street
                &&  (
                    // Case insensitive comparision for both swedish and finnish versions
                    q.street == row[FieldNames.STREETNAME_FI.ordinal].uppercase()
                    || q.street == row[FieldNames.STREETNAME_SE.ordinal].uppercase()
                    )
                ){                    
                    debugPrint("Street " + q.street + " found", false)
                    // Smallest house number in BAF row
                    val minNumber = row[FieldNames.SMALL_NUM1.ordinal].toInt()

                    // Largest house number in BAF row, use MAX_VALUE is upperlimit does not exist
                    var maxNumber = Int.MAX_VALUE
                    if (row[FieldNames.HIGH_NUM2.ordinal] == "") {
                        if (row[FieldNames.HIGH_NUM1.ordinal] != "") {
                            maxNumber = row[FieldNames.HIGH_NUM1.ordinal].toInt()
                        }
                    } else {
                        maxNumber = row[FieldNames.HIGH_NUM2.ordinal].toInt()
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
                            "${row[FieldNames.STREETNAME_FI.ordinal]};" +
                            "${row[FieldNames.STREETNAME_SE.ordinal]};" +
                            "${row[FieldNames.POSTALCODE.ordinal]};" +
                            "${row[FieldNames.POSTALCODE_NAME_FI.ordinal]};" +
                            "${row[FieldNames.POSTALCODE_NAME_SE.ordinal]};" +
                            "${row[FieldNames.POSTALCODE_ABRR_FI.ordinal]};" +
                            "${row[FieldNames.POSTALCODE_ABRR_SE.ordinal]};" +
                            "${q.number};" +
                            "${q.staircase};" +
                            "${row[FieldNames.STREETNAME_FI.ordinal]} ${q.number}${q.staircase};" +
                            "${row[FieldNames.STREETNAME_SE.ordinal]} ${q.number}${q.staircase};" +
                            "$minNumber;" +
                            l + ";" +
                            // Date is in YYYYMMDD format, lets convert to YYYY-MM-DD
                            "${row[FieldNames.DATE.ordinal].substring(0,4)}" +       // YYYY
                            "-" + "${row[FieldNames.DATE.ordinal].substring(4,6)}"   // MM
                            + "-" + "${row[FieldNames.DATE.ordinal].substring(6,8)}" // DD
                        )
                        q.found = true
                    }
                    else {
                        debugPrint("${q.number} is not between $minNumber and $maxNumber")
                    } 
                } else {
                    debugPrint("No match: ${row[FieldNames.STREETNAME_FI.ordinal]}/${row[FieldNames.STREETNAME_SE.ordinal]}/${row[FieldNames.ADRESS_TYPE.ordinal]}/${q.found}",true)
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