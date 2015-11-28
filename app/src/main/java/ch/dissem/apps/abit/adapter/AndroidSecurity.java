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
    private final SharedPreferences preferences;

    public AndroidSecurity(Context ctx) {
        PRNGFixes.apply();
        preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    @Override
    public void doProofOfWork(ObjectMessage object, long nonceTrialsPerByte, long extraBytes, ProofOfWorkEngine.Callback callback) {
        if (preferences.getBoolean(PREFERENCE_SERVER_POW, false)) {
            object.setNonce(new byte[8]);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                object.write(out);
                sendAsBroadcast(getContext().getAddressRepo().getIdentities().get(0), out.toByteArray());
                if (object.getPayload() instanceof PlaintextHolder) {
                    Plaintext plaintext = ((PlaintextHolder) object.getPayload()).getPlaintext();
                    plaintext.setInventoryVector(object.getInventoryVector());
                    plaintext.setStatus(SENT);
                    plaintext.removeLabel(Label.Type.OUTBOX);
                    plaintext.addLabels(getContext().getMessageRepository().getLabels(Label.Type.SENT));
                    getContext().getMessageRepository().save(plaintext);

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            super.doProofOfWork(object, nonceTrialsPerByte, extraBytes, callback);
        }
    }

    private void sendAsBroadcast(BitmessageAddress identity, byte[] data) throws IOException {
        Plaintext msg = new Plaintext.Builder(BROADCAST)
                .from(identity)
                .message(data)
                .build();
        Broadcast payload = Factory.getBroadcast(identity, msg);
        long expires = UnixTime.now(+2 * DAY);
        final ObjectMessage object = new ObjectMessage.Builder()
                .stream(identity.getStream())
                .expiresTime(expires)
                .payload(payload)
                .build();
        object.sign(identity.getPrivateKey());
        payload.encrypt();
        object.setNonce(new byte[8]);

        getContext().getInventory().storeObject(object);
        getContext().getNetworkHandler().offer(object.getInventoryVector());
        // TODO: offer to the trusted node only?
        // at least make sure it is offered to the trusted node!
    }
}
