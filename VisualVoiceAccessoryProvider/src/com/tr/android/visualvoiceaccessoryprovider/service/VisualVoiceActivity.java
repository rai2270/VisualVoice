package com.tr.android.visualvoiceaccessoryprovider.service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VisualVoiceActivity extends Activity implements RecognitionListener {

	public static boolean isUp = false;
	
	private static boolean isDebug = false;
	
	//public static TextView methodText;
	//public static TextView resultsText;

	private VisualVoiceAccessoryProviderService mSAService;
	
	public static BlockingQueue<String> mReceiveVoiceQ = null;

	// ----- TYPES ----- //
	// Timer task used to reproduce the timeout input error that seems not be called on android 4.1.2
	/*public class SilenceTimer extends TimerTask {
		@Override
		public void run() {
			onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
		}
	}*/

	// ---- MEMBERS ---- //
	// Callback activity called following dictation process
	// Logger tag
	private static final String TAG = "" + VisualVoiceActivity.class;
	// Speech recognizer instance
	private static SpeechRecognizer speech = null;
	// Timer used as timeout for the speech recognition
	//private Timer speechTimeout = null;
	
	protected static AudioManager mAudioManager; 

	// Lazy instantiation method for getting the speech recognizer
	private SpeechRecognizer getSpeechRevognizer(){
		if (speech == null) {
			speech = SpeechRecognizer.createSpeechRecognizer(this);
			speech.setRecognitionListener(this);
		}

		return speech;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE); 
		
		isUp = true;
		
		if (mReceiveVoiceQ != null)
		{
			mReceiveVoiceQ.clear();
			mReceiveVoiceQ = null;
		}
		mReceiveVoiceQ = new LinkedBlockingQueue<String>();

		/*LinearLayout layout = (LinearLayout) findViewById(R.id.container);
		Button closeButton = new Button(this);
		closeButton.setText("Close");
		closeButton.setLayoutParams(new LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		layout.addView(closeButton);
		closeButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				finish();
			}
		});*/

		//methodText = (TextView) findViewById(R.id.textView1);
		//resultsText = (TextView) findViewById(R.id.textView2);

		getApplicationContext().bindService(new Intent(getApplicationContext(), VisualVoiceAccessoryProviderService.class), 
				this.mSAConnection, Context.BIND_AUTO_CREATE);

		//this.runOnUiThread(new Runnable() {
		//	public void run() {
		//		startVoiceRecognitionCycle();
		//	}
		//});
	}
	
	@Override
    protected void onStart() {
        super.onStart();
        isUp = true;
    }

	@Override
	protected void onResume()
	{
		super.onResume(); 
		isUp = true;
		
		this.runOnUiThread(new Runnable() {
			public void run() {
				startVoiceRecognitionCycle();
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();  
		isUp = false;
		
		stopVoiceRecognition();
	}
	
	@Override
    public void onBackPressed() {
        super.onBackPressed();
        isUp = false;
    }
	
	@Override
    protected void onStop() {
        super.onStop();
        isUp = false;
    }

	protected void onDestroy() {
		super.onDestroy();
		isUp = false;
		stopVoiceRecognition();
	}

	/**
	 * Fire an intent to start the voice recognition process.
	 */
	public void startVoiceRecognitionCycle()
	{
		if(isDebug)
			Log.d(TAG,"startVoiceRecognitionCycle:Start");
		
		try {
			stopVoiceRecognition();
			//getSpeechRevognizer().cancel();
			//getSpeechRevognizer().destroy();
			//speech = null;
		} catch (Exception e) {
			if(isDebug)
				Log.d(TAG,"startVoiceRecognitionCycle:getSpeechRevognizer().cancel()  Error: " + e.toString());
		}
		
		try {
			
			// turn off beep sound  
			mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
			
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			//Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		    //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
		    //intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
		    //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
		    //intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
		    
			getSpeechRevognizer().startListening(intent);
		} catch (Exception e) {
			if(isDebug)
				Log.d(TAG,"startVoiceRecognitionCycle:getSpeechRevognizer().startListening(intent)  Error: " + e.toString());
		}
		
		if(isDebug)
			Log.d(TAG,"startVoiceRecognitionCycle:End");
	}

	/**
	 * Stop the voice recognition process and destroy the recognizer.
	 */
	public void stopVoiceRecognition()
	{
		
		
		/*if(speechTimeout!=null)
		{
			speechTimeout.cancel();
			speechTimeout = null;
		}*/
		
		try {
			mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
			
			if(isDebug)
				Log.d(TAG,"stopVoiceRecognition");
			
			if (speech != null) {
				speech.cancel();
				speech.destroy();
				speech = null;
			}
		} catch (Exception e) {
			
		}
	}

	/* RecognitionListener interface implementation */

	@Override
	public void onReadyForSpeech(Bundle params) {
		if(isDebug)
			Log.d(TAG,"onReadyForSpeech");
		// create and schedule the input speech timeout
		//speechTimeout = new Timer();
		//speechTimeout.schedule(new SilenceTimer(), 5000);
	}

	@Override
	public void onBeginningOfSpeech() {
		if(isDebug)
			Log.d(TAG,"onBeginningOfSpeech");
		// Cancel the timeout because voice is arriving
		//speechTimeout.cancel();

		// Notify the container activity that dictation is started
		//mCallback.onDictationStart();
	}

	@Override
	public void onBufferReceived(byte[] buffer) {
		if(isDebug)
			Log.d(TAG,"onBufferReceived");
	}

	@Override
	public void onEndOfSpeech() {
		if(isDebug)
			Log.d(TAG,"onEndOfSpeech");

		// Notify the container activity that dictation is finished
		//mCallback.onDictationFinish();
	}

	@Override
	public void onError(int error) {
		if(isDebug)
			Log.d(TAG,"onError");
		
		String message;
		boolean restart = true;
		switch (error)
		{
			case SpeechRecognizer.ERROR_AUDIO:
				message = "Audio recording error";
				break;
			case SpeechRecognizer.ERROR_CLIENT:
				message = "Client side error";
				restart = false;
				break;
			case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
				message = "Insufficient permissions";
				restart = false;
				break;
			case SpeechRecognizer.ERROR_NETWORK:
				message = "Network error";
				break;
			case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
				message = "Network timeout";
				break;
			case SpeechRecognizer.ERROR_NO_MATCH:
				message = "No match";
				break;
			case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
				message = "RecognitionService busy";
				restart = false;
				break;
			case SpeechRecognizer.ERROR_SERVER:
				message = "error from server";
				break;
			case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
				message = "No speech input";
				break;
			default:
				message = "Not recognised";
				break;
		}
		//if(isDebug)
		//	Log.d(TAG,"onError code:" + error + " message: " + message);
		
		if ((error == SpeechRecognizer.ERROR_NO_MATCH)
	            || (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
	    {
			if(isDebug)
				Log.d(TAG, "onError: Code: " + error + ". Didn't recognize anything");
	        // keep going
	        this.runOnUiThread(new Runnable() {
				public void run() {
					startVoiceRecognitionCycle();
				}
			});
	    }
	    else
	    {
	    	if(isDebug)
	    		Log.d(TAG,"onError: Code: " + error);
	    }

		//if (restart) {
			/*this.runOnUiThread(new Runnable() {
				public void run() {
					startVoiceRecognitionCycle();
				}
			});*/
		//}
	}

	@Override
	public void onEvent(int eventType, Bundle params) {
		if(isDebug)
			Log.d(TAG,"onEvent");
	}

	@Override
	public void onPartialResults(Bundle partialResults) {
		if(isDebug)
			Log.d(TAG,"onPartialResults");
		
		
	}

	@Override
	public void onResults(Bundle results) {
		if(isDebug)
			Log.d(TAG,"onResults");
		
		
		
		try {
			StringBuilder scores = new StringBuilder();
			for (int i = 0; i < results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES).length; i++) {
				scores.append(results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)[i] + " ");
			}
			if(isDebug)
				Log.d(TAG,"onResults: " + results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) + " scores: " + scores.toString());
			// Return to the container activity dictation results 
			if (results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) != null) {
				String resultStr = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0).toString();
				mReceiveVoiceQ.add(resultStr);
				//Log.d(TAG,"added: " + resultStr);
				//resultsText.setText(resultStr);
			}
		} catch (Exception e) {
			
		}
		
		// Restart new dictation cycle
		this.runOnUiThread(new Runnable() {
			public void run() {
				startVoiceRecognitionCycle();
			}
		});
	}

	@Override
	public void onRmsChanged(float rmsdB) {
		//if(isDebug)
		//	Log.d(TAG,"onRmsChanged");
	}



	// Connection for VisualVoiceAccessoryProviderService
	private ServiceConnection mSAConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			if(isDebug)
				Log.d(TAG, "SA service connection lost");
			mSAService = null;
		}

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			if(isDebug)
				Log.d(TAG, "SA service connected");
			//mSAService = ((LocalBinder) service).getService();
			//mSAService.registerFileAction(getFileAction());
		}
	};



}