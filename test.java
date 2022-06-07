import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * You have been hired to support the Discord audio chat system.
 * </p>
 * <p>
 * The system relies on user's microphone data coming into a shared system for playback to all participants (and
 * possibly recording!).  But they need a CS expert to handle input from the microphones and ensure that all the audio
 * is correctly interleaved into understandable output.
 * </p>
 * <p>
 * Your job is to collect raw data streams (audio tones) from microphones and save them into an organized data stream in 
 * a virtualized audio concentrator. This task is not trivial - if data from one microphone is included in the feed
 * from another microphone, the audio recording will be unusable.
 * </p>
 * <p>
 * The Microphone and Player class have been provided for you by the microphone manufacturer. You will not need to
 * customize them.
 * </p>
 * <ul>
 * <li>The <strong>Player</strong> class is simply a player of audio data. If you are successful at keeping the audio
 * data in order, the data will play as tones.</li>
 * <li>The <strong>Microphone</strong> class takes the analog data coming from the microphone and sends it in digital
 * (16-bit wav) format to the audio concentrator.</li>
 * <li>You must complete the <strong>AudioConcentrator</strong> class. An audio concentrator is a device that accepts
 * multiple audio inputs. The audio chat system has a microphone for each of the six participants: partA, partB, partC, 
 * partD, partE, and partF. The Audio concentrator must accept an input from each of these participants' microphone. The
 * microphones have a limited amount of memory, so you must periodically copy each sampled audio to the AudioConcentrator 
 * data structure. The AudioConcentrator class maintains a Queue data structure to store all the data, accepting samples
 * from each microphone and adding them to the queue of data. Once a sampling sequence is complete, the Player will play 
 * the audio files and display the audiowave before saving the recording. If the data are in the correct sequence, tones
 * will be played of a length equal to the total audio time. For example, six microphones recording one second of audio 
 * data should play back six seconds of data. If the data for samples are not being recorded in order, you will hear
 * static. If the samples are not being saved to the shared data structure Queue, the recording will be too short. 
 * You should hear one tone for each sample taken by each microphone. e.g., 6 microphones and 2 samples = 12 tones. 
 * (no static)</li>
 * </ul>
 * <p>
 * The main method has been supplied for you.
 * </p>
 * <p>
 * 
 * @author Your friendly CS Professors 
 * @date 4/30/2021
 *         </p>
 */
public class AudioConcentrator {
    public boolean playing = false;
    private int numberOfSamplesToCollect;
    private int sampleSize;
    
    /**
     * Shared storage array
     */
    private byte[] audioDataStorage;
    
    /**
     * Queue implementation details
     */
    private int storageTail = 0;

    /**
     * Microphones of the 6 participants
     */
    private int activeMicrophoneCount;
    private Microphone partAMicrophone;
    private Microphone partBMicrophone;
    private Microphone partCMicrophone;
    private Microphone partDMicrophone;
    private Microphone partEMicrophone;
    private Microphone partFMicrophone;
    
    // lock instance fields
    private Lock go;
    private Lock end;
    private Lock store;
    private Lock get;
    private Lock xpand;
    
    // condition instance fields
    private Condition noMics;

    /**
     * Constructor
     * 
     * @param sampleSize Size of the audio samples to collect
     * @param numberOfSamplesToCollect Number of samples to collect
     */
    public AudioConcentrator(int sampleSize, int numberOfSamplesToCollect) {
        this.sampleSize = sampleSize;
        this.numberOfSamplesToCollect = numberOfSamplesToCollect;
        activeMicrophoneCount = 0;

        // all locks are initialized
        go = new ReentrantLock();
        end = new ReentrantLock();
        store = new ReentrantLock();
        get = new ReentrantLock();
        xpand = new ReentrantLock();
        
        // all conditions are initialized
        noMics = get.newCondition();
        
        // All notes are shifted 2 octaves.
        partAMicrophone = new Microphone(this, 523.25 / 4); // C Records 3 C
        partBMicrophone = new Microphone(this, 587.33 / 4); // D
        partCMicrophone = new Microphone(this, 662.25 / 4); // D#
        partDMicrophone = new Microphone(this, 659.26 / 4); // E
        partEMicrophone = new Microphone(this, 783.99 / 4); // G
        partFMicrophone = new Microphone(this, 880.00 / 4); // A

        audioDataStorage = new byte[sampleSize * 8];
    }

