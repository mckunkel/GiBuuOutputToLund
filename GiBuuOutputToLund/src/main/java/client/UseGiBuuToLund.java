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
		ReadGiBuuOutput myreader = new ReadGiBuuOutput(args[0], args[1]);
		myreader.setNEvents(Integer.valueOf(args[2]));
		// myreader.runConversion(106);
		myreader.runConversion();

		System.exit(0);
	}

}
