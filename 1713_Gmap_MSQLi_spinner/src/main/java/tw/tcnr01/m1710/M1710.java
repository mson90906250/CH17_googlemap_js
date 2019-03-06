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

    //--選擇spinner---
    private int Spinnersel=0;
    private int MyspinnerNo=0;


    //所需要申請的權限數組
    private static final String[] permissionsArray = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE};

    private List<String> permissionsList = new ArrayList<String>();

    //申請權限後的返回碼
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

//    private static String[][] locations = {
//            { "My_Location", "0,0" },
//            { "中區職訓", "24.172127,120.610313" },
//            { "東海大學路思義教堂", "24.179051,120.600610" },
//            { "台中公園湖心亭", "24.144671,120.683981" },
//            { "秋紅谷", "24.1674900,120.6398902" },
//            { "台中火車站", "24.136829,120.685011" },
//            { "國立科學博物館", "24.1579361,120.6659828" }};

    private Spinner mSpnLocation;

    private static final String MAP_URL = "file:///android_asset/GoogleMap3.html";
//    private static final String MAP_URL = "file:///android_asset/GoogleMap2.html";
    // 自建的html檔名
    private WebView webView;
    private String Lat;
    private String Lon;
    private String jcontent;//地名變數
    private TextView txtOutput;
    /*** GPS*/
    private LocationManager locationMgr;
    private String provider; // 提供資料

    private final String TAG = "TCNR09=>";

    // ========= SQL Database =========
    private static ContentResolver mContRes;
    private String[] MYCOLUMN = new String[]{"id", "name", "grp", "address"};
    // ========= Thread Hander =============
    private Handler mHandler = new Handler();
    private long timer = 30; // thread每幾秒run
    private long timerang = 10; // 設定幾秒刷新Mysql
    private Long startTime = System.currentTimeMillis(); // 上回執行thread time
    private Long spentTime;
    // ======== GPS ==========
    private  long minTime = 5000;// ms
    private  float minDist = 5.0f;// meter

    // ========= map html ============
    private LocationManager locmar;
    private String Myname = "01號林繨菖"; // insert update 使用
    private String Myaddress = "24.172127,120.610313";
    /********************************************
     * Myname Myaddress Mygroup
     *  可由login 取得
     *  使用下列方法:
     *   SelectMysql(Myname) ;
     *   InsertMysql(Myname,Mygroup,Myaddress);
     *********************************************/
    private String Myid = "0";
    private String Mygroup = "D"; //群組

    //
    private String Selname = "我的位置";
    private String Seladdress = "24.172127,120.610313";
    int index = 0;
    int count = 1;
    private TextView tsql_count;


    /*** Navigation **/
    private Button bNav;
    String[] sLocation;
    String Navon = "off";
    String Navstart = "24.172127,120.610313"; // 起始點
    String Navend = "24.144671,120.683981"; // 結束點
    private int iSelect;

    String aton="0,0";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m1710);

        checkRequiredPermission(this);

        //-------------抓取遠端資料庫設定執行續------------------------------
        StrictMode.setThreadPolicy(new
                StrictMode.
                        ThreadPolicy.Builder().
                detectDiskReads().
                detectDiskWrites().
                detectNetwork().
                penaltyLog().
                build());
        StrictMode.setVmPolicy(
                new
                        StrictMode.
                                VmPolicy.
                                Builder().
                        detectLeakedSqlLiteObjects().
                        penaltyLog().
                        penaltyDeath().
                        build());
        //---------------------------------------------------------------------

        setupViewComponent();
    }

    private void checkRequiredPermission(final Activity activity){
        for (String permission : permissionsArray) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
            }
        }
        if(permissionsList.size()!=0){
            ActivityCompat.requestPermissions(activity,permissionsList.toArray(new
                    String[permissionsList.size()]),REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for(int i=0;i<permissions.length;i++){
                    if(grantResults[i]==PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(getApplicationContext(),permissions[i]+"權限申請成功!",Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(getApplicationContext(),"權限被拒絕："+permissions[i],Toast.LENGTH_LONG).show();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private void setupViewComponent() {
        webView = (WebView) findViewById(R.id.webview);
        mSpnLocation = (Spinner) this.findViewById(R.id.spnLocation);
        txtOutput = (TextView) findViewById(R.id.txtOutput);
        tsql_count = (TextView) findViewById(R.id.sql_count);

        //---------檢查使用者是否存在---------------------
        SelectMysql(Myname);
        // ---------------------------------------------------

        // -----------------------
        // 設定Delay的時間
        mHandler.postDelayed(updateTimer, timer * 1000);
        // -------------------------

        Showspinner(); // 刷新spinner

        //		--導航監聽--
        bNav = (Button) findViewById(R.id.Navigation);
        bNav.setOnClickListener(bNavselectOn);

//        mSpnLocation.getBackground().setAlpha(150);//0-255
//
//        webView.getSettings().setJavaScriptEnabled(true);
//        webView.addJavascriptInterface(M1710.this, "AndroidFunction");
//        webView.loadUrl(MAP_URL);


        // ----Location-----------
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//				android.R.layout.simple_spinner_item);
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_style);
//        for (int i = 0; i < locations.length; i++)
//            adapter.add(locations[i][0]);

        //		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        adapter.setDropDownViewResource(R.layout.spinner_style);
//        mSpnLocation.setAdapter(adapter);

        mSpnLocation.setOnItemSelectedListener(mSpnLocationOnItemSelLis);

    }

    private void SelectMysql(String myname) {
        //檢查使否會員是否存在
        // -------------------------------
        try {
            String selectMYSQL = "SELECT name FROM member WHERE name = '" + myname + "' ORDER BY id";
            String result = DBConnector.executeQuery(selectMYSQL);

            if (result.length() <= 17) {//回傳值大小

                Log.d(TAG, "SelectMysql=不存在 新增");
                /********************************
                 * 執行InsertMysql()新增個人資料
                 * 也可以直接呼叫 DBConnector.executeInsert(a,b,c);
                 *******************************/
                InsertMysql(myname, Mygroup, Myaddress);
            } else {
                Log.d(TAG, "SelectMysql=已存在");

            }
            //重新取值
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

    private void Showspinner() {
        /***************************************
         * 讀取 SQLite => Spinner
         *****************************************/
        mContRes = getContentResolver();//抓取SQLite
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
        //mSpnLocation.setSelection(index, true); //spinner 框架設定

        //---------------------------------------
        if(Spinnersel >0){
            mSpnLocation.setSelection(Spinnersel, true); // spinner 框架設定	點選位置
        }else{
            mSpnLocation.setSelection(MyspinnerNo, true); // spinner 框架設定 會員現在位置
        }



    }

    //	--導航監聽--
    private Button.OnClickListener bNavselectOn = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Navon == "off") {
                bNav.setTextColor(getResources().getColor(R.color.Blue));
                Navon = "on";
                bNav.setText("關閉路徑規劃");
                setMapLocation();
            } else {
                bNav.setTextColor(getResources().getColor(R.color.Red));
                Navon = "off";
                bNav.setText("開啟路徑規劃");
                setMapLocation();
            }
        }
    };


    private AdapterView.OnItemSelectedListener mSpnLocationOnItemSelLis = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView parent, View v, int position, long id) {

            setMapLocation();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {

        }
    };

    private void setMapLocation() {
//        iSelect = mSpnLocation.getSelectedItemPosition();
//        sLocation = locations[iSelect][1].split(",");
//        Lat = sLocation[0]; // 南北緯
//        Lon = sLocation[1]; // 東西經
//        jcontent = locations[iSelect][0]; // 地名
////---------增加判斷是否規畫路徑------------------
//        if (Navon == "on" && iSelect != 0) {
//            Navstart = locations[0][1];
//            //Navend = locations[iSelect][1];
//            final String deleteOverlays = "javascript: RoutePlanning()";
//            webView.loadUrl(deleteOverlays);
//        }else{
//            webView.getSettings().setJavaScriptEnabled(true);
//            webView.addJavascriptInterface(M1710.this, "AndroidFunction");
//            webView.loadUrl(MAP_URL);
//        }

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
////---------增加判斷是否規畫路徑------------------
        if (Navon == "on" && iSelect != 0) {
            Navstart = Myaddress;  //現在位置
            Navend = Seladdress;   //目標位置
            final String RoutePlanningOverlays = "javascript: RoutePlanning()";
            webView.loadUrl(RoutePlanningOverlays);
        } else {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(M1710.this, "AndroidFunction");
            webView.loadUrl(MAP_URL);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(initLocationProvider()){
            nowaddress();
        }else {
            txtOutput.setText("GPS未開啟,請先開啟定位！");
        }

    }

    private void nowaddress() {

        //檢查是否有權限-------------------------------------------
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationMgr.getLastKnownLocation(provider);
        updateWithNewLocation(location);

        // 監聽 GPS Listener
        locationMgr.addGpsStatusListener(gpsListener);

        // Location Listener
        minTime = 5000;// ms
        minDist = 5.0f;// meter
        locationMgr.requestLocationUpdates(provider, minTime, minDist,this);


    }

    // -------------------------------
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
                Showspinner(); // 刷新spinner不能用重構
            }
            mHandler.postDelayed(this, timer * 1000);// time轉換成毫秒 updateTime
        }
    };

    private void dbmysql() {
        mContRes = getContentResolver();//---取得SQLite---
        // --------------------------- 先刪除 SQLite 資料------------
        Uri uri = FriendsContentProvider.CONTENT_URI;
        mContRes.delete(uri, null, null); // 刪除所有資料
        Cursor cur_dbmysql = mContRes.query(FriendsContentProvider.CONTENT_URI, MYCOLUMN, null, null, null);
        cur_dbmysql.moveToFirst(); // 一定要寫，不然會出錯 0筆資料
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
                String chk_name=  jsonData.getString("id").toString();
                if ( jsonData.getString("id").toString().trim().equals(Myid))
                    MyspinnerNo = i;   // 儲存 會員在spinner 的位置

                // ---------
                mContRes.insert(FriendsContentProvider.CONTENT_URI, newRow); //MySQL to SQLite

            }
        } catch (Exception e) {
            // Log.e("log_tag", e.toString());
        }
        cur_dbmysql.close();


    }


    private void updateWithNewLocation(Location location) {
        //location GPS
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
//            locations[0][1] = lat + "," + lng; // 用GPS找到的位置更換 陣列的目前位置
            Myaddress = lat + "," + lng;

            //--------------  變更 mysql 會員的座標	--------------------------
            if(Integer.valueOf(Myid) !=0) {
                UpdateMysql(Myid, Myname, Mygroup, Myaddress);
            }

            //-------------------------------------------------------------------------

        } else {
            where = "*位置訊號消失*";
        }
        // 位置改變顯示
        txtOutput.setText(where);

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

    private String getTimeString(long timeInMilliseconds) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(timeInMilliseconds);
    }

    private boolean initLocationProvider() {
        locationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
            return true;
        }
        return false;

    }

    @Override
    protected void onStop() {
        super.onStop();
        locationMgr.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    //-----------------------------

    @JavascriptInterface
    public String GetLat(){
        return Lat;
    }

    @JavascriptInterface
    public String GetLon(){
        return Lon;
    }

    @JavascriptInterface
    public String Getjcontent(){
        return jcontent;
    }

//    @JavascriptInterface
//    public String GetJsonArry(){
//        return ArryToJson();
//    }

    //-----傳送導航資訊-------------------------------
    @JavascriptInterface
    public String Navon() {
        return Navon;
    }

    @JavascriptInterface
    public String Getstart() {
//        return aton;
        return Navstart;
    }

    @JavascriptInterface
    public String Getend() {
        return Navend;
    }





//    private String ArryToJson() {
//        JSONArray jArry = new JSONArray();
//
//        for (int i = 0; i < locations.length; i++) {
//            JSONObject jObj = new JSONObject();// 一定要放在這裡
//            String[] arr = locations[i][1].split(",");
//
//            try {
//                jObj.put("title", locations[i][0]);
//                jObj.put("jlat", arr[0]);
//                jObj.put("jlon", arr[1]);
//
//                jArry.put(jObj);
//
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
//        String string_jArry=jArry.toString();
//        return string_jArry;
//
//    }

    @Override
    public void onLocationChanged(Location location) {
        //定位改變時
        updateWithNewLocation(location);
        //將畫面移至定位點的位置
//        aton= location.getLatitude() + "," + location.getLongitude();
        Navstart= location.getLatitude() + "," + location.getLongitude();
        final String centerURL = "javascript:centerAt(" + location.getLatitude() + "," + location.getLongitude() + ")";

        final String deleteOverlays = "javascript:deleteOverlays()";

        webView.loadUrl(deleteOverlays);
        webView.loadUrl(centerURL);



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

//-------------------------------

}