    /**
     * Launch each microphone in a separate thread.
     */
    public void startRecording() {
        Thread tm1 = new Thread(partDMicrophone);
        Thread tm2 = new Thread(partCMicrophone);
        Thread tm3 = new Thread(partAMicrophone);
        Thread tm4 = new Thread(partBMicrophone);
        Thread tm5 = new Thread(partEMicrophone);
        Thread tm6 = new Thread(partFMicrophone);
        tm1.start();
        tm2.start();
        tm3.start();
        tm4.start();
        tm5.start();
        tm6.start();
    }

    /**
     * Play the recording once all the microphones have finished recording.
     */
    public void playRecording() {
        Player p = new Player(getAudioDataStorage());
        Thread t1 = new Thread(p);
        t1.start();
        playing = true;

    }

    /**
     * Allows a microphone to tell the AudioConcentrator that it is "going hot" (that is, that there is an active
     * microphone).
     * 
     * TODO: Add appropriate locking
     */
    public void startMicrophone() {
    	xpand.lock();
    	try {
    		go.lock();
        	try {
        		activeMicrophoneCount++;
        	} finally {
        		go.unlock();
        	}
    	} finally {
    		xpand.unlock();
    	}
    	
        
    }

    /**
     * Allows a microphone to tell the audio concentrator it is finished collecting audio samples.
     * 
     * TODO: Add appropriate locking
     */
    public void endMicrophone() {
    	get.lock();
    	try {
    		xpand.lock();
        	try {
        		end.lock();
            	try {
            		activeMicrophoneCount--;
            		noMics.signalAll();
            	} finally {
            		end.unlock();
            	}
        	} finally {
        		xpand.unlock();
        	}
    	} finally {
    		get.unlock();
    	}
    	
    	
    }

    /**
     * Write data to the shared data array.
     * 
     * TODO: Add appropriate locking
     * 
     * @param data The data to write to the array
     */
    public void storeData(byte[] data) {
    	xpand.lock();
    	try {
    		store.lock();
        	try {
        		for (int i = 0; i < data.length; i++) {
        			if (storageTail == audioDataStorage.length) {
        				// we need to grow our array
        				growStorage();

        			}
        			audioDataStorage[storageTail] = data[i];
        			storageTail++;
        		}
        	} finally {
        		store.unlock();
        	}
    	} finally {
    		xpand.unlock();
    	}
    	
    }

    /**
     * Grows the data array if we run out of room.
     */
    public void growStorage() {
    	xpand.lock();
    	try {
    		byte[] newAudioDataStorage = new byte[audioDataStorage.length * 2];
            for (int i = 0; i < audioDataStorage.length; i++) {
                newAudioDataStorage[i] = audioDataStorage[i];
            }
            audioDataStorage = newAudioDataStorage;
    	} finally {
    		xpand.unlock();
    	}
       
    }

    /**
     * Returns the data storage array.
     * 
     * TODO: Add appropriate locking
     * 
     * @return The data storage array
     */
    public byte[] getAudioDataStorage() {
    	get.lock();
    	try {
    		while (activeMicrophoneCount != 0)
    			noMics.await();
  
    	} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
    		get.unlock();
    	}
        return this.audioDataStorage;
    }

    /**
     * Gets the current number of active microphones
     * 
     * @return the activeMicrophoneCount
     */
    public int getActiveMicrophoneCount() {
        return activeMicrophoneCount;
    }

    /**
     * Gets the number of samples to collect by a microphone.
     * 
     * @return The number of samples to collect
     */
    public int getNumberOfSamplesToCollect() {
        return numberOfSamplesToCollect;
    }

    /**
     * Gets the audio sample size to collect by a microphone.
     * 
     * @return the sampleSize
     */
    public int getSampleSize() {
        return sampleSize;
    }


    /**
     * Main Method
     * 
     * Creates an AudioConcentrator, starts the recording from the 6 microphones, then plays the recording to
     * audio (and displays the resulting audiowave).
     * 
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        AudioConcentrator discordSession = new AudioConcentrator(32000, 3);

        discordSession.startRecording();
        discordSession.playRecording();

    }

}
