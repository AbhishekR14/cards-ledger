package in.local.cards;

import android.content.Context;
import android.content.pm.PackageManager;

/** How alerts reach the app: new incoming SMS, under the RECEIVE_SMS permission. */
public final class CaptureMode {
 private CaptureMode(){}
 /** Stored as {@code captureEdition}; capture state saved under any other value is reset. */
 public static final String ID="sms";
 public static boolean access(Context context){return context.checkSelfPermission("android.permission.RECEIVE_SMS")==PackageManager.PERMISSION_GRANTED;}
}
