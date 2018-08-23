
package com.github.mauricio.async.db.util


//object Version {
//
//
//
////  fun apply( version : String ) : Version {
////    val pieces = version.split('.')
////    Version( tryParse(0, pieces), tryParse(1, pieces), tryParse(2, pieces) )
////  }
//
//}

data class Version( val major : Int, val minor : Int, val maintenance : Int ) {//: Ordered<Version> {
  companion object {
    private fun tryParse( index : Int, pieces : Array<String> ) : Int {

      return try {
        pieces[index].toInt()
      } catch (e: Exception) {
        0
      }

    }
  }
  fun compare( y: Version): Int {
    TODO()
//    if ( this == y ) {
//      return 0
//    }
//
//    if ( this.major != y.major ) {
//      return this.major.compare(y.major)
//    }
//
//    if ( this.minor != y.minor ) {
//      return this.minor.compare(y.minor)
//    }
//
//    return this.maintenance.compare(y.maintenance)
  }
}
