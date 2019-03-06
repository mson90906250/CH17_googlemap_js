package tw.tcnr21.m1706;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import tw.tcnr21.m1706.providers.FriendsContentProvider;

public class M1706 extends AppCompatActivity implements LocationListener {
    private Spinner mSpnLocation;
    private static final String MAP_URL = "file:///android_asset/GoogleMap.html";
    // 自建的html檔名
    private WebView webView;
    private String Lat;
    private String Lon;
    private String jcontent;// 地名變數
    /*** GPS***/
    private LocationManager locationMgr;
    private String provider; // 提供資料
    private TextView txtOutput;
    int iSelect;
    private final String TAG = "oldpa=>";
    /*** Navigation **/
    private Button bNav;
    String[] sLocation;
    String Navon = "off";
    String Navstart = "24.172127,120.610313"; // 起始點
    String Navend = "24.144671,120.683981"; // 結束點

    // ========= SQL Database =========
    private static ContentResolver mContRes;
    private String[] MYCOLUMN = new String[]{"id", "name", "grp", "address"};
    // ========= Thread Hander =============
    private Handler mHandler = new Handler();
    private long timer = 10; // thread每幾秒run
    private long timerang = 5; // 設定幾秒刷新Mysql
    private Long startTime = System.currentTimeMillis(); // 上回執行thread time
    private Long spentTime;
    // ======== GPS ==========
    long minTime = 2000; //msec
    float minDist = 5.0f;   //公尺
    // ========= map html ============
    private LocationManager locmar;
    //
    private String Selname = "我的位置";
    private String Seladdress = "24.172127,120.610313";
    int index = 0;
    int count = 1;
    private TextView tsql_count;
    /********************************************
     * Myname Myaddress Mygroup
     *  可由login 取得
     *  使用下列方法:
     *   SelectMysql(Myname) ;
     *   InsertMysql(Myname,Mygroup,Myaddress);
     *********************************************/
    private String Myid = "0";
    private String Myname = "1號林繨菖";
    private String Myaddress = "24.172127,120.610313";
    private String Mygroup = "B"; //群組
    //--選擇spinner---
    private int Spinnersel = 0;
    private int MyspinnerNo = 0;
    // =============================================
    //-----------------所需要申請的權限數組---------------
    private static final String[] permissionsArray = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};
    private List<String> permissionsList = new ArrayList<String>();
    //申請權限後的返回碼
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;
    //-----------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m1706);
        checkRequiredPermission(this);     //  檢查SDK版本, 確認是否獲得權限.
        // ---------------------------
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites()
                .detectNetwork().penaltyLog().build());
        StrictMode.setVmPolicy(
                new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath().build());
        // --------------------------------
        setupViewComponent();
    }

    private void checkRequiredPermission(Activity activity) {
        for (String permission : permissionsArray) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
            }
        }
        if (permissionsList.size() != 0) {
            ActivityCompat.requestPermissions(activity, permissionsList.toArray(new
                    String[permissionsList.size()]), REQUEST_CODE_ASK_PERMISSIONS);
        }
    }
    private void setupViewComponent() {
        webView = (WebView) findViewById(R.id.webview);
        mSpnLocation = (Spinner) findViewById(R.id.spnLocation);
        mSpnLocation.getBackground().setAlpha(150);//0-255
        txtOutput = (TextView) findViewById(R.id.txtOutput);
        tsql_count = (TextView) findViewById(R.id.sql_count);
        bNav = (Button) findViewById(R.id.Navigation);
        //------------檢查使用者是否存在--------------
        SelectMysql(Myname);
        //------------------------------------------
        // 設定Delay的時間
        mHandler.postDelayed(updateTimer, timer * 1000);
        // -------------------------
        Showspinner(); // 刷新spinner
        // -------------------------
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(M1706.this, "AndroidFunction");
        webView.loadUrl(MAP_URL);
        //--導航監聽--
        bNav.setOnClickListener(NavOnClick);
    }
    private void Showspinner() {
        /***************************************
         * 讀取 SQLite => Spinner
         *****************************************/
        mContRes = getContentResolver();
        Cursor cur_Spinner = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur_Spinner.moveToFirst(); // 一定要寫，不然會出錯
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        for (int i = 0; i < cur_Spinner.getCount(); i++) {
            cur_Spinner.moveToPosition(i);
            // adapter.add("id:"+c.getString(0) + " 名:" + c.getString(1)+" 組"+
            // c.getString(2) + " 位:" + c.getString(3));
            adapter.add(cur_Spinner.getString(1) + " " + cur_Spinner.getString(3));
        }
        cur_Spinner.close();
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpnLocation.setAdapter(adapter);

        //---------------------------------------
        if (Spinnersel > 0) {
            mSpnLocation.setSelection(Spinnersel, true); //spinner 框架設定
        } else {
            mSpnLocation.setSelection(MyspinnerNo, true); //spinner 框架設定
        }
        mSpnLocation.setOnItemSelectedListener(mSpnLocationOnItemSelLis);
    }

    //	--導航監聽--
    private Button.OnClickListener NavOnClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Navon == "off") {
                bNav.setTextColor(getResources().getColor(R.color.blue));
                Navon = "on";
                bNav.setText("關閉路徑規劃");
                setMapLocation2();
                //--------------------------------
//                oldseleposition=mSpnLocation.getSelectedItemPosition();
                //--------------------------------
            } else {
                bNav.setTextColor(getResources().getColor(R.color.red));
                Navon = "off";
                bNav.setText("開啟路徑規劃");
                setMapLocation2();
                mSpnLocation.setSelection(MyspinnerNo, true); //spinner 框架設定跳回路徑規劃前
            }
        }
    };

    private AdapterView.OnItemSelectedListener mSpnLocationOnItemSelLis = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView parent, View v, int position, long id) {
            //------------------------------------------
            iSelect = mSpnLocation.getSelectedItemPosition(); // 找到按何項
            Spinnersel = position; // 設定 spinner 框頭為選擇項目
            // ----------------------
            setMapLocation();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    };

    private void setMapLocation() {//非規畫路徑使用
        iSelect = mSpnLocation.getSelectedItemPosition();
        Cursor cur_setmap = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur_setmap.moveToPosition(iSelect);
        /**************************************
         * id: cur_setmap.getString(0) name: cur_setmap.getString(1) grp:
         * cur_setmap.getString(2) address:cur_setmap.getString(3)
         **************************************/
        Selname = cur_setmap.getString(1);// 地名
        Seladdress = cur_setmap.getString(3);// 緯經
        cur_setmap.close();
        String[] sLocation = Myaddress.split(",");
        Lat = sLocation[0]; // 南北緯
        Lon = sLocation[1]; // 東西經
        jcontent = Myname; // 地名

        // --------判斷是否規劃路徑----------------------------
        if (Navon == "on" && iSelect != 0) {
            Navstart = Myaddress; //現在位置
            Navend = Seladdress; //目標位置
        } else {
            sLocation = Seladdress.split(",");
            Lat = sLocation[0];//南北緯
            Lon = sLocation[1];//東西經
            jcontent = Selname;//地名
            final String centerURL_b = "javascript:centerAt_b(" + Lat + "," + Lon + ")";
            webView.loadUrl(centerURL_b);
        }
        // ---------更新地圖資料------------------
        final String centerURL = "Javascript:centerAt(" + Lat + "," + Lon + ")";// 帶入參數到javascript的centerAt
        webView.loadUrl(centerURL);
    }

    private void setMapLocation2() {//規畫路徑使用
        int iSelect = mSpnLocation.getSelectedItemPosition();
        index = iSelect;
        // --------判斷是否規劃路徑----------------------------
        if (Navon == "on" && iSelect != 0) {
            Navstart = Myaddress;
            Navend = Seladdress;
        }
        // ----------------------------------------------------
        Cursor cur_setmap2 = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur_setmap2.moveToPosition(iSelect);
        //----------------------
        Selname = cur_setmap2.getString(1);// 地名
        Seladdress = cur_setmap2.getString(3);// 緯經
        cur_setmap2.close();
// -----------------------------------------------------
        String[] sLocation = Seladdress.split(",");
        Lat = sLocation[0]; // 南北緯
        Lon = sLocation[1]; // 東西經
        jcontent = Selname; // 地名
// ---------更新地圖資料------------------
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(M1706.this, "AndroidFunction");
        webView.loadUrl(MAP_URL);
    }

    // -------------------------------
    /* 開啟時先檢查是否有啟動GPS精緻定位 */
    @Override
    protected void onStart() {
        super.onStart();

        if (initLocationProvider()) {
            nowaddress();
        } else {
            txtOutput.setText("GPS未開啟,請先開啟定位！");
        }
    }

    @Override
    protected void onStop() {
        locationMgr.removeUpdates(this);
        if (locationMgr != null) {
            locationMgr.removeUpdates(this);
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /************************************************
     * GPS部份
     ***********************************************/
    /* 檢查GPS 設定GPS服務 */
    private boolean initLocationProvider() {
        locationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
            return true;
        }

        return false;
    }

    /* 建立位置改變偵聽器 預先顯示上次的已知位置 */
    private void nowaddress() {
        // 取得上次已知的位置
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplication(), "No Permission", Toast.LENGTH_LONG).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplication(), "No Permission", Toast.LENGTH_LONG).show();
            return;
        }
        Location location = locationMgr.getLastKnownLocation(provider);
        updateWithNewLocation(location);

        // 監聽 GPS Listener
        locationMgr.addGpsStatusListener(gpsListener);

        // Location Listener
        long minTime = 5000;// ms
        float minDist = 5.0f;// meter
        locationMgr.requestLocationUpdates(provider, minTime, minDist,
                this);
    }

    GpsStatus.Listener gpsListener = new GpsStatus.Listener() {
        /* 監聽GPS 狀態 */
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.d(TAG, "GPS_EVENT_STARTED");
                    break;

                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.d(TAG, "GPS_EVENT_STOPPED");
                    break;

                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.d(TAG, "GPS_EVENT_FIRST_FIX");
                    break;

                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    Log.d(TAG, "GPS_EVENT_SATELLITE_STATUS");
                    break;
            }
        }
    };

    private void updateWithNewLocation(Location location) {
        String where = "";
        if (location != null) {
            double lng = location.getLongitude();// 經度
            double lat = location.getLatitude();// 緯度
            float speed = location.getSpeed();// 速度
            long time = location.getTime();// 時間
            String timeString = getTimeString(time);
            where = "經度: " + lng + "\n緯度: " + lat + "\n速度: " + speed + "\n時間: "
                    + timeString + "\nProvider: " + provider;
            // 標記"我的位置"
            Myaddress = lat + "," + lng;
            //--------------  變更 mysql 會員的座標	--------------------------
            if (Integer.valueOf(Myid) != 0) {
                UpdateMysql(Myid, Myname, Mygroup, Myaddress);
            }
            //-------------------------------------------------------------------------
        } else {
            where = "*位置訊號消失*";
        }

        // 位置改變顯示
        txtOutput.setText(where);
    }

    @SuppressLint("NewApi")
    private String getTimeString(long timeInMilliseconds) {
        SimpleDateFormat format = null;

            format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return format.format(timeInMilliseconds);
    }

    /* 位置變更狀態監視 */
    @Override
    public void onLocationChanged(Location location) {
        // ----------定位改變時----------------
        updateWithNewLocation(location);
        //地圖以給予的座標為中心，即移動地圖至給定位的座標
        Navstart = location.getLatitude() + "," + location.getLongitude();
        //----設定end
        //Navend = 從MySQL找出選擇對象,目前位置;
        Navend = Seladdress;
        String[] sLocation = Seladdress.split(",");
        Lat = sLocation[0];//南北緯
        Lon = sLocation[1];//東西經
        final String centerURL_b = "javascript:centerAt_b(" + Lat + "," + Lon + ")";
        webView.loadUrl(centerURL_b);
        // 將畫面移至位置
        final String centerURL = "javascript:centerAt(" + location.getLatitude() + "," + location.getLongitude() + ")";
        webView.loadUrl(centerURL);
        //--------------
        final String deletedeleteOverlays = "javascript:deleteOverlays()";
        webView.loadUrl(deletedeleteOverlays);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                Log.v(TAG, "Status Changed: Out of Service");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.v(TAG, "Status Changed: Temporarily Unavailable");
                break;
            case LocationProvider.AVAILABLE:
                Log.v(TAG, "Status Changed: Available");
                break;
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "onProviderEnabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
        updateWithNewLocation(null);
        Log.d(TAG, "onProviderDisabled");
    }

    /************************************************
     * Thread Hander 固定要執行的方法
     ***********************************************/
    private final Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            spentTime = System.currentTimeMillis() - startTime;
            Long second = (spentTime / 1000);// 將運行時間後，轉換成秒數
            if (second >= timerang) {
                startTime = System.currentTimeMillis();
                dbmysql(); // 匯入database
                Showspinner(); // 刷新spinner
                //-------------------------------
                tsql_count.setText(getString(R.string.t_sql_count) + count);
                count++;
                //-------------------------------
                Showspinner(); // 刷新spinner
            }
            mHandler.postDelayed(this, timer * 1000);// time轉換成毫秒 updateTime
        }
    };
    private void dbmysql() {
        mContRes = getContentResolver();
        // --------------------------- 先刪除 SQLite 資料------------
        Uri uri = FriendsContentProvider.CONTENT_URI;
        mContRes.delete(uri, null, null); // 刪除所有資料
        Cursor cur_dbmysql = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur_dbmysql.moveToFirst(); // 一定要寫，不然會出錯
        // ------
        try {
            String result = DBConnector.executeQuery("SELECT * FROM member ORDER BY id");
            /** SQL 結果有多筆資料時使用JSONArray 只有一筆資料時直接建立JSONObject物件 JSONObject
             * jsonData = new JSONObject(result); */
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);
                //
                ContentValues newRow = new ContentValues();
                newRow.put("id", jsonData.getString("id").toString());
                newRow.put("name", jsonData.getString("name").toString());
                newRow.put("grp", jsonData.getString("grp").toString());
                newRow.put("address", jsonData.getString("address").toString());
                // ---------
                String chk_name = jsonData.getString("id").toString();
                if (jsonData.getString("id").toString().trim().equals(Myid))
                    MyspinnerNo = i;   // 儲存 會員在spinner 的位置
                // ---------

                // ---------
                mContRes.insert(FriendsContentProvider.CONTENT_URI, newRow);

            }
        } catch (Exception e) {
            // Log.e("log_tag", e.toString());
        }
        cur_dbmysql.close();
    }

    private void SelectMysql(String myname) { //檢查使否會員是否存在
        // -------------------------------
        try {
            String selectMYSQL = "SELECT name FROM member WHERE name = '" + myname + "' ORDER BY id";
            String result = DBConnector.executeQuery(selectMYSQL);

            if (result.length() <= 12) {

                Log.d(TAG, "SelectMysql=不存在 新增");
                /********************************
                 * 執行InsertMysql()新增個人資料
                 * 也可以直接呼叫 DBConnector.executeInsert(a,b,c);
                 *******************************/
                InsertMysql(myname, Mygroup, Myaddress);
            } else {
                Log.d(TAG, "SelectMysql=已存在");
            }
            //
            selectMYSQL = "SELECT * FROM member WHERE name = '" + myname + "' ORDER BY id";
            result = DBConnector.executeQuery(selectMYSQL);
            JSONArray jsonArray = new JSONArray(result);
            JSONObject jsonData = jsonArray.getJSONObject(0);
            Log.d(TAG, "SelectMysql=已存在 object=" + jsonData);
            Myid = jsonData.getString("id").toString();
            Myname = jsonData.getString("name").toString();
            Mygroup = jsonData.getString("grp").toString();
            Myaddress = jsonData.getString("address").toString();

        } catch (Exception e) {
            // Log.e("log_tag", e.toString());
        }
    }


    private void InsertMysql(String insmyname, String insmygroup, String insmyaddress) {
        /********************************
         * 使用 DBConnector 的新增函數
         *******************************/
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("name", insmyname));
        nameValuePairs.add(new BasicNameValuePair("grp", insmygroup));
        nameValuePairs.add(new BasicNameValuePair("address", insmyaddress));
        String result = DBConnector.executeInsert("", nameValuePairs);
    }

    private void UpdateMysql(String upmyid, String upmyname, String upmygroup, String upmyaddress) {
        /********************************
         * 使用 DBConnector 的mysql_update函數
         *******************************/
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("id", upmyid));
        nameValuePairs.add(new BasicNameValuePair("name", upmyname));
        nameValuePairs.add(new BasicNameValuePair("grp", upmygroup));
        nameValuePairs.add(new BasicNameValuePair("address", upmyaddress));
        DBConnector.executeUpdate("", nameValuePairs);
        // ------------------------------

    }

    // ===========================================================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getApplicationContext(), permissions[i] + "權限申請成功!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "權限被拒絕： " + permissions[i], Toast.LENGTH_LONG).show();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // -----------------------------
    @JavascriptInterface
    public String GetLat() {
        return Lat;
    }

    @JavascriptInterface
    public String GetLon() {
        return Lon;
    }

    @JavascriptInterface
    public String Getjcontent() {
        return jcontent;
    }

    //-----傳送導航資訊-------------------------------
    @JavascriptInterface
    public String Navon() {
        return Navon;
    }

    @JavascriptInterface
    public String Getstart() {
        return Navstart;
    }

    @JavascriptInterface
    public String Getend() {
        return Navend;
    }
    //------------------------------------------------
    @JavascriptInterface
    public String GetJsonArry(){
        return ArryToJson();
    }

    //------------------------------------------------------------
    private String ArryToJson() {
        dbmysql();
        Cursor cur = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur.moveToFirst(); // 一定要寫，不然會出錯
        JSONArray jArry = new JSONArray();
        for(int i=0;i<cur.getCount();i++){
            cur.moveToPosition(i);
            JSONObject jObj = new JSONObject();//一定要放在這裡
            String[] arr = cur.getString(3).split(",");
            try {
                jObj.put("title", cur.getString(1));
                jObj.put("jlat", arr[0]);
                jObj.put("jlon", arr[1]);

                jArry.put(jObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String str=jArry.toString();
        cur.close();
        return str;
    }

    //-----------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent it = new Intent();
        switch (item.getItemId()) {
            case R.id.action_settings:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


}

// =============================================================


