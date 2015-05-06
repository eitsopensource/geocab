package br.com.geocab.controller.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import br.com.geocab.R;
import br.com.geocab.controller.app.AppController;
import br.com.geocab.controller.delegate.MarkerDelegate;
import br.com.geocab.entity.Layer;
import br.com.geocab.entity.Marker;
import br.com.geocab.entity.MarkerStatus;

public class MarkerActivity extends Activity {

    /**
     *
     */
    private String wktCoordenate;
    private Button buttonSelectPhoto;
    private ImageView imgMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker);

        imgMarker = (ImageView) findViewById(R.id.img_marker);
        buttonSelectPhoto = (Button) findViewById(R.id.btn_select_photo);
        this.wktCoordenate = this.getIntent().getStringExtra("wktCoordenate");

        final ArrayAdapter<Layer> dataAdapter = new ArrayAdapter<Layer>(this, android.R.layout.simple_spinner_item, new ArrayList());
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner layers_spinner = (Spinner) findViewById(R.id.spinner_layers);
        layers_spinner.setAdapter(dataAdapter);

        String url = "http://geocab.sbox.me" + "/layergroup/layers";

        JsonArrayRequest jReq = new JsonArrayRequest(url,
                new Response.Listener<JSONArray>() {

                    Gson json = new Gson();

                    @Override
                    public void onResponse(JSONArray response) {
                        ArrayList<Layer> result = new ArrayList<Layer>();

                        for (int i = 0; i < response.length(); i++)
                        {
                            try
                            {
                                Layer layer = json.fromJson(response.getString(i), Layer.class);
                                if( layer.getIsChecked() == null )
                                {
                                    layer.setIsChecked(false);
                                }
                                result.add(layer);

                            }
                            catch (JSONException e)
                            {

                            }
                        }

                        dataAdapter.addAll(result);
                        dataAdapter.notifyDataSetChanged();

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {


            }
        });

        AppController.getInstance().addToRequestQueue(jReq);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_marker, menu);
        return true;
    }

    public void selectMarkerImage(View v) {

        final CharSequence[] options = { "Galeria", "Tirar foto", "Cancelar" };

        AlertDialog.Builder builder = new AlertDialog.Builder(MarkerActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Tirar foto"))
                {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File f = new File(android.os.Environment.getExternalStorageDirectory(), "temp.jpg");
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                    startActivityForResult(intent, 1);
                }
                else if (options[item].equals("Galeria"))
                {
                    Intent intent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 2);

                }
                else if (options[item].equals("Cancelar")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                File f = new File(Environment.getExternalStorageDirectory().toString());
                for (File temp : f.listFiles()) {
                    if (temp.getName().equals("temp.jpg")) {
                        f = temp;
                        break;
                    }
                }
                try {
                    Bitmap bitmap;
                    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

                    bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(),
                            bitmapOptions);

                    imgMarker.setImageBitmap(bitmap);

                    String path = android.os.Environment
                            .getExternalStorageDirectory()
                            + File.separator
                            + "Phoenix" + File.separator + "default";
                    f.delete();
                    OutputStream outFile = null;
                    File file = new File(path, String.valueOf(System.currentTimeMillis()) + ".jpg");
                    try {
                        outFile = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outFile);
                        outFile.flush();
                        outFile.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == 2) {

                Uri selectedImage = data.getData();
                String[] filePath = { MediaStore.Images.Media.DATA };
                Cursor c = getContentResolver().query(selectedImage,filePath, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String picturePath = c.getString(columnIndex);
                c.close();
                Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
                imgMarker.setImageBitmap(thumbnail);
            }
        }
    }

    public void insertMarker(View v){

        Marker marker = new Marker();
        marker.setStatus(MarkerStatus.PENDING);
        marker.setWktCoordenate(this.wktCoordenate);

        Layer layer = (Layer) ( (Spinner) findViewById(R.id.spinner_layers) ).getSelectedItem();
        marker.setLayer(layer);

        MarkerDelegate markerDelegate = new MarkerDelegate(this, null);
        markerDelegate.insert(marker);

    }

}
