package com.tanim.ccepedia;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

public class AIChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure you are using the revised XML layout: R.layout.activity_ai_chat
        setContentView(R.layout.activity_ai_chat);

        // 1. Find the Toolbar defined in the XML
        Toolbar toolbar = findViewById(R.id.toolbar);

        // 2. Set the Toolbar as the Activity's ActionBar
        setSupportActionBar(toolbar);

        // 3. Enable the Back Button (Up button)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("AI Tutor");
            // This line tells Android to display the standard back arrow (Up button)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.Green));
        }

        // 4. Handle the back button click directly on the navigation icon
        toolbar.setNavigationOnClickListener(v -> finish());

        // TODO: Continue with other chat initialization:
        // RecyclerView, EditText, and Send Button initialization here...
    }

    // Optional: If you want to use the standard onOptionsItemSelected method
    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle the Up button being pressed (if not handled by setNavigationOnClickListener)
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    */
}