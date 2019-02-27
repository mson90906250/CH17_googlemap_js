package tw.tcnr01.m1705;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class M1705 extends AppCompatActivity {
    private static final String MAP_URL = "file:///android_asset/youtube.html";// 自建的html檔名
    private WebView webView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m1705);
        setupViewComponent();
    }

    private void setupViewComponent() {
        webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(MAP_URL);
    }

}
