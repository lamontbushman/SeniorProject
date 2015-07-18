package lbushman.audioToMIDI.processing;

import java.util.List;

import javax.sound.sampled.AudioFormat;

public class AudioData {
	private AudioFormat format;
	private byte[] sampledData;
	
	//TODO get better name for this
	private int[] originalSignal;
	private Complex[] complexData;
	private double overlapPercent;
	private int fftLength;
	private boolean dataWindowed;
	private Complex[] fft;
	private Double[] fftAbsolute;
	private Double[] fftLowPassAbsolute;
//	private Double[] fftCepstrum;
	private Double[] frequencies;
	private String[] noteNames;
	private Double[] normalizedFrequencies;
	private Double[] fftInverseTest;
	private Double[] autoCorrelationAbsolute;
	private Integer numFFT;
	
	private boolean dataHanned;
	private List<Double> spectralFlux;
	private List<Integer> onsets;
	
	public AudioData(byte[] samples, AudioFormat audioFormat) {
		format = audioFormat;
		//Data in bytes.
		sampledData = samples;
		//Format first needs to be set before toIntArray() called.
		originalSignal = toIntArray(samples);
		numFFT = null;
	}
	
	private int[] toIntArray(byte[] bites) {
		//TODO Check format is set
		if(format.getSampleSizeInBits() == 16) {
			int[] array = new int[bites.length / 2];
			int arrayIndex = 0;
			int first = 0 ;
			int second = 1;
			if(!format.isBigEndian()) {
				first = 1;
				second = 0;
			} 
			for(int i = 0; i < bites.length - 1; i+=2) {
				array[arrayIndex] = (bites[i + first] << 8) | (bites[i + second] & 0xFF);
				//System.out.print(Integer.toHexString(array[arrayIndex]) + " ");
				arrayIndex++;
			}
			return array;
		} else {
			int[] array = new int[bites.length];
			for(int i = 0; i < bites.length; i++) {
				array[i] = bites[i];
//				System.out.println(array[i] + "  " + bites[i]);
			}
			return array;
		}
	}
	
	public int getNumFFT() {
		if(numFFT == null) {
			if(getFftAbsolute() != null) {
				//TODO getFFTLength() better have the updated FFT length
				numFFT = getFftAbsolute().length / getFftLength();
			} else {
				return 0;
			}
		}
		return numFFT;
	}
	
	public AudioFormat getFormat() {
		return format;
	}

	public byte[] getSampledData() {
		return sampledData;
	}
	
	public int[] getOriginalSignal() {
		return originalSignal;
	}
	
	public Complex[] getComplexData() {
		return complexData;
	}

	public void setComplexData(Complex[] complexData) {
		this.complexData = complexData;
	}

	public double getOverlapPercentage() {
		return overlapPercent;
	}

	public void setOverlapPercentage(double overlapPercentage) {
		overlapPercent = overlapPercentage;
	}
	
	public int getFftLength() {
		return fftLength;
	}

	public void setFftLength(int fftLength) {
		this.fftLength = fftLength;
	}
	
	public boolean isDataWindowed() {
		return dataWindowed;
	}

	public void setDataWindowed() {
		this.dataWindowed = true;
	}
	
	public Double[] getFftAbsolute() {
		return fftAbsolute;
	}

	public void setFftAbsolute(Double[] fftAbsolute) {
		this.fftAbsolute = fftAbsolute;
	}

	public Double[] getFrequencies() {
		return frequencies;
	}

	public void setFrequencies(Double[] frequencies) {
		this.frequencies = frequencies;
	}

	public String[] getNoteNames() {
		return noteNames;
	}

	public void setNoteNames(String[] noteNames) {
		this.noteNames = noteNames;
	}

	public Double[] getNormalizedFrequencies() {
		return normalizedFrequencies;
	}

	public void setNormalizedFrequencies(Double[] normalizedFrequencies) {
		this.normalizedFrequencies = normalizedFrequencies;
	}

	public void setFftInverseTest(Double[] fftData) {
		fftInverseTest = fftData;
		// TODO Auto-generated method stub
	}
	
	public Double[] getFftInverseTest() {
		return fftInverseTest;
	}

	public Complex[] getFft() {
		return fft;
	}

	public void setFft(Complex[] fft) {
		this.fft = fft;
	}

	public Double[] getAutoCorrelationAbsolute() {
		return autoCorrelationAbsolute;
	}

	public void setAutoCorrelationAbsolute(Double[] autoCorrelationAbsolute) {
		this.autoCorrelationAbsolute = autoCorrelationAbsolute;
	}

	public boolean isDataHanned() {
		return dataHanned;
	}

	public void setDataHanned() {
		this.dataHanned = true;
	}

	public Double[] getFftLowPassAbsolute() {
		return fftLowPassAbsolute;
	}

	public void setFftLowPassAbsolute(Double[] fftLowPassAbsolute) {
		this.fftLowPassAbsolute = fftLowPassAbsolute;
	}
	
	public List<Double> getSpectralFlux() {
		return spectralFlux;
	}

	public void setSpecralFlux(List<Double> spectralFlux) {
		// TODO Auto-generated method stub
		this.spectralFlux = spectralFlux;
	}
	
	public List<Integer> getOnsets() {
		return onsets;
	}

	public void setOnsets(List<Integer> onsets) {
		this.onsets = onsets;
	}
}