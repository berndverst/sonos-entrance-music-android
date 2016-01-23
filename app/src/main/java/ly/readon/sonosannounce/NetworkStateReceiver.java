package ly.readon.sonosannounce;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
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
    private Lock readLock;
    private Lock writeLock;

    private Context ctx;

    public NetworkStateReceiver() {
        super();
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        ctx = context;
        readLock = lock.readLock();
        writeLock = lock.writeLock();

        if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            boolean connected = info.isConnected();

            if (connected) {
                readLock.lock();
                boolean isProcessing = Storage.getBoolean(ctx, IS_PROCESSING);
                long lastTimeMillis = Storage.getLong(ctx, LAST_TIME_MILLIS);
                readLock.unlock();

                boolean shouldAlert = true;
                if (lastTimeMillis != 0L) {
                    shouldAlert = System.currentTimeMillis() - lastTimeMillis > 9 * 3600000 ? true : false;
                }

                if (!isProcessing && shouldAlert && writeLock.tryLock()) {

                    new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected Void doInBackground(Void... params) {
                            Storage.putBoolean(ctx, IS_PROCESSING, true);

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {}

                            try {
                                // TODO(berndverst): Troubleshoot Sonos Speaker Discovery
                                // Returns: Null pointer exception during instruction 'monitor-enter v6'
                                // Discover sonosDiscovery = new Discover();
                                // String[] speakers = sonosDiscovery.getList();
                                String[] speakers = new String[1];
                                speakers[0] = "10.0.1.3";

                                if (speakers.length != 0) {
                                    Sonos speaker = new Sonos(speakers[0]);
                                    speaker.setTransportURI("x-rincon-mp3radio://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3");
                                    speaker.play();
                                    Storage.putLong(ctx, LAST_TIME_MILLIS, System.currentTimeMillis());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            };
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            Storage.putBoolean(ctx, IS_PROCESSING, false);
                            writeLock.unlock();
                            return;
                        }
                    }.execute();
                }
            }
        }
    }
}
