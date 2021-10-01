package com.gopal.livemapchat;

import static android.text.TextUtils.isEmpty;

import static com.gopal.livemapchat.utils.Checks.doStringsMatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.auth.User;
import com.gopal.livemapchat.loginregister.LoginActivity;
import com.gopal.livemapchat.models.Users;

public class RegisterActivity extends AppCompatActivity {


    private EditText emailEt;
    private EditText passwordEt;
    private EditText passwordConEt;
    private Button register_btn;
    private FirebaseAuth firebaseAuth;
    private static final String TAG = LoginActivity.class.getSimpleName();
    private FirebaseFirestore firebaseFirestore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_register );

        emailEt = findViewById( R.id.email_ET );
        passwordEt = findViewById( R.id.password );
        register_btn = findViewById( R.id.regsiter_btn );
        passwordConEt = findViewById( R.id.password_con );

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        hideSoftKeyboard();

        register_btn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check for null valued EditText fields
                if (!isEmpty( emailEt.getText().toString() )
                        && !isEmpty( passwordEt.getText().toString() )
                        && !isEmpty( passwordConEt.getText().toString() )) {

                    //check if passwords match
                    if (doStringsMatch( passwordEt.getText().toString(), passwordConEt.getText().toString() )) {

                        //Initiate registration task
                        registerNewEmail( emailEt.getText().toString(), passwordConEt.getText().toString() );
                    } else {
                        Toast.makeText( RegisterActivity.this, "Passwords do not Match", Toast.LENGTH_SHORT ).show();
                    }

                } else {
                    Toast.makeText( RegisterActivity.this, "You must fill out all the fields", Toast.LENGTH_SHORT ).show();
                }
            }
        } );
    }

    private void registerNewEmail(String email, String pass) {
        firebaseAuth.createUserWithEmailAndPassword( email, pass ).addOnCompleteListener( new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful() && FirebaseAuth.getInstance().getUid() != null) {
                    Users user = new Users();
                    user.setEmail( email );
                    user.setUsername( email.substring( 0, email.indexOf( '@' ) ) );
                    user.setUser_id( FirebaseAuth.getInstance().getUid() );

                    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                            .build();

                    firebaseFirestore.setFirestoreSettings( settings );

                    DocumentReference newUserRef = firebaseFirestore.collection( getString( R.string.collection_users ) )
                            .document( FirebaseAuth.getInstance().getUid() );

                    newUserRef.set( user ).addOnCompleteListener( new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                redirectLoginScreen();
                            } else {
                                View view = findViewById( android.R.id.content );
                                Snackbar.make( view, "Something went wrong", Snackbar.LENGTH_SHORT ).show();
                            }
                        }
                    } );
                } else {
                    View parentLayout = findViewById( android.R.id.content );
                    Snackbar.make( parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT ).show();
                }
            }
        } ).addOnFailureListener( new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                View parentLayout = findViewById( android.R.id.content );
                Snackbar.make( parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT ).show();

            }
        } );
    }

    private void redirectLoginScreen() {
        Intent intent = new Intent( RegisterActivity.this, LoginActivity.class );
        startActivity( intent );
        finish();
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN );
    }
}