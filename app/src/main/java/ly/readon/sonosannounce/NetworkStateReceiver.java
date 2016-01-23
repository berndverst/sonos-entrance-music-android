package ly.readon.sonosannounce;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import net.frotz.sonos.Discover;
import net.frotz.sonos.Sonos;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Bernd Verst(@berndverst)
 */
public class NetworkStateReceiver extends BroadcastReceiver {
    private final String TAG = NetworkStateReceiver.this.getClass().getSimpleName();
    private static final String IS_PROCESSING = "isProcessing";
    private static final String LAST_TIME_MILLIS = "lastTimeMillis";

    private static ReadWriteLock lock = new ReentrantReadWriteLock();

    public NetworkStateReceiver() {
        super();
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        Lock readLock = lock.readLock();
        Lock writeLock = lock.writeLock();

        if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            boolean connected = info.isConnected();

            Log.i(TAG, "Connection status: " + connected);

            if (connected) {

                readLock.lock();
                boolean isProcessing = Storage.getBoolean(context, IS_PROCESSING);
                long lastTimeMillis = Storage.getLong(context, LAST_TIME_MILLIS);
                readLock.unlock();

                boolean shouldAlert = System.currentTimeMillis() - lastTimeMillis > 9 * 3600000 ? true : false;

                if (!isProcessing && shouldAlert && writeLock.tryLock()) {
                    Storage.putBoolean(context, IS_PROCESSING, true);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {}

                    Discover sonosDiscovery = new Discover();
                    String[] speakers = sonosDiscovery.getList();

                    if (speakers.length != 0)
                    {
                        Sonos speaker = new Sonos(speakers[0]);
                        speaker.setTransportURI("x-rincon-mp3radio://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3");
                        speaker.play();
                        Storage.putLong(context, LAST_TIME_MILLIS, System.currentTimeMillis());
                    }
                    Storage.putBoolean(context, IS_PROCESSING, false);
                    writeLock.unlock();
                }
            }
        }
    }
}
