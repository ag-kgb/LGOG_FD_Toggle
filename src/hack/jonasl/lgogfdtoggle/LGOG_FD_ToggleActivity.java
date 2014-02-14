package hack.jonasl.lgogfdtoggle;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class LGOG_FD_ToggleActivity extends Activity {

	private static String TAG = "LGOGFDToggle";
	private static String DEFAULT = "1,0,0,1000,5000,60000,3000,5000,1,8";
	private static Uri URI = Uri.parse("content://telephony/dcm_settings");
	private static String SELECTION = "numeric = \'22003\'";

	private TextView mStatus;
	private TextView mLast;
	private Button mEnable;
	private Button mDisable;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.setTitle(R.string.app_name);
        mStatus = (TextView)findViewById(R.id.status);
        mLast = (TextView)findViewById(R.id.last);
        mEnable = (Button)findViewById(R.id.enable);
        mDisable = (Button)findViewById(R.id.disable);

        mEnable.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setStatus(true);
			}
		});
        mDisable.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setStatus(false);
			}
		});
        
        // Doing DB I/O in main thread is never a good thing
        // but this is just a quick and dirty hack.
        updateStatus();
    }

	private void updateStatus() {
		mStatus.setText(String.format("%s %s", getString(R.string.status), getString(R.string.unknown)));
		mEnable.setEnabled(true);
		mDisable.setEnabled(true);
		try {
			String[] projection = {"_id", "fastdormancy"};
			Cursor c = getContentResolver().query(URI, projection, SELECTION, null, null);
			// There should be one entry, don't bother checking count (lazy)
			c.moveToFirst();
			String fd = c.getString(1).trim();
			Log.d(TAG, "FD value: \"" + fd + "\"");
			if ("0".equals(fd)) {
				mStatus.setText(String.format("%s %s", getString(R.string.status), getString(R.string.disabled)));
				mDisable.setEnabled(false);
			} else if (DEFAULT.equals(fd)){
				mStatus.setText(String.format("%s %s", getString(R.string.status), getString(R.string.enabled)));
				mEnable.setEnabled(false);
			} else {
				// Bleh, some weird value we don't handle
				throw new Exception("Unknown value: \"" + fd + "\"");
			}
			mLast.setText(String.format("%s %s", getString(R.string.last), getString(R.string.read_ok)));
		}
		catch (Exception e) {
			mLast.setText(String.format("%s %s", getString(R.string.last),e.getMessage()));
			Log.e(TAG, e.toString());
		}
	}

	private void setStatus(boolean enabled) {
		boolean success = false;
		try {
			ContentValues vals = new ContentValues();
			vals.put("fastdormancy", enabled ? DEFAULT : "0");
			int rows = getContentResolver().update(URI, vals, SELECTION, null);
			Log.d(TAG, rows + " row(s) updated");
			if (rows != 1) {
				// Bleh, expected one update
				throw new Exception(rows + " row(s) updated, expected 1");
			}
			success = true;
		}
		catch (Exception e) {
			Log.e(TAG, e.toString());
		} finally {
			updateStatus();
		}
		mLast.setText(String.format("%s %s", getString(R.string.last),
				success ? getString(R.string.success): getString(R.string.error)));		
	}
}
