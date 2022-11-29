package com.example.flutterinterview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class ContactProfile extends AppCompatActivity {

    //Database
    static final String TAG = "FI-ContactProfile";
    String reference = "Flutter-Users";
    String instance = "https://dashlabs-2910d-default-rtdb.asia-southeast1.firebasedatabase.app/";
    FirebaseStorage storage;
    StorageReference storageReference;
    private int userID, userEdit;
    private String userEmail, userAvatar, userFirstName, userLastName;
    private boolean userFavorite;
    private Uri userAvatarURI;

    //References
    TextView text_userName, text_email, tvTitle, text_edit;
    ImageView civ_profile, image_favorite, image_back, image_email_icon;
    EditText et_firstname, et_lastname, et_email;
    ConstraintLayout layout_email;
    ViewFlipper vf_profile;
    Button button_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_profile);

        //Action bar
        getSupportActionBar().setTitle("Profile");
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);
        tvTitle = getSupportActionBar().getCustomView().findViewById(R.id.tvTitle);
        image_back = getSupportActionBar().getCustomView().findViewById(R.id.image_back);
        image_back.setVisibility(View.VISIBLE);
        tvTitle.setText("Profile");
        image_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ContactProfile.this, MyContactsActivity.class);
                startActivity(intent);
            }
        });

        //References
        text_userName = findViewById(R.id.text_userName);
        text_email = findViewById(R.id.text_email);
        civ_profile = findViewById(R.id.civ_profile);
        image_favorite = findViewById(R.id.image_favorite);
        button_send = findViewById(R.id.button_send);
        image_favorite = findViewById(R.id.image_favorite);
        text_edit = findViewById(R.id.text_edit);
        vf_profile = findViewById(R.id.vf_profile);
        et_firstname = findViewById(R.id.et_firstname);
        et_lastname = findViewById(R.id.et_lastname);
        et_email = findViewById(R.id.et_email);

        //Disable the favorite star by default
        image_favorite.setVisibility(View.INVISIBLE);

        //Get intent from list
        userID = getIntent().getIntExtra("USER_ID", 0);
        userEdit = getIntent().getIntExtra("EDIT", 0);

        //Retrieve data from firebase
        //IF the ID is -1, the user has selected 'Add User' button from homepage, else show profile view of user
        if(userID != -1){

            FirebaseDatabase database = FirebaseDatabase.getInstance(instance);
            DatabaseReference myRef = database.getReference(reference);
            // Read from the database
            myRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        //If somehow database fails to get any records
                        // This will happen when project record doesn't exist. So don't panic!
                        Log.d(TAG, "Operation Failed " + task.getResult().toString());

                    } else {
                        //Check if the user input id is correct or not
                        if (task.getResult().child(String.valueOf(userID)).exists()) {
                            try {
                                User_Firebase getUser = task.getResult().child(String.valueOf(userID)).getValue(User_Firebase.class);
                                if (getUser.isFavorite()) {
                                    image_favorite.setVisibility(View.VISIBLE);
                                }
                                //Set Views and data
                                text_userName.setText(getUser.getFirst_name() + " " + getUser.getLast_name());
                                userFavorite = getUser.isFavorite();
                                userEmail = getUser.getEmail();
                                Log.d(TAG, "onComplete: useremail=" + userEmail);
                                text_email.setText(userEmail);
                                userAvatar = getUser.getAvatar();
                                userFirstName = getUser.getFirst_name();
                                userLastName = getUser.getLast_name();
                                //Set Profile
                                String avatar = getUser.getAvatar();
                                URL url = new URL(avatar);
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setDoInput(true);
                                connection.connect();
                                InputStream input = connection.getInputStream();
                                Bitmap bmAvatar = BitmapFactory.decodeStream(input);
                                civ_profile.setImageBitmap(bmAvatar);
                                //Set Edit
                                et_firstname.setText(getUser.getFirst_name());
                                et_lastname.setText(getUser.getLast_name());
                                et_email.setText(userEmail);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ContactProfile.this, "Unable to retrieve user data!", Toast.LENGTH_SHORT).show();
                }
            });

            if(userEdit == 1){
                editUserSwitcher();
            }

        } else {
            //If the user id is -1, means that the user has clicked on 'add user' option.
            editUserSwitcher(); //Switch the views
            getAvailableID();
            userAvatar = "https://firebasestorage.googleapis.com/v0/b/dashlabs-2910d.appspot.com/o/users%2Fuserdefault.png?alt=media&token=33024242-795c-4f3e-86cd-9c93975ce79e";

        }

        //Get storage from firebase
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

    }


    //When user clicks on send email, open up email app with written data in it
    public void sendEmail(View view) {
        //If the current display is in VIEW PROFILE mode, make button send email
        if (vf_profile.getDisplayedChild() == 0) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{userEmail}); //Insert user email
            i.putExtra(Intent.EXTRA_SUBJECT, "RF INFINITY INTERVIEW");
            i.putExtra(Intent.EXTRA_TEXT, "Greetings Sir/Madam...");
            try {
                startActivity(Intent.createChooser(i, "Choose email..."));
            } catch (android.content.ActivityNotFoundException ex) {
                // TODO: handle edge case where no email client is installed
            }

        //If the current display is in EDIT mode, make button modify record in database
        } else if (vf_profile.getDisplayedChild() == 1) {

            //Init var
            int errorCounter = 0;
            String userEmail, userFirstName, userLastName;
            userEmail = et_email.getText().toString();
            userFirstName = et_firstname.getText().toString();
            userLastName = et_lastname.getText().toString();

            //REGEX
            String pattern_email = "([a-zA-Z0-9]+(?:[._+-][a-zA-Z0-9]+)*)@([a-zA-Z0-9]+(?:[.-][a-zA-Z0-9]+)*[.][a-zA-Z]{2,})";

            //Check for errors before submitted to database
            if (!userEmail.matches(pattern_email)) {
                et_email.setError("Not a valid email. Please enter a correct one!");
                et_email.requestFocus();
                errorCounter++;

            } else
                et_email.setError(null);

            if (userFirstName.isEmpty()) {
                et_firstname.setError("Entry is empty! Please enter a valid first name!");
                et_firstname.requestFocus();
                errorCounter++;

            } else
                et_firstname.setError(null);

            if (userLastName.isEmpty()) {
                et_lastname.setError("Entry is empty! Please enter a valid last name!");
                et_lastname.requestFocus();
                errorCounter++;

            } else
                et_lastname.setError(null);

            if (errorCounter == 0) {
                if (userAvatarURI != null) {

                    // Code for showing progressDialog while uploading
                    ProgressDialog progressDialog
                            = new ProgressDialog(this);
                    progressDialog.setTitle("Uploading Profile to Server...");
                    progressDialog.show();

                    // Defining the child of storageReference
                    StorageReference ref = storageReference.child("users/" + userEmail);

                    // adding listeners on upload
                    // or failure of image
                    ref.putFile(userAvatarURI)
                            .addOnSuccessListener(
                                    new OnSuccessListener<UploadTask.TaskSnapshot>() {

                                        @Override
                                        public void onSuccess(
                                                UploadTask.TaskSnapshot taskSnapshot) {
                                            // Image uploaded successfully

                                            // Get the image URI from firebase
                                            storageReference.child("users/" + userEmail).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    // Got the download URL for 'users/me/profile.png'
                                                    userAvatar = uri.toString();
                                                    Log.d(TAG, "onSuccess: IMAGE FILE FROM FIREBASE " + uri.toString());
                                                    //Update the rest of the user data
                                                    //FIREBASE STUFF
                                                    User_Firebase regUser = new User_Firebase(userFirstName, userLastName, userAvatar, userEmail, userID, userFavorite);
                                                    FirebaseDatabase.getInstance(instance).getReference(reference).child(String.valueOf(userID)).setValue(regUser).addOnCompleteListener(task1 -> {
                                                        if (task1.isSuccessful()) {
                                                            Toast.makeText(ContactProfile.this, "User Updated!", Toast.LENGTH_LONG).show();
                                                            text_userName.setText(userFirstName + " " + userLastName);
                                                            text_email.setText(userEmail);
                                                        } else {
                                                            Toast.makeText(ContactProfile.this, "Something went wrong! Failed to update user!", Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception exception) {
                                                    // Handle any errors
                                                }
                                            });


                                            // Dismiss dialog
                                            progressDialog.dismiss();
                                            Toast
                                                    .makeText(ContactProfile.this,
                                                            "Image Uploaded!",
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                        }
                                    })

                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                    // Error, Image not uploaded
                                    progressDialog.dismiss();
                                    Toast
                                            .makeText(ContactProfile.this,
                                                    "Failed " + e.getMessage(),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }
                            })
                            .addOnProgressListener(
                                    new OnProgressListener<UploadTask.TaskSnapshot>() {

                                        // Progress Listener for loading
                                        // percentage on the dialog box
                                        @Override
                                        public void onProgress(
                                                UploadTask.TaskSnapshot taskSnapshot) {
                                            double progress
                                                    = (100.0
                                                    * taskSnapshot.getBytesTransferred()
                                                    / taskSnapshot.getTotalByteCount());
                                            progressDialog.setMessage(
                                                    "Uploaded "
                                                            + (int) progress + "%");
                                        }
                                    });
                } else {
                    // Code for showing progressDialog while uploading
                    ProgressDialog progressDialog
                            = new ProgressDialog(this);
                    progressDialog.setTitle("Uploading Profile to Server...");
                    progressDialog.show();

                    // If the user did not enter any images, then just use their default image
                    User_Firebase regUser = new User_Firebase(userFirstName, userLastName, userAvatar, userEmail, userID, userFavorite);
                    FirebaseDatabase.getInstance(instance).getReference(reference).child(String.valueOf(userID)).setValue(regUser).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            text_userName.setText(userFirstName + " " + userLastName);
                            text_email.setText(userEmail);
                            // Dismiss dialog
                            progressDialog.dismiss();
                            Toast
                                    .makeText(ContactProfile.this,
                                            "Profile Updated!",
                                            Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            progressDialog.dismiss();
                            Toast
                                    .makeText(ContactProfile.this,
                                            "Failed to Update User!",
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                }
                //Clear any important info
                userAvatarURI = null;
                //Switch
                editUserSwitcher();
            }

        }

    }

    //Get Available ID
    public void getAvailableID(){

        //Get ID
        FirebaseDatabase.getInstance(instance).getReference(reference)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int recordID = 1;
                        long childCount = dataSnapshot.getChildrenCount();
                        boolean unique_bool = true;
                        while(unique_bool == true){
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                User_Firebase t = snapshot.getValue(User_Firebase.class);
                                Log.d(TAG, "onDataChange: Snapshot Key " + snapshot.getKey());
                                if(recordID == Integer.parseInt(snapshot.getKey())){
                                    Log.d(TAG, "onDataChange: " + t.getId());
                                    recordID++;
                                    text_userName.setText("ID: " + String.valueOf(recordID));
                                    userID = recordID;
                                    Log.d(TAG, "onDataChange: Available ID: " + userID);
                                    Log.d(TAG, "onDataChange: Record ID: " + recordID);
                                } else {
                                    childCount--;
                                }
                            }
                            if(childCount == 0){
                                unique_bool = false;
                            } else {
                                childCount = dataSnapshot.getChildrenCount();
                                unique_bool = true;
                            }
                        }
                        Log.d(TAG, "onDataChange: Available ID: " + userID);


                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    //Pressing 'edit' switch between different view screens
    public void editUserSwitcher() {
        if (vf_profile.getDisplayedChild() == 0) {
            //Setup Screen
            button_send.setText("Done");
            text_edit.setVisibility(View.GONE);
            image_favorite.setImageResource(R.drawable.ic_pencil);
            vf_profile.setDisplayedChild(1);
            image_favorite.setVisibility(View.VISIBLE);
        } else if (vf_profile.getDisplayedChild() == 1) {
            //Setup Screen
            button_send.setText("Send Email");
            text_edit.setVisibility(View.VISIBLE);
            image_favorite.setImageResource(R.drawable.ic_star);
            vf_profile.setDisplayedChild(0);
            if (!userFavorite) {
                image_favorite.setVisibility(View.GONE);
            }
        }

    }

    // UploadImage method
    private void uploadImage() {

    }

    public void editUser(View view) {
        editUserSwitcher();
    }

    // the activity result code
    int SELECT_PICTURE = 200;


    public void changePicture(View view) {
        if (vf_profile.getDisplayedChild() == 1) {
            // create an instance of the
            // intent of the type image
            Intent i = new Intent();
            i.setType("image/*");
            i.setAction(Intent.ACTION_GET_CONTENT);
            // pass the constant to compare it
            // with the returned requestCode
            startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
        }


    }


    // this function is triggered when user
    // selects the image from the imageChooser
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == SELECT_PICTURE) {
                // Get the url of the image from data
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    // update the preview image in the layout
                    civ_profile.setImageURI(selectedImageUri);
                    userAvatarURI = selectedImageUri;


                }
            }
        }
    }

    public void qucikFavUser(boolean favorite) {
        // Code for showing progressDialog while uploading
        ProgressDialog progressDialog
                = new ProgressDialog(this);
        if(favorite){
            progressDialog.setTitle("Favoriting User...");
            userFavorite = true;
            favorite = true;
        } else {
            progressDialog.setTitle("Unfavoriting User...");
            userFavorite = false;
            favorite = false;

        }
        progressDialog.show();


        User_Firebase regUser = new User_Firebase(userFirstName, userLastName, userAvatar, userEmail, userID, favorite);
        boolean finalFavorite = favorite;
        FirebaseDatabase.getInstance(instance).getReference(reference).child(String.valueOf(userID)).setValue(regUser).addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                progressDialog.dismiss();
                if (!finalFavorite) {
                    Toast
                            .makeText(ContactProfile.this,
                                    "User no longer favorited!",
                                    Toast.LENGTH_SHORT)
                            .show();
                    image_favorite.setVisibility(View.GONE);
                } else {
                    Toast
                            .makeText(ContactProfile.this,
                                    "User favorited!",
                                    Toast.LENGTH_SHORT)
                            .show();
                    image_favorite.setVisibility(View.VISIBLE);
                }

            } else {
                progressDialog.dismiss();
                Toast
                        .makeText(ContactProfile.this,
                                "Failed to update user!",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    public void favoriteUser(View view) {
        if (vf_profile.getDisplayedChild() == 0) {
            //Favorite and unfavorite child in normal profile viewing mode.
            if (userFavorite) {
                //If user is already favorited, unfavorite them, vice versa.
                // If the user did not enter any images, then just use their default image
                qucikFavUser(false);
            } else if (!userFavorite) {
                qucikFavUser(true);
            }
        }
    }
}