package rxlocation.github.pitt.location;

import android.content.Context;
import android.support.annotation.NonNull;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * 百度RxLocationManager的实现类.
 * Created by pitt on 2017/3/16.
 */
public final class BaiduRxLocationManager implements RxLocationManager<BDLocation, LocationClient> {
    private static volatile BaiduRxLocationManager sINSTANCE;

    private volatile LocationClient mBdlocationClient;


    private BaiduRxLocationManager(final Context context) {
        mBdlocationClient = new LocationClient(context.getApplicationContext());
    }

    public static BaiduRxLocationManager getInstance() {
        if (sINSTANCE == null) {
            throw new IllegalArgumentException("BaiduRxLocationManager was not initialized!");
        }
        return sINSTANCE;
    }

    public static void initialize(final Context context) {
        synchronized (BaiduRxLocationManager.class) {
            if (sINSTANCE == null) {
                sINSTANCE = new BaiduRxLocationManager(context);
            }
        }
    }

    @Override
    public Observable<BDLocation> getLastLocation() {
        return Observable.just(mBdlocationClient.getLastKnownLocation());
    }

    @Override
    public Observable<BDLocation> requestLocation() {
        final RxLocationListener rxLocationListener = new RxLocationListener(mBdlocationClient);
        return Observable.unsafeCreate(rxLocationListener);
    }

    @Override
    public LocationClient currentClient() {
        return mBdlocationClient;
    }

    @Override
    public void setOption(final @NonNull ClientOption option) {
        if (option == null) {
            throw new IllegalArgumentException("Option can't be null.");
        }
        mBdlocationClient.setLocOption(option.getBaiduOption());
    }

    @Override
    public Observable<BDLocation> requestLocationByOption(final @NonNull ClientOption option) {
        setOption(option);
        return requestLocation();
    }

    @Override
    public String getVersion() {
        return mBdlocationClient.getVersion();
    }

    @Override
    public LocationClient getClient() {
        return mBdlocationClient;
    }

    @SuppressWarnings("PMD.NullAssignment")
    @Override
    public void shutDown() {
        synchronized (BaiduRxLocationManager.class) {
            if (mBdlocationClient != null) {
                if (mBdlocationClient.isStarted()) {
                    mBdlocationClient.stop();
                }
                mBdlocationClient = null;
            }
            if (null != sINSTANCE) {
                sINSTANCE = null;
            }
        }
    }

    final class RxLocationListener implements Observable.OnSubscribe<BDLocation> {
        private final LocationClient mLocationClient;
        private BDLocationListener mListener;

        RxLocationListener(final LocationClient locationClient) {
            mLocationClient = locationClient;
        }

        @Override
        public void call(final Subscriber<? super BDLocation> subscriber) {
            mListener = new BDLocationListener() {
                @Override
                public void onReceiveLocation(final BDLocation bdLocation) {
                    subscriber.onNext(bdLocation);
                }

                @Override
                public void onConnectHotSpotMessage(final String s, final int i) {

                }
            };
            mLocationClient.registerLocationListener(mListener);
            synchronized (RxLocationListener.class) {
                mLocationClient.start();
            }
            subscriber.add(new Subscription() {
                @Override
                public void unsubscribe() {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.unsubscribe();
                    }
                    removeListener();
                }

                @Override
                public boolean isUnsubscribed() {
                    return subscriber.isUnsubscribed();
                }
            });
        }

        private void removeListener() {
            if (mListener != null) {
                mLocationClient.registerLocationListener(mListener);
                synchronized (RxLocationListener.class) {
                    mLocationClient.stop();
                }
            }
        }
    }


}
