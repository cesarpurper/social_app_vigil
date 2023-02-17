package vigil.cesar.socialApp.util

import java.text.SimpleDateFormat
import java.util.Date

/**
 * This singleton object is a helper to the whole app
 */
object Utils {
  val DATE_FORMAT = "dd/MM/yyyy HH:mm:ss"


  /**
   * This function receives a object date and converts it to a string formated as DATE_FORMAT
   * @param date
   * @return
   */
  def getDateAsString(date: Date): String = {
    val dateFormat = new SimpleDateFormat(DATE_FORMAT)
    dateFormat.format(date)
  }

  /**
   * This function receives a date string formatted as DATE_FORMAT and converts it to Date
   * @param dateAsString
   * @return
   */
  def convertStringToDate(dateAsString: String): Date = {
    val dateFormat = new SimpleDateFormat(DATE_FORMAT)
    dateFormat.parse(dateAsString)
  }
}
