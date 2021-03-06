package lbushman.audioToMIDI.processing;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import lbushman.audioToMIDI.util.Util;


public class RunningWindowStats {
	private Queue<Number> window;
	private double total;
	private final int CAPACITY;
	
	public RunningWindowStats(int windowLength) {
		window = new LinkedList<Number>();
		CAPACITY = windowLength;
		total = 0;
	}
	
	public void add(Number number) {
		window.add(number);
		total += number.doubleValue();
		if(window.size() > CAPACITY) {
			double value = window.remove().doubleValue();
			total -= value;
		}
	}
	
	public void clear() {
		window.clear();
		total = 0;
	}
	
	public Number peek() {
		return window.peek();//element() Throws an exception over peek()
	}
	
	public boolean isFull() {
		return window.size() == CAPACITY;
	}
	
	public int size() {
		return window.size();
	}
	
	public List<Number> mode() {
		List<Number> modes = Util.mode(new ArrayList<Number>(window));
		return modes;
		
		/*
		switch(modes.size()) {
			case 0:
				//return -1;
			case 1:
				//return modes.get(0);
			default:
				Double sum = 0.0;
				for(Number n : modes) {
					sum += n.doubleValue();
				}
				//return sum / modes.size();
		}*/
	}
	
	public double mean() {
		return total / window.size();
	}
	
	public double variance() {
		return variance(mean());
	}
	
	public double variance(double mean) {
		if(window.size() < 2)
			return 0;
		
		double sum = 0;
		for(Number number : window) {
			sum += Math.pow(number.doubleValue() - mean, 2);
		}

		return sum / (window.size() - 1); //(window.size());
	}
	
	public double stdDevi() {
		return Math.sqrt(variance());
	}
	
	public double zScore(Number number) {
		double avg = mean();		
		double diff = number.doubleValue() - avg;
		return diff / variance(avg);
	}
	
	/**
	 * Degrees of freedom for Welch's T Test
	 * redwoods.edu WelchTTest.html
	 * @param rws1
	 * @param rws2
	 * @return
	 */
	private static int doF(double v1, double v2, int s1, int s2) {
		double a = v1 / s1;
		double b = v2 / s2;
		double numerator = Math.pow(a + b, 2);
		double leftDen = Math.pow(a, 2) / (s1 - 1);
		double rightDen = Math.pow(b, 2) / (s2 - 1);
		double denominator = leftDen + rightDen;
		//System.out.println((numerator / denominator));
		return (int) Math.round(numerator / denominator);
	}
	
	private static double tStatistic(double m1, double m2, double v1,
											double v2, int s1, int s2) {
		return Math.abs(m1 - m2) / Math.sqrt(v1/s1 + v2/s2);
	}
	
	public static double pValue(RunningWindowStats rws1, RunningWindowStats rws2) {
		double m1 = rws1.mean();
		double m2 = rws2.mean();
		double v1 = rws1.variance(m1);
		double v2 = rws2.variance(m2);
		int s1 = rws1.size();
		int s2 = rws2.size();
		double zScore = rws1.zScore(m2);
		double zScore3 = rws1.zScore(m1);
		double tstat = tStatistic(m1, m2, v1, v2, s1, s2);
		int dF = doF(v1, v2, s1, s2);
		//System.out.println("t: " + tstat + " df: " + dF);
		
		int valueI = 0;
		if(dF < Ttable.table.length) {		
			while(valueI < Ttable.table[dF].length && 
					Ttable.table[dF][valueI] <= tstat) {
				valueI++;
			}

			if(valueI == 0) {
				return Ttable.table[0][valueI];
			} else if(valueI == Ttable.table[dF].length) {
				return Ttable.table[0][valueI - 1];
			} else {
				double valueDiff = Ttable.table[dF][valueI] - tstat;
				double valueM1Diff = tstat - Ttable.table[dF][valueI - 1];
			//	System.out.println("values: " + Ttable.table[dF][valueI  - 1] + " "+ Ttable.table[dF][valueI]);
				// in case equals favor the higher precision.
				if (valueDiff <= valueM1Diff) {
					return Ttable.table[0][valueI];
				} else {
					return Ttable.table[0][valueI - 1];
				}
			}			
		} else {
			//TODO probably just index into the largest array. Or just maybe use ztable
			System.err.println("Degree of Freedom outside of table");
			return 100;
		}
	}
	
