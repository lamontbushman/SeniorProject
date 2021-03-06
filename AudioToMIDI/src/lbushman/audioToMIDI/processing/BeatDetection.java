/**
 * TODO When an FFT is ready. A process will feed the FFt into multiple LinkedBlockingQueues.
 */
package lbushman.audioToMIDI.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import lbushman.audioToMIDI.io.TimeSignature;
import lbushman.audioToMIDI.util.Util;

/**
 * Ideas for this came from:
 * http://archive.gamedev.net/archive/reference/programming
 * /features/beatdetection/index.html
 * 
 * @author Lamont Bushman
 *
 */
public class BeatDetection extends Thread {
	// Instead we can have varying width subbands according to a linear
	// progression.
	// Can try the Bark scale
	// Or my own scale
	private static int NUM_SUBBANDS = 4;// 2 //They used 32 then 64
												// //last at 32
	private static final double THRESHOLD = 1.55;// 1.74 //and 1.55/1.6
	// 1.4; Seems like I was using this for seeing which beats were louder by
	// having neighboring beats
	// 1.55; This works great for
	// 1.3 from article //This can also be calculated based on variance

	// 32, 1.2
	// 64,1.3
	private  int SUBBAND_WIDTH;
	private  double SUBBAND_PORTION;
	private final int HALF_FFT_LENGTH;
	private final int FFT_LENGTH;
	private final int NUM_FFTS;
	private  int HISTORY_SIZE; // They calculate this as the number of FFTs
									// in a second. Mine should

	private ProcessQueue<Double, Double> pq;
	private final List<Double> ffts;
	private List<Integer> beats;
	private ArrayList<List<Double>> energySubbands;
	private List<List<MeanNVariance>> meanNVarianceEnergies;
	private LinkedList<Integer> secondBeats;
	private ArrayList<List<Integer>> combinedBeats;
	private List<Double> totalPercentEnergiesAboveAverage;
	private AudioData audioData;
	
	public List<Double> percentages;
	
	BeatDetection(AudioData data, List<Double> ffts, int numThreads) { // int
																		// fftLength,
																		// int
																		// numFFts,
		this.ffts = ffts;
		audioData = data;
		FFT_LENGTH = data.getFftLength();
		NUM_FFTS = data.getNumFFT();
		HISTORY_SIZE = data.getNumFftsInOneSecond();// 2 // Theirs was 43 mine
													// is 32 under current
													// calculations.

		HALF_FFT_LENGTH = FFT_LENGTH / 2;
		SUBBAND_WIDTH = HALF_FFT_LENGTH / NUM_SUBBANDS;
		SUBBAND_PORTION = SUBBAND_WIDTH / (double) HALF_FFT_LENGTH;

		// resultQueue = new LinkedBlockingQueue<Element<Double>>();


		// TODO find a better way to concatenate strings. Like PHP.
	}

	/**
	 * Divide the FFT into subbands and return the energy of each of the
	 * subbands.
	 * 
	 * @param fft
	 * @return
	 */
	private List<Double> divideIntoSubbands(List<Double> halfFFT) {
		List<Double> energyOfSubbands = new ArrayList<Double>(NUM_SUBBANDS);
		// possibly split up based on BARK scale or other one.

		int currentPosition = 0;
		for (int i = 0; i < NUM_SUBBANDS; i++) {
			// TODO
			// Possibly square values first. It asks for modulo square.
			// Doing only the modulo square at FFT creation might speed up my
			// process.
			List<Double> subband = halfFFT.subList(currentPosition,
					currentPosition + SUBBAND_WIDTH);
			Double sum = Util.sum(subband);
			Double energyAverage = sum * SUBBAND_PORTION;
			energyOfSubbands.add(energyAverage);
			currentPosition += SUBBAND_WIDTH;
		}
		return energyOfSubbands;
	}

