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
package domain;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jlab.clas.pdg.PDGParticle;

public class ReadGiBuuOutput {

	private String inputFileName;
	private String outputFileName;

	private int nParticles = 0;
	private int partIndex = 0;
	private double partWeight;
	private double rotationAngle;
	private List<String> allLines;
	private List<List<String>> chunks;

	private List<LundHeader> lundHeader;
	private List<LundParticle> lundParts;

	private int setmotherPID;
	private boolean skimFile = false;

	OpenCloseLundFileImpl openCloseLundFile = null;
	private int nEventsOut;
	private int eventsDone;
	private int lundPartNum;

	public ReadGiBuuOutput(String inputFileName) {
		initArrays();
		this.inputFileName = inputFileName;
		eventsDone = 0;
	}

	public ReadGiBuuOutput(String inputFileName, String outFileName) {
		initArrays();

		eventsDone = 0;
		lundPartNum = 1;

		this.inputFileName = inputFileName;
		outFileName = outFileName.replaceAll("\\.lund", "_" + lundPartNum + ".lund");

		this.outputFileName = outFileName;

		openCloseLundFile = new OpenCloseLundFileImpl(outputFileName);
		openCloseLundFile.openLundFile();
	}

	private void initArrays() {
		allLines = new ArrayList<String>();
		chunks = new ArrayList<List<String>>();
		lundHeader = new ArrayList<LundHeader>();
		lundParts = new ArrayList<LundParticle>();
	}

	public void runConversion() {
		readFile();
	}

	public void runConversion(int motherPID) {
		this.setmotherPID = motherPID;
		this.skimFile = true;
		readFile();
		openCloseLundFile.closeLundFile();

	}

