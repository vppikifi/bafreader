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

val fieldNameFieldID        : String = "Field id"
val fieldNameDate           : String = "Date"
val fieldNamePostalCode     : String = "Postalcode"
val fieldNamePostOfficeFI   : String = "Name of the postal code FI"
val fieldNamePostOfficeSE   : String = "Name of the postal code SE"
val fieldNameAbbreviationFI : String = "Abbreviation of the postal code FI"
val fieldNameAbbreviationSE : String = "Abbreviation of the postal code SE"
val fieldNameStreetFI       : String = "Street (place) name FI"
val fieldNameStreetSE       : String = "Street (place) name SE"
val fieldNameType           : String = "Type of address"
val fieldNameSmallNum1      : String = "Smallest housenumber 1"
val fieldNameSmallDivision1 : String = "Smallest housenumber 1/division letter"
val fieldNameSmallPunctation: String = "Smallest housenumber/punctation"
val fieldNameSmallNum2      : String = "Smallest housenumber 2"
val fieldNameSmallDivision2 : String = "Smallest housenumber 2/division letter"
val fieldNameHighNum1     : String = "Highest housenumber 1"
val fieldNameHighDivision1  : String = "Highest housenumber 1/division letter"
val fieldNameHighPunctation : String = "Highest housenumber/punctation"
val fieldNameHighNum2       : String = "Highest housenumber 2"
val fieldNameHighDivision2  : String = "Smallest housenumber 2/division letter"
val fieldNameCityCode       : String = "City code"
val fieldNameCityFI         : String = "Name of the city FI"
val fieldNameCitySE         : String = "Name of the city SE"
val fieldName : String = ""

val FIELDS = arrayOf(
    Field(fieldNameFieldID,                         5), // Starts at position 1
    Field(fieldNameDate,                            8), // YYYYMMDD, Starts at position 6
    Field(fieldNamePostalCode,                      5), // Starts at position 14
    Field(fieldNamePostOfficeFI,                    30),// Starts at position 19
    Field(fieldNamePostOfficeSE,                    30),// Starts at position 49
    Field(fieldNameAbbreviationFI,                  12),// Starts at position 79
    Field(fieldNameAbbreviationSE,                  12),// Starts at position 91
    Field(fieldNameStreetFI,                        30),// Starts at position 103
    Field(fieldNameStreetSE,                        30),// Starts at position 133    
    Field("Empty",                                  24),// Starts at position 163
    Field(fieldNameType,                            1), // Starts at position 187, 1 = even side, 2 = odd side
    Field(fieldNameSmallNum1,                       5), // Starts at position 188
    Field(fieldNameSmallDivision1,                  1), // Starts at position 193, example 12a
    Field(fieldNameSmallPunctation,                 1), // Starts at position 194, example 30-32
    Field(fieldNameSmallNum2,                       5), // Starts at position 195, example 30-32
    Field(fieldNameSmallDivision2,                  1), // Starts at position 200, example 30a-30c   
    Field(fieldNameHighNum1,                      5), // Starts at position 201, example 72
    Field(fieldNameHighDivision1,                   1), // Starts at position 206, example 72a
    Field(fieldNameHighPunctation,                  1), // Starts at position 207, example 72 - 74
    Field(fieldNameHighNum2,                        5), // Starts at position 208, example 72 - 74   
    Field(fieldNameHighDivision2,                   1), // Starts at position 213, example 72a - 72c   
    Field(fieldNameCityCode,                        3), // Starts at position 214   
    Field(fieldNameCityFI,                          20),// Starts at position 217   
    Field(fieldNameCitySE,                          20),// Starts at position 237   
    Field("Linefeed", 1)                                // Starts at position 257   
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

    // List of quaries
    var quaries: MutableList<Query> = mutableListOf<Query>()

    fun debugPrint(msg : String, more : Boolean) {
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
                println("Query;$fieldNameStreetFI;$fieldNameStreetSE;$fieldNamePostalCode;$fieldNamePostOfficeFI;$fieldNamePostOfficeSE;$fieldNameAbbreviationFI;$fieldNameAbbreviationSE;Number;End of the address;Address (FI);Address (SE);BAF Entry start;BAF Entry End;Rundate (YYYY-MM-DD)")
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
            debugPrint(e.toString(), false)
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
                        debugPrint("Truncating too long street name to \"$s\" since BAF entry can not have more than 30 characters", false)
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
            val row = HashMap<String, String>()
            for ((title, bytes) in FIELDS) {
                // Read BAF field and strip extra spaces
                row[title] = inputStream.readNBytes(bytes).toString(Charsets.ISO_8859_1).trim()
            }

            // Incorrect city?
            if(row["City code"] != kunta) continue

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
                && q.type == row["Type of address",]

                // Case insensitive comparision for both swedish and finnish versions
                &&  (
                    // Case insensitive comparision for both swedish and finnish versions
                    q.street == (row[fieldNameStreetFI] ?: "").uppercase()
                    || q.street == (row[fieldNameStreetSE] ?: "").uppercase()
                    )
                ){                    
                    debugPrint("Street " + q.street + " found", false)
                    // Smallest house number in BAF row
                    val minNumber = (row[fieldNameSmallNum1] ?: "0").toInt()

                    // Largest house number in BAF row, use MAX_VALUE is upperlimit does not exist
                    var maxNumber = Int.MAX_VALUE
                    if ((row[fieldNameHighNum2] ?: "") == "") {
                        if ((row[fieldNameHighNum1] ?: "") != "") {
                            maxNumber = (row[fieldNameHighNum1] ?: "0").toInt()
                        }
                    } else {
                        maxNumber = (row[fieldNameHighNum2] ?: "0").toInt()
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
                            "${row[fieldNameStreetFI]};" +
                            "${row[fieldNameStreetSE]};" +
                            "${row[fieldNamePostalCode]};" +
                            "${row[fieldNamePostOfficeFI]};" +
                            "${row[fieldNamePostOfficeSE]};" +
                            "${row[fieldNameAbbreviationFI]};" +
                            "${row[fieldNameAbbreviationSE]};" +
                            "${q.number};" +
                            "${q.staircase};" +
                            "${row[fieldNameStreetFI]} ${q.number}${q.staircase};" +
                            "${row[fieldNameStreetSE]} ${q.number}${q.staircase};" +
                            "$minNumber;" +
                            l + ";" +
                            // Date is in YYYYMMDD format, lets convert to YYYY-MM-DD
                            "${(row[fieldNameDate] ?: "").substring(0,4)}" +       // YYYY
                            "-" + "${(row[fieldNameDate] ?: "").substring(4,6)}"   // MM
                            + "-" + "${(row[fieldNameDate] ?: "").substring(6,8)}" // DD
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