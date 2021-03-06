package tesis.test.sos1;

import org.doubango.ngn.NgnApplication;
import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View;

public class MainActivity extends Activity {

	/*private final NgnEngine mEngine;
	private final INgnConfigurationService mConfigurationService;
	private final INgnSipService mSipService;
	
	public MainActivity(){
		mEngine = NgnEngine.getInstance();
		mConfigurationService = mEngine.getConfigurationService();
		mSipService = mEngine.getSipService();
	}*/
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	public void onLogInButtonClick(View view){
		EditText userNameEditText = (EditText) findViewById(R.id.userNameEditText);
		String userNameString = userNameEditText.getText().toString();
		EditText passwordEditText = (EditText) findViewById(R.id.passwordEditText);
		String passwordString = passwordEditText.getText().toString();
		Toast.makeText(this, "text: " + NgnApplication.getContext().toString(), Toast.LENGTH_SHORT).show();
	    
		Intent intent = new Intent(this, RegistrationActivity.class);
	    startActivity(intent);
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
