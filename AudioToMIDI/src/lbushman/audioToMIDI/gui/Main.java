package lbushman.audioToMIDI.gui;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;

import lbushman.audioToMIDI.io.CaptureAudio;
import lbushman.audioToMIDI.io.PlayAudio;
import lbushman.audioToMIDI.io.ReadAudioFile;
import lbushman.audioToMIDI.io.WriteAudioFile;
import lbushman.audioToMIDI.processing.AudioData;
import lbushman.audioToMIDI.processing.Complex;
import lbushman.audioToMIDI.processing.FrequencyToNote;
import lbushman.audioToMIDI.processing.FundamentalFrequency;
import lbushman.audioToMIDI.processing.ProcessSignal;
import lbushman.audioToMIDI.processing.RunningWindowStats;
import lbushman.audioToMIDI.util.Util;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Main extends Application {
	CaptureAudio audio;
    Button captureButton;
    Button openButton;
    Button displayFFTButton;
    AudioData audioData;
//	private Graph acGraph;
    private Graph centerGraph;
    
	private Graph fftGraph;
		
  //  @SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	@Override public void start(Stage stage) {
        stage.setTitle("Signal Processing Senior Project");       
        
//        acGraph = new Graph("Autocorrelation", "Nth Sample", "Power");
        centerGraph = new Graph("Spectral Flux", "Nth sample", "Power");
        fftGraph = new Graph("FFT", "Nth Sample", "Power");
        
        
        
       	BorderPane border = new BorderPane();
    	HBox box = addHBox();
    	border.setTop(box);
//    	border.setCenter(acGraph);
    	border.setCenter(centerGraph);
    	border.setBottom(fftGraph);
    	
    	Scene scene = new Scene(border,800,600);
        
        //Scene scene  = new Scene(lineChart,800,600);
    	box.requestFocus();
        stage.setScene(scene);
        stage.show();
    }
	
	public List<Integer> findPeaks(List<Number> signal, int firstPeak, int windowLength) {
		List<Integer> peakIndexes = new ArrayList<Integer>();
		peakIndexes.add(firstPeak); //assuming start is the first peak of concern
		RunningWindowStats firstWindow = new RunningWindowStats(windowLength);
		
		int index = firstPeak;
		
		int end = signal.size() / 2;
		
		while(!firstWindow.isFull()) {
			firstWindow.add(signal.get(index));
			index++;
			//for(; !firstWindow.isFull() && index < end; index++) {
		}
		
		for(; index < end; index++) {
			System.out.println(index + "," + firstWindow.variance());
			firstWindow.add(signal.get(index));
		}
		
		return peakIndexes;
	}
	
	/**
	 * Finds the peaks, including firstPeak, in the signal starting the search from firstPeak.
	 * Only searches up to half of the signal assuming the signal is a FFT with duplicate information.
	 * 
	 * Assumes that peaks are at least two windowLengths apart.
	 * 
	 * @param signal
	 * @param windowLength - a length to compare the current versus the next portions of the graph.
	 * @param firstPeak
	 * @param zAllowance - how far the next window is allowed to be off from the current window
	 * before it is counted as the beginning of a peak. It is based off of the z-score from the deviation
	 * of the current window and the average of the next window.
	 * @return
	 */
	public List<Integer> findPeaks(List<Double> signal, int windowLength, int firstPeak, double zAllowance) {	
	/*	if(true) {
			return findPeaks(signal, firstPeak, windowLength);
		}*/
		
		windowLength = 6;//7, 10
		
		List<Integer> peakIndexes = new ArrayList<Integer>();
		peakIndexes.add(firstPeak); //assuming start is the first peak of concern
		
		RunningWindowStats firstWindow = new RunningWindowStats(windowLength);
		RunningWindowStats secondWindow = new RunningWindowStats(windowLength);
		
		int index = firstPeak;
		
		//Only search the first half of the FFT.
		//Because of this I am not worrying about hitting the end of the array
		//Wishy washiness is allowed.
		int end = signal.size() / 2;
		
		//Hopefully this length reaches the peak, but not the next peak.
		int searchPeakLen = (int) (windowLength * 1.5);
		boolean newPeakFound = false;
		//int searchAheadLen = windowLength * 2;
		
		
/*		//TODO possible problem if there is no bottom
		
		//Find bottom of first peak
		while(signal.get(index).doubleValue() > signal.get(index + 1).doubleValue()) {
			index++;
		}
		
		//TODO see if this needs to be removed and not worry about filling the window
		//maybe a modified zAllowance based off rws.size()
		
		//Fill up windows (may be full from previous peak)		
		while(!firstWindow.isFull()) {
			firstWindow.add(signal.get(index));
			index++;
			//for(; !firstWindow.isFull() && index < end; index++) {
		}
		
		while(!secondWindow.isFull()) {
			secondWindow.add(signal.get(index));
			index++;
		}
*/		
		int windowSep = windowLength;
		
		while(index < end) {
			//Find bottom of peak
			while(index < end &&
					signal.get(index).doubleValue() > signal.get(index + 1).doubleValue()) {
				index++;
			}
			
			//TODO see if this needs to be removed and not worry about filling the window
			//maybe a modified zAllowance based off rws.size()
			
			//Fill up windows (may be full from previous peak)		
			while(!firstWindow.isFull()) {
				firstWindow.add(signal.get(index));
				index++;
				//for(; !firstWindow.isFull() && index < end; index++) {
			}
			
			while(!secondWindow.isFull()) {
				secondWindow.add(signal.get(index));
				index++;
			}
			
			//Find beginning of next peak
			for(; index < end; index++) {
				//double mean = secondWindow.mean();
				//double zScore = Math.abs(firstWindow.zScore(mean));
				//System.out.print(index + ": " + zScore + ":");
				//System.out.println(index + ":" + firstWindow.mean() + ":" + mean);
				
				double pValue = RunningWindowStats.pValue(firstWindow, secondWindow);
				System.out.println(index + "," + pValue + "," + ((pValue <= 0.05)?"T":"F") + "," + ((pValue <= 0.1)?"T":"F"));
				
//				System.out.println("zScore[" + (index - windowLength) + "-" + index + "] = " + zScore);
			
				/*
				 55
110
165
220
275
330
385

				*/
				firstWindow.add(secondWindow.peek());
				secondWindow.add(signal.get(index));
				
				
		//		if(index != 109 && index != 165 && index != 218 && index != 274 && index != 328/*zScore <= zAllowance*/) {
/*				if(index != 110 && index != 165 && index != 220 && index != 275 && index != 330 && index != 385zScore <= zAllowance) {
					firstWindow.add(secondWindow.peek());
					secondWindow.add(signal.get(index));
				} else {
					newPeakFound = true;
					break;
				}*/
			}
			
			if(newPeakFound) {
				//Search for peak within a range
				//Start at the beginning of the second window
				int searchBeg = index - windowLength; 
				int searchEnd = searchBeg + searchPeakLen;
				//probably not needed
				if(searchEnd >= end) {
					searchEnd = end;
				}
				
//				System.out.println("Searching from " + searchBeg + " to " + searchEnd);
				index = Util.maxIndex(signal, searchBeg, searchEnd);
				
				//Clear both windows, since the data is probably not accurate
				// with the advent of the new peak.
				//However, doing so, I am forced to assume that the next peak
				// will not start within two windowLengths;
				firstWindow.clear();
				secondWindow.clear();
				
			
				//TODO The top of the peak may depend on hitting end of signal
				peakIndexes.add(index);
//				System.out.println("Peak at index: " + index);
			} else {
				//System.out.println("No new peak found");
				break;
			}
			newPeakFound = false;
		}
		return peakIndexes;
	}
	
	//TODO TODO TODO delta and skip ??? https://www.youtube.com/watch?v=nYED_-eY4Ys , .1 seconds, difficult to analyze with 
	//Enumeration.class
	
	public List<Integer> findPeaks(Number[] signal, int windowLength, int start, int end, int firstPeak, double zAllowance) {
		List<Integer> peakIndexes = new ArrayList<Integer>();
		//TODO firstPeak - start
		peakIndexes.add(firstPeak - start); //assuming start is the first peak of concern
		RunningWindowStats rws = new RunningWindowStats(windowLength);
		end = start + (end - start) / 2;
		int index = firstPeak;
		
		int findPeakWindow = -1;
		
	//	System.out.println("Start: " + (firstPeak - start));
	//	System.out.println("Window Length : " + windowLength);
		boolean newPeakFound = false;
		
		while(index+1 < end) {
			//Find bottom of peak
			while(index+1 < end &&
					signal[index].doubleValue() > signal[index + 1].doubleValue()) {
				index++;
			}
//			System.out.println("Bottom of peak at index: " + index);
				
			//Fill up window (may be full from previous peak)
			//TODO see if this needs to be removed and not worry about filling the window
			// maybe a modified zAllowance based off rws.size()
			for(; index < end && !rws.isFull(); index++) {
/*				if(rws.isFull()) {
					System.out.println("Window is full at index: " + index);
					break;
				} else {
					rws.add(signal[index]);
				}*/
				rws.add(signal[index]);
//				System.out.println("Window filling: " + index);
			}
		
			//Find beginning of next peak
			for(; index + 3< end; index++) {
				double zValue = Math.abs(rws.zScore(signal[index+3]));
//				System.out.println("i: " + index + " zV: " + zValue);
				if(zValue <= zAllowance) {
					rws.add(signal[index]);
				} else {
					newPeakFound = true;
//					System.out.println("Beg of peak at index: " + index);
					break;
				}
			}
			
			if(!newPeakFound) {
//				System.out.println("No new peak found");
				
				//break;
			}
			
			/*//Find top of peak  (may have to adjust zAllowance to get above local peaks)
			while(index+1 < end &&
					signal[index].doubleValue() < signal[index + 1].doubleValue()) {
				index++;
			}*/
			
			//Find top of peak
			if(findPeakWindow == -1) {
				//As soon as the base of the first peak is found.
				//Calculate the window based of 1/4 the distance between the
				//base and the first peak
				findPeakWindow = (index - start) / 4;
			}
			int maxEnd = index + findPeakWindow;
			if(maxEnd >= end) {
				maxEnd = end;
			}
			
			if(newPeakFound) {
				index = Util.maxIndex(signal, index, maxEnd);
			
				//TODO The top of the peak may depend on hitting end of signal
				peakIndexes.add((index - start));
				//System.out.println("Peak at index: " + (index - start));
			} else {
				//System.out.println("No new peak found");
				break;
			}
			newPeakFound = false;
		}
		return peakIndexes;
	}
        
    private void readFromFile(File file) {
		ReadAudioFile audio = new ReadAudioFile(file);
		audio.readFile();
		AudioData audioData = new AudioData(
				audio.getStream().toByteArray(),
				audio.getFormat());
		processSignal(audioData, null);
    }
    
    private void processSignal(AudioData audioData, File file) {		
		// Play the data
		playClip(audioData.getSampledData(), audioData.getFormat());
		
		// Save the data
		if(file != null) {
	    	writeToFile(audioData.getSampledData(), file, audioData.getFormat());
		}
		
		ProcessSignal ps = new ProcessSignal(audioData, 
				0.5 /*overlap of FFTs*/, 1024 /*original fftLength */); //8192
		ps.process();
		
		//TODO why is audioData being passed in?
		this.audioData = audioData;
		
		//TODO ensure multiple reads resets the data appropriately. I think it is.
		
		
		//TODO use this to limit graph indexing, etc.
		//	data.getNumFFT();
													
		
		//TODO possibly show frequencies, normalizedFrequencies, and note names in a graph.
		
		//TODO show notes where consecutive duplicates are not shown.
		//ps.printNonConsecutiveNotes(false);

		//Calculate the amps at time periods
		Double[] maxAmp = ps.computeAmp();
		
		
		List<Double> spectralFlux = audioData.getSpectralFlux();
		displayCenterGraph(spectralFlux);
		
		//Double fftAbsolute[] = audioData.getFftAbsolute();
		Double fftLowpass[] = audioData.getFftLowPassAbsolute();
		
		/*for(int i = 0; i < fftAbsolute.length;i++) {
			if(fftAbsolute[i] != fftLowpass[i]) {
				System.out.println(i + " " + fftAbsolute[i] + "\t" + fftLowpass[i]);
			}
		}
		System.err.println("END OF DIFF!");*/
		
		
		for(int i = 0; i < audioData.getNumFFT(); i++) {
			//TODO needs to be the updated FFT length.
			int start = i * audioData.getFftLength();
			int end = start + audioData.getFftLength();
			
			/*int index = findMax(Arrays.copyOfRange(acAbsolute, start, end), 10);
			double freq = ProcessSignal.computeFrequency(index, audioData);
			String note = FrequencyToNote.findNote(freq);*/
			
			int indexF = findMax(Arrays.copyOfRange(fftLowpass, start, end), 0);
			double freqF = ProcessSignal.computeFrequency(indexF, audioData);
			String noteF = FrequencyToNote.findNote(freqF);
			
			int baseFI = calculateBaseFrequencyIndex(fftLowpass, i, 0);
			double baseF = ProcessSignal.computeFrequency(baseFI, audioData);
			String baseNote = FrequencyToNote.findNote(baseF);
			
        	System.out.println("FFT  i: " + i + "Index: " + indexF + " frequency: " + freqF + " note: " + noteF + " maxAmp[i]: " + maxAmp[i]);
        	//System.out.println("AC   i: " + i + "Index: " + index + " frequency: " + freq + " note: " + note + " maxAmp[i]: " + maxAmp[i]);
        	System.out.println("Base i: " + i + "Index: " + baseFI + " frequency: " + baseF + " note: " + baseNote + " maxAmp[i]: " + maxAmp[i]);
        	System.out.println();
		}
		//displayAC(0);
		displayFFt(0, fftLowpass);
    }
    
    private String getFreqAndNote(int index) {
    	double frequency = ProcessSignal.computeFrequency(index, audioData);
    	String closestNote = FrequencyToNote.findNote(frequency);
    	return frequency + "Hz " + closestNote;
    }
    
    private void updateGraph(Graph graph, Number[] data) {
    	graph.updateList(data);
    }
    
    private <T> void updateGraph(Graph graph, List<T> dataList) {
    	Number[] data = dataList.toArray(new Number[dataList.size()]);
    	graph.updateList(data);
    }
    
    private <T> void updateGraph(Graph graph, List<T> dataList, int index) {
    	//number of fft's
    	Number[] data = dataList.toArray(new Number[dataList.size()]);
    	
    	System.out.println(audioData.getFftAbsolute().length / audioData.getFftLength());	
    	int start = index * audioData.getFftLength();
    	int end = start + audioData.getFftLength();
    	Number[] range = Arrays.copyOfRange(data, start, end);
    	
    	//System.out.println("Index: " + index + " frequency: " + ProcessSignal.computeFrequency(index, audioData));
    	//updateGraph(fft);
    	
/*		if(fft[i] == null)
			System.out.println(i + " is null");
		else
			;//fft[i] = Math.log(fft[i]);*/
    	graph.updateList(range);
    }
    
    private int findMax(Number[] data, final Integer dataStart, final Integer dataEnd, int ignoreFirstN) {
    	int start = dataStart + ignoreFirstN;//114688
    	//Only search half of the data.
    	int end = dataStart + (dataEnd - dataStart) / 2;//116736
    	return Util.maxIndex(data, start, end);
    }
        
    private int findMax(Number[] fft, int ignoreFirstN) {
    	double max = -1;
    	int maxIndex = -1;
    	int start = ignoreFirstN;//toFft.length/4 - 2048; // ignore DC 
    	int end = fft.length/2;// - 2048- fudge;
    	
    	for(int j = start; j <  end; j++) {
    		if(fft == null)
    			System.out.println("it is null");
    		else if(fft[j] == null)
    			System.out.println("it is null!!");
    		double test = fft[j].doubleValue();
    		if(test > max) {
    			max = test;
    			maxIndex = j;
    		}
    	}
    	//maximums[i/fftLength] = max;
    	//4949
    	//System.out.println("!!!!!!!Max Index: " + maxIndex + "  " + toFft[maxIndex] + " " + toFft[maxIndex].absolute());

    	if(maxIndex == start || maxIndex == end)
    		System.out.println("ALERT!!! At the beg/end: " + maxIndex);
    		
		/*if(indexes.size() == -1 || indexes.size() == 0 || prevIndex != indexes.get(indexes.size()-1));
			indexes.add(maxIndex);
		prevIndex = maxIndex;*/
    	
    	return maxIndex;
    }
     
    private void playClip(byte[] signal, AudioFormat format) {
    	PlayAudio play = new PlayAudio(signal, format);
    	play.playClip();	
    }
    
    private HBox addHBox() {
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(10, 12, 10, 12));
        hbox.setSpacing(10);
        hbox.setStyle("-fx-background-color: #336699;");
        TextField captureField = new TextField();
        captureField.setPrefSize(125, 20);
        captureField.promptTextProperty().set(".wav");
        TextField openField = new TextField();
        openField.setPrefSize(125, 20);
        openField.promptTextProperty().set(".wav");
        captureButton = new Button("Capture");
        openButton = new Button("Open File");
        
        Label label1 = new Label("Save File:");

        
        captureField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable,
					String oldValue, String newValue) {
				if(newValue.length() < 5 || !newValue.endsWith(".wav")) {
					captureButton.setDisable(true);
				} else {
					captureButton.setDisable(false);
				}	
			}
         });
        
        captureButton.setDisable(true);
        captureButton.setPrefSize(100, 20);
        captureButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(captureButton.getText() == "Capture") {
					captureField.setDisable(true);
					//captureButton.setDisable(false);
					captureButton.setText("Stop");
					Thread myThread = new Thread() {
						@Override
						public void run() {
							//readData(false, new File(captureField.getText()));
							startCapture();
						}
					};
					myThread.start();
				} else {
					stopCapture(new File(captureField.getText()));
					captureButton.setText("Capture");
					captureField.setDisable(false);
				}	
			}
		});

        Label label2 = new Label("Open File:");
        
        openField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable,
					String oldValue, String newValue) {
				if(newValue.length() < 5 || !newValue.endsWith(".wav") || 
						!(new File(newValue).isFile())) {
					openButton.setDisable(true);
				} else {
					openButton.setDisable(false);
				}	
			}
         });
        
        openButton = new Button("Open File");
        openButton.setDisable(true);
        openButton.setPrefSize(100, 20);
        openButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
