package ch.dissem.apps.abit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import ch.dissem.bitmessage.entity.BitmessageAddress;

/**
 * Compose a new message.
 */
public class ComposeMessageActivity extends AppCompatActivity {
    public static final String EXTRA_IDENTITY = "ch.dissem.abit.Message.SENDER";
    public static final String EXTRA_RECIPIENT = "ch.dissem.abit.Message.RECIPIENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toolbar_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_close);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(false);

        // Display the fragment as the main content.
        ComposeMessageFragment fragment = new ComposeMessageFragment();
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
    }
}