	@Override
	public String toString() {
		String text = "";
		for(Number n : window) {
			text += n.doubleValue() + ", ";
		}
		return text;
		//return window.toString();
		
	}
	
	public static void main(String args[]) {
		
		
		double[] treatment = {24,43,58,71,43,49,61,44,67,49,53,56,59,52,62,54,57,33,46,43,57};
		
		RunningWindowStats rws1 = new RunningWindowStats(treatment.length);
		
		for(double d : treatment) {
			rws1.add(d);
		}
		
		double[] control = {42,43,55,26,62,37,33,41,19,54,20,85,46,10,17,60,53,42,37,42,55,28,48};
		
		RunningWindowStats rws2 = new RunningWindowStats(control.length);
		
		for(double d : control) {
			rws2.add(d);
		}
		
		double pValue = RunningWindowStats.pValue(rws1, rws2);
		System.out.println(pValue);
		
		
		/*
		RunningWindowStats rws = new RunningWindowStats(4);
		for(int i = 0; i < 40; i++) {
			rws.add(i);
			System.out.println("i: " + i);
			System.out.println("mean: " + rws.mean());
			System.out.println("variance: " + rws.variance());
			System.out.println("dev: " + rws.stdDevi());
			System.out.println("zscore: " + rws.zScore(i+1));
			System.out.println("zscore: " + rws.zScore(rws.mean()+2.5));
			System.out.println();
		}*/
	}
	
