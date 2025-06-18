package com.example.my12qstring;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;


import java.util.HashMap;

public class MainActivity extends AppCompatActivity {


    private Button create_btn; //用于创建二维码的按钮
    private Button show_btn; //用于显示二维码内容的按钮
    private ImageView pic;  //展示二维码
    private TextView show_txt; //显示二维码的内容
    private HashMap hashMap; //用hasmap放置二维码的参数
    private Bitmap bitmap;//声明一个bitmap对象用于放置图片;

    int SELECT_PICTURE = 2000;
    int TAKE_A_PICTURE = 2001;
    Context con;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        con = getApplicationContext();
        initView();//初始化控件(这里用的是插件LayoutCreator)

    }



    private void initView() {
        hashMap = new HashMap();
        create_btn = (Button) findViewById(R.id.create_btn);
        show_btn = (Button) findViewById(R.id.show_btn);
        pic = (ImageView) findViewById(R.id.pic);
        show_txt = (TextView) findViewById(R.id.show_txt);
        create_btn.setOnClickListener(this::onClick);
        show_btn.setOnClickListener(this::onClick);
    }

    //@Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.create_btn)
                create_QR_code(); //此方法用于创建二维码
        else if (vid == R.id.show_btn)
                show_QR_code();//此方法用于显示二维码的内容
        else
            ;
    }

    private void get_perm(View view)
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);
        }
        return;
    }

    public void try_expit(View v) {
        String ifp= show_txt.getText().toString();

        // 还没有成功，需要继续改进。。。
        if (ifp.startsWith("WIFI:"))  //  if a wifi in qrcode/string,
        // string like: "WIFI:T:WPA;P:12345678;S:fake_netap;"
        {   // https://blog.csdn.net/weixin_29860267/article/details/117500558
            // http://tools.jb51.net/code/java_format
            get_perm(v);

            WifiConfiguration config = new WifiConfiguration();
            config.allowedAuthAlgorithms.clear();
            config.allowedGroupCiphers.clear();
            config.allowedKeyManagement.clear();
            config.allowedPairwiseCiphers.clear();
            config.allowedProtocols.clear();
            config.SSID = "\"" + "fake_wifi" + "\""; // 需要一个真的wifi和口令
            //config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.preSharedKey = "\"" + "12345678" + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.status = WifiConfiguration.Status.ENABLED;

            WifiManager mwf = (WifiManager) con.getSystemService(Context.WIFI_SERVICE);
            if (!mwf.isWifiEnabled())
                mwf.setWifiEnabled(true);  // maybe need extra steps

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Toast.makeText(getApplicationContext(), "andr10+ is need at curr", Toast.LENGTH_LONG).show();
                return;
            }

            NetworkSpecifier specifier =
                    new WifiNetworkSpecifier.Builder()
                            .setSsidPattern(new PatternMatcher("CU_beishufang", PatternMatcher.PATTERN_PREFIX))
                            .setWpa2Passphrase("14191419")
                            .build();
            NetworkRequest request1 =
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .setNetworkSpecifier(specifier)
                            .build();
            ConnectivityManager.NetworkCallback ncb = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Toast.makeText(getApplicationContext(), "new ap ok", Toast.LENGTH_LONG).show();
                }
                @Override
                public void onUnavailable() {
                    Toast.makeText(getApplicationContext(), "new ap bad", Toast.LENGTH_LONG).show();
                }
            };
            ConnectivityManager cm1 = (ConnectivityManager)con.getSystemService(Context.CONNECTIVITY_SERVICE);
            cm1.requestNetwork(request1, ncb);
            //
        }
        else
            Toast.makeText(MainActivity.this, "cannot recognize", Toast.LENGTH_LONG).show();
    };


    View.OnClickListener take_from_cam = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent pi = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (pi.resolveActivity(getPackageManager()) != null)
                startActivityForResult(pi, TAKE_A_PICTURE);
        }
    };

    // https://www.geeksforgeeks.org/how-to-select-an-image-from-gallery-in-android/
    public void select_pic(View v) {
        get_perm(v);
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "select a pic"), SELECT_PICTURE);

        // test add a button in fly
        Button bt2 = new Button(this);
        bt2.setText("take a pic from camra");
        bt2.setOnClickListener(take_from_cam);
        LinearLayout lo1 = (LinearLayout)findViewById(R.id.lo1);
        lo1.addView(bt2);
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    pic.setImageURI(selectedImageUri);
                }
            } else if (requestCode == TAKE_A_PICTURE) {
                Bundle extras = data.getExtras();
                Bitmap bitmap = (Bitmap) extras.get("data");
                pic.setImageBitmap(bitmap);
            }
        }
    };

    private void create_QR_code() {
        hashMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        //定义二维码的纠错级别，为L
        hashMap.put(EncodeHintType.CHARACTER_SET, "utf-8");
        //设置字符编码为utf-8
        hashMap.put(EncodeHintType.MARGIN, 2);
        //设置margin属性为2,也可以不设置
        //String contents = "最简单的Demo"; //定义二维码的内容
        String contents = show_txt.getText().toString();
        BitMatrix bitMatrix = null;   //这个类是用来描述二维码的,可以看做是个布尔类型的数组
        try {
            bitMatrix = new MultiFormatWriter().encode(contents, BarcodeFormat.QR_CODE, 250, 250, hashMap);
            //调用encode()方法,第一次参数是二维码的内容，第二个参数是生二维码的类型，第三个参数是width，第四个参数是height，最后一个参数是hints属性
        } catch (WriterException e) {
            e.printStackTrace();
        }

        int width = bitMatrix.getWidth();//获取width
        int height = bitMatrix.getHeight();//获取height
        int[] pixels = new int[width * height]; //创建一个新的数组,大小是width*height
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //通过两层循环,为二维码设置颜色
                if (bitMatrix.get(i, j)) {
                    pixels[i * width + j] = Color.BLACK;  //设置为黑色
                } else {
                    pixels[i * width + j] = Color.WHITE; //设置为白色
                }
            }
        }

        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        //调用Bitmap的createBitmap()，第一个参数是width,第二个参数是height,最后一个是config配置，可以设置成RGB_565
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        //调用setPixels(),第一个参数就是上面的那个数组，偏移为0，x,y也都可为0，根据实际需求来,最后是width ,和height
        pic.setImageBitmap(bitmap);
        //调用setImageBitmap()方法，将二维码设置到imageview控件里

    }

    private void show_QR_code() {
        hashMap.put(DecodeHintType.CHARACTER_SET, "utf-8");//设置解码的字符，为utf-8
        bitmap=((BitmapDrawable)(pic.getDrawable())).getBitmap();
        int width = bitmap.getWidth();//现在是从那个bitmap中得到width和height
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];//新建数组，大小为width*height
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height); //和什么的setPixels()方法对应
        Result result = null;//Result类主要是用于保存展示二维码的内容的
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels)));
        //BinaryBitmap这个类是用于反转二维码的，HybridBinarizer这个类是zxing在对图像进行二值化算法的一个类
        try {
            result = new MultiFormatReader().decode(binaryBitmap);//调用MultiFormatReader()方法的decode()，传入参数就是上面用的反转二维码的
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        if (result==null)
            show_txt.setText("err: none string found.");
        else
            show_txt.setText(result.toString());//设置文字

    }


}