//				readData(true, new File(openField.getText()));
				openButton.setDisable(true);
				openField.setDisable(true);
				readFromFile(new File(openField.getText()));
				openButton.setDisable(false);
				openField.setDisable(false);
			}
		});
        
        Button frequencyButton = new Button("Show Frequencies");
        frequencyButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				displayFrequencies();
			}
		});
        
        TextField nthField = new TextField();
        nthField.setPrefSize(50, 20);
        nthField.promptTextProperty().set("Nth Graph");

/*        Button nthAutoCorrelation = new Button("Display AC[N]");
        nthAutoCorrelation.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				displayAC(Integer.parseInt(nthField.getText()));
			}
		});*/

        Button nthFFT = new Button("Display FFT[N]");
        nthFFT.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				displayFFt(Integer.parseInt(nthField.getText()), audioData.getFftLowPassAbsolute());
			}
		});
        
        TextField freqIndex = new TextField();
        freqIndex.promptTextProperty().set("Note index");
        
        TextField noteAndFreqField = new TextField("Freq and Note");
        noteAndFreqField.setEditable(false);
        
        Button noteAndFreqButton = new Button("Show Note");
        noteAndFreqButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				String text = getFreqAndNote(Integer.parseInt(freqIndex.getText()));
				noteAndFreqField.setText(text);
			}
		});
        
        hbox.getChildren().addAll(label1, captureField, captureButton, 
        		label2, openField, openButton, frequencyButton, nthField, 
        		/*nthAutoCorrelation,*/ nthFFT, freqIndex, noteAndFreqButton,
        		noteAndFreqField);
        
        return hbox;
    }
    
    private void displayFrequencies() {
    	//TODO get its own graph
		updateGraph(fftGraph, audioData.getFrequencies());
    }
    
    private void displayFFt(int n, final Double[] absoluteData) {
		updateGraph(fftGraph, Arrays.asList(absoluteData), n);
		//Number[] range = Arrays.copyOfRange(audioData.getFftAbsolute(), start, end);
		calculateBaseFrequencyIndex(absoluteData, n, 0);
    }
    
    private int calculateBaseFrequencyIndex(Double[] fftData, int nth, int startOffset) {
    	int start = nth * audioData.getFftLength();
    	int end = start + audioData.getFftLength();
    	int maxI = findMax(fftData, start, end, 0);
    	
    	int subMaxI = maxI - start;
    	
    	List<Double> data = Arrays.asList(fftData).subList(start, end);
    	List<Integer> peaks = FundamentalFrequency.findPeaks(data, subMaxI, 6, .05);
    	    	
		List<Integer> peaks2 = findPeaks(fftData, 25,/*(int) audioData.getFormat().getSampleRate() / audioData.getFftLength(),*/ // A good guess for window size
				start, end, maxI,0.000013); // 1 std deviation. Really no backing to this
		
		List<Integer> peakDiff = new LinkedList<Integer>();
		for(int i = 0; i < peaks.size() - 1; i++) {
			peakDiff.add(peaks.get(i+1) - peaks.get(i));
		}
		//This all will not work if the peaks are not exactly correct.
		List<Integer> modes = Util.mode(peakDiff);
		if(modes.size() > 1) {
			System.err.println("You have multiple modes at FFT number: " + nth);
			//System.exit(1);
		}
		return (modes.size() >= 1) ? Util.round(Util.average(modes))/*modes.get(0)*/ : -1; //probably replace with average of modes.
    }
    
    private <T> void displayCenterGraph(List<T> data) {
    	updateGraph(centerGraph, data);
    }
    
    private <T> void displayCenterGraph(int n, List<T> data) {
		updateGraph(centerGraph, data, n);
    }
    