	public ArrayList<List<Double>> calculateFftsEnergySubbands() {
		// TODO replace with a call to process(void). Where the thread is
		// internal. And a call to await()
		pq.start();

		int current = 0;
		for (int i = 0; i < NUM_FFTS; i++) {
			pq.add(ffts.subList(current, current + HALF_FFT_LENGTH));
			current += FFT_LENGTH;
		}
		pq.signalFinished();

		try {
			pq.join();
		} catch (InterruptedException e) {
			System.err.println("Subband ProcessQueue joining was interrupted");
			e.printStackTrace();
		}
		// TODO probably remove
		if (!pq.isFinished())
			System.err.println("We have problems! Finished but not");

		ArrayList<List<Double>> fftsEnergySubbands = pq.processedList();

		System.out.println("Energy subbands:");
		int count = 0;
		for (List<Double> fftEnergySubband : fftsEnergySubbands) {
			System.out.println(count + ": ");
			for (Double d : fftEnergySubband) {
				System.out.print(d + " ");
			}
			System.out.println();
			count++;
		}
		return fftsEnergySubbands;
	}

	// TOOD if this is too slow, we can probably calculate the averages as I am
	// calculating
	// the energy subbands.
	private List<List<MeanNVariance>> calculateRunningAverage(
			ArrayList<List<Double>> fftsSubbands) {
		List<List<MeanNVariance>> averagesAtFFTs = new ArrayList<List<MeanNVariance>>(
				fftsSubbands.size());

		// TODO possibly calculate the threshold. The negative seems counter
		// intuitive to me.
		// THRESHOLD = (-0.0025714*varianceOfWindow)+1.5142857

		RunningWindowStats[] subbandRunningAverages = new RunningWindowStats[NUM_SUBBANDS];
		// TODO how to initialize a generic default constructor.
		for (int i = 0; i < subbandRunningAverages.length; i++) {
			subbandRunningAverages[i] = new RunningWindowStats(HISTORY_SIZE);
		}

		for (List<Double> fftEnergies : fftsSubbands) {
			int nthSubband = 0;
			List<MeanNVariance> nthFFtSubbandAverages = new ArrayList<MeanNVariance>(
					NUM_SUBBANDS);
			for (Double energy : fftEnergies) {
				subbandRunningAverages[nthSubband].add(energy);
				MeanNVariance mnv = new MeanNVariance();
				mnv.mean = subbandRunningAverages[nthSubband].mean();
				mnv.variance = subbandRunningAverages[nthSubband]
						.variance(mnv.mean);
				nthFFtSubbandAverages.add(mnv);
				nthSubband++;
			}
			averagesAtFFTs.add(nthFFtSubbandAverages);
		}

		/*
		 * for(RunningWindowStats subbandRunningAverage: subbandRunningAverages)
		 * { subbandRunningAverage. }
		 */

		return averagesAtFFTs;

	}

	@SuppressWarnings("unused")
	private double variableThreshold(double variance) {
		if (false)
			return (/*-0.00000025714*/-0.000000000000000000000000000000000000000000 * variance) + 1.55;//142857;
		else
			return THRESHOLD;
	}
	
