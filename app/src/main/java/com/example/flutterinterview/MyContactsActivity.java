package com.example.flutterinterview;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class MyContactsActivity extends AppCompatActivity {

    ArrayList<String> userList;
    Handler mainHandler = new Handler();

    //TAG
    static final String TAG = "FI-MyContactsActivity";
    String reference = "Flutter-Users";
    String instance = "https://dashlabs-2910d-default-rtdb.asia-southeast1.firebasedatabase.app/";


    //RECYCLER VIEW VARIABLES
    private RecyclerView mRecyclerView, mRecyclerView_Favorite;
    private ContactAdapter mAdapter, nAdapter_Favorite;
    private RecyclerView.LayoutManager mLayoutManager, mLayoutManager_Favorite;
    ProgressDialog progressDialog;

    //Swiper
    SwipeController swipeController = null;

    //REFERENCES
    SearchView menu_search_view;
    ViewFlipper viewFlipper;
    Button button_showall, button_showfavorite;

    //INSTANCE VARIABLES
    public final ArrayList<Users> addUser = new ArrayList<>();
    public final ArrayList<Users> favUser = new ArrayList<>();
    private boolean currentView = false;
    // False - All view
    // True - Favorite view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        //Action bar
        getSupportActionBar().setTitle("My Contacts");
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);

        initializeUserList(); // append user list to the card view
        //new fetchData().start(); //Register users based on the json field

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //References
        menu_search_view = findViewById(R.id.menu_search_view);
        viewFlipper = findViewById(R.id.viewFlipper);
        button_showall = findViewById(R.id.button_showall);
        button_showfavorite = findViewById(R.id.button_showfavorite);




    }

    //Generate the list of contacts
    private void initializeUserList() {
        FirebaseDatabase.getInstance(instance).getReference(reference)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (addUser != null) {
                            addUser.clear();
                        }
                        if (favUser != null) {
                            favUser.clear();
                        }

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            User_Firebase getUser = snapshot.getValue(User_Firebase.class);
                            //Append all and favorite records to each of their lists
                            String avatar = getUser.getAvatar();
                            Uri uriAvatar = Uri.parse(avatar);
                            Log.d(TAG, "onDataChange: uriAvatar " + uriAvatar);
                            if (getUser.isFavorite()) {
                                addUser.add(new Users(getUser.first_name, getUser.last_name + " ⭐", uriAvatar, getUser.getEmail(), getUser.getId(), false));
                                favUser.add(new Users(getUser.first_name, getUser.last_name + " ⭐", uriAvatar, getUser.getEmail(), getUser.getId(), false));
                            } else {
                                addUser.add(new Users(getUser.first_name, getUser.last_name, uriAvatar, getUser.getEmail(), getUser.getId(), false));
                            }

                        }


                        //Insert adapters
                        mRecyclerView_Favorite = findViewById(R.id.my_recyclerView_favorite);
                        mRecyclerView_Favorite.setHasFixedSize(true);
                        mLayoutManager_Favorite = new LinearLayoutManager(MyContactsActivity.this);
                        nAdapter_Favorite = new ContactAdapter(favUser);
                        mRecyclerView_Favorite.setLayoutManager(mLayoutManager_Favorite);
                        mRecyclerView_Favorite.setAdapter(nAdapter_Favorite);

                        mRecyclerView = findViewById(R.id.my_recyclerView);
                        mRecyclerView.setHasFixedSize(true);
                        mLayoutManager = new LinearLayoutManager(MyContactsActivity.this);
                        mAdapter = new ContactAdapter(addUser);
                        mRecyclerView.setLayoutManager(mLayoutManager);
                        mRecyclerView.setAdapter(mAdapter);

                        //Search view command
                        menu_search_view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                            @Override
                            public boolean onQueryTextSubmit(String s) {
                                if(viewFlipper.getDisplayedChild() == 0){
                                    mAdapter.getFilter().filter(s);
                                } else if(viewFlipper.getDisplayedChild() == 1){
                                    nAdapter_Favorite.getFilter().filter(s);
                                }

                                return false;
                            }

                            @Override
                            public boolean onQueryTextChange(String s) {
                                //Searches based on letter by letter input - preferred
                                if(viewFlipper.getDisplayedChild() == 0){
                                    mAdapter.getFilter().filter(s);

                                } else if(viewFlipper.getDisplayedChild() == 1){
                                    nAdapter_Favorite.getFilter().filter(s);
                                }
                                return false;
                            }
                        });

                        //Swipe left feature
                        SwiperHelper swipeHelper = new SwiperHelper(MyContactsActivity.this, mRecyclerView, 200) {
                            @Override
                            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<SwiperHelper.MyButton> buffer) {
                                buffer.add(new MyButton("Delete",
                                        R.drawable.ic_baseline_delete_24,
                                        30,
                                        Color.parseColor("#FFFFFF"),
                                        new MyButtonClickListener() {
                                            @Override
                                            public void onClick(int pos) {
                                                String getUserEmail = addUser.get(pos).getEmail();
                                                AlertDialog.Builder builder1 = new AlertDialog.Builder(MyContactsActivity.this);
                                                FirebaseDatabase.getInstance(instance).getReference(reference)
                                                        .addValueEventListener(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                                    User_Firebase getUser = snapshot.getValue(User_Firebase.class);
                                                                    if (getUser.getEmail().equals(getUserEmail)) {
                                                                        builder1.setMessage("Are you sure you want to delete this contact?");
                                                                        builder1.setCancelable(true);
                                                                        builder1.setPositiveButton(
                                                                                "Yes",
                                                                                new DialogInterface.OnClickListener() {
                                                                                    public void onClick(DialogInterface dialog, int id) {
                                                                                        dialog.cancel();
                                                                                        Toast.makeText(MyContactsActivity.this, "User (" + addUser.get(pos).getFirst_name() + " " + addUser.get(pos).getLast_name() + ") has been deleted!", Toast.LENGTH_SHORT).show();
                                                                                        snapshot.getRef().removeValue();
                                                                                        finish();
                                                                                        startActivity(getIntent());
                                                                                    }
                                                                                });
                                                                        builder1.setNegativeButton("No",
                                                                                new DialogInterface.OnClickListener() {
                                                                                    public void onClick(DialogInterface dialog, int id) {
                                                                                        dialog.cancel();

                                                                                    }
                                                                                });
                                                                        AlertDialog alertDescription = builder1.create();
                                                                        alertDescription.show();
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(DatabaseError databaseError) {
                                                            }
                                                        });
                                            }
                                        }, MyContactsActivity.this));
                                buffer.add(new MyButton("Edit",
                                        R.drawable.ic_edit,
                                        30,
                                        Color.parseColor("#FFFFFF"),
                                        new MyButtonClickListener() {
                                            @Override
                                            public void onClick(int pos) {
                                                Intent intent = new Intent(getBaseContext(), ContactProfile.class);
                                                intent.putExtra("USER_ID", (addUser.get(pos).getId()));
                                                intent.putExtra("USER_EMAIL", (addUser.get(pos).getEmail()));
                                                intent.putExtra("EDIT", 1);
                                                finish();
                                                startActivity(intent);
                                            }
                                        }, MyContactsActivity.this));
                            }
                        };

                        SwiperHelper swipeHelper_favorite = new SwiperHelper(MyContactsActivity.this, mRecyclerView_Favorite, 200) {
                            @Override
                            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<SwiperHelper.MyButton> buffer) {
                                buffer.add(new MyButton("Delete",
                                        R.drawable.ic_baseline_delete_24,
                                        30,
                                        Color.parseColor("#FFFFFF"),
                                        new MyButtonClickListener() {
                                            @Override
                                            public void onClick(int pos) {
                                                String getUserEmail = favUser.get(pos).getEmail();
                                                AlertDialog.Builder builder1 = new AlertDialog.Builder(MyContactsActivity.this);
                                                FirebaseDatabase.getInstance(instance).getReference(reference)
                                                        .addValueEventListener(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                                    User_Firebase getUser = snapshot.getValue(User_Firebase.class);
                                                                    if (getUser.getEmail().equals(getUserEmail)) {
                                                                        builder1.setMessage("Are you sure you want to delete this contact?");
                                                                        builder1.setCancelable(true);
                                                                        builder1.setPositiveButton(
                                                                                "Yes",
                                                                                new DialogInterface.OnClickListener() {
                                                                                    public void onClick(DialogInterface dialog, int id) {
                                                                                        dialog.cancel();
                                                                                        Toast.makeText(MyContactsActivity.this, "User (" + favUser.get(pos).getFirst_name() + " " + addUser.get(pos).getLast_name() + ") has been deleted!", Toast.LENGTH_SHORT).show();
                                                                                        snapshot.getRef().removeValue();
                                                                                        finish();
                                                                                        startActivity(getIntent());
                                                                                    }
                                                                                });
                                                                        builder1.setNegativeButton("No",
                                                                                new DialogInterface.OnClickListener() {
                                                                                    public void onClick(DialogInterface dialog, int id) {
                                                                                        dialog.cancel();

                                                                                    }
                                                                                });
                                                                        AlertDialog alertDescription = builder1.create();
                                                                        alertDescription.show();
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(DatabaseError databaseError) {
                                                            }
                                                        });
                                            }
                                        }, MyContactsActivity.this));
                                buffer.add(new MyButton("Edit",
                                        R.drawable.ic_edit,
                                        30,
                                        Color.parseColor("#FFFFFF"),
                                        new MyButtonClickListener() {
                                            @Override
                                            public void onClick(int pos) {
                                                Intent intent = new Intent(getBaseContext(), ContactProfile.class);
                                                intent.putExtra("USER_ID", (favUser.get(pos).getId()));
                                                intent.putExtra("USER_EMAIL", (favUser.get(pos).getEmail()));
                                                intent.putExtra("EDIT", 1);
                                                finish();
                                                startActivity(intent);
                                            }
                                        }, MyContactsActivity.this));
                            }
                        };


                        mAdapter.setOnItemClickListener(new ContactAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(int position) {

                            }

                            @Override
                            public void onSendEmailClick(int position) {
                                Intent intent = new Intent(getBaseContext(), ContactProfile.class);
                                intent.putExtra("USER_ID", (addUser.get(position).getId()));
                                intent.putExtra("USER_EMAIL", (addUser.get(position).getEmail()));
                                finish();
                                startActivity(intent);

                            }
                        });
                        nAdapter_Favorite.setOnItemClickListener(new ContactAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(int position) {

                            }

                            @Override
                            public void onSendEmailClick(int position) {
                                Intent intent = new Intent(getBaseContext(), ContactProfile.class);
                                intent.putExtra("USER_ID", (favUser.get(position).getId()));
                                intent.putExtra("USER_EMAIL", (favUser.get(position).getEmail()));
                                finish();
                                startActivity(intent);

                            }
                        });




                    }


                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

    }

    //ALL button
    public void vf_showAll(View view) {
        setShowButton(true);
        Log.d(TAG, "vf_showAll: currentView" + viewFlipper.getDisplayedChild());
        if (addUser.isEmpty()) {
            viewFlipper.setDisplayedChild(2); //Empty contact image
        } else {
            viewFlipper.setDisplayedChild(0); //Original ALL contacts screen
        }
    }

    //Switch between ALL and FAVORITE view
    public void setShowButton(boolean switcher) {
        if (switcher) {
            button_showfavorite.setTextColor(Color.parseColor("#8D8DAB"));
            button_showfavorite.setBackgroundColor(ContextCompat.getColor(MyContactsActivity.this, R.color.white));
            button_showall.setTextColor(ContextCompat.getColor(MyContactsActivity.this, R.color.white));
            button_showall.setBackgroundColor(ContextCompat.getColor(MyContactsActivity.this, R.color.colorPrimary));
            viewFlipper.setDisplayedChild(0);
        } else {
            button_showall.setTextColor(Color.parseColor("#8D8DAB"));
            button_showall.setBackgroundColor(ContextCompat.getColor(MyContactsActivity.this, R.color.white));
            button_showfavorite.setTextColor(ContextCompat.getColor(MyContactsActivity.this, R.color.white));
            button_showfavorite.setBackgroundColor(ContextCompat.getColor(MyContactsActivity.this, R.color.colorPrimary));
            viewFlipper.setDisplayedChild(1);
        }

    }

    //FAVORITE button
    public void vf_showFavorite(View view) {
        setShowButton(false);
        Log.d(TAG, "vf_showAll: currentView" + viewFlipper.getDisplayedChild());
        //Show the custom image when content is empty
        if (favUser.isEmpty()) {
            Log.d(TAG, "onDataChange: Favorite User: Empty!");
            viewFlipper.setDisplayedChild(2);
        } else {
            viewFlipper.setDisplayedChild(1);
        }
    }

    public void addUser(View view) {
        Intent intent = new Intent(getBaseContext(), ContactProfile.class);
        intent.putExtra("USER_ID", -1);
        startActivity(intent);
    }

    //Get the json list of data from the given url
    class fetchData extends Thread {

        String data = "";

        @Override
        public void run() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog = new ProgressDialog(MyContactsActivity.this);
                    progressDialog.setMessage("Fetching Data");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            });
            try {
                URL url = new URL("https://reqres.in/api/users?page=1");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    data = data + line;
                }

                if (!data.isEmpty()) {
                    JSONObject jsonObject = new JSONObject((data));
                    JSONArray users = jsonObject.getJSONArray("data");
                    addUser.clear();
                    for (int i = 0; i < users.length(); i++) {
                        JSONObject names = users.getJSONObject(i);
                        String first_name = names.getString("first_name");
                        String last_name = names.getString("last_name");
                        String avatar = names.getString("avatar");
                        Uri uriAvatar = Uri.parse(avatar);
                        String email = names.getString("email");
                        int id = names.getInt("id");
                        //For testing, assign the favorite value to the first user
                        boolean favorite = false;
                        if (i == 1) {
                            favorite = true;
                        }
                        //int avatar = names.getInt("avatar");
                        //addUser.add(new Users(first_name, last_name, bmAvatar, email, id));
                        Log.d(TAG, "run: " + first_name + last_name + uriAvatar + email + id);
                        //FIREBASE STUFF
                        User_Firebase createUser = new User_Firebase(first_name, last_name, avatar, email, id, favorite);
                        FirebaseDatabase.getInstance(instance).getReference(reference).child(String.valueOf(id)).setValue(createUser).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Log.d(TAG, "run: Added user!");
                            } else {
                                Log.d(TAG, "run: Unable to add user!");
                            }
                        });
                    }
                    Log.d(TAG, "run: if data is empty");
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    mAdapter.notifyDataSetChanged();

                    //FIXME no internet connection causes CTD
                }
            });

        }
    }


}


