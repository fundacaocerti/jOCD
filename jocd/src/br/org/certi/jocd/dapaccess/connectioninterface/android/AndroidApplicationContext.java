package br.org.certi.jocd.dapaccess.connectioninterface.android;

import android.content.Context;

public class AndroidApplicationContext {

  private static AndroidApplicationContext instance = new AndroidApplicationContext();

  private Context applicationContext;

  private Context getContext() {
    return applicationContext;
  }

  public void init(Context context) {
    if (applicationContext == null) {
      applicationContext = context;
    }
  }

  public static AndroidApplicationContext getInstance() {
    return instance == null ? (instance = new AndroidApplicationContext()) : instance;
  }

  public static Context get() {
    return getInstance().getContext();
  }
}
