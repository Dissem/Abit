package ch.dissem.apps.abit.service;

import android.content.Context;

import ch.dissem.apps.abit.MessageListActivity;
import ch.dissem.apps.abit.listener.MessageListener;
import ch.dissem.apps.abit.repository.AndroidAddressRepository;
import ch.dissem.apps.abit.repository.AndroidInventory;
import ch.dissem.apps.abit.repository.AndroidMessageRepository;
import ch.dissem.apps.abit.repository.SqlHelper;
import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.networking.DefaultNetworkHandler;
import ch.dissem.bitmessage.ports.AddressRepository;
import ch.dissem.bitmessage.ports.MemoryNodeRegistry;
import ch.dissem.bitmessage.ports.MessageRepository;
import ch.dissem.bitmessage.ports.Security;
import ch.dissem.bitmessage.security.sc.SpongySecurity;

/**
 * Provides singleton objects across the application.
 */
public class Singleton {
    private static SqlHelper sqlHelper;
    private static Security security;
    private static MessageRepository messageRepository;
    private static MessageListener messageListener;
    private static AddressRepository addressRepository;

    static {
        ch.dissem.bitmessage.utils.Singleton.initialize(new SpongySecurity());
    }

    public static MessageListener getMessageListener(Context ctx) {
        if (messageListener == null) {
            synchronized (Singleton.class) {
                if (messageListener == null) {
                    messageListener = new MessageListener(ctx);
                }
            }
        }
        return messageListener;
    }

    public static SqlHelper getSqlHelper(Context ctx) {
        if (sqlHelper == null) {
            synchronized (Singleton.class) {
                if (sqlHelper == null) {
                    sqlHelper = new SqlHelper(ctx.getApplicationContext());
                }
            }
        }
        return sqlHelper;
    }

    public static MessageRepository getMessageRepository(Context ctx) {
        if (messageRepository == null) {
            ctx = ctx.getApplicationContext();
            getSqlHelper(ctx);
            synchronized (Singleton.class) {
                if (messageRepository == null) {
                    messageRepository = new AndroidMessageRepository(sqlHelper, ctx);
                }
            }
        }
        return messageRepository;
    }

    public static AddressRepository getAddressRepository(Context ctx) {
        if (addressRepository == null) {
            ctx = ctx.getApplicationContext();
            getSqlHelper(ctx);
            synchronized (Singleton.class) {
                if (addressRepository == null) {
                    addressRepository = new AndroidAddressRepository(sqlHelper);
                }
            }
        }
        return addressRepository;
    }
}
