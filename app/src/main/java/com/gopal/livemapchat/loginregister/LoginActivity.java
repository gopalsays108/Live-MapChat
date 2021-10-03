package com.gopal.livemapchat.loginregister;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.auth.User;
import com.gopal.livemapchat.MainActivity;
import com.gopal.livemapchat.R;
import com.gopal.livemapchat.RegisterActivity;
import com.gopal.livemapchat.UserClient;
import com.gopal.livemapchat.models.Users;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEt;
    private EditText passwordEt;
    private Button loginBtn;
    private TextView registerNewTv;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private static final String TAG = LoginActivity.class.getSimpleName();
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_login );

        emailEt = findViewById( R.id.email_ET );
        passwordEt = findViewById( R.id.password );
        loginBtn = findViewById( R.id.login_btn );
        registerNewTv = findViewById( R.id.register_new_tv );
        firebaseAuth = FirebaseAuth.getInstance();
        progressBar = findViewById( R.id.progress );
        settUpFirebaseAuth();

        loginBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLogin();
            }
        } );

        registerNewTv.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent( LoginActivity.this, RegisterActivity.class );
                startActivity( intent );
            }
        } );
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener( authStateListener );
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener( authStateListener );
        }
    }

    private void settUpFirebaseAuth() {
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Toast.makeText( getApplicationContext(), "Siged in as " + user.getEmail(), Toast.LENGTH_SHORT ).show();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                            .build();
                    db.setFirestoreSettings( settings );
                    DocumentReference documentReference = db.collection( getString( R.string.collection_users ) )
                            .document( user.getUid() );
                    documentReference.get().addOnCompleteListener( new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                Users users = task.getResult().toObject( Users.class );


                                //This is user singleton class, now we can call user obejct from any activity anytime
                                ((UserClient) (getApplicationContext())).setUsers( users );
                            }
                        }
                    } );

                    Intent intent = new Intent( getApplicationContext(), MainActivity.class );
                    intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK );
                    startActivity( intent );
                    finish();
                }
            }
        };
    }

    private void startLogin() {
        String email = emailEt.getText().toString();
        String pass = passwordEt.getText().toString();

        if (!email.isEmpty() && !pass.isEmpty()) {
            loginToFirebaseUsingEmailAndPass( email, pass );
        } else {
            Toast.makeText( getApplicationContext(), "Enter above details", Toast.LENGTH_SHORT ).show();
        }
    }

    private void loginToFirebaseUsingEmailAndPass(String email, String pass) {
        loginBtn.setVisibility( View.GONE );
        progressBar.setVisibility( View.VISIBLE );

        firebaseAuth.signInWithEmailAndPassword( email, pass ).addOnCompleteListener( new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                loginBtn.setVisibility( View.VISIBLE );
                progressBar.setVisibility( View.GONE );

                if (task.isSuccessful()) {

                } else {
                    Toast.makeText( getApplicationContext(), "Failed to signin", Toast.LENGTH_SHORT ).show();
                }
            }
        } ).addOnFailureListener( new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                loginBtn.setVisibility( View.VISIBLE );
                progressBar.setVisibility( View.GONE );
                Log.i( TAG, "onFailure: Exception failure: " + e.getLocalizedMessage() );
            }
        } );
    }
}