package tesis.test.sos1;

import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.doubango.imsdroid.Screens.ScreenAV;
import org.doubango.imsdroid.Services.IScreenService;
import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.sip.NgnSipStack;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.ngn.utils.NgnStringUtils;
import org.doubango.ngn.utils.NgnUriUtils;

public class RegistrationActivity extends Activity {
	private static String TAG = RegistrationActivity.class.getCanonicalName();
	
	private TextView registrationStatusText;
	private Button signInOutButton;
	private Button callButton;

	private BroadcastReceiver mSipBroadCastRecv;
	private final NgnEngine mEngine;
	private final INgnConfigurationService mConfigurationService;
	private final INgnSipService mSipService;
	
	public RegistrationActivity(){
		mEngine = NgnEngine.getInstance();
		mConfigurationService = mEngine.getConfigurationService();
		mSipService = mEngine.getSipService();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_registration);
		
		registrationStatusText = (TextView)findViewById(R.id.textViewInfo);
		signInOutButton = (Button)findViewById(R.id.signInOutButton);
		callButton = (Button)findViewById(R.id.CallButton);
		
		// Subscribe for registration state changes
        mSipBroadCastRecv = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final String action = intent.getAction();
				
				//registration event
				if(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT.equals(action)){
					NgnRegistrationEventArgs args = intent.getParcelableExtra(NgnEventArgs.EXTRA_EMBEDDED);
					if(args == null){
						Log.e(TAG, "Invalid event args");
						return;
					}
					switch(args.getEventType()){
						case REGISTRATION_NOK:
							registrationStatusText.setText("Failed to register");
							break;
						case UNREGISTRATION_OK:
							registrationStatusText.setText("You are now unregistered");
							break;
						case REGISTRATION_OK:
							registrationStatusText.setText("You are now registered");
							break;
						case REGISTRATION_INPROGRESS:
							registrationStatusText.setText("Trying to register...");
							break;
						case UNREGISTRATION_INPROGRESS:
							registrationStatusText.setText("Trying to unregister...");
							break;
						case UNREGISTRATION_NOK:
							registrationStatusText.setText("Failed to unregister");
							break;
					}
					signInOutButton.setText(mSipService.isRegistered() ? "Sign Out" : "Sign In");
				}
			}
        };
        final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT);
	    registerReceiver(mSipBroadCastRecv, intentFilter);
	    
	    signInOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mEngine.isStarted()){
					if(!mSipService.isRegistered()){
						// Set credentials (get them from SOS BD or sip server data) 
						mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI, 
								"6002");
						mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU, 
								"sip:6002@192.168.2.17");
						mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD,
								"bob123");
						mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST,
								"192.168.2.17");
						mConfigurationService.putInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT,
								5060);
						mConfigurationService.putString(NgnConfigurationEntry.NETWORK_REALM,
								"192.168.2.17");
						// VERY IMPORTANT: Commit changes
						mConfigurationService.commit();
						// register (log in)
						mSipService.register(RegistrationActivity.this);
					}
					else{
						// unregister (log out)
						mSipService.unRegister();
					}
				}
				else{
					registrationStatusText.setText("Engine not started yet");
				}
				
			}
		});
	}
	public boolean makeCall(String remoteUri, NgnMediaType mediaType){
		/*needs:
		 * ngnEngine
		 * sipService -> sipstack
		 * configuration service
		 * screen service (not sure)
		 * string valid uri
		 * 
		 *  create session (NgnAVSession) -> Use NgnAVSession to make the call
		 *  	Set session type (audio/video)
		 *  	call make call
		 *  
		 *  check ScreenAV from IMSDroid
		*/
		boolean result = false;
		String validUri = NgnUriUtils.makeValidSipUri(remoteUri);
		final INgnConfigurationService configurationService = mEngine.getConfigurationService();
		

		if(validUri == null){
			Log.e(TAG, "failed to normalize sip uri '" + remoteUri + "'");
			return result;
		}
		else
		{
			remoteUri = validUri;
			if(remoteUri.startsWith("tel:")){
				final NgnSipStack sipStack = mEngine.getSipService().getSipStack();
				if(sipStack != null){
					String phoneNumber = NgnUriUtils.getValidPhoneNumber(remoteUri);
					if(phoneNumber != null){
						String enumDomain = configurationService.getString(
								NgnConfigurationEntry.GENERAL_ENUM_DOMAIN, NgnConfigurationEntry.DEFAULT_GENERAL_ENUM_DOMAIN);
						String sipUri = sipStack.dnsENUM("E2U+SIP", phoneNumber, enumDomain);
						if(sipUri != null){
							remoteUri = sipUri;
						}
					}
				}
			}
		}
		final NgnAVSession avSession = NgnAVSession.createOutgoingSession(sipService.getSipStack(), mediaType);
		avSession.setRemotePartyUri(remoteUri); // HACK
		screenService.show(ScreenAV.class, Long.toString(avSession.getId()));	
		
		// Hold the active call
		final NgnAVSession activeCall = NgnAVSession.getFirstActiveCallAndNot(avSession.getId());
		if(activeCall != null){
			activeCall.holdCall();
		}
		
		return avSession.makeCall(remoteUri);
		return result;
	}
	public void onCallButtonClick(View view){
		int tag = 1; //audio=0 video=1
		final String number = "6001"; //call number
		
		/*if(tag == DialerUtils.TAG_CHAT){
			if(mSipService.isRegistered() && !NgnStringUtils.isNullOrEmpty(number)){
				// ScreenChat.startChat(number);
				mEtNumber.setText(NgnStringUtils.emptyValue());
			}
		}
		else*/ /*if(tag == DialerUtils.TAG_DELETE){
			final int selStart = mEtNumber.getSelectionStart();
			if(selStart >0){
				final StringBuffer sb = new StringBuffer(number);
				sb.delete(selStart-1, selStart);
				//mEtNumber.setText(sb.toString());
				//mEtNumber.setSelection(selStart-1);
			}
		}
		else*/ if(tag == 0){
			if(mSipService.isRegistered() && !NgnStringUtils.isNullOrEmpty(number)){
				makeCall(number, NgnMediaType.Audio);
				//mEtNumber.setText(NgnStringUtils.emptyValue());
			}
		}
		else if(tag == 1){
			if(mSipService.isRegistered() && !NgnStringUtils.isNullOrEmpty(number)){
				makeCall(number, NgnMediaType.AudioVideo);
				//mEtNumber.setText(NgnStringUtils.emptyValue());
			}
		}
		/*else{
			final String textToAppend = tag == DialerUtils.TAG_STAR ? "*" : (tag == DialerUtils.TAG_SHARP ? "#" : Integer.toString(tag));
			appendText(textToAppend);
		}*/
	}
	
	@Override
	protected void onDestroy() {
		// Stops the engine
		if(mEngine.isStarted()){
			mEngine.stop();
		}
		// release the listener
		if (mSipBroadCastRecv != null) {
			unregisterReceiver(mSipBroadCastRecv);
			mSipBroadCastRecv = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Starts the engine
		if(!mEngine.isStarted()){
			if(mEngine.start()){
				registrationStatusText.setText("Engine started :)");
			}
			else{
				registrationStatusText.setText("Failed to start the engine :(");
			}
		}
	} 
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.registration, menu);
		return true;
	}

}
