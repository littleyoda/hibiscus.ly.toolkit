package de.open4me.ly.webscraper.runner.chromejsdriver;

public class WebProgressMonitor {

	   /**
	    * Schreibt die Meldung mit den ProgressMonitor.
	    *
	    * @param message zu anzuzeigende Nachricht.
	    */
	   public static void log(String message) {

//	      BeanService service = Application.getBootLoader().getBootable(BeanService.class);
//	      Logger.trace("MonitorLog-Invoke eingegangen - BeanService: " + service.toString());
//	      SynchronizeSession session = service.get(WebSynchronizeBackend.class).getCurrentSession();
//	      Logger.trace("MonitorLog-Invoke eingegangen - SynchronizeSession: ");
//
//	      if (session != null) {
//	         ProgressMonitor monitor = session.getProgressMonitor();
//	         monitor.log(message);
//	      }
//	      else {
//	         Logger.warn("SynchronizeSession ist null; daher kann keine MonitorLog ausgegeben werden");
//	      }
	   }





	   /**
	    * Setzt den aktuellen Fortschritt des ProgressMonitor-Balken in Prozent.
	    *
	    * @param Prozentwert als ganzer Integer.
	    */
	   public static void setPercentComplete(int prozent) {

//	      BeanService service = Application.getBootLoader().getBootable(BeanService.class);
//	      SynchronizeSession session = service.get(WebSynchronizeBackend.class).getCurrentSession();
//
//	      if (session != null) {
//
//	         ProgressMonitor monitor = session.getProgressMonitor();
//	         Logger.trace("MonitorLog-Fortschritt wird per Invoke auf '" + String.valueOf(prozent) + "' Prozent gesetzt");
//	         monitor.setPercentComplete(prozent);
//	      }
//	      else {
//	         Logger.warn("SynchronizeSession ist null; daher kann kein Monitor-Fortschritt ausgegeben werden");
//	      }
	   }
	}