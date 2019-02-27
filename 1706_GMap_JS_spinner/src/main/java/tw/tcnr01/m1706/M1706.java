package tw.tcnr01.m1706;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class M1706 extends AppCompatActivity {
    private static String[][] locations = {
            { "中區職訓", "24.172127,120.610313" },
            { "東海大學路思義教堂", "24.179051,120.600610" },
            { "台中公園湖心亭", "24.144671,120.683981" },
            { "秋紅谷", "24.1674900,120.6398902" },
            { "台中火車站", "24.136829,120.685011" },
            { "國立科學博物館", "24.1579361,120.6659828" } };
    private static final String MAP_URL = "file:///android_asset/GoogleMap.html";
    private Spinner mSpnLocation;
    private WebView webView;
    private String Lat,Lon;
    private String jcontent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m1706);
        setupViewComponent();
    }

    private void setupViewComponent() {
        webView = (WebView) findViewById(R.id.webview);
        mSpnLocation = (Spinner) this.findViewById(R.id.spnLocation);
        // ----Location-----------
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//				android.R.layout.simple_spinner_item);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.spinner_style);
        for (int i = 0; i < locations.length; i++)
            adapter.add(locations[i][0]);

//		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.spinner_style);
        mSpnLocation.setAdapter(adapter);
        mSpnLocation.setOnItemSelectedListener(mSpnLocationOnItemSelLis);
    }
    private AdapterView.OnItemSelectedListener mSpnLocationOnItemSelLis = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView parent, View v, int position,
                                   long id) {

            setMapLocation();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {

        }
    };

    private void setMapLocation() {
        int iSelect = mSpnLocation.getSelectedItemPosition();

        String[] sLocation = locations[iSelect][1].split(",");

        Lat = sLocation[0]; // 南北緯
        Lon = sLocation[1]; // 東西經

        jcontent = locations[iSelect][0];  //地名

        webView.getSettings().setJavaScriptEnabled(true);                         //
        webView.addJavascriptInterface(M1706.this, "AndroidFunction");//

        webView.loadUrl(MAP_URL);

    }
//-----------------------------------------------做interface,用來將class裡的值傳遞到html
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


}