/*    private void displayAC(int n) {
    	Number[] data = audioData.getAutoCorrelationAbsolute();
    	Number[] logData = new Number[data.length];
    	for(int i = 0; i < data.length; i++) {
    		logData[i] = Math.log10(data[i].doubleValue());
    	}
		updateGraph(acGraph, logData, n);
    }*/
    
	private void writeToFile(byte[] signalBites, File file, AudioFormat format) {
    	WriteAudioFile toFile = new WriteAudioFile(signalBites, format, file);
    	toFile.start();
	}
    
    public void startCapture() {
    	if(audio == null || audio.isStopped()) {
	    	try {
				audio = new CaptureAudio(
						new LineListener() {
							@Override
							public void update(LineEvent event) {
								Type type = event.getType();
								if (type == Type.CLOSE || type == Type.STOP) {
									if(!audio.isStopped()) {
										audio.stopCapture();
										System.err.println("Audio capture was stopped unexpectedly.");
									}
								}
							}
						});
				System.out.println("STARTED");
				audio.start();
				captureButton.setText("Stop");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
    	} else {
    		System.err.println("Capture is already in process");
    	}
    }
    
    public void stopCapture(File file) {
    	if(audio != null && !audio.isStopped()) {
    		audio.stopCapture();
    		System.out.println("STOPPED");
    		
    		
	    	//TODO study about Thread.getStackTrace()
	  //  	currentSignal = audio.getStream().toByteArray();
    	//	int[] signal = toIntArray(currentSignal);
	//		updateGraph(signal);

    		
    		AudioData audioData = new AudioData(
    				audio.getStream().toByteArray(),
    				audio.getFormat());
    		processSignal(audioData, file);
    		
    		
    		
    		

			
/*			captureButton.setDisable(false);
			captureButton.setText("Capture");*/
    	}
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}