	private int getSubband(ArrayList<List<Double>> fftsSubbands, List<List<MeanNVariance>> fftsAverageEnergies, int i) {
		List<Double> fftEnergies = fftsSubbands.get(i);
		List<MeanNVariance> meanNVariances = fftsAverageEnergies.get(i);
		double frequency = audioData.getFrequencies().get(i);
		int bin = FundamentalFrequency.computeBin(frequency, audioData);
		int subbandBin = bin / SUBBAND_WIDTH;
		return subbandBin;
	}
	
	
	private List<Double> getPercentages(ArrayList<List<Double>> fftsSubbands,
			List<List<MeanNVariance>> fftsAverageEnergies) {
		List<Double> beats = new LinkedList<Double>();
		if (fftsSubbands.size() != fftsAverageEnergies.size()) {
			System.err.println("Both lists should be the same size.");
		}
		
		Map<Integer,Integer> map = new HashMap<Integer, Integer>();
		
		int wl = 4;		 // window length
		int fr = 0; // front runner
		RunningWindowStats rws = new RunningWindowStats(wl);
		
		for(fr = 0; fr < wl/2 && fr < fftsSubbands.size(); fr++) {
			rws.add(getSubband(fftsSubbands, fftsAverageEnergies, fr));
			beats.add(0.0);
		}
	
		double lastEnergy = 0;
		double lastValue = 0;
		for (int i = 0; i < fftsSubbands.size(); i++) {
			List<Double> fftEnergies = fftsSubbands.get(i);
			List<MeanNVariance> meanNVariances = fftsAverageEnergies.get(i);
			double frequency = audioData.getFrequencies().get(i);
			int bin = FundamentalFrequency.computeBin(frequency, audioData);
			int subbandBin = bin / SUBBAND_WIDTH;
			
			if(fr < fftsSubbands.size()) {
				rws.add(getSubband(fftsSubbands, fftsAverageEnergies, fr));
				fr++;
			} else {
				rws.clear();
				rws.add(getSubband(fftsSubbands, fftsAverageEnergies, i));
			}
			//subbandBin = (int) Math.round(rws.mode().doubleValue());
			
			List<Number> modes = rws.mode();
			switch(modes.size()) {
				case 0:
					return null;
				case 1:
					subbandBin = (int) Math.round(modes.get(0).doubleValue());
					break;
				default:
					subbandBin = getSubband(fftsSubbands, fftsAverageEnergies, i);
					break;
					/*Double sum = 0.0;
					for(Number n : modes) {
						sum += n.doubleValue();
					}
					subbandBin = (int) Math.round(sum / modes.size());*/
			}
			

			
			
			int count = map.containsKey(subbandBin) ? map.get(subbandBin) : 0;
			map.put(subbandBin, count + 1);
			
			int width = 2;
			double totalValue = 0;
			double mean = 0;
			double energy = 0;
			if(subbandBin > 3) {
				int j = subbandBin;
				//for(int j = Math.max(subbandBin - width, 0); j < subbandBin + width; j++) {
					energy = fftEnergies.get(j);
					mean = meanNVariances.get(j).mean;
					double variance = meanNVariances.get(j).variance;
					double value = (energy / mean) /** variance*/;
					totalValue += value;
				//}
			}
			
			//if(totalValue > 120000)
			//	totalValue*= (2/7);
			totalValue = Math.min(totalValue, 200000);
			//totalValue -= 30000;
			//totalValue *= -1;
			//totalValue -= 20000; 
			
			/*totalValue *= 3000;*/

			if(energy <= lastEnergy /*|| totalValue <= 1.2*/ /*|| totalValue <= lastValue */) {
				//totalValue = 0;
			}
			lastEnergy = energy;
			lastValue = totalValue;
			if(totalValue <= 1.4)
				totalValue = 0;
			beats.add(totalValue);
			
/*			double maxValue = 0.0;
			double secondMaxValue = 0.0;
			int k = 0;
			for (int j = 0; j < fftEnergies.size(); j++) {
				double value = fftEnergies.get(j) / meanNVariances.get(j).mean;
				double secondValue = meanNVariances.get(j).variance;
				if(value > maxValue) {
					maxValue = value;
					k = j;
				}
			}
//			System.out.print("(" + i + ")" + k + " ");
			System.out.println(i +", " + k);
			if(k < 100 && maxValue > 1.5)
				beats.add(maxValue);
			else
				beats.add(0.0);*/
		}
		System.out.println();
		
		for(Integer key : map.keySet()) {
			System.out.println(key + " " + map.get(key));
		}
				
		return beats;
	}

