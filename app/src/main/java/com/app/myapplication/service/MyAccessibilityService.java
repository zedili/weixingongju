package com.dt.haoyuntong.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.dt.haoyuntong.Constants;
import com.dt.haoyuntong.util.ThreadUtil;
import com.dt.haoyuntong.util.ViewUtil;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MyAccessibilityService extends AccessibilityService {
    public static final String TAG = MyAccessibilityService.class.getSimpleName();

    public static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";

    public static final String WECHAT_APP_NAME = "微信";

    public static final String DOUYIN_PACKAGE_NAME = "com.ss.android.ugc.aweme";

    public static final String DOUYIN_APP_NAME = "抖音";

    private String currentAppPackage;

    private AtomicInteger whTryCount = new AtomicInteger();


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }
        Log.d(TAG, "onStartCommand, intent: " + intent);
        if (Constants.ACTION_WECHAT_START_VIDEO_CALL.equals(intent.getAction())) {
            startWechatCall(intent.getStringExtra(Constants.EXTRA_CONTACT_NAME), (stop) -> ViewUtil.click(findNodeByText("视频通话")));
        }
        if (Constants.ACTION_WECHAT_START_AUDIO_CALL.equals(intent.getAction())) {
            startWechatCall(intent.getStringExtra(Constants.EXTRA_CONTACT_NAME), (stop) -> ViewUtil.click(findNodeByText("语音通话")));
        }
        if (Constants.ACTION_OPEN_DOUYIN.equals(intent.getAction())) {
            openApp(DOUYIN_PACKAGE_NAME, DOUYIN_APP_NAME);
        }
        return START_STICKY;
    }

    private void openDouyin() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.ss.android.ugc.aweme");
        if (intent == null) {
            ViewUtil.toast("请先安装抖音");
        }
        startActivity(intent);
    }

    @Override
    public void onInterrupt() {
        // 可访问性服务被打断时的处理
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
//        String source = event.getSource() == null ? "NULL" : Integer.toHexString(event.getSource().hashCode());
//        Log.d(TAG, "onAccessibilityEvent() called with source=@" + source + ", event = [" + event + "]");
////        register(event);

        AccessibilityNodeInfo source = event.getSource();
        String sourceHex = source == null ? "NULL" : Integer.toHexString(source.hashCode());
        Log.d(TAG, "onAccessibilityEvent() called with source=@" + sourceHex + ", event = [" + event + "]");
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentAppPackage = event.getPackageName().toString();
        }
    }

    /**
     * 拨打微信视频/语音电话
     * @param contactName 联系人的昵称/备注名/微信id
     * @param lastStep 最后一步做什么，拨打语音/视频
     */
    private void startWechatCall(String contactName, Consumer<AtomicBoolean> lastStep) {
        if (!openApp(WECHAT_PACKAGE_NAME, WECHAT_APP_NAME)) {
            return;
        }
        whTryCount.set(0);
        goWechatHomePage(() -> {
            ViewUtil.actAsHuman(
                (stop) -> stop.set(!ViewUtil.click(findNodeByText("通讯录"))),
                (stop) -> stop.set(!ViewUtil.click(findSearchButton())),
                (stop) -> {
                    AccessibilityNodeInfo view = ViewUtil.findChild(getRootInActiveWindow(), (v) -> {
                        return v.getText() != null && v.getText().toString().equals("搜索")
                            && v.getClassName() != null && v.getClassName().toString().contains("EditText");
                    });
                    ViewUtil.sendText(view, contactName);
                },
                (stop) -> {
                    AccessibilityNodeInfo contact = findNodeByText(contactName);
                    if (contact == null) {
                        Toast.makeText(getApplicationContext(), "没有找到联系人'" + contactName + "'", Toast.LENGTH_LONG).show();
                        stop.set(true);
                    } else {
                        ViewUtil.click(contact);
                    }
                },
                (stop) -> ViewUtil.click(findMoreActionBtn()),
                (stop) -> {
                    AccessibilityNodeInfo addMember = findNodeByText("添加成员");
                    if (addMember == null) {
                        stop.set(true);
                        return;
                    }
                    AccessibilityNodeInfo contact = Optional.ofNullable(addMember.getParent())
                                                            .map(AccessibilityNodeInfo::getParent)
                                                            .map(p -> p.getChild(0))
                                                            .orElse(null);
                    stop.set(contact == null);
                    if (contact == null) {
                        stop.set(true);
                        return;
                    }
                    ViewUtil.click(contact);
                },
                (stop) -> ViewUtil.click(findNodeByText("音视频通话")),
                lastStep
            );
        }, true);
    }

    private AccessibilityNodeInfo findMoreActionBtn() {
        return ViewUtil.findChild(getRootInActiveWindow(), v -> v.isClickable() && isContentDescEquals(v, "更多信息"));
    }

    private AccessibilityNodeInfo findSearchButton() {
        return ViewUtil.findChild(getRootInActiveWindow(), v -> v.isClickable() && isContentDescEquals(v, "搜索"));
    }

    private void goWechatHomePage(Runnable callback, boolean skipCheckInWechat) {
        int i = whTryCount.incrementAndGet();
        ViewUtil.toast("whTryCount:" + whTryCount.get());
        if (i > 3){return;}
//        Log.d(TAG, "goWechatHomePage() called by: " + ThreadUtil.getCurrentStackTrace());
        ThreadUtil.runInBackground(() -> {
            try {
                if (findNodeByText("微信") != null && findNodeByText("通讯录") != null && findNodeByText("我") != null) {
                    Log.i(TAG, "goWechatHomePage: Done");
                    whTryCount.set(0);
                    callback.run();
                } else {
                    Log.i(TAG, "goWechatHomePage: Not home yet, will try again");
                    ThreadUtil.runInUI(() -> {
                        if (WECHAT_PACKAGE_NAME.equals(currentAppPackage)) {
                            performGlobalAction(GLOBAL_ACTION_BACK);
                            goWechatHomePage(callback, false);
                        } else {
                            if (skipCheckInWechat) {
                                openApp(getPackageName(), "好运通");
                                openApp(WECHAT_PACKAGE_NAME, WECHAT_APP_NAME);
                                performGlobalAction(GLOBAL_ACTION_BACK);
                                goWechatHomePage(callback, true);
                            }
                        }
                    });
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "goWechatHomePage: error" + e.getMessage());
            }
        }, 2000);
    }

    private AccessibilityNodeInfo findNodeById(String id, Predicate<AccessibilityNodeInfo> viewPredicate) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return null;
        }

        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(id);
        for (AccessibilityNodeInfo node : nodes) {
            if (viewPredicate.test(node)) {
                return node;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findNodeByText(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return null;
        }

        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        if (!nodes.isEmpty()) {
            return nodes.get(0);
        }
        return null;
    }

    private boolean isContentDescEquals(AccessibilityNodeInfo node, String text) {
        CharSequence contentDescription = node.getContentDescription();
        return contentDescription != null && text.contentEquals(contentDescription);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private boolean openApp(String packageName, String appName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            Log.d(TAG, appName + "未安装");
            ViewUtil.toast("请先安装" + appName);
            return false;
        } else {
            Log.d(TAG, "启动" + appName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
    }

//    private void tapConnectBtnOrSpreadBtn(AccessibilityNodeInfo var4, final MyAccessibilityService AutoHelperServer) {
//        final AccessibilityNodeInfo child = var4.getChild(var4.getChildCount() - 1);
//        if (child != null) {
//            CharSequence contentDescription = child.getContentDescription();
//            String contentDescriptionTrim = "";
//            if (contentDescription != null) {
//                contentDescriptionTrim = contentDescription.toString().trim();
//                Log.i(TAG, "contentDescriptionTrim:::" + contentDescriptionTrim);
//            }
//
//            if ("接听".equals(contentDescriptionTrim)) {
//                Log.i(TAG, "接听:::");
//                tap(AutoHelperServer, child);
//                this.startDelay = true;
//            } else if ("扬声器已关".equals(contentDescriptionTrim)) {
//                Log.i(TAG, "开启扬声器:::");
//                tap(AutoHelperServer, child);
//                this.startDelay = true;
//            }
//        }
//    }




    //=======================================================

    public static final void tap(AccessibilityService var0, AccessibilityNodeInfo var1) {
        if (var1 != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Log.i(TAG, "开始  tap....." + Thread.currentThread().getName());
                Rect rect = new Rect();
                var1.getBoundsInScreen(rect);
                GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
                Path path = new Path();
                Log.i(TAG, "rect.centerX()：：" + rect.centerX() + "\t rect.centerY()::" + rect.centerY());
                Log.i(TAG, "rect.left：：" + rect.left + "\t rect.right()::" + rect.right + "\t rect.top()::" + rect.top + "\t rect.bottom()::" + rect.bottom);
                path.moveTo(rect.centerX(), rect.centerY());
                gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0L, 200L));
                GestureDescription gestureDescription = gestureBuilder.build();
                var0.dispatchGesture(gestureDescription, new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        super.onCompleted(gestureDescription);
                        Log.i(TAG, "onCompleted：：");
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        super.onCancelled(gestureDescription);
                        Log.i(TAG, "onCancelled：：");
                    }
                }, null);
            } else {
                Log.i(TAG, "收拾只能在7.0以上使用了");
            }
        } else {
            Log.i(TAG, "var1 是空的.....");
        }
    }
}