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
import org.jlab.clas.physics.LorentzVector;
import org.jlab.clas.physics.Particle;

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
	private List<LorentzVector> particles;

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

		particles = new ArrayList<LorentzVector>();
	}

	public void runConversion() {
		readFile();
		openCloseLundFile.closeLundFile();
		System.out.println("Finished with " + eventsDone + " closing file " + outputFileName);
	}

	public void runConversion(int motherPID) {
		this.setmotherPID = motherPID;
		this.skimFile = true;
		readFile();
		openCloseLundFile.closeLundFile();
		System.out.println("Finished with " + eventsDone + " closing file " + outputFileName);

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
		// System.out.println(" Invariant EpEmGam.mass " + IVEpEmGam.mass() + "
		// mxPEscat " + MxPElScat.mass() + " mxpEmEpEm "
		// + MxPElScatEpEm.mass() + " mxPEmEpEmGam " + MxPElScatEpEmGam.mass() +
		// " proton mass? "
		// + MxElScatEpEmGam.mass() + " recoilPmass" + recoilP.mass());
		if (skimFile) {
			boolean skimmed = skimmer();

			if (skimmed) {

				rotateIntoYPlane(lundParts);

				rotateScatteredLepton(lundParts);
				// printMass(lundParts);
				// System.out.println("before scatter rotation " +
				// lundParts.get(lundParts.size() - 1) + " "
				// + lundParts.get(lundParts.size() - 1).getPhi() + " "
				// + lundParts.get(lundParts.size() - 1).getTheta() + " py = "
				// + lundParts.get(lundParts.size() - 1).getPy() + " magMomenta
				// "
				// + lundParts.get(lundParts.size() - 1).getMag());

				// System.out.println("After scatter rotation " +
				// lundParts.get(lundParts.size() - 1) + " "
				// + lundParts.get(lundParts.size() - 1).getPhi() + " "
				// + lundParts.get(lundParts.size() - 1).getTheta() + " py = "
				// + lundParts.get(lundParts.size() - 1).getPy() + " magMomenta
				// "
				// + lundParts.get(lundParts.size() - 1).getMag());
				// createLorentzList(lundParts);
				// printIVMass(lundParts);
				writeFile();
			}

		} else {
			rotateIntoYPlane(lundParts);

			rotateScatteredLepton(lundParts);

			writeFile();
		}

	}

	private void writeFile() {
		for (LundHeader lh : lundHeader) {
			if (eventsDone == nEventsOut) {
				System.out.println("Finished with " + eventsDone + " closing file " + outputFileName);
				eventsDone = 0;
				lundPartNum++;
				openCloseLundFile.closeLundFile();
				outputFileName = outputFileName.replaceAll((lundPartNum - 1) + "\\.lund", lundPartNum + ".lund");
				openCloseLundFile.openLundFile(outputFileName);
				System.out.println("Opening new file " + outputFileName);

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

	private void createLorentzList(List<LundParticle> aList) {
		for (LundParticle lp : aList) {
			particles.add(lp.vector());
		}

	}

	private void printMass(List<LundParticle> aList) {
		// public Particle(int pid, double px, double py, double pz, double vx,
		// double vy, double vz) {

		Particle beam = new Particle(11, 0.0, 0.0, 12.0, 0.0, 0.0, 0.0);// 11.991
		Particle target = new Particle(2212, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		Particle scatElectron = new Particle();
		Particle recoilP = new Particle();
		Particle dalitzElectron = new Particle();
		Particle dalitzPositron = new Particle();
		Particle dalitzPhoton = new Particle();

		int placer = 0;
		for (LundParticle lundParticle : aList) {

			if (lundParticle.getPid() == 11 && placer != 4) {
				dalitzElectron.setVector(11, lundParticle.vector().px(), lundParticle.vector().py(),
						lundParticle.vector().pz(), 0.0, 0.0, 0.0);
			}
			if (lundParticle.getPid() == 11 && placer == 4) {
				scatElectron.setVector(11, lundParticle.vector().px(), lundParticle.vector().py(),
						lundParticle.vector().pz(), 0.0, 0.0, 0.0);
			}
			if (lundParticle.getPid() == 2212) {
				recoilP.setVector(2212, lundParticle.vector().px(), lundParticle.vector().py(),
						lundParticle.vector().pz(), 0.0, 0.0, 0.0);
			}
			if (lundParticle.getPid() == -11) {
				dalitzPositron.setVector(-11, lundParticle.vector().px(), lundParticle.vector().py(),
						lundParticle.vector().pz(), 0.0, 0.0, 0.0);
			}
			if (lundParticle.getPid() == 22) {
				dalitzPhoton.setVector(22, lundParticle.vector().px(), lundParticle.vector().py(),
						lundParticle.vector().pz(), 0.0, 0.0, 0.0);
			}
			placer++;
		}
		Particle IVEpEmGam = new Particle();
		IVEpEmGam.copy(dalitzElectron);
		IVEpEmGam.combine(dalitzPositron, +1);
		IVEpEmGam.combine(dalitzPhoton, +1);

		Particle MxPElScat = new Particle();
		MxPElScat.copy(beam);
		MxPElScat.combine(target, +1);
		MxPElScat.combine(recoilP, -1);
		MxPElScat.combine(scatElectron, -1);

		Particle MxPElScatEpEm = new Particle();
		MxPElScatEpEm.copy(MxPElScat);
		MxPElScatEpEm.combine(dalitzElectron, -1);
		MxPElScatEpEm.combine(dalitzPositron, -1);

		Particle MxPElScatEpEmGam = new Particle();
		MxPElScatEpEmGam.copy(MxPElScatEpEm);
		MxPElScatEpEmGam.combine(dalitzPhoton, -1);

		Particle MxElScatEpEmGam = new Particle();
		MxElScatEpEmGam.copy(MxPElScatEpEmGam);
		MxElScatEpEmGam.combine(recoilP, +1);

		System.out.println(IVEpEmGam.mass() + "    " + MxPElScat.mass() + "    " + MxPElScatEpEm.mass() + "    "
				+ MxPElScatEpEmGam.mass() + "    " + MxElScatEpEmGam.mass() + "    " + recoilP.mass());
		// System.out.println(scatElectron.mass() + " scatter mass ???? " +
		// scatElectron.e() + " energy ???" + scatpt);

		//
		// if (Math.abs(l1.mass() - 0.9578447451185326) > 0.01) {
		// for (LundParticle lundParticle : aList) {
		// System.out.println(lundParticle);
		//
		// }
		//
		// }
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
		for (LundParticle lp : aList) {
			LundParticle lp2 = lp.rotateZ(newAngle);
			aList.set(aList.indexOf(lp), lp2);
		}
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