	private List<Integer> findBeats(ArrayList<List<Double>> fftsSubbands,
			List<List<MeanNVariance>> fftsAverageEnergies) {
		List<Integer> beats = new LinkedList<Integer>();
		if (fftsSubbands.size() != fftsAverageEnergies.size()) {
			System.err.println("Both lists should be the same size.");
		}

		for (int i = 0; i < fftsSubbands.size(); i++) {
			List<Double> fftEnergies = fftsSubbands.get(i);
			List<MeanNVariance> meanNVariances = fftsAverageEnergies.get(i);
			for (int j = 0; j < fftEnergies.size(); j++) {
				if (fftEnergies.get(j) > variableThreshold(meanNVariances
						.get(j).variance) * meanNVariances.get(j).mean) {
					System.out.println(i + ", " + j + ", " + meanNVariances.get(j).mean 
							+ ", " + meanNVariances.get(j).variance + ", " + variableThreshold(meanNVariances
									.get(j).variance) + ", " + variableThreshold(meanNVariances
											.get(j).variance) * meanNVariances.get(j).mean + ", " + 
									fftEnergies.get(j) + ", " + fftEnergies.get(j) / meanNVariances.get(j).mean + ", " +
											(fftEnergies.get(j) - (variableThreshold(meanNVariances
													.get(j).variance) * meanNVariances.get(j).mean)));
	/*				
					System.out.println("i: " + i + " j: " + j + " mean: " + meanNVariances.get(j).mean 
							+ " var: " + meanNVariances.get(j).variance + " THR: " + variableThreshold(meanNVariances
									.get(j).variance) + " prod: " + variableThreshold(meanNVariances
											.get(j).variance) * meanNVariances.get(j).mean + " current: " + fftEnergies.get(j));*/
					
					beats.add(i); // curious which bands are causing it to
									// trigger. Maybe ignore some of the
									// subbands. Or!!! Only count the subband(s)
									// with the possible frequencies.
					break;
				}
			}
		}
		return beats;
	}

	private List<Integer> findBeatsNearPosition(int position, int searchLen) {
		int lower = position - searchLen;
		int upper = position + searchLen + 1;
		List<Integer> positions = Util.findIntegersInSortedRange(beats, lower,
				upper);
		return positions;
	}

	private int findHighestBeatNearPosition(int position, int searchLen) {
		int lower = Math.max(position - searchLen, 0);
		int upper = Math.min(position + searchLen + 1,
				energySubbands.size() - 1);
		// List<Integer> positions = new LinkedList<Integer>();

		double highestMean = Double.NEGATIVE_INFINITY;
		int highestPosition = -1;
		// I need the averages so this class was and still useful!!!
		for (int i = lower; i < upper; i++) {
			// TODO maybe only search a certain subband. i.e. Maybe higher
			// pitched from the sound of the pluck (not the note)
			double mean = getPercentHigherThanAverage(i);

			/*
			 * List<Double> fftEnergies =
			 * energySubbands.get(i);//subbandAverageEnerg List<Double>
			 * fftAverageEnergies = averageEnergies.get(i); double mean = 0; int
			 * count = 0; for(int j = 0; j < fftEnergies.size(); j++) { //TODO
			 * maybe only search a certain subband. i.e. Maybe higher pitched
			 * from the sound of the pluck (not the note) if(fftEnergies.get(j)
			 * > THRESHOLD * fftAverageEnergies.get(j)) { mean +=
			 * fftEnergies.get(j) / fftAverageEnergies.get(j); count++; } mean
			 * /= count; }
			 */

			if (mean >= highestMean) {
				if (mean == highestMean) {
					System.err.println("mean and highest mean are equal");
				}
				highestMean = mean;
				highestPosition = i;
			}
		}
		return highestPosition;
	}

	public double getPercentHigherThanAverage(int position) {
		if (energySubbands == null || energySubbands.size() == 0) {
			System.err.println("energySubbands Is null");
			return -1;
		}
		int numFFTS = energySubbands.size();
		return getPercentHigherThanAverageTotal(position) / numFFTS;
	}

