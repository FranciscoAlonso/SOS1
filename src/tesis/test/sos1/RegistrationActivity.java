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

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.events.NgnEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.ngn.utils.NgnStringUtils;

public class RegistrationActivity extends Activity {
	private static String TAG = RegistrationActivity.class.getCanonicalName();
	
	private TextView registrationStatusText;
	private Button signInOutButton;

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
								"6001");
						mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU, 
								"sip:6001@192.168.1.20");
						mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD,
								"bob123");
						mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST,
								"192.168.1.20");
						mConfigurationService.putInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT,
								5060);
						mConfigurationService.putString(NgnConfigurationEntry.NETWORK_REALM,
								"192.168.1.20");
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
