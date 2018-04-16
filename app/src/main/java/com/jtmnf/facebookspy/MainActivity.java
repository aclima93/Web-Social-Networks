package com.jtmnf.facebookspy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Facebook SDK Manager
    private CallbackManager callbackManager;

    private Bitmap image;
    private TextView text;
    private Uri imageUri;

    // ID for the camera activity
    static final int REQUEST_IMAGE_CAPTURE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Standard Android Stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the call manager
        callbackManager = CallbackManager.Factory.create();

        // Get ID for the buttons
        final LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        final Button otherButton = (Button) findViewById(R.id.other);

        text = (TextView) findViewById(R.id.hello);

        // Set permissions for this app (when pressing the button)
        loginButton.setReadPermissions(Arrays.asList("email"));
        loginButton.setReadPermissions(Arrays.asList("public_profile"));

        // Register button press and do stuff
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                checkUsername(false);
            }

            @Override
            public void onCancel() {
                Toast.makeText(MainActivity.this, "CANCEL", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException exception) {
                Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_SHORT).show();
            }
        });

        // Take other picture if there is a token available
        otherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AccessToken.getCurrentAccessToken() != null) {
                    checkUsername(true);
                } else {
                    Toast.makeText(MainActivity.this, "Missing the login", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Check if Token exists
        if (AccessToken.getCurrentAccessToken() != null) {
            checkUsername(false);
        }
    }

    private void checkUsername(final boolean takePicture) {
        // Start a request for user's information
        // https://developers.facebook.com/docs/graph-api/reference/user
        GraphRequest request = GraphRequest.newGraphPathRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + AccessToken.getCurrentAccessToken().getUserId(),
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        try {
                            // Retrieve Facebook response
                            JSONObject object = response.getJSONObject();

                            if (takePicture) {
                                // Initiate Camera
                                dispatchTakePictureIntent();
                            }

                            text.setText("Welcome " + response.getJSONObject().get("name").toString());
                        } catch (JSONException e) {
                        }
                    }
                });

        // Pass fields to request
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,link,address,birthday,gender,timezone,work,website," +
                "email,location,about,age_range,cover,devices,education,favorite_teams," +
                "first_name,hometown,languages,locale," +
                "meeting_for,relationship_status,religion,significant_other,sports,updated_time");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                // Store image (temporarily) and share it
                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                sharePhoto();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void sharePhoto() {
        // Start to sharing the photo
        SharePhoto photo = new SharePhoto.Builder().setBitmap(image).build();
        SharePhotoContent content = new SharePhotoContent.Builder().addPhoto(photo).build();

        // Initialize dialog to publish photo
        ShareDialog shareDialog = new ShareDialog(this);
        shareDialog.show(content, ShareDialog.Mode.AUTOMATIC);
    }
    
    // Code from: https://developer.android.com/training/camera/photobasics.html
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,".jpg", storageDir);
        return image;
    }
}