	public double getPercentHigherThanAverageTotal(int position) {
		if (position >= energySubbands.size()) {
			return 0;
		}
		List<Double> fftEnergies = energySubbands.get(position);// subbandAverageEnerg
		List<MeanNVariance> fftAverageEnergies = meanNVarianceEnergies
				.get(position);
		double total = 0;
		// double mean = 0;
		// int count = 0;
		for (int j = 0; j < fftEnergies.size(); j++) { // TODO maybe only search
														// a certain subband.
														// i.e. Maybe higher
														// pitched from the
														// sound of the pluck
														// (not the note)
			if (fftEnergies.get(j) > /* THRESHOLD * */fftAverageEnergies.get(j).mean) {
				total += fftEnergies.get(j) / fftAverageEnergies.get(j).mean;
				// count++;
			}
		}
		/*
		 * if(mean != 0) mean /= count;
		 */
		return total;
	}

	public List<Double> getTotalBeatsEnergies(final double beatWidth) {
		List<Double> energies = new ArrayList<Double>();
		for (List<Integer> groupedBeat : combinedBeats) {
			Double energyTotal = 0.0;
			for (Integer beatPos : groupedBeat) {
				for (Double energy : energySubbands.get(beatPos)) {
					energyTotal += energy;
				}
			}
			energies.add(energyTotal);
		}
		return energies;
	}

	public List<Double> getTotalPercentEnergiesAboveAverage(
			final double beatWidth) {
		if (this.totalPercentEnergiesAboveAverage != null) {
			return this.totalPercentEnergiesAboveAverage;
		}
		// Perhaps widen the beats (based on a new threshold).
		List<Double> perecentages = new ArrayList<Double>();
		for (List<Integer> groupedBeat : combinedBeats) {
			Double totalEnergy = 0.0;
			for (Integer beatPos : groupedBeat) {
				totalEnergy += getPercentHigherThanAverageTotal(beatPos);// TODO
																			// maybe
																			// replace
																			// with
																			// non
																			// total
																			// version.
			}
			perecentages.add(totalEnergy);
		}
		this.totalPercentEnergiesAboveAverage = perecentages;
		return perecentages;
	}

	public List<Integer> mergedBeats() {
		List<Integer> mergedBeats = new LinkedList<Integer>();
		for (List<Integer> groupedBeats : combinedBeats) {
			int beatSum = 0;
			for (Integer beat : groupedBeats) {
				beatSum += beat;
			}
			int mergedBeat = (int) Math.round((beatSum / ((double) groupedBeats
					.size())));
			mergedBeats.add(mergedBeat);
		}
		return mergedBeats;
	}

	public int beatWidth(List<Integer> beatPositions) {
		// TODO maybe replace this with what I had implemented in main of
		// FFT.java.
		int last = 0;
		int totalDifferences = 0;
		int count = 0;
		for (Integer current : beatPositions) {
			int difference = current - last;
			totalDifferences += difference;

			count++;
			last = current;
		}

		return (int) Math.round(totalDifferences / ((double) count));
	}

