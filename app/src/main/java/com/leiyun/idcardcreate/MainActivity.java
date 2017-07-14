package com.leiyun.idcardcreate;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 类名：MainActivity
 * 描述：
 * 创建人：Yun.Lei on 2017/7/12
 * 修改人：
 * 修改时间：
 * 修改备注：
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView province;
    private TextView city;
    private TextView district;
    private TextView dateTv;
    private TextView idCard;
    private Button btnCreate;
    private Button btnCheck;
    private View rootView;
    private TextInputEditText editText;

    private List<String> provinceList;
    private String provinceName;
    private String cityName;
    private List<AreaCode> list;
    private String dateStr;
    private String districtName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootView = findViewById(R.id.root_view);
        province = (TextView) findViewById(R.id.province);
        city = (TextView) findViewById(R.id.city);
        district = (TextView) findViewById(R.id.district);
        dateTv = (TextView) findViewById(R.id.date);
        idCard = (TextView) findViewById(R.id.id_card);
        btnCreate = (Button) findViewById(R.id.btn_create);
        btnCheck = (Button) findViewById(R.id.btn_check);
        editText = (TextInputEditText) findViewById(R.id.id_card_input);
        findViewById(R.id.btn_copy).setOnClickListener(this);
        province.setOnClickListener(this);
        city.setOnClickListener(this);
        district.setOnClickListener(this);
        dateTv.setOnClickListener(this);
        btnCreate.setOnClickListener(this);
        btnCheck.setOnClickListener(this);
        provinceList = new ArrayList<>(IDCardUtil.zoneNum.values());
        String json = getFromAssets(this, "areaCode");
        AreaCode[] areaCodeArr = new Gson().fromJson(json, AreaCode[].class);
        list = Arrays.asList(areaCodeArr);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.province:
                showProvinceDialog();
                break;
            case R.id.city:
                if (!TextUtils.isEmpty(provinceName)) {
                    showCityDialog(getCityList(provinceName));
                } else {
                    showTips("请选择province");
                }
                break;
            case R.id.district:
                if (!TextUtils.isEmpty(cityName)) {
                    showDistrictDialog(getDetailList(cityName));
                } else {
                    showTips("请选择City");
                }
                break;
            case R.id.date:
                showDateDialog();
                break;
            case R.id.btn_create:
                if (TextUtils.isEmpty(districtName) || TextUtils.isEmpty(dateStr)) {
                    showTips("请先完善信息");
                } else {
                    String areaCode = getAreaCode(districtName);
                    String randomStr = String.valueOf((int) (Math.random() * 900 + 100));  //15~16位是判断同一地区出生的不同小孩，第17位是判断性别不是强校验，随机生成即可
                    idCard.setText(String.format("%s%s%s%s", areaCode, dateStr, randomStr, getParityBit(areaCode + dateStr + randomStr)));
                }
                break;
            case R.id.btn_check:
                String idCard = editText.getText().toString();
                if (!TextUtils.isEmpty(idCard)) {
                    boolean isIDCard = IDCardUtil.isIDCard(idCard);
                    editText.setError(String.valueOf(isIDCard));
                } else {
                    showTips("请输入需要校验的身份证");
                }
                break;
            case R.id.btn_copy:
                if (!TextUtils.isEmpty(this.idCard.getText())) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText(null, this.idCard.getText()));
                    showTips("已复制到剪切板");
                } else {
                    showTips("请先生成身份证号");
                }
                break;
        }
    }

    /**
     * 根据前17位号码生成最后一位校验码
     *
     * @param cardCode17 身份证前17位
     * @return
     */
    private char getParityBit(String cardCode17) {
        final char[] cs = cardCode17.toUpperCase().toCharArray();
        int power = 0;
        for (int i = 0; i < cs.length; i++) {
            power += (cs[i] - '0') * IDCardUtil.POWER_LIST[i];
        }
        char keyChar = IDCardUtil.PARITYBIT[power % 11];
        return keyChar;
    }

    private String getAreaCode(String districtName) {
        for (AreaCode areaCode : list) {
            if (districtName.equals(areaCode.getDetail())) {
                return areaCode.getAreaCode();
            }
        }
        return "110100";
    }

    private void showDateDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_choose_date, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setView(view);
        final AlertDialog dialog = builder.create();
        MaterialCalendarView calendarView = (MaterialCalendarView) view.findViewById(R.id.calendarView);
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (selected) {
                    dialog.dismiss();
                    dateStr = new SimpleDateFormat("yyyyMMdd").format(date.getDate());
                    dateTv.setText(String.format("出生日期：%s", dateStr));
                }
            }
        });
        dialog.show();
    }

    private void showTips(String text) {
        final Snackbar bar = Snackbar.make(rootView, text, Snackbar.LENGTH_SHORT);
        bar.setAction("确定", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bar.dismiss();
            }
        });
        bar.show();
    }

    private void showProvinceDialog() {
        RecyclerView recyclerView = (RecyclerView) View.inflate(this, R.layout.layout_choose, null);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.set(0, 0, 0, 2);
            }
        });
        MyAdapter adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(recyclerView);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(MyAdapter adapter, String name) {
                province.setText(String.format("省/直辖市：%s", name));
                provinceName = name;
                city.setText("市/地区：");
                dialog.dismiss();
            }
        });
        adapter.setList(provinceList);
        dialog.show();
    }

    private List<String> getCityList(String provinceName) {
        Set<String> cityList = new HashSet<>();
        for (AreaCode areaCode : list) {
            if (provinceName.contains(areaCode.getProvince()) || areaCode.getProvince().contains(provinceName)) {
                if (!TextUtils.isEmpty(areaCode.getCity()))
                    cityList.add(areaCode.getCity());
            }
        }
        return new ArrayList<>(cityList);
    }

    private List<String> getDetailList(String city) {
        Set<String> districtList = new HashSet<>();
        for (AreaCode areaCode : list) {
            if (city != null && areaCode.getCity() != null) {
                if (city.contains(areaCode.getCity()) || areaCode.getCity().contains(city)) {
                    if (!TextUtils.isEmpty(areaCode.getDetail()))
                        districtList.add(areaCode.getDetail());
                }
            }
        }
        return new ArrayList<>(districtList);
    }

    private void showCityDialog(List<String> cityList) {
        RecyclerView recyclerView = (RecyclerView) View.inflate(this, R.layout.layout_choose, null);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.set(0, 0, 0, 2);
            }
        });
        MyAdapter adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setList(cityList);
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(recyclerView);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(MyAdapter adapter, String name) {
                city.setText(String.format("市/地区：%s", name));
                cityName = name;
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void showDistrictDialog(List<String> cityList) {
        RecyclerView recyclerView = (RecyclerView) View.inflate(this, R.layout.layout_choose, null);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.set(0, 0, 0, 2);
            }
        });
        MyAdapter adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setList(cityList);
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(recyclerView);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(MyAdapter adapter, String name) {
                district.setText(String.format("县/区：%s", name));
                districtName = name;
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private class MyAdapter extends RecyclerView.Adapter<ViewHolder> implements View.OnClickListener {

        private List<String> list;
        private OnItemClickListener onItemClickListener;

        public MyAdapter() {
            list = new ArrayList<>();
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }

        public void setList(List<String> list) {
            this.list.clear();
            this.list.addAll(list);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_choose, parent, false));
            viewHolder.textView.setOnClickListener(this);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.textView.setText(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.tv_name) {
                TextView name = (TextView) v;
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(this, name.getText().toString());
                }
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(MyAdapter adapter, String name);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.tv_name);
        }
    }

    /**
     * 从文件中读取json字符串
     *
     * @param context
     * @param fileName
     * @return
     */
    public static String getFromAssets(Context context, String fileName) {
        StringBuffer result = new StringBuffer();
        InputStreamReader inputReader = null;
        BufferedReader bufReader = null;
        try {
            inputReader = new InputStreamReader(context.getResources().getAssets().open(fileName), "UTF-8");
            bufReader = new BufferedReader(inputReader);
            String lineTxt = null;
            while ((lineTxt = bufReader.readLine()) != null)
                result.append(lineTxt);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputReader != null) {
                    inputReader.close();
                }
                if (bufReader != null) {
                    bufReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result.toString().trim();
    }
}
