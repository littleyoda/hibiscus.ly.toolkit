package de.open4me.ly.webscraper.runner.chromejsdriver;
 public class ExternalLogger {

   /**
    * Schreibt eine Log-Nachricht im Modus 'Info' in das eigene System-Log.
    *
    * @param message zu loggende Nachricht.
    */
   public static void info(String message) {

      System.out.println(message);
   }





   /**
    * Schreibt eine Log-Nachricht im Modus 'Warn' in das eigene System-Log.
    *
    * @param message zu loggende Nachricht.
    */
   public static void warn(String message) {

	   System.out.println(message);
   }





   /**
    * Schreibt eine Log-Nachricht im Modus 'Error' in das eigene System-Log.
    *
    * @param message zu loggende Nachricht.
    */
   public static void error(String message) {

	   System.out.println(message);
   }





   /**
    * Schreibt eine Log-Nachricht im Modus 'Debug' in das eigene System-Log.
    *
    * @param message zu loggende Nachricht.
    */
   public static void debug(String message) {

	   System.out.println(message);
   }





   /**
    * Schreibt eine Log-Nachricht im Modus 'Trace' in das eigene System-Log.
    *
    * @param message zu loggende Nachricht.
    */
   public static void trace(String message) {

	   System.out.println(message);
   }
}