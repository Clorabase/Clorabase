package com.clorabase.console;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.clorabase.console.adapters.GithubFilesAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class DocumentActivity extends AppCompatActivity {
    private ViewGroup layout;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document);


        layout = findViewById(R.id.layout);
        var addField = findViewById(R.id.add_field);

        var intent = getIntent();
        var mode = intent.getStringExtra("mode");
        isEditMode = mode.equals("edit");

        if (isEditMode) {
            try {
                var dec = Utils.decrypt(intent.getByteArrayExtra("enc_data"), MainActivity.CURRENT_PROJECT);
                assert dec != null;
                var data = new JSONObject(dec);
                var keys = data.keys();
                while (keys.hasNext()) {
                    var key = keys.next();
                    var type = data.get(key).getClass();
                    String value;
                    if (type == JSONArray.class) {
                        value = data.getJSONArray(key).toString(3);
                    } else
                        value = data.getString(key);

                    populateFields(key, value, type);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
            }
        } else {
            addField.performClick();
        }

        addField.setOnClickListener(v -> {
            if (isEditMode) {
                new AlertDialog.Builder(this)
                        .setTitle("Select Type")
                        .setCancelable(true)
                        .setItems(new String[]{"String", "Number", "Boolean", "Array"}, (dialog, which) -> {
                            switch (which) {
                                case 0 -> populateFields("", "", String.class);
                                case 1 -> populateFields("", "", Integer.class);
                                case 2 -> populateFields("", "", Boolean.class);
                                case 3 -> populateFields("", "[]", JSONArray.class);
                            }
                        }).show();
            } else {
                var view = LayoutInflater.from(this).inflate(R.layout.document_fields, layout,false);
                layout.addView(view, layout.getChildCount() - 1);

                var delete = view.findViewById(R.id.title);
                EditText value = view.findViewById(R.id.value);
                Spinner datatypeSpinner = view.findViewById(R.id.datatype);

                delete.setOnClickListener(v1 -> layout.removeView(view));
                datatypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case 0 -> value.setTag(String.class);
                            case 1 -> {
                                value.setTag(Number.class);
                                value.setInputType(EditorInfo.TYPE_NUMBER_FLAG_SIGNED);
                            }
                            case 2 -> {
                                value.setTag(Boolean.class);
                                value.setInputType(InputType.TYPE_NULL);
                            }
                            case 3 -> value.setTag(JSONArray.class);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }

        });
    }

    private void populateFields(String key, String value, Class type) {
        var layout = new LinearLayout(this);
        var lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 50;
        layout.setLayoutParams(lp);

        EditText keyFiled = new EditText(this);
        EditText valueFiled = new EditText(this);

        var background = new GradientDrawable();
        background.setStroke(1, Color.BLACK);

        var editTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editTextParams.leftMargin = 20;
        editTextParams.gravity = Gravity.CENTER;
        valueFiled.setLayoutParams(editTextParams);
        valueFiled.setText(value);
        valueFiled.setTag(type);
        valueFiled.setPadding(10, 10, 10, 10);
        valueFiled.setHint("Value");
        valueFiled.setMinEms(10);
        valueFiled.setBackground(background);
        keyFiled.setText(key);
        keyFiled.setHint("key");
        keyFiled.setMinEms(3);

        layout.addView(keyFiled);
        layout.addView(valueFiled);
        this.layout.addView(layout, this.layout.getChildCount() - 1);
    }

    private String compileJSON() {
        var json = new JSONObject();
        try {
            for (int i = 0; i < layout.getChildCount() - 1; i++) {
                var child = layout.getChildAt(i);
                String key = "";
                String value = "";
                Object parsed = null;
                Class type;

                if (isEditMode) {
                    if (child instanceof LinearLayout) {
                        var edittext = ((EditText) ((LinearLayout) child).getChildAt(1));
                        key = ((EditText) ((LinearLayout) child).getChildAt(0)).getText().toString();
                        type = ((Class) edittext.getTag());
                        value = edittext.getText().toString();
                    } else
                        continue;
                } else {
                    if (child instanceof CardView) {
                        child = ((CardView) child).getChildAt(0);
                        var valueField = ((EditText) ((LinearLayout) child).getChildAt(3));
                        key = ((EditText) ((LinearLayout) child).getChildAt(1)).getText().toString();
                        type = ((Class) valueField.getTag());
                        value = valueField.getText().toString();
                    } else
                        continue;
                }

                if (!(value.isEmpty() || value.isBlank() || value.equalsIgnoreCase("null"))){
                    if (type == Integer.class) {
                        parsed = Integer.parseInt(value);
                    } else if (type == Number.class) {
                        parsed = Double.parseDouble(value);
                    } else if (type == Boolean.class) {
                        parsed = Boolean.parseBoolean(value);
                    } else if (type == String.class){
                        parsed = value;
                    } else if (type == JSONArray.class) {
                        try {
                            parsed = new JSONArray(value);
                        } catch (JSONException e) {
                            Toast.makeText(this, "Invalid JSON array", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                    json.put(key, parsed);
                }
            }
            json.put("timestamp",new Date().getTime());
            return json.toString(3);
        } catch (JSONException | NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.document_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        var path = getIntent().getStringExtra("path");
        var callback = new Utils.AsyncCallback() {
            @Override
            public void onComplete(String sha) {
                var name = path.substring(path.lastIndexOf('/') + 1);
                var file = new GithubFilesAdapter.File(true,name, sha, path);
                Toast.makeText(DocumentActivity.this, "Operation success", Toast.LENGTH_SHORT).show();

                setResult(RESULT_OK,new Intent().putExtra("file", file));
                finish();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(DocumentActivity.this, "Operation failed, Please report bug if problem persist", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        };

        if (item.getItemId() == R.id.save) {
            var contents = compileJSON();
            if (contents == null) {
                finish();
                Toast.makeText(this, "There was an error in your document", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    var enc = Utils.encrypt(contents,MainActivity.CURRENT_PROJECT);
                    if (isEditMode) {
                        Utils.update(enc, path, callback);
                    } else {
                        Utils.create(enc, path, callback);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        } else if (item.getItemId() == R.id.delete) {
            Utils.delete(path, callback);
            return true;
        } else
            return false;
    }
}