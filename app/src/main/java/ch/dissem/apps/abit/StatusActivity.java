package ch.dissem.apps.abit;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import ch.dissem.apps.abit.service.Singleton;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;

public class StatusActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(false);

        BitmessageContext bmc = Singleton.getBitmessageContext(this);
        StringBuilder status = new StringBuilder();
        for (BitmessageAddress address : bmc.addresses().getIdentities()) {
            status.append(address.getAddress()).append('\n');
        }
        status.append('\n');
        status.append(bmc.status());
        ((TextView) findViewById(R.id.content)).setText(status);
    }

}
