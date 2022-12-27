package com.android.sherlock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import hb.sherlock.R;

/**
 * 隐私合规结果页面
 */
public class PrivacyCheckResultActivity extends AppCompatActivity {

    private List<Object> mList;
    private RecyclerView mRecyclerView;

    public static void launch(Context context, String cacheDir) {
        Intent intent = new Intent();
        intent.setClassName("hb.sherlock", PrivacyCheckResultActivity.class.getName());
        intent.putExtra("cacheDir", cacheDir);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_check_result);
        getSupportActionBar().hide();

        mRecyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layout = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        layout.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(layout);
        mRecyclerView.setAdapter(new RecyclerView.Adapter<ActionViewHolder>() {
            @NonNull
            @Override
            public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ActionViewHolder(parent);
            }

            @Override
            public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
                holder.onBindView((JSONObject) mList.get(position));
            }

            @Override
            public int getItemCount() {
                return mList == null ? 0 : mList.size();
            }
        });

        findViewById(R.id.tvRefresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });

        loadData();
    }


    private void loadData() {
        String cacheDir = getIntent().getStringExtra("cacheDir");
        JSONArray actionArray = ACache.get(new File(cacheDir)).getAsJSONArray("AppPrivacyAction");

        // 如果数据为空，则会添加测试数据
        actionArray = addTestData(actionArray);

        try {
            if (actionArray != null) {
                Field valuesField = JSONArray.class.getDeclaredField("values");
                valuesField.setAccessible(true);
                List<Object> list = (List<Object>) valuesField.get(actionArray);

                mList = list;
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 隐私行为ViewHolder
     */
    public static class ActionViewHolder extends RecyclerView.ViewHolder {

        private TextView tvTime;
        private TextView tvType;
        private TextView tvTypeDesc;
        private TextView tvContent;
        private TextView tvExpansion;
        private boolean mIsExpansion = false;

        public static final int MAX_CONTENT_LINES = 8;

        public ActionViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.privacy_check_result_item, parent, false));
            tvTime = itemView.findViewById(R.id.tvTime);
            tvType = itemView.findViewById(R.id.tvType);
            tvTypeDesc = itemView.findViewById(R.id.tvTypeDesc);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvExpansion = itemView.findViewById(R.id.tvExpansion);
            tvContent.getViewTreeObserver().addOnGlobalLayoutListener(new OnTvGlobalLayoutListener(tvContent));
            tvContent.setMaxLines(MAX_CONTENT_LINES);
        }

        public void onBindView(JSONObject item) {
            long time = item.optLong("time");
            String type = item.optString("type");
            String actionType = item.optString("action_type");
            String actionStack = item.optString("action_stack");

            tvTime.setText(format(time));
            tvType.setText(type);
            tvContent.setText(actionStack);
            if ("default".equalsIgnoreCase(type)) {
                tvType.setTextColor(0x5b000000);
            } else {
                tvType.setTextColor(0xffff4456);
            }
            if ("default".equalsIgnoreCase(type)) {
                tvTypeDesc.setText("调用行为记录");
            }
            else if ("limit".equalsIgnoreCase(type)) {
                tvTypeDesc.setText("1秒内调用多次");
            }
            else if ("no_phone".equalsIgnoreCase(type)) {
                tvTypeDesc.setText("没有【电话】权限");
            }
            else if ("no_storage".equalsIgnoreCase(type)) {
                tvTypeDesc.setText("没有【存储】权限");
            } else {
                tvTypeDesc.setText("未知");
            }

            mIsExpansion = false;
            if (!mIsExpansion) {
                tvExpansion.setText("全部");
            }
            else {
                tvExpansion.setText("收起");
            }
            tvExpansion.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mIsExpansion = !mIsExpansion;
                    if (!mIsExpansion) {
                        tvExpansion.setText("全部");
                        tvContent.setMaxLines(MAX_CONTENT_LINES);
                    }
                    else {
                        tvExpansion.setText("收起");
                        tvContent.setMaxLines(Integer.MAX_VALUE);
                    }

                }
            });
        }
    }

    private static String format(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String strDate = sdf.format(new Date(time));
        return strDate;
    }


    /**
     * TextView设置内容后加上此监听 实现自动排版
     */
    public static class OnTvGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        private TextView tv;

        public OnTvGlobalLayoutListener(TextView tv) {
            this.tv = tv;
        }

        @Override
        public void onGlobalLayout() {
            tv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            final String newText = autoSplitText(tv);
            if (!TextUtils.isEmpty(newText)) {
                tv.setText(newText);
            }
        }

        /**
         * 针对TextView文字排版问题 进行自动排版
         *
         * @param tv
         * @return
         */
        private String autoSplitText(final TextView tv) {
            final String rawText = tv.getText().toString(); //原始文本
            final Paint tvPaint = tv.getPaint(); //paint，包含字体等信息
            final float tvWidth = tv.getWidth() - tv.getPaddingLeft() - tv.getPaddingRight(); //控件可用宽度

            //将原始文本按行拆分
            String[] rawTextLines = rawText.replaceAll("\r", "").split("\n");
            StringBuilder sbNewText = new StringBuilder();
            float max = 0;
            for (String rawTextLine : rawTextLines) {
                float currentTextWidth = tvPaint.measureText(rawTextLine);
                max = Math.max(max, currentTextWidth);
                //if (true) {
                //    continue;
                //}
                if (currentTextWidth <= tvWidth) {
                    //如果整行宽度在控件可用宽度之内，就不处理了
                    sbNewText.append(rawTextLine);
                } else {
                    //如果整行宽度超过控件可用宽度，则按字符测量，在超过可用宽度的前一个字符处手动换行
                    float lineWidth = 0;
                    for (int cnt = 0; cnt != rawTextLine.length(); ++cnt) {
                        char ch = rawTextLine.charAt(cnt);
                        lineWidth += tvPaint.measureText(String.valueOf(ch));
                        if (lineWidth <= tvWidth) {
                            sbNewText.append(ch);
                        } else {
                            sbNewText.append("\n");
                            lineWidth = 0;
                            --cnt;
                        }
                    }
                }
                sbNewText.append("\n");
            }
            //tv.getLayoutParams().width = (int) max;

            //把结尾多余的\n去掉
            if (!rawText.endsWith("\n")) {
                sbNewText.deleteCharAt(sbNewText.length() - 1);
            }

            return sbNewText.toString();
        }
    }

    @NonNull
    private static JSONArray addTestData(JSONArray actionArray) {
        if (actionArray == null) {
            actionArray = new JSONArray();
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.putOpt("time", System.currentTimeMillis());
                jsonObject.putOpt("type", "测试数据");
                jsonObject.putOpt("action_type", "XXX");
                jsonObject.putOpt("action_stack", "调用XXX获取XXX：\n" +
                        "...\n" +
                        "具体堆栈信息\n" +
                        "...\n" +
                        "com.lody.virtual.client.hook.delegate.InstrumentationDelegate.callActivityOnCreate(InstrumentationDelegate.java:244)\n" +
                        "com.lody.virtual.client.hook.delegate.AppInstrumentation.callActivityOnCreate(AppInstrumentation.java:99)\n" +
                        "android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2467)\n" +
                        "android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2574)\n" +
                        "android.app.ActivityThread.access$1100(ActivityThread.java:153)\n" +
                        "android.app.ActivityThread$H.handleMessage(ActivityThread.java:1425)\n" +
                        "android.os.Handler.dispatchMessage(Handler.java:102)\n" +
                        "android.os.Looper.loop(Looper.java:157)\n" +
                        "android.app.ActivityThread.main(ActivityThread.java:5684)\n" +
                        "java.lang.reflect.Method.invoke(Native Method)\n" +
                        "com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:746)\n" +
                        "com.android.internal.os.ZygoteInit.main(ZygoteInit.java:636)\n" +
                        "package:hb.devtools\n" +
                        "pid:14365\n" +
                        "thread id:1-main\n" +
                        "result:[PackageInfo{be9a1aa hb.xdevtools.app}, PackageInfo{537d19b de.robv.android.xposed.installer}, PackageInfo{383a938 com.android.sherlock}, PackageInfo{5ee4d11 hb.common.app}, PackageInfo{e182e76 hb.antirisk.app}, PackageInfo{3b7ec77 eu.faircode.xlua}, PackageInfo{a6620e4 com.amaze.filemanager}, PackageInfo{fd9a94d hb.xlocation.app}, PackageInfo{fd0bc02 com.xingjiabi.shengsheng}]");
                actionArray.put(jsonObject);

                jsonObject = new JSONObject();
                jsonObject.putOpt("time", System.currentTimeMillis());
                jsonObject.putOpt("type", "NO_PHONE");
                jsonObject.putOpt("action_type", "XXX");
                jsonObject.putOpt("action_stack", "调用XXX获取XXX：\n" +
                        "...\n" +
                        "具体堆栈信息\n" +
                        "...\n" +
                        "com.lody.virtual.client.hook.delegate.InstrumentationDelegate.callActivityOnCreate(InstrumentationDelegate.java:244)\n" +
                        "com.lody.virtual.client.hook.delegate.AppInstrumentation.callActivityOnCreate(AppInstrumentation.java:99)\n" +
                        "android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2467)\n" +
                        "android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2574)\n" +
                        "android.app.ActivityThread.access$1100(ActivityThread.java:153)\n" +
                        "android.app.ActivityThread$H.handleMessage(ActivityThread.java:1425)\n" +
                        "android.os.Handler.dispatchMessage(Handler.java:102)\n" +
                        "android.os.Looper.loop(Looper.java:157)\n" +
                        "android.app.ActivityThread.main(ActivityThread.java:5684)\n" +
                        "java.lang.reflect.Method.invoke(Native Method)\n" +
                        "com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:746)\n" +
                        "com.android.internal.os.ZygoteInit.main(ZygoteInit.java:636)\n" +
                        "package:hb.devtools\n" +
                        "pid:14365\n" +
                        "thread id:1-main\n" +
                        "result:[PackageInfo{be9a1aa hb.xdevtools.app}, PackageInfo{537d19b de.robv.android.xposed.installer}, PackageInfo{383a938 com.android.sherlock}, PackageInfo{5ee4d11 hb.common.app}, PackageInfo{e182e76 hb.antirisk.app}, PackageInfo{3b7ec77 eu.faircode.xlua}, PackageInfo{a6620e4 com.amaze.filemanager}, PackageInfo{fd9a94d hb.xlocation.app}, PackageInfo{fd0bc02 com.xingjiabi.shengsheng}]");
                actionArray.put(jsonObject);

                jsonObject = new JSONObject();
                jsonObject.putOpt("time", System.currentTimeMillis());
                jsonObject.putOpt("type", "NO_STORAGE");
                jsonObject.putOpt("action_type", "XXX");
                jsonObject.putOpt("action_stack", "调用XXX获取XXX：\n" +
                        "...\n" +
                        "具体堆栈信息\n" +
                        "...\n" +
                        "com.lody.virtual.client.hook.delegate.InstrumentationDelegate.callActivityOnCreate(InstrumentationDelegate.java:244)\n" +
                        "com.lody.virtual.client.hook.delegate.AppInstrumentation.callActivityOnCreate(AppInstrumentation.java:99)\n" +
                        "android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2467)\n" +
                        "android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2574)\n" +
                        "android.app.ActivityThread.access$1100(ActivityThread.java:153)\n" +
                        "android.app.ActivityThread$H.handleMessage(ActivityThread.java:1425)\n" +
                        "android.os.Handler.dispatchMessage(Handler.java:102)\n" +
                        "android.os.Looper.loop(Looper.java:157)\n" +
                        "android.app.ActivityThread.main(ActivityThread.java:5684)\n" +
                        "java.lang.reflect.Method.invoke(Native Method)\n" +
                        "com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:746)\n" +
                        "com.android.internal.os.ZygoteInit.main(ZygoteInit.java:636)\n" +
                        "package:hb.devtools\n" +
                        "pid:14365\n" +
                        "thread id:1-main\n" +
                        "result:[PackageInfo{be9a1aa hb.xdevtools.app}, PackageInfo{537d19b de.robv.android.xposed.installer}, PackageInfo{383a938 com.android.sherlock}, PackageInfo{5ee4d11 hb.common.app}, PackageInfo{e182e76 hb.antirisk.app}, PackageInfo{3b7ec77 eu.faircode.xlua}, PackageInfo{a6620e4 com.amaze.filemanager}, PackageInfo{fd9a94d hb.xlocation.app}, PackageInfo{fd0bc02 com.xingjiabi.shengsheng}]");
                actionArray.put(jsonObject);

                jsonObject = new JSONObject();
                jsonObject.putOpt("time", System.currentTimeMillis());
                jsonObject.putOpt("type", "LIMIT");
                jsonObject.putOpt("action_type", "XXX");
                jsonObject.putOpt("action_stack", "调用XXX获取XXX：\n" +
                        "...\n" +
                        "具体堆栈信息\n" +
                        "...\n" +
                        "com.lody.virtual.client.hook.delegate.InstrumentationDelegate.callActivityOnCreate(InstrumentationDelegate.java:244)\n" +
                        "com.lody.virtual.client.hook.delegate.AppInstrumentation.callActivityOnCreate(AppInstrumentation.java:99)\n" +
                        "android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2467)\n" +
                        "android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2574)\n" +
                        "android.app.ActivityThread.access$1100(ActivityThread.java:153)\n" +
                        "android.app.ActivityThread$H.handleMessage(ActivityThread.java:1425)\n" +
                        "android.os.Handler.dispatchMessage(Handler.java:102)\n" +
                        "android.os.Looper.loop(Looper.java:157)\n" +
                        "android.app.ActivityThread.main(ActivityThread.java:5684)\n" +
                        "java.lang.reflect.Method.invoke(Native Method)\n" +
                        "com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:746)\n" +
                        "com.android.internal.os.ZygoteInit.main(ZygoteInit.java:636)\n" +
                        "package:hb.devtools\n" +
                        "pid:14365\n" +
                        "thread id:1-main\n" +
                        "result:[PackageInfo{be9a1aa hb.xdevtools.app}, PackageInfo{537d19b de.robv.android.xposed.installer}, PackageInfo{383a938 com.android.sherlock}, PackageInfo{5ee4d11 hb.common.app}, PackageInfo{e182e76 hb.antirisk.app}, PackageInfo{3b7ec77 eu.faircode.xlua}, PackageInfo{a6620e4 com.amaze.filemanager}, PackageInfo{fd9a94d hb.xlocation.app}, PackageInfo{fd0bc02 com.xingjiabi.shengsheng}]");
                actionArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return actionArray;
    }
}