	public TimeSignature findTimeSignature2(int estimatedBeatWidth) {
		if (energySubbands != null && meanNVarianceEnergies != null
				&& beats != null) {
			Double BEAT_WIDTH_PERCENTAGE = 0.315;

			// List<Double> percentsHigherThanAverage = new
			// LinkedList<Double>();

			// combineBeats(estimatedBeatWidth);
			List<Double> percentages = getTotalPercentEnergiesAboveAverage(/* beatDifference */0);
			List<Integer> beatPositions = mergedBeats();
			int beatWidth = beatWidth(beatPositions);

			int firstBeat = 0;
			if (beatPositions.size() > 1)
				firstBeat = beatPositions.get(0);
			double currentSearchBeat = firstBeat;

			List<Double> valuesAtBeatWidths = new ArrayList<Double>();
			for (int i = 0; i < beatPositions.size(); i++) {
				int bp = beatPositions.get(i);
				double percentageOfBeat = Math.abs(currentSearchBeat - bp)
						/ beatWidth /* currentSearchBeat */;
				if (currentSearchBeat < bp
						&& percentageOfBeat > BEAT_WIDTH_PERCENTAGE) {
					valuesAtBeatWidths.add(0.0);
					currentSearchBeat += beatWidth;
					i--;
				} else if (percentageOfBeat <= BEAT_WIDTH_PERCENTAGE) {
					valuesAtBeatWidths.add(percentages.get(i));
					currentSearchBeat = bp + beatWidth;
				}
			}
			System.out.println("Beat Width Beats: ");
			for (Double d : valuesAtBeatWidths) {
				System.out.print(d + " ");
			}

			// For now only worry about songs that have a full measure for
			// time!!!!

			int numBeatsPerMeasure = 8; // number of beats per measure to test.
			List<Double> totalAmp = new ArrayList<Double>(numBeatsPerMeasure);
			List<Double> averageAmp = new ArrayList<Double>(numBeatsPerMeasure);

			for (int i = 1; i <= numBeatsPerMeasure; i++) {
				double total = 0;
				int count = 0;

				// TODO I am sure this is necessary. Offset
				// percentsHigherThanAverage by numBeatsPerMeasure, (unless
				// requirements doesn't care for pick up first measures).
				for (int j = 0; j < percentages.size();) {
					total += percentages.get(j);
					count++;
					j += i;
				}
				totalAmp.add(total);
				if (count != 0)
					averageAmp.add(total / count);
				else
					averageAmp.add(total);
			}

			int maxAveragePosition = Util.maxIndex(averageAmp, 0,
					averageAmp.size());
			int maxTotalPosition = Util.maxIndex(totalAmp, 0, totalAmp.size());
			TimeSignature ts = new TimeSignature();
			ts.numerator = maxAveragePosition + 1;
			ts.denominator = 4;
			return ts;

			/*
			 * while(currentBeat < beatPositions.get(beatPositions.size() - 1))
			 * { int nextBeat while(currentBeat < ) }
			 */

			/*
			 * //perhaps the highest is not even required. Just get the offset.
			 * //(probably not the best though because of non perfect distances.
			 * This will account for changes in BPM). //What to do for positions
			 * past last onset? int position = startBeat; while(position <
			 * energySubbands.size()) { List<Integer> positions =
			 * findBeatsNearPosition(position, searchLen); position =
			 * findHighestBeatNearPosition(position, searchLen);
			 * secondBeats.add(position); double percent =
			 * getPercentHigherThanAverage(position);
			 * percentsHigherThanAverage.add(percent);
			 * 
			 * position += beatDifference; }
			 * 
			 * int numBeatsPerMeasure = 8; //number of beats per measure to
			 * test. List<Double> totalAmp = new
			 * ArrayList<Double>(numBeatsPerMeasure); List<Double> averageAmp =
			 * new ArrayList<Double>(numBeatsPerMeasure);
			 * 
			 * for(int i = 1; i <= numBeatsPerMeasure; i++) { double total = 0;
			 * int count = 0;
			 * 
			 * //TODO I am sure this is necessary. Offset
			 * percentsHigherThanAverage by numBeatsPerMeasure, (unless
			 * requirements doesn't care for pick up first measures). for(int j
			 * = 0; j < percentsHigherThanAverage.size(); ) { total +=
			 * percentsHigherThanAverage.get(j); count++; j += i; }
			 * totalAmp.add(total); if(count != 0) averageAmp.add(total/count);
			 * else averageAmp.add(total); }
			 * 
			 * int maxAveragePosition = Util.maxIndex(averageAmp, 0,
			 * averageAmp.size()); int maxTotalPosition =
			 * Util.maxIndex(totalAmp, 0, totalAmp.size()); TimeSignature ts =
			 * new TimeSignature(); ts.numerator = maxAveragePosition + 1;
			 * ts.denominator = 4; return ts;
			 */
		}

		return null;
	}

	public List<List<Integer>> getCombinedBeats() {
		return this.combinedBeats;
	}