	private void readFile() {
		try {
			allLines = Files.readAllLines(new File(inputFileName).toPath(), Charset.defaultCharset());
			chunks = getChunks(allLines);
			for (int j = 0; j < chunks.size(); j++) {
				// List<String> strList = chunks.get(j);
				setNParticles(chunks.get(j));
				seperateList(chunks.get(j));
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private static List<List<String>> getChunks(List<String> allLines) {
		List<List<String>> result = new ArrayList<List<String>>();
		int i = 0;
		int fromIndex = 0;
		int toIndex = 0;
		for (String line : allLines) {
			i++;
			if (line.startsWith("</event>")) {
				toIndex = i - 1;
				result.add(allLines.subList(fromIndex, toIndex));
				fromIndex = i;
			}
		}
		return result;
	}

	private void seperateList(List<String> strList) {
		lundHeader.clear();
		lundParts.clear();
		for (String string : strList) {
			List<String> partList = new ArrayList<String>();
			partList.add(string);
			if (string.contains("<event>") || string.contains("#")) {
				if (string.contains("<event>")) {
					partIndex = 0;
					partWeight = Double.parseDouble(string.split("\\s+")[1]);
				}
				if (string.contains("#")) {
					createLundHeader(partList);
				}
				continue;
			} else if (string.isEmpty()) {
				continue;
			} else {
				createLundParts(partList);
			}
			partList.clear();
		}
		createLundFile();
	}

	private void createLundHeader(List<String> list) {
		for (String string : list) {
			String[] splited = string.split("\\s+");
			addHeader(getNParticles(), 1, 1, 0.0, 0.0, 0.0, 0.0, Double.parseDouble(splited[3]),
					Double.parseDouble(splited[2]));
			rotationAngle = Double.parseDouble(splited[6]);
		}
	}

	private void createLundParts(List<String> list) {
		partIndex++;
		for (String string : list) {
			String[] splited = string.trim().split("\\s+");
			PDGParticle pdgParticle = GiBUUDatabase.getParticleById(Integer.parseInt(splited[0]),
					Integer.parseInt(splited[1]));
			addParticle(partIndex, pdgParticle.charge(), 1, pdgParticle.pid(), Integer.parseInt(splited[6]), 0,
					Double.parseDouble(splited[3]), Double.parseDouble(splited[4]), Double.parseDouble(splited[5]),
					Double.parseDouble(splited[2]), pdgParticle.mass(), 0.0, 0.0, 0.0);
		}
	}

	private void addParticle(int index, int charge, int type, int pid, int parentIndex, int daughterIndex, double px,
			double py, double pz, double energy, double mass, double vx, double vy, double vz) {
		lundParts.add(new LundParticle(index, charge, type, pid, parentIndex, daughterIndex, px, py, pz, energy, mass,
				vx, vy, vz));
	}

	private void addHeader(int numParticles, int numTargetNuc, int numTargetProt, double targetPol, double beamPol,
			double x, double y, double q2, double nu) {
		double W = Math.sqrt(-q2 + Math.pow(0.938272, 2) + 2.0 * 0.938272 * nu);
		lundHeader.add(new LundHeader(numParticles, numTargetNuc, numTargetProt, targetPol, beamPol, x, partWeight, W,
				q2, nu));
	}

	private void createLundFile() {
		// rotate scattered lepton around z-axis angle 0<theta<2pi
		// rotateIntoYPlane(lundParts);

		if (skimFile) {
			boolean skimmed = skimmer();

			if (skimmed) {
				rotateIntoYPlane(lundParts);

				// System.out.println("before scatter rotation " +
				// lundParts.get(lundParts.size() - 1) + " "
				// + lundParts.get(lundParts.size() - 1).getPhi() + " "
				// + lundParts.get(lundParts.size() - 1).getTheta() + " py = "
				// + lundParts.get(lundParts.size() - 1).getPy() + " magMomenta
				// "
				// + lundParts.get(lundParts.size() - 1).getMag());
				rotateScatteredLepton(lundParts);
				// System.out.println("After scatter rotation " +
				// lundParts.get(lundParts.size() - 1) + " "
				// + lundParts.get(lundParts.size() - 1).getPhi() + " "
				// + lundParts.get(lundParts.size() - 1).getTheta() + " py = "
				// + lundParts.get(lundParts.size() - 1).getPy() + " magMomenta
				// "
				// + lundParts.get(lundParts.size() - 1).getMag());
				writeFile();
			}

		} else
			writeFile();

	}

	private void writeFile() {
		for (LundHeader lh : lundHeader) {
			if (eventsDone == nEventsOut) {
				eventsDone = 0;
				lundPartNum++;
				openCloseLundFile.closeLundFile();
				outputFileName = outputFileName.replaceAll((lundPartNum - 1) + "\\.lund", lundPartNum + ".lund");
				openCloseLundFile.openLundFile(outputFileName);
			}
			openCloseLundFile.writeEvent(lh);
			for (LundParticle lp : lundParts) {
				openCloseLundFile.writeEvent(lp);
			}
			openCloseLundFile.writeFlush();
			eventsDone++;

		}
	}

	private boolean skimmer() {
		int gammaCount = 0;
		int electronCount = 0;
		int positronCount = 0;
		int protonCount = 0;
		int mothercount = 0;

		if (this.lundParts.size() <= 5) {
			for (LundParticle lp : lundParts) {

				if (lp.getPid() == -11) {
					positronCount++;
				}
				if (lp.getPid() == 11) {
					electronCount++;
				}
				if (lp.getPid() == 2212) {
					protonCount++;
				}
				if (lp.getPid() == 22) {
					gammaCount++;
				}
				if (lp.getParentIndex() == setmotherPID) {
					mothercount++;
				}
			}
			if (mothercount == 3 && protonCount == 1 && positronCount == 1 && electronCount == 2 && gammaCount > 0) {

				return true;
			} else
				return false;
		}
		return false;

	}

	private void rotateIntoYPlane(List<LundParticle> aList) {
		for (LundParticle lp : aList) {
			LundParticle lp2 = lp.rotateY(-rotationAngle);
			aList.set(aList.indexOf(lp), lp2);
		}

	}

	private void rotateScatteredLepton(List<LundParticle> aList) {
		Random rn = new Random();
		double newAngle = (2.0 * Math.PI) * rn.nextDouble();
		LundParticle lParticle = aList.get(aList.size() - 1);
		aList.set(aList.indexOf(lParticle), lParticle.rotateZ(newAngle));
	}

	private int getNParticles() {
		return nParticles;
	}

	private void setNParticles(List<String> list) {
		this.nParticles = list.size() - 2;
	}

	public void setNEvents(int nEventsOut) {
		this.nEventsOut = nEventsOut;
	}

}
