#!/usr/bin/env bash
set -eu
mkdir -p /tmp/snapshot/classes /tmp/snapshot/dex
cat > /tmp/snapshot/AndroidManifest.xml <<'MANIFEST'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="org.dotwel.snapshot"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="28"/><application android:label="Snapshot test helper" android:debuggable="true"/><instrumentation android:name=".Snapshot" android:targetPackage="org.dotwel.snapshot"/></manifest>
MANIFEST
cat > /tmp/snapshot/Snapshot.java <<'JAVA'
package org.dotwel.snapshot;
import android.app.Instrumentation;
import android.os.Bundle;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
public class Snapshot extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 static String esc(CharSequence s){return s==null?"":s.toString().replace("&","&amp;").replace("<","&lt;").replace("\"","&quot;").replace("\n","&#10;");}
 void node(AccessibilityNodeInfo n,StringBuilder s){if(n==null || !n.isVisibleToUser())return;Rect r=new Rect();n.getBoundsInScreen(r);if(r.width()<=0 || r.height()<=0)return;s.append("<node text=\"").append(esc(n.getText())).append("\" content-desc=\"").append(esc(n.getContentDescription())).append("\" resource-id=\"").append(esc(n.getViewIdResourceName())).append("\" package=\"").append(esc(n.getPackageName())).append("\" clickable=\"").append(n.isClickable()).append("\" bounds=\"[").append(r.left).append(',').append(r.top).append("][").append(r.right).append(',').append(r.bottom).append("]\">");for(int i=0;i<n.getChildCount();i++)node(n.getChild(i),s);s.append("</node>");}
 public void onStart(){Bundle out=new Bundle();try{android.app.UiAutomation u=getUiAutomation();android.accessibilityservice.AccessibilityServiceInfo info=u.getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;u.setServiceInfo(info);AccessibilityNodeInfo root=null;for(int i=0;i<20 && root==null;i++){root=u.getRootInActiveWindow();if(root==null)Thread.sleep(100);}StringBuilder s=new StringBuilder("<hierarchy>");node(root,s);s.append("</hierarchy>");out.putString("xml",s.toString());finish(-1,out);}catch(Exception e){out.putString("error",e.toString());finish(1,out);}}
}
JAVA
SDK="$ANDROID_HOME"
ANDROID_JAR=$(find "$SDK/platforms" -name android.jar | sort -V | tail -1)
BT=$(find "$SDK/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/snapshot/classes /tmp/snapshot/Snapshot.java
"$BT/d8" --lib "$ANDROID_JAR" --output /tmp/snapshot/dex /tmp/snapshot/classes/org/dotwel/snapshot/Snapshot.class
"$BT/aapt2" link -o /tmp/snapshot/helper.apk --manifest /tmp/snapshot/AndroidManifest.xml -I "$ANDROID_JAR"
(cd /tmp/snapshot/dex && zip -q /tmp/snapshot/helper.apk classes.dex)
keytool -genkeypair -keystore /tmp/snapshot/debug.keystore -storepass android -keypass android -alias androiddebugkey -dname CN=Android -keyalg RSA -validity 1
"$BT/apksigner" sign --ks /tmp/snapshot/debug.keystore --ks-pass pass:android /tmp/snapshot/helper.apk