	public void combineBeats(final double beatWidth) {
		double BEAT_WIDTH_PERCENTAGE = 0.4;

		this.combinedBeats = new ArrayList<List<Integer>>();

		List<Integer> beatsList = getBeats();

		// int positionSum = beatsList.get(0);

		LinkedList<Integer> currentList = new LinkedList<Integer>();

		for (int currentBeat : beatsList) {
			// Put first one in
			if (currentList.isEmpty()) {
				currentList.add(currentBeat);
			} else {
				int last = currentList.getLast();
				double percentageOfBeat = (currentBeat - last) / beatWidth;
				if (percentageOfBeat < BEAT_WIDTH_PERCENTAGE) {
					currentList.add(currentBeat);
				} else {
					combinedBeats.add(currentList);
					currentList = new LinkedList<Integer>();
					currentList.add(currentBeat);
				}
			}
		}
		if (combinedBeats.size() > 1)
			if (combinedBeats.get(combinedBeats.size() - 1) != currentList) {
				combinedBeats.add(currentList);
			}

		/*
		 * int last = positionSum; int sumCount = 1;
		 * 
		 * for(int i = 1; i < beatsList.size(); i++) { int onset =
		 * beatsList.get(i); if((onset - last) / beatWidth <
		 * BEAT_WIDTH_PERCENTAGE) { positionSum += onset; sumCount++; } else {
		 * int index = (int) Math.round(positionSum/ ((double) sumCount));
		 * positionSum = onset; sumCount = 1; } last = onset; } }
		 */
	}

	/*
	 * public void asdf() { List<Integer> beatsList = getBeats();
	 * 
	 * int positionSum = beatsList.get(0); int last = positionSum; double
	 * percentSum = audioData.getBeatsPercent().get(0); int percPosition = 0;
	 * int sumCount = 1;
	 * 
	 * for(int i = 1; i < beatsList.size(); i++) { int onset = beatsList.get(i);
	 * if((onset - last) / 14.0 < .30) {//.30 //TODO if percent is above a
	 * threshold. Probably not (well that is how I can adjust threshold)
	 * percentSum += audioData.getBeatsPercent().get(percPosition i);
	 * positionSum += onset; sumCount++; } else {
	 * 
	 * if(positionSum == 0) { beatPerc[i] = percentSum / sumCount; } else {
	 * 
	 * 
	 * int index = (int) Math.round(positionSum/ ((double) sumCount)); if(index
	 * == -1) index = positionSum; beatPerc[index] = percentSum / sumCount; // }
	 * 
	 * percentSum = audioData.getBeatsPercent().get(i i); positionSum = onset;
	 * sumCount = 1; } last = onset; percPosition++; }
	 * 
	 * for(int i = 0; i < beatPerc.length; i++) { beatPerc[i] = (beatPerc[i] -
	 * 1.2) * 7002500; if(beatPerc[i] < 0 ) { beatPerc[i] = 0.0; } }
	 * 
	 * 
	 * }
	 */

