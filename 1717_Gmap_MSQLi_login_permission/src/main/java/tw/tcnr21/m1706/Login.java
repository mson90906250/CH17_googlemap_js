package tw.tcnr21.m1706;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class Login extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "tcnr01=>";
    private TextView mStatusTextView;
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private CircleImageView img;
    private Uri User_IMAGE;
    private Button bmap_button;
    private Intent intent01 = new Intent();
    private String g_DisplayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        setupViewComponent();
    }

    private void setupViewComponent() {
        mStatusTextView = findViewById(R.id.status);
        bmap_button =(Button)findViewById(R.id.map_button);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);
        findViewById(R.id.map_button).setOnClickListener(this);

        // START configure_signin
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .requestEmail()
                .build();
        //END configure_signin

        //START build_client
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        //END build_client

        // START customize_button
        // Set the dimensions of the sign-in button.
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setColorScheme(SignInButton.COLOR_LIGHT);
        // END customize_button
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;
            case R.id.map_button:
                intent01.putExtra("googlename", g_DisplayName);
                intent01.setClass(Login.this, M1706.class);
                startActivity(intent01);
                break;

        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }
    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // START_EXCLUDE
                        updateUI(null);
                        // END_EXCLUDE
                    }
                });
    }
    private void revokeAccess() {
        mGoogleSignInClient.revokeAccess()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // START_EXCLUDE
                        updateUI(null);
                        // END_EXCLUDE
                    }
                });
    }
    @Override
    public void onStart() {
        super.onStart();

        //檢查權限
        // Check if the user is already signed in and all required scopes are granted
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && GoogleSignIn.hasPermissions(account, new Scope(Scopes.DRIVE_APPFOLDER))) {
            updateUI(account);
        } else {
            updateUI(null);
        }

    }

    // START onActivityResult
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    // END onActivityResult

    // START handleSignInResult
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.d(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }
    // END handleSignInResult

    private void updateUI(GoogleSignInAccount account) {
        if (account != null) {
            g_DisplayName=account.getDisplayName(); //暱稱
           /* String g_Email=account.getEmail();  //信箱
            String g_GivenName=account.getGivenName(); //Firstname
            String g_FamilyName=account.getFamilyName(); //Last name
            String g_id=account.getId();*/
            //mStatusTextView.setText(getString(R.string.signed_in_fmt, account.getDisplayName())+getString(R.string.ID, account.getId()));
            mStatusTextView.setText(getString(R.string.signed_in_fmt, account.getDisplayName())+"\n Email:"+
                            account.getEmail()+"\n Firstname:"+
                            account.getGivenName()+"\n Last name:"+
                            account.getFamilyName()
            );

//-------改變圖像--------------
            User_IMAGE = account.getPhotoUrl();
            if(User_IMAGE==null){
                return;
            }
            img = (CircleImageView) findViewById(R.id.google_icon);
            new AsyncTask<String,Void,Bitmap>() {
                @Override
                protected Bitmap doInBackground(String... params) {
                    String url = params[0];
                    return getBitmapFromURL(url);
                }

                @Override
                protected void onPostExecute(Bitmap result) {
                    img.setImageBitmap(result);
                    super.onPostExecute(result);
                }
            }.execute(User_IMAGE.toString().trim());

            //-------------------------
            Log.d(TAG,"123456");
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }
    //--------------------------------------------
    public static Bitmap getBitmapFromURL(String imageUrl) {
        try{
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            return bitmap;
        }  catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
