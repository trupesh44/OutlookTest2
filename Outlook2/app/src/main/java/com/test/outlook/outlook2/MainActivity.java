package com.test.outlook.outlook2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;

import com.test.outlook.outlook2.adapter.GalleryAdapter;
import com.test.outlook.outlook2.app.AppController;
import com.test.outlook.outlook2.config.Params;
import com.test.outlook.outlook2.model.Data;
import com.test.outlook.outlook2.model.PageInfo;
import com.test.outlook.outlook2.model.Thumbnail;
import com.test.outlook.outlook2.utils.Utils;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private final String endpoint = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=thumbnail&pithumbsize=100&pilimit=50&generator=prefixsearch&gpslimit=50&gpssearch=";
    private String contentUrl;
    private ArrayList<Thumbnail> alThumbnail;
    private GalleryAdapter mAdapter;
    private RecyclerView recyclerView;
    private EditText editText;
    private Gson gson;
    private static Bundle bundleRecyclerViewState;

    private Runnable fetchImage = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, contentUrl);
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, contentUrl, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                    Log.d(TAG, response.toString());
                    alThumbnail.clear();
                    gson = new Gson();
                    Data data = gson.fromJson(response.toString(), Data.class);

                    if(data != null && data.getQuery() != null
                            && data.getQuery().getPages() != null) {
                        ArrayList<PageInfo> alPageInfo = new ArrayList<PageInfo>(data
                                .getQuery().getPages().values());
                        for (int i = 0; i < alPageInfo.size(); i++) {
                            Thumbnail thumbnail = alPageInfo.get(i).getThumbnail();
                            alThumbnail.add(thumbnail);
                        }
                    }
                    mAdapter.notifyDataSetChanged();

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                    Log.e(TAG, "Error: " + error.getMessage());
                    alertUser("Error","There was error in fetching data, please try after sometime");
                }
            });

            // Adding request to request queue
            AppController.getInstance().addToRequestQueue(req);
        }
    };
    private Handler fetchImageListHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editText = (EditText) findViewById(R.id.et_search);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        alThumbnail = new ArrayList<>();
        mAdapter = new GalleryAdapter(getApplicationContext(), alThumbnail);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(Utils.isNetworkAvailable(MainActivity.this)) {
                    contentUrl = "";
                    if (!TextUtils.isEmpty(s)) {
                        contentUrl = endpoint + s.toString();
                        fetchImages();
                    } else {
                        // Don't show images if user clears the text
                        alThumbnail.clear();
                        mAdapter.notifyDataSetChanged();
                    }
                    Log.d(TAG, "Content URL = " + contentUrl);
                } else {
                    alertUser("No Internet Connection","Please connect to a Internet Network to search Images");
                }
            }
        });

    }

    private void fetchImages() {
        fetchImageListHandler.removeCallbacks(fetchImage);
        // Send the request after some time so if user is typing multiple requests are not send
        fetchImageListHandler.postDelayed(fetchImage, Params.WAIT_FOR_USER_TEXT * Params.MILLI_SECONDS);
    }

    private void alertUser(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNeutralButton(R.string.label_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setTitle(title);
        builder.setMessage(message);
        builder.create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // restore RecyclerView state
        if (bundleRecyclerViewState != null) {
            Parcelable listState = bundleRecyclerViewState.getParcelable(Params.KEY_RECYCLER_STATE);
            recyclerView.getLayoutManager().onRestoreInstanceState(listState);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        bundleRecyclerViewState = new Bundle();
        Parcelable listState = recyclerView.getLayoutManager().onSaveInstanceState();
        bundleRecyclerViewState.putParcelable(Params.KEY_RECYCLER_STATE, listState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alThumbnail.clear();
    }
}