	public TimeSignature findTimeSignature(int startBeat, int beatDifference) {
		if (energySubbands != null && meanNVarianceEnergies != null
				&& beats != null) {
			// Optimize the percentage here.
			int searchLen = (int) (beatDifference * 0.80);
			searchLen = (searchLen == 0) ? 1 : searchLen;
			this.secondBeats = new LinkedList<Integer>();

			List<Double> percentsHigherThanAverage = new LinkedList<Double>();

			// perhaps the highest is not even required. Just get the offset.
			// (probably not the best though because of non perfect distances.
			// This will account for changes in BPM).
			// What to do for positions past last onset?
			int position = startBeat;
			while (position < energySubbands.size()) {
				List<Integer> positions = findBeatsNearPosition(position,
						searchLen);
				position = findHighestBeatNearPosition(position, searchLen);
				secondBeats.add(position);
				double percent = getPercentHigherThanAverage(position);
				percentsHigherThanAverage.add(percent);

				position += beatDifference;
			}

			int numBeatsPerMeasure = 8; // number of beats per measure to test.
			List<Double> totalAmp = new ArrayList<Double>(numBeatsPerMeasure);
			List<Double> averageAmp = new ArrayList<Double>(numBeatsPerMeasure);

			for (int i = 1; i <= numBeatsPerMeasure; i++) {
				double total = 0;
				int count = 0;

				// TODO I am sure this is necessary. Offset
				// percentsHigherThanAverage by numBeatsPerMeasure, (unless
				// requirements doesn't care for pick up first measures).
				for (int j = 0; j < percentsHigherThanAverage.size();) {
					total += percentsHigherThanAverage.get(j);
					count++;
					j += i;
				}
				totalAmp.add(total);
				if (count != 0)
					averageAmp.add(total / count);
				else
					averageAmp.add(total);
			}

			int maxAveragePosition = Util.maxIndex(averageAmp, 0,
					averageAmp.size());
			int maxTotalPosition = Util.maxIndex(totalAmp, 0, totalAmp.size());
			TimeSignature ts = new TimeSignature();
			ts.numerator = maxAveragePosition + 1;
			ts.denominator = 4;
			return ts;
		}

		return null;
	}

	public List<Integer> getSecondBeats() {
		return secondBeats;
	}

	public List<Double> getBeatPercent() {
		List<Double> amps = new ArrayList<Double>(beats.size());
		for (Integer i : beats) {
			Double d = getPercentHigherThanAverage(i);
			amps.add(d);
		}
		return amps;
	}

	public void detect() {
		HISTORY_SIZE = 9;//data.getNumFftsInOneSecond();// 2 // Theirs was 43 mine
		// is 32 under current
		// calculations.
NUM_SUBBANDS = 128;
SUBBAND_WIDTH = HALF_FFT_LENGTH / NUM_SUBBANDS;
SUBBAND_PORTION = SUBBAND_WIDTH / (double) HALF_FFT_LENGTH;
		pq = new ProcessQueue<Double, Double>(4) {
			@Override
			protected List<Double> process(List<Double> element) {
				return divideIntoSubbands(element);
			}
		};
		
		
		this.energySubbands = calculateFftsEnergySubbands();
		this.meanNVarianceEnergies = calculateRunningAverage(energySubbands);
		beats = findBeats(energySubbands, meanNVarianceEnergies);
		this.percentages = getPercentages(energySubbands, meanNVarianceEnergies);
	}
	
	public List<Integer> getBeats() {
		if (beats != null)
			return beats;
		return null;
	}

	public void run() {
		detect();
	}

	public static void main(String args[]) {
		List<Double> ffts = new LinkedList<Double>();
		final int numFFts = 5;
		final int fftLength = 16;
		final int half = fftLength / 2;

		for (int i = 0; i < numFFts; i++) {
			List<Double> fft = new LinkedList<Double>();
			for (int j = 0; j < half; j++) {
				double numToAdd = (i + 1) * (half - j);
				// System.out.print(numToAdd + " ");
				fft.add(fft.size() / 2, numToAdd);
				fft.add(fft.size() / 2, numToAdd);
			}
			ffts.addAll(fft);
		}

		int count = 0;
		for (Double d : ffts) {
			System.out.print(d + " ");
			count++;
			if (count % fftLength == 0)
				System.out.println();
		}
		System.out.println("\n\n");
		AudioFormat format = new AudioFormat(null, -2.0f, 1, 1, 1, 2.0f, false);

		AudioData data = new AudioData(null, format);
		data.setFftLength(fftLength);
		data.setNumFFT(numFFts);

		BeatDetection bt = new BeatDetection(data, ffts, 1);
		bt.detect();

		System.out.println("Beats");
		for (int i : bt.getBeats()) {
			System.out.print(i + " ");
		}
		System.out.println();
	}
}

class MeanNVariance {
	Double mean;
	Double variance;
}
