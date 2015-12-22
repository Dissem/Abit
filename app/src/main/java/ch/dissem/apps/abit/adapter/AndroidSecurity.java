package ch.dissem.apps.abit.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ch.dissem.apps.abit.util.PRNGFixes;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.ObjectMessage;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.PlaintextHolder;
import ch.dissem.bitmessage.entity.payload.Broadcast;
import ch.dissem.bitmessage.entity.valueobject.Label;
import ch.dissem.bitmessage.factory.Factory;
import ch.dissem.bitmessage.ports.ProofOfWorkEngine;
import ch.dissem.bitmessage.security.sc.SpongySecurity;
import ch.dissem.bitmessage.utils.UnixTime;

import static ch.dissem.apps.abit.util.Constants.PREFERENCE_SERVER_POW;
import static ch.dissem.bitmessage.entity.Plaintext.Status.SENT;
import static ch.dissem.bitmessage.entity.Plaintext.Type.BROADCAST;
import static ch.dissem.bitmessage.utils.UnixTime.DAY;

/**
 * @author Christian Basler
 */
public class AndroidSecurity extends SpongySecurity {
    public AndroidSecurity() {
        PRNGFixes.apply();
    }
}