	private static class Ttable{
		public static final double[][] table = {
			{0.1,0.05,0.025,0.01,0.005,0.001},
			{3.078,6.314,12.706,31.821,63.657,318.313},
			{1.886,2.92,4.303,6.965,9.925,22.327},
			{1.638,2.353,3.182,4.541,5.841,10.215},
			{1.533,2.132,2.776,3.747,4.604,7.173},
			{1.476,2.015,2.571,3.365,4.032,5.893},
			{1.44,1.943,2.447,3.143,3.707,5.208},
			{1.415,1.895,2.365,2.998,3.499,4.782},
			{1.397,1.86,2.306,2.896,3.355,4.499},
			{1.383,1.833,2.262,2.821,3.25,4.296},
			{1.372,1.812,2.228,2.764,3.169,4.143},
			{1.363,1.796,2.201,2.718,3.106,4.024},
			{1.356,1.782,2.179,2.681,3.055,3.929},
			{1.35,1.771,2.16,2.65,3.012,3.852},
			{1.345,1.761,2.145,2.624,2.977,3.787},
			{1.341,1.753,2.131,2.602,2.947,3.733},
			{1.337,1.746,2.12,2.583,2.921,3.686},
			{1.333,1.74,2.11,2.567,2.898,3.646},
			{1.33,1.734,2.101,2.552,2.878,3.61},
			{1.328,1.729,2.093,2.539,2.861,3.579},
			{1.325,1.725,2.086,2.528,2.845,3.552},
			{1.323,1.721,2.08,2.518,2.831,3.527},
			{1.321,1.717,2.074,2.508,2.819,3.505},
			{1.319,1.714,2.069,2.5,2.807,3.485},
			{1.318,1.711,2.064,2.492,2.797,3.467},
			{1.316,1.708,2.06,2.485,2.787,3.45},
			{1.315,1.706,2.056,2.479,2.779,3.435},
			{1.314,1.703,2.052,2.473,2.771,3.421},
			{1.313,1.701,2.048,2.467,2.763,3.408},
			{1.311,1.699,2.045,2.462,2.756,3.396},
			{1.31,1.697,2.042,2.457,2.75,3.385},
			{1.309,1.696,2.04,2.453,2.744,3.375},
			{1.309,1.694,2.037,2.449,2.738,3.365},
			{1.308,1.692,2.035,2.445,2.733,3.356},
			{1.307,1.691,2.032,2.441,2.728,3.348},
			{1.306,1.69,2.03,2.438,2.724,3.34},
			{1.306,1.688,2.028,2.434,2.719,3.333},
			{1.305,1.687,2.026,2.431,2.715,3.326},
			{1.304,1.686,2.024,2.429,2.712,3.319},
			{1.304,1.685,2.023,2.426,2.708,3.313},
			{1.303,1.684,2.021,2.423,2.704,3.307},
			{1.303,1.683,2.02,2.421,2.701,3.301},
			{1.302,1.682,2.018,2.418,2.698,3.296},
			{1.302,1.681,2.017,2.416,2.695,3.291},
			{1.301,1.68,2.015,2.414,2.692,3.286},
			{1.301,1.679,2.014,2.412,2.69,3.281},
			{1.3,1.679,2.013,2.41,2.687,3.277},
			{1.3,1.678,2.012,2.408,2.685,3.273},
			{1.299,1.677,2.011,2.407,2.682,3.269},
			{1.299,1.677,2.01,2.405,2.68,3.265},
			{1.299,1.676,2.009,2.403,2.678,3.261},
			{1.298,1.675,2.008,2.402,2.676,3.258},
			{1.298,1.675,2.007,2.4,2.674,3.255},
			{1.298,1.674,2.006,2.399,2.672,3.251},
			{1.297,1.674,2.005,2.397,2.67,3.248},
			{1.297,1.673,2.004,2.396,2.668,3.245},
			{1.297,1.673,2.003,2.395,2.667,3.242},
			{1.297,1.672,2.002,2.394,2.665,3.239},
			{1.296,1.672,2.002,2.392,2.663,3.237},
			{1.296,1.671,2.001,2.391,2.662,3.234},
			{1.296,1.671,2,2.39,2.66,3.232},
			{1.296,1.67,2,2.389,2.659,3.229},
			{1.295,1.67,1.999,2.388,2.657,3.227},
			{1.295,1.669,1.998,2.387,2.656,3.225},
			{1.295,1.669,1.998,2.386,2.655,3.223},
			{1.295,1.669,1.997,2.385,2.654,3.22},
			{1.295,1.668,1.997,2.384,2.652,3.218},
			{1.294,1.668,1.996,2.383,2.651,3.216},
			{1.294,1.668,1.995,2.382,2.65,3.214},
			{1.294,1.667,1.995,2.382,2.649,3.213},
			{1.294,1.667,1.994,2.381,2.648,3.211},
			{1.294,1.667,1.994,2.38,2.647,3.209},
			{1.293,1.666,1.993,2.379,2.646,3.207},
			{1.293,1.666,1.993,2.379,2.645,3.206},
			{1.293,1.666,1.993,2.378,2.644,3.204},
			{1.293,1.665,1.992,2.377,2.643,3.202},
			{1.293,1.665,1.992,2.376,2.642,3.201},
			{1.293,1.665,1.991,2.376,2.641,3.199},
			{1.292,1.665,1.991,2.375,2.64,3.198},
			{1.292,1.664,1.99,2.374,2.64,3.197},
			{1.292,1.664,1.99,2.374,2.639,3.195},
			{1.292,1.664,1.99,2.373,2.638,3.194},
			{1.292,1.664,1.989,2.373,2.637,3.193},
			{1.292,1.663,1.989,2.372,2.636,3.191},
			{1.292,1.663,1.989,2.372,2.636,3.19},
			{1.292,1.663,1.988,2.371,2.635,3.189},
			{1.291,1.663,1.988,2.37,2.634,3.188},
			{1.291,1.663,1.988,2.37,2.634,3.187},
			{1.291,1.662,1.987,2.369,2.633,3.185},
			{1.291,1.662,1.987,2.369,2.632,3.184},
			{1.291,1.662,1.987,2.368,2.632,3.183},
			{1.291,1.662,1.986,2.368,2.631,3.182},
			{1.291,1.662,1.986,2.368,2.63,3.181},
			{1.291,1.661,1.986,2.367,2.63,3.18},
			{1.291,1.661,1.986,2.367,2.629,3.179},
			{1.291,1.661,1.985,2.366,2.629,3.178},
			{1.29,1.661,1.985,2.366,2.628,3.177},
			{1.29,1.661,1.985,2.365,2.627,3.176},
			{1.29,1.661,1.984,2.365,2.627,3.175},
			{1.29,1.66,1.984,2.365,2.626,3.175},
			{1.29,1.66,1.984,2.364,2.626,3.174}
		};
	}
}
