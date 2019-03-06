package tw.tcnr01.m1710;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import tw.tcnr01.m1710.providers.FriendsContentProvider;


public class M1710 extends AppCompatActivity implements LocationListener {

    private Spinner mSpnLocation;
    private static final String MAP_URL = "file:///android_asset/GoogleMap3.html";// 自建的HTML檔名
    private WebView webView;
    //----------------------------------------
    private String Lat;//緯度
    private String Lon;//經度
    private String jcontent;//地名變數
    //-------GPS-----------------------------
    private LocationManager locationMgr;
    private String provider;
    private TextView txtOutput;
    private TextView t_count;
    int iSelect;
    int count = 1;
    private final String TAG = "tcnr01=>";
    //-------導航--------------------------
    private Button bNav;
    String[] sLocation;
    String Navon = "off";
    String Navstart = "0,0"; // 起始點
    String Navend = "0,24"; // 結束點

    // ========= Thread Hander =============
    private Handler mHandler = new Handler();
    private long timer = 10; // thread每幾秒run
    private long timerang = 5; // 設定幾秒刷新Mysql
    private Long startTime = System.currentTimeMillis(); // 上回執行thread time
    private Long spentTime;
    // ========= SQL Database =========
    private static ContentResolver mContRes;
    private String[] MYCOLUMN = new String[]{"id", "name", "grp", "address"};
    // ======== GPS ==========
    long minTime = 5000; //msec
    float minDist = 5.0f; //公尺
    // ========= map html ============
    private LocationManager locmar;
    //----------------------------------------
    private String Selname = "初始位置";
    private String Seladdress = "24.172127,120.610313";
    int index = 0;
    //----------------------------------------
    private String Myid = "0";
    private String Myname = "01號林繨菖";
    private String Myaddress = "24.172127,120.610313";
    private String Mygroup = "B"; //群組
    //----------------------------------------
    int MyspinnerNo = 0;
    int Spinnersel = 0;
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
        setContentView(R.layout.m1710);
        // ---------------------------
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites()
                .detectNetwork().penaltyLog().build());
        StrictMode.setVmPolicy(
                new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath().build());
        // --------------------------------
        checkRequiredPermission(this);     //  檢查SDK版本, 確認是否獲得權限.
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
        t_count = (TextView) findViewById(R.id.sql_count);
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
        webView.addJavascriptInterface(M1710.this, "AndroidFunction");
        webView.loadUrl(MAP_URL);
        //--導航監聽--
        bNav.setOnClickListener(NavOnClick );
    }

    private void Showspinner() {
        /***************************************
         * 讀取SQLite => Spinner
         *****************************************/
        mContRes = getContentResolver();
        Cursor cur_Spinner = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur_Spinner.moveToFirst(); // 一定要寫，不然會出錯

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        for (int i = 0; i < cur_Spinner.getCount(); i++) {
            cur_Spinner.moveToPosition(i);
            adapter.add(cur_Spinner.getString(1) + "" + cur_Spinner.getString(3));
        }
        cur_Spinner.close();
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpnLocation.setAdapter(adapter);
        if (Spinnersel > 0) {
            mSpnLocation.setSelection(Spinnersel, true); //spinner 框架設定
        } else {
            mSpnLocation.setSelection(MyspinnerNo, true); //spinner 框架設定
        }
        mSpnLocation.setOnItemSelectedListener(mSpnLocationOnItemSelLis);
    }

    //--導航監聽--
    private Button.OnClickListener NavOnClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Navon == "off") {
                bNav.setTextColor(getResources().getColor(R.color.Blue));
                Navon = "on";
                bNav.setText("關閉路徑規劃");
                setMapLocation2();
                //--------------------------------
//                oldseleposition=mSpnLocation.getSelectedItemPosition();
                //--------------------------------
            } else {
                bNav.setTextColor(getResources().getColor(R.color.Red));
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
            int iSelect = mSpnLocation.getSelectedItemPosition(); // 找到按何項
            Spinnersel = position; // 設定spinner 框頭為選擇項目
            //------------------
            setMapLocation();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    };

    private void setMapLocation() {//非規畫路徑使用
        int iSelect = mSpnLocation.getSelectedItemPosition();
        index = iSelect;
        Cursor cur_setmap = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur_setmap.moveToPosition(iSelect);

        /**************************************
         * id: cur_setmap.getString(0) name: cur_setmap.getString(1) grp:
         * cur_setmap.getString(2) address:cur_setmap.getString(3)
         **************************************/
        Selname = cur_setmap.getString(1);// 地名
        Seladdress = cur_setmap.getString(3);// 緯經
        cur_setmap.close();

        // --------判斷是否規劃路徑----------------------------
        if (Navon == "on" && iSelect != 0) {
            Navstart = Myaddress; //現在位置
            Navend = Seladdress; //目標位置
            String[] sLocation = Myaddress.split(",");
            Lat = sLocation[0];//南北緯
            Lon = sLocation[1];//東西經
            jcontent = Myname; // Title
        } else {
            String[] sLocation = Seladdress.split(",");
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
        Log.d(TAG, "setMapLocation2() =>" + iSelect);
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
        webView.addJavascriptInterface(M1710.this, "AndroidFunction");
        webView.loadUrl(MAP_URL);
    }

    /* 開啟時先檢查是否有啟動GPS精緻定位 */
    @Override
    protected void onStart() {
        super.onStart();
        if (initLocationProvider()) {
            nowaddress();
        } else {
            txtOutput.setText("GPS未開啟,請先開啟定位!");
        }
    }

    //    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mHandler.removeCallbacks(updateTimer); // 設定定時要執行的方法
//    }
    @Override
    protected void onStop() {
        mHandler.removeCallbacks(updateTimer); // 設定定時要執行的方法
        if (locationMgr !=null){
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
    /* 檢查GPS 是否開啟*/
    private boolean initLocationProvider() {
        locmar = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locmar.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
            return true;
        }
        return false;
    }
    /* 建立位置改變偵聽器 預先顯示上次的已知位置 */

    private void nowaddress() {
        // 取得上次已知的位置
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locmar.getLastKnownLocation(provider);
        updateWithNewLocation(location);

        // 監聽GPS Listener
        locmar.addGpsStatusListener(gpsListener);
        locmar.requestLocationUpdates(provider, minTime, minDist, this);
    }

    //-------------------------------------------------------------
    GpsStatus.Listener gpsListener = new GpsStatus.Listener() {
        /* 監聽GPS 狀態 */
        @Override
        public void onGpsStatusChanged(int event) {
        }
    };

    /**
     * 更新並顯示新位置@param location
     */
    private void updateWithNewLocation(Location location) {
        Log.d(TAG, "updateWithNewLocation(Location location)");
        String where = "";
        if (location != null) {
            double lng = location.getLongitude();//經度
            double lat = location.getLatitude();//緯度
            float speed = location.getSpeed();// 速度
            long time = location.getTime();// 時間
            String timeString = getTimeString(time);
            where = "經度: " + lng + "\n緯度: " + lat + "\n速度: " + speed + "\n時間: "
                    + timeString + "\nProvider: " + provider;
            // 標記"我的位置"
            //locations[0][1] = lat + "," + lng; // 用GPS找到的位置更換陣列的目前位置
            Myaddress = lat + "," + lng;
            //--------------  變更 mysql 會員的座標	--------------------------
            if(Integer.valueOf(Myid) !=0) {
                UpdateMysql(Myid, Myname, Mygroup, Myaddress);
            }
            //-------------------------------------------------------------------------
        } else {
            where = "位置訊號消失";
        }
        //位置改變顯示
        txtOutput.setText(where);
    }

    private String getTimeString(long timeInMilliseconds) {
        SimpleDateFormat format = null;

            format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return format.format(timeInMilliseconds);
    }

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
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        updateWithNewLocation(null);
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
                //-----------------------
                t_count.setText(getString(R.string.t_sql_count) + count);
                count++;
                //-----------------------
                Showspinner(); // 刷新spinner
            }
            mHandler.postDelayed(this, timer * 1000);// time轉換成毫秒updateTime
        }
    };

    private void dbmysql() {
        mContRes = getContentResolver();
        // ---------------------------先刪除SQLite 資料------------
        Uri uri = FriendsContentProvider.CONTENT_URI;
        mContRes.delete(uri, null, null); // 刪除所有資料
        Cursor cur_dbmysql = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur_dbmysql.moveToFirst(); // 一定要寫，不然會出錯
        //---------
        try {
            String result = DBConnector.executeQuery("SELECT * FROM member ORDER BY id");
            /* SQL 結果有多筆資料時使用JSONArray 只有一筆資料時直接建立JSONObject物件JSONObject
             * jsonData = new JSONObject(result);       */
            JSONArray jsonArray = new JSONArray(result);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);
                //---------
                ContentValues newRow = new ContentValues();
                newRow.put("id", jsonData.getString("id").toString());
                newRow.put("name", jsonData.getString("name").toString());
                newRow.put("grp", jsonData.getString("grp").toString());
                newRow.put("address", jsonData.getString("address").toString());
                //-------------
                String chk_name = jsonData.getString("id").toString();
                if (jsonData.getString("id").toString().trim().equals(Myid))
                    MyspinnerNo = i; // 儲存會員在spinner 的位置
                //---------------------------
                mContRes.insert(FriendsContentProvider.CONTENT_URI, newRow);
            }
        } catch (Exception e) {
            // Log.e("log_tag", e.toString());
        }
        cur_dbmysql.close();
    }

    private void SelectMysql(String myname) {//檢查使否會員是否存在
        try {
            String selectMYSQL = "SELECT name FROM member WHERE name = '" + myname + " ' ORDER BY id";
            //String selectMYSQL = "SELECT name FROM member WHERE name = '\" + myname + \" ' ORDER BY id";
            String result = DBConnector.executeQuery(selectMYSQL);
            if (result.length() <= 6) {
                Log.d(TAG, "SelectMysql=不存在新增");
                /********************************
                 * 執行InsertMysql()新增個人資料
                 * 也可以直接呼叫DBConnector.executeInsert(a,b,c);
                 *******************************/
                InsertMysql(Myname, Mygroup, Myaddress);
            }
            selectMYSQL = "SELECT * FROM member WHERE name = '" + myname + "' ORDER BY id";
            result = DBConnector.executeQuery(selectMYSQL);
            JSONArray jsonArray = new JSONArray(result);
            JSONObject jsonData = jsonArray.getJSONObject(0);
            Log.d(TAG, "SelectMysql=已存在object=" + jsonData);
            Myid = jsonData.getString("id").toString();
            Myname = jsonData.getString("name").toString();
            Mygroup = jsonData.getString("grp").toString();
            Myaddress = jsonData.getString("address").toString();

        } catch (Exception e) {
            //Log.e("log_tag", e.toString());
        }
    }

    private void InsertMysql(String insmyname, String insmygroup, String insmyaddress) {
        /********************************
         * 使用DBConnector 的新增函數
         *******************************/
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("name", insmyname));
        nameValuePairs.add(new BasicNameValuePair("grp", insmygroup));
        nameValuePairs.add(new BasicNameValuePair("address", insmyaddress));
        String result = DBConnector.executeInsert("", nameValuePairs);
    }

    private void UpdateMysql(String upmyid, String upmyname, String upmygroup, String upmyaddress) {
        /********************************
         * 使用DBConnector 的mysql_update函數
         *******************************/
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("id", upmyid));
        nameValuePairs.add(new BasicNameValuePair("name", upmyname));
        nameValuePairs.add(new BasicNameValuePair("grp", upmygroup));
        nameValuePairs.add(new BasicNameValuePair("address", upmyaddress));
        DBConnector.executeUpdate("", nameValuePairs);
    }

    /************************************************
     * JavascriptInterface 傳給webhtml 的變數値
     ***********************************************/
    @JavascriptInterface
    public String GetLat() {
        return Lat;//值傳至GoogleMap.html
    }

    @JavascriptInterface
    public String GetLon() {
        return Lon;//值傳至GoogleMap.html
    }

    @JavascriptInterface
    public String Getjcontent() {
        return jcontent;//值傳至GoogleMap.html
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.m1710, menu);
        return true;
    }

}
