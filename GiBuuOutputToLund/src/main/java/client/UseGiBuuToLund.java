/*  +__^_________,_________,_____,________^-.-------------------,
 *  | |||||||||   `--------'     |          |                   O
 *  `+-------------USMC----------^----------|___________________|
 *    `\_,---------,---------,--------------'
 *      / X MK X /'|       /'
 *     / X MK X /  `\    /'
 *    / X MK X /`-------'
 *   / X MK X /
 *  / X MK X /
 * (________(                @author m.c.kunkel
 *  `------'
*/
package client;

import domain.ReadGiBuuOutput;

public class UseGiBuuToLund {

	public static void main(String[] args) {
		// System.out.println("In new build");
		ReadGiBuuOutput myreader = new ReadGiBuuOutput(args[0], args[1]);
		myreader.setNEvents(Integer.valueOf(args[2]));
		myreader.runConversion(106);

		// Map<LundHeader, List<LundParticle>> lundMap = myreader.getLundMap();
		// String outputStr = null;
		//
		// if (args[1] instanceof String) {
		// outputStr = args[1];
		//
		// } else {
		// System.err.println("Second arguement must be a String");
		// System.exit(1);
		// }
		// int eventNumber = 0;
		// int lundPartNum = 1;
		//
		// outputStr = outputStr.replaceAll("\\.lund", "_" + lundPartNum +
		// ".lund");
		// OpenCloseLundFile openCloseLundFile = new
		// OpenCloseLundFileImpl(outputStr);
		// openCloseLundFile.openLundFile();
		// for (Map.Entry<LundHeader, List<LundParticle>> entry :
		// lundMap.entrySet()) {
		// if (eventNumber == Integer.valueOf(args[2])) {
		// eventNumber = 0;
		// lundPartNum++;
		// openCloseLundFile.closeLundFile();
		// outputStr = outputStr.replaceAll((lundPartNum - 1) + "\\.lund",
		// lundPartNum + ".lund");
		// openCloseLundFile.openLundFile(outputStr);
		// }
		// openCloseLundFile.writeEvent(entry.getKey());
		// for (LundParticle lundParticle : entry.getValue()) {
		// openCloseLundFile.writeEvent(lundParticle);
		// }
		// openCloseLundFile.writeFlush();
		// eventNumber++;
		// }
		// openCloseLundFile.closeLundFile();
		System.exit(0);
	}

}
