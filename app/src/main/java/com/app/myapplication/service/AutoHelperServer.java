package com.dt.haoyuntong.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.dt.haoyuntong.util.SharedPreferencesHelper;
import com.dt.haoyuntong.util.ThreadUtil;
import com.dt.haoyuntong.util.ViewUtil;
import com.longrenzhu.base.toast.ToastUtils;

import java.util.List;

public class AutoHelperServer extends AccessibilityService {
   public static final String TAG = "zyb";
   public static final String wechatPackageName = "com.tencent.mm";
   public static final String systemuiPackageName = "com.android.systemui";
   private boolean startDelay;
   private long lastExecTimeStamp;
   private static final String INVISIBLE_CHARACTERS_REGEX = "[\\u200B\\u200C\\u200D\\u200E\\u200F\\uFEFF\\p{Cntrl}&&[^\\t\\n\\r\\f]]";


   @Override
   protected void onServiceConnected() {
      super.onServiceConnected();
      Log.i(TAG, "====建立服务链接====");
      ToastUtils.show("自动接听服务启动成功");
   }

   @Override
   public void onAccessibilityEvent(AccessibilityEvent event) {
      String[] names = new String[0];
      int eventType = event.getEventType();
      CharSequence packageName = event.getPackageName();
      AccessibilityNodeInfo source = event.getSource();

      Log.d(TAG, "eventType: " + eventType + "\t PackageName:" + event.getPackageName() + "\t source == null:" + (source==null));

      if ((eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)) {
         try {
            handWindow(event, packageName);
         }catch (Exception e){
            ViewUtil.toast("handWindow:" + e.getMessage());
         }

      } else if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
         try {
            //处理通知
            handNotification(event, eventType);
         }catch (Exception e){
            ViewUtil.toast("handNotification:" + e.getMessage());
         }

      }



   }

   private void handWindow(AccessibilityEvent event, CharSequence packageName) {
      String[] names;
      if (!wechatPackageName.equals(packageName)) {
         return;
      }
      if (event.getSource() == null) {
         return;
      }

      names = SharedPreferencesHelper.getArray(ThreadUtil.context);

      if (this.startDelay && System.currentTimeMillis() - this.lastExecTimeStamp < 300L) {
         Log.d(TAG, "1秒内可以多次点击 : " + packageName);
         return;
      }
      this.startDelay = false;
      this.lastExecTimeStamp = System.currentTimeMillis();
      autoConnectWeChatCall(event,names);
   }

   private void handNotification(AccessibilityEvent event, int eventType) throws Exception {
      Log.d(TAG+"1", "eventType: " + eventType + "\t PackageName:" + event.getPackageName());
      if (!wechatPackageName.equals(event.getPackageName()) && !systemuiPackageName.equals(event.getPackageName())) {
         return;
      }

      if (event.getParcelableData() == null) {
         return;
      }

      if (!(event.getParcelableData() instanceof Notification)) {
         return;
      }

      Notification notification = (Notification) event.getParcelableData();
      PendingIntent pendingIntent1 = notification.contentIntent;
      Log.d(TAG+"1", "pendingIntent..." + pendingIntent1);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
         String title = notification.extras.getString(Notification.EXTRA_TITLE);
         String content = notification.extras.getString(Notification.EXTRA_TEXT);
         Log.d(TAG+"1", "title..." + title + "\tcontent :" + content);
            if (pendingIntent1 != null) {
               Log.d(TAG+"1", pendingIntent1.toString());
               pendingIntent1.send();
            }
            // 模拟点击
            mockClickByPointOnScreen();

      } else {
         Log.d(TAG, "版本比较低..." + pendingIntent1);
      }


   }


   private void autoConnectWeChatCall(AccessibilityEvent event, String[] names) {
      AccessibilityNodeInfo source = event.getSource();
      if (source != null) {
         //微信通知栏上拒绝、接听页面
         List<AccessibilityNodeInfo> accessibilityNodeInfos = source.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/kg6");
         Log.d(TAG, "autoConnectWeChatCall..." + accessibilityNodeInfos.size() + "\t ");
         if (accessibilityNodeInfos.size() >= 1 && accessibilityNodeInfos.get(0).getChildCount() == 0) {
            tap(this, accessibilityNodeInfos.get(0));
         }
      }
//      Log.d(TAG, "event.getClassName() == null:" + (event.getClassName() == null) + "this.getRootInActiveWindow() != null:" + (this.getRootInActiveWindow() != null)) ;

      if (this.getRootInActiveWindow() == null) {
         return;
      }
         Log.d(TAG, "微信窗口自动变化");
//         List<AccessibilityNodeInfo> crdList = this.getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/crd");
         List<AccessibilityNodeInfo> crdList = this.getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/nsv");

         if (crdList == null || crdList.size() == 0) {
            return;
         }
         AccessibilityNodeInfo nodeInfo = crdList.get(0); //crd
         AccessibilityNodeInfo answerButton = getAccessibilityNodeInfo(nodeInfo);
         if (answerButton != null) {
//            pressAnserButton_Option1(answerButton,names);
            pressAnserButton_Option2(answerButton,names);
            return;
         }

   }

   private static AccessibilityNodeInfo getAccessibilityNodeInfo(AccessibilityNodeInfo rootNode) {
      if (rootNode == null) {
         return null;
      }
      int childCount = rootNode.getChildCount();
      if (childCount >= 7) {
           return rootNode;
      }else {
         for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = rootNode.getChild(i);
            AccessibilityNodeInfo result = getAccessibilityNodeInfo(child);
            if (result != null) {
               return result;
            }
         }
      }
      return null;
   }

   private void pressAnserButton_Option1(AccessibilityNodeInfo var4, String[] names) {
      int type = 1;
      if (var4 != null && var4.getChildCount() >= 7) {
         CharSequence var13 = var4.getChild(0).getContentDescription();
         if (var13 == null) {
            var13 = (CharSequence) "";
         } else {
            Log.d(TAG, "getChild(0).contentDescription...");
         }

         String var13trim = var13.toString().trim();

         Log.d(TAG, "var13trim" + var13trim);

         String[] result = var13trim.split(" ");
         Log.d(TAG, "result length" + result.length);

         if (result.length >= 2) {
            String wechatName = result[0];
            String wechatType = result[1];
            Log.d(TAG, "wechatName" + wechatName + "\t wechatType" + wechatType);

            if (!isInList(names, wechatName)) {
              return;
            }

            if (!wechatType.contains("视频通话")) {
               if (wechatType.contains("语音通话")) {
                  type = 0;
               } else {
                  type = -1;
               }
            }
            if (type == -1) {
               Log.d(TAG, "wechatType 是-1 ");
            } else {
               Log.d(TAG, "接听微信电话和视频 ");
               tapConnectBtnOrSpreadBtn(var4, this);
            }
         }
      }
   }

   private static boolean isInList(String[] names, String wechatName) {
      boolean inList = false;
      for (String name : names) {
         if (name.equals(wechatName)) {
            inList = true;
            break;
         }
      }
      return inList;
   }

   private void pressAnserButton_Option2(AccessibilityNodeInfo var4, String[] names) {
      if (var4 != null && var4.getChildCount() < 7) {
         return;
      }
      CharSequence description = var4.getContentDescription();
      String wechatName = description == null?"":description.toString();
      wechatName = wechatName.replaceAll(INVISIBLE_CHARACTERS_REGEX, "");
      if (!isInList(names, wechatName)) {
            return;
      }
//      for (int i = 0; i < chinese.length(); i++) {
//         int codePoint = chinese.codePointAt(i);
//         System.out.printf("Character: %s, Unicode: %d%n", chinese.charAt(i), codePoint);
//      }
//      StringBuffer stringBuffer = new StringBuffer();
//      for (int i = 0; i < var4.getChildCount(); i++) {
//         AccessibilityNodeInfo child = var4.getChild(i);
//         stringBuffer.append(child.getContentDescription());
//      }
//      if (stringBuffer.indexOf("视频") > 0) {
//
//      }

      for (int i = 0; i < var4.getChildCount(); i++) {
         AccessibilityNodeInfo child = var4.getChild(i);
         CharSequence contentDescription = child.getContentDescription();
         contentDescription = contentDescription == null?"":contentDescription;
//         Log.d(TAG, contentDescription.toString());
         if ("接听接聽Accept".contains(contentDescription)) {
            if (!child.isVisibleToUser()) {
               return;
            }
            tap(this, child);
            this.startDelay = true;
         }
      }
   }

   private void tapConnectBtnOrSpreadBtn(AccessibilityNodeInfo var4_1, final AutoHelperServer AutoHelperServer) {
      final AccessibilityNodeInfo child = var4_1.getChild(var4_1.getChildCount() - 1);
      if (child != null) {
         CharSequence contentDescription = child.getContentDescription();
         String contentDescriptionTrim = "";
         if (contentDescription != null) {
            contentDescriptionTrim = contentDescription.toString().trim();
            Log.i(TAG, "contentDescriptionTrim:::" + contentDescriptionTrim);
         }

         if ("接听".equals(contentDescriptionTrim)) {
            Log.i(TAG, "接听:::");
            tap(AutoHelperServer, child);
            this.startDelay = true;
         } else if ("扬声器已关".equals(contentDescriptionTrim)) {
            Log.i(TAG, "开启扬声器:::");
            tap(AutoHelperServer, child);
            this.startDelay = true;
         }
      }
   }


   @Override
   public void onInterrupt() {
      Log.i(TAG, "====onInterrupt====");
   }


   //=======================================================

   public static final void tap(AccessibilityService var0, AccessibilityNodeInfo var1) {
//      ToastUtils.show("点击接听" + System.currentTimeMillis());
      if (var1 != null) {
         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Log.i(TAG, "开始  tap....." + Thread.currentThread().getName());
            Rect rect = new Rect();
            var1.getBoundsInScreen(rect);
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            Path path = new Path();
//            Log.i(TAG, "rect.centerX()：：" + rect.centerX() + "\t rect.centerY()::" + rect.centerY());
//            Log.i(TAG, "rect.left：：" + rect.left + "\t rect.right()::" + rect.right + "\t rect.top()::" + rect.top + "\t rect.bottom()::" + rect.bottom);
            path.moveTo(rect.centerX(), rect.centerY());
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0L, 200L));
            GestureDescription gestureDescription = gestureBuilder.build();
            var0.dispatchGesture(gestureDescription, new GestureResultCallback() {
               @Override
               public void onCompleted(GestureDescription gestureDescription) {
                  super.onCompleted(gestureDescription);
//                  Log.i(TAG, "onCompleted：：");
//                  ToastUtils.show("onCompleted：：");
               }

               @Override
               public void onCancelled(GestureDescription gestureDescription) {
                  super.onCancelled(gestureDescription);
//                  Log.i(TAG, "onCancelled：：");
//                  ToastUtils.show("onCancelled：：");

               }
            }, null);
         } else {
            Log.i(TAG, "收拾只能在7.0以上使用了");
         }
      } else {
         Log.i(TAG, "var1 是空的.....");
//         ToastUtils.show("var1 是空的.....");
      }
   }



   //模拟点击
   private void mockClickByPointOnScreen() {
      Log.d(TAG, "mockClickByPointOnScreen");

      for (int i = 0; i < 2; i++) {
       try {
         Thread.sleep(1000);
      }catch (Exception e){}
      }
         handNotiFication();

   }

   private void handNotiFication() {
      float x = 336;
      float y = 150;

      // 使用 GestureDescription 模拟点击
      Path path = new Path();
      path.moveTo(x, y);

      GestureDescription.Builder builder = new GestureDescription.Builder();
      builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
      dispatchGesture(builder.build(), null, null);
   }

   public class ZeroWidthCharacterRemover {

      // 正则表达式，匹配包括零宽字符在内的多种不可见字符
      // 注意：这里只列出了几个示例，可能需要根据实际需求调整
      private static final String INVISIBLE_CHARACTERS_REGEX = "[\\u200B\\u200C\\u200D\\u200E\\u200F\\uFEFF\\p{Cntrl}&&[^\\t\\n\\r\\f]]";

      /**
       * 去除字符串中的零宽字符和其他不可见字符
       *
       * @param input 输入的字符串
       * @return 去除不可见字符后的字符串
       */
      public String removeInvisibleCharacters(String input) {
         return input.replaceAll(INVISIBLE_CHARACTERS_REGEX, "");
      }

//      public static void main(String[] args) {
//         String textWithInvisibleChars = "这是一段文本\u200B包含\uFEFF零宽字符\u200D和\u200E其他不可见字符\u200F。";
//         String cleanedText = removeInvisibleCharacters(textWithInvisibleChars);
//         System.out.println("原始文本: \"" + textWithInvisibleChars + "\"");
//         System.out.println("清理后的文本: \"" + cleanedText + "\"");
//      }
   }